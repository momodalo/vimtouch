package net.momodalo.app.vimtouch;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.TermSession;
import jackpal.androidterm.emulatorview.UpdateCallback;

import net.momodalo.app.vimtouch.compat.FileCompat;

/**
 * A terminal session, consisting of a TerminalEmulator, a TranscriptScreen,
 * the PID of the process attached to the session, and the I/O streams used to
 * talk to the process.
 */
public class VimTermSession extends TermSession {
    private VimSettings mSettings;

    private FileDescriptor mTermFd;
    private Thread mWatcherThread;

    // A cookie which uniquely identifies this session.
    private String mHandle;

    private String mInitialCommand;

    private String mApp;
    private String mUrl;

    public static final int PROCESS_EXIT_FINISHES_SESSION = 0;
    public static final int PROCESS_EXIT_DISPLAYS_MESSAGE = 1;
    private int mProcessExitBehavior = PROCESS_EXIT_FINISHES_SESSION;

    private String mProcessExitMessage;

    private static final int PROCESS_EXITED = 1;

    static class ProcessHandler extends Handler {
        private final WeakReference<VimTermSession> mSession;

        ProcessHandler(VimTermSession session) {
            mSession = new WeakReference<VimTermSession>(session);
        }

        @Override
        public void handleMessage(Message msg) {
            VimTermSession session = mSession.get();

            if (session == null)
                return;

            if (!session.isRunning())
                return;

            if (msg.what == PROCESS_EXITED)
                session.onProcessExit((Integer) msg.obj);
        }
    }
    private ProcessHandler mMsgHandler = new ProcessHandler(this);

    private UpdateCallback mUTF8ModeNotify = new UpdateCallback() {
        public void onUpdate() {
            Exec.setPtyUTF8Mode(mTermFd, getUTF8Mode());
        }
    };

    public VimTermSession(String app, String url, VimSettings settings,
                          String initialCommand) {
        super();

        mApp = app;
        mUrl = url;

        updatePrefs(settings);

        initializeSession();
        mInitialCommand = initialCommand;

        mWatcherThread = new Thread() {
             @Override
             public void run() {
                Log.i(VimTouch.LOG_TAG, "waiting for exec vim" );
                int result = Exec.nativeWait();
                Log.i(VimTouch.LOG_TAG, "Subprocess exited: " + result);
                mMsgHandler.sendMessage(mMsgHandler.obtainMessage(PROCESS_EXITED, result));
             }
        };
        mWatcherThread.setName("Process watcher");
    }

    public void updatePrefs(VimSettings settings) {
        mSettings = settings;
        setColorScheme(new ColorScheme(settings.getColorScheme()));
        setDefaultUTF8Mode(settings.defaultToUTF8Mode());
    }

    private void initializeSession() {
        VimSettings settings = mSettings;

        String path = System.getenv("PATH");
        if (settings.doPathExtensions()) {
            String appendPath = settings.getAppendPath();
            if (appendPath != null && appendPath.length() > 0) {
                path = path + ":" + appendPath;
            }

            if (settings.allowPathPrepend()) {
                String prependPath = settings.getPrependPath();
                if (prependPath != null && prependPath.length() > 0) {
                    path = prependPath + ":" + path;
                }
            }
        }
        if (settings.verifyPath()) {
            path = checkPath(path);
        }
        String[] env = new String[3];
        env[0] = "TERM=" + settings.getTermType();
        env[1] = "PATH=" + path;

        if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            env[2] = "HOME=/sdcard/";
        else
            env[2] = "HOME="+mApp;


        createSubprocess(settings.getShell(), env);

        setTermOut(new FileOutputStream(mTermFd));
        setTermIn(new FileInputStream(mTermFd));
    }

    private String checkPath(String path) {
        String[] dirs = path.split(":");
        StringBuilder checkedPath = new StringBuilder(path.length());
        for (String dirname : dirs) {
            File dir = new File(dirname);
            if (dir.isDirectory() && FileCompat.canExecute(dir)) {
                checkedPath.append(dirname);
                checkedPath.append(":");
            }
        }
        return checkedPath.substring(0, checkedPath.length()-1);
    }

    @Override
    public void initializeEmulator(int columns, int rows) {
        super.initializeEmulator(columns, rows);

        Exec.setPtyWindowSize(mTermFd, rows, columns, 0, 0);
        Exec.setPtyUTF8Mode(mTermFd, getUTF8Mode());
        setUTF8ModeUpdateCallback(mUTF8ModeNotify);

        mWatcherThread.start();
        Exec.startVim();
        //sendInitialCommand(mInitialCommand);
    }

    private void sendInitialCommand(String initialCommand) {
        if (initialCommand.length() > 0) {
            write(initialCommand + '\r');
        }
    }

    private void createSubprocess(String shell, String[] env) {

        mTermFd = Exec.createSubprocess(mApp, mUrl, null, env);
    }

    private ArrayList<String> parse(String cmd) {
        final int PLAIN = 0;
        final int WHITESPACE = 1;
        final int INQUOTE = 2;
        int state = WHITESPACE;
        ArrayList<String> result =  new ArrayList<String>();
        int cmdLen = cmd.length();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cmdLen; i++) {
            char c = cmd.charAt(i);
            if (state == PLAIN) {
                if (Character.isWhitespace(c)) {
                    result.add(builder.toString());
                    builder.delete(0,builder.length());
                    state = WHITESPACE;
                } else if (c == '"') {
                    state = INQUOTE;
                } else {
                    builder.append(c);
                }
            } else if (state == WHITESPACE) {
                if (Character.isWhitespace(c)) {
                    // do nothing
                } else if (c == '"') {
                    state = INQUOTE;
                } else {
                    state = PLAIN;
                    builder.append(c);
                }
            } else if (state == INQUOTE) {
                if (c == '\\') {
                    if (i + 1 < cmdLen) {
                        i += 1;
                        builder.append(cmd.charAt(i));
                    }
                } else if (c == '"') {
                    state = PLAIN;
                } else {
                    builder.append(c);
                }
            }
        }
        if (builder.length() > 0) {
            result.add(builder.toString());
        }
        return result;
    }

    @Override
    public void updateSize(int columns, int rows) {
        // Inform the attached pty of our new size:
        Exec.setPtyWindowSize(mTermFd, rows, columns, 0, 0);
        super.updateSize(columns, rows);
    }

    /* XXX We should really get this ourselves from the resource bundle, but
       we cannot hold a context */
    public void setProcessExitMessage(String message) {
        mProcessExitMessage = message;
    }

    private void onProcessExit(int result) {
        if (mSettings.closeWindowOnProcessExit()) {
            finish();
        } else if (mProcessExitMessage != null) {
            try {
                byte[] msg = ("\r\n[" + mProcessExitMessage + "]").getBytes("UTF-8");
                appendToEmulator(msg, 0, msg.length);
                notifyUpdate();
            } catch (UnsupportedEncodingException e) {
                // Never happens
            }
        }
    }

    @Override
    public void finish() {
        Exec.close(mTermFd);
        super.finish();
    }

    public void setHandle(String handle) {
        if (mHandle != null) {
            throw new IllegalStateException("Cannot change handle once set");
        }
        mHandle = handle;
    }

    public String getHandle() {
        return mHandle;
    }
}
