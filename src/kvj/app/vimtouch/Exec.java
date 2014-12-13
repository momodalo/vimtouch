/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kvj.app.vimtouch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kvj.app.vimtouch.ext.manager.IntegrationManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

/**
 * Utility methods for creating and managing a subprocess.
 * <p>
 * Note: The native methods access a package-private
 * java.io.FileDescriptor field to get and set the raw Linux
 * file descriptor. This might break if the implementation of
 * java.io.FileDescriptor is changed.
 */

public class Exec
{
    static {
        System.loadLibrary("vimtouch");
    }

    static final String TAG = "Exec";
    static public VimTouch vimtouch;
    static private int dialogState = 0;
    static private int dialogDefaultState = 0;

    static private int State;
    static private int Cursor_col;
    static private int Cursor_lnum;

    static public final int DIALOG_INPROGRESS = -1;

    static public void resultDialogState(int state){
        dialogState = state;
        returnDialog(dialogState);
    }

    static public void resultDialogDefaultState(){
        dialogState = dialogDefaultState;
        returnDialog(dialogState);
    }

    public static void showDialog(
int type, String title, String message,
			String buttons, int default_button, String textfield
    ) {
        dialogState = DIALOG_INPROGRESS;
        dialogDefaultState = default_button;
        vimtouch.nativeShowDialog(type,title, message, buttons, default_button, textfield);
    }

    public static void quit() {
        vimtouch.hideIme();
        vimtouch.finish();
    }

    public static void setCurTab(int n){
        vimtouch.nativeSetCurTab(n);
    }

    public static void showTab(int n){
        vimtouch.nativeShowTab(n);
    }

    public static void setTabLabels(String[] labels){
        vimtouch.nativeSetTabs(labels);
    }

    public static void setClipText(String text){
        vimtouch.nativeSetClipText(text);
    }

    public static String getClipText() throws InterruptedException {
        vimtouch.nativeSyncClipText();
        String clipText = vimtouch.getClipText();

        while (clipText == null) {
            Thread.sleep(100);
            clipText = vimtouch.getClipText();
        }

        return clipText;
    }

    /**
     * Create a subprocess. Differs from java.lang.ProcessBuilder in
     * that a pty is used to communicate with the subprocess.
     * <p>
     * Callers are responsible for calling Exec.close() on the returned
     * file descriptor.
     *
     * @param cmd The command to execute
     * @param arg0 The first argument to the command, may be null
     * @param arg1 the second argument to the command, may be null
     * started process will be written.
     * @return the file descriptor of the started process.
     *
     */
    public static native FileDescriptor nativeCreateSubprocess(
        String cmd, String sock, String arg0, String arg1, String[] envp);

    public static native void startVim();
        
    /**
     * Set the widow size for a given pty. Allows programs
     * connected to the pty learn how large their screen is.
     */
    public static native void setPtyWindowSize(FileDescriptor fd,
       int row, int col, int xpixel, int ypixel);

    public static native void setPtyUTF8Mode(FileDescriptor fd,
       boolean utf8Mode);

    public static native int scrollBy(int processId);

    /**
     * Causes the calling thread to wait for the process associated with the
     * receiver to finish executing.
     *
     * @return The exit value of the Process being waited on
     *
     */
    public static native int nativeWait();

    /**
     * Close a given file descriptor.
     */
    public static native void close(FileDescriptor fd);

    public static native void updateScreen();

    public static native void doCommand(String cmd);

    public static native void mouseDown(int row, int col);
    public static native void mouseDrag(int row, int col);
    public static native void mouseUp(int row, int col);

    public static native void lineReplace(String line);

    public static native String getCurrentLine(int size);
    public static native void setCursorCol(int col);
    public static native void setCursorPos(int row, int col);
    public static native String getCurrBuffer();
    public static native void setTab(int nr);
    public static native String getcwd();
    public static native void setSocket(int fd);
    public static native void returnClipText(String text);
    public static native void returnDialog(int s);
    public static native void returnExtensionResult(String text);

	public static native void sendAndroidEvent(int type, String object);

    public static native void getHistory();

    public static int getCursorLine() {
        return Cursor_lnum;
    }

    public static int getCursorCol() {
        return Cursor_col;
    }

    public static int getState(){
        return State;
    }

    public static void syncVim(int s, int c, int l){
        State = s;
        Cursor_col = c;
        Cursor_lnum = l;
    }

    public final static int CMDLINE = 0x08;
    public final static int INSERT = 0x10;
    public static boolean isCmdLine(){
        return (Exec.getState() & CMDLINE) != 0;
    }

    public static boolean isInsertMode(){
        return (Exec.getState() & INSERT) != 0;
    }

    public static void launchCommandService() {
    }

    static VimTouchSocketServer mSocketServer;

    public static FileDescriptor createSubprocess(
        String cmd, String arg0, String arg1, String[] envp){
        mSocketServer = new VimTouchSocketServer();
        mSocketServer.start();
        return nativeCreateSubprocess( cmd, mSocketServer.getSocketName(), arg0, arg1, envp);
    }

    public static class VimTouchSocketServer extends Thread {

        int mBufferSize = 8192;
        byte[] mBuffer;
        LocalServerSocket mServerSocket;
        LocalSocket mReceiver;
        static final String VIMTOUCH_SOCKET = "/tmp/vimtouch/";

        String mSocketName;

        public VimTouchSocketServer() {
            mBuffer = new byte[mBufferSize];

            try {
                mSocketName = UUID.randomUUID().toString();
                mServerSocket = new LocalServerSocket(VIMTOUCH_SOCKET + mSocketName);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "The localSocketServer created failed !!!");
                e.printStackTrace();
            }

            LocalSocketAddress localSocketAddress; 
            localSocketAddress = mServerSocket.getLocalSocketAddress();
            String str = localSocketAddress.getName();

            Log.d(TAG, "The LocalSocketAddress = " + str);

        }

        public String getSocketName() {
            return mSocketName;
        }

		private static final Pattern COMMAND_REXP = Pattern
				.compile("([A-Z ]{7}):");

		/**
		 * Splits str with commands by regexp. Puts commands and values into
		 * List
		 * 
		 * @param str
		 * @return
		 */
		private List<String> splitCommands(String str) {
			Matcher m = COMMAND_REXP.matcher(str);
			String name = null;
			List<String> result = new ArrayList<String>();
			while (m.find()) {
				StringBuffer sb = new StringBuffer();
				m.appendReplacement(sb, "");
				if (null != name) {
					// Not a first time, name is set
					result.add(name);
					result.add(sb.toString());
				}
				name = m.group(1).trim();
			}
			StringBuffer sb = new StringBuffer();
			m.appendTail(sb);
			if (null != name) {
				// Last time, name (if set) and tail as a value
				result.add(name);
				result.add(sb.toString());
			}
			return result;
		}

		public void run() {
			try {
                while (!isInterrupted()) {

                    if (null == mServerSocket){
                        break;
                    }

                    try {
                        mReceiver = mServerSocket.accept();
                    } catch (IOException e) {
                        Log.d(TAG, "localSocketServer accept() failed !!!");
                        e.printStackTrace();
                        continue;
                    }                   
                    Log.e(TAG, "localSocketServer accepted");

                    InputStream input;

                    try {
                        input = mReceiver.getInputStream();
                    } catch (IOException e) {
                        Log.d(TAG, "getInputStream() failed !!!");
                        e.printStackTrace();
                        continue;
                    }

                    Field __fd;
                    try {
                        __fd = FileDescriptor.class.getDeclaredField("descriptor");
                        __fd.setAccessible(true);
                        setSocket(__fd.getInt(mReceiver.getFileDescriptor()));
                    } catch (Exception ex) {
                        __fd = null;
                        Log.e(TAG,"set socket error " + ex);
                    }   

                    int bytesRead;
                    while (mReceiver != null) {
                        try {
                            bytesRead = input.read(mBuffer, 0, mBufferSize );
                        } catch (Exception e) {
                            Log.d(TAG, "There is an exception when reading socket");
                            e.printStackTrace();
                            break;
                        }

                        if(bytesRead <= 0)continue;
                        String str = new String(mBuffer, 0, bytesRead);
						List<String> parts = splitCommands(str);
                        Log.d(TAG, "get "+str);
						for (int i = 0; i < parts.size() - 1; i += 2) {
							// Process commands (name, value, name, value ...)
							String name = parts.get(i);
							String value = parts.get(i + 1).trim();
							if (name.equals("SETCTAB")) {
								setCurTab(Integer.parseInt(value));
							} else if (name.equals("SYNC")) {
								String[] array = value.split(",");
								syncVim(Integer.parseInt(array[0]),
										Integer.parseInt(array[1]),
										Integer.parseInt(array[2]));
							} else if (name.equals("SHOWTAB")) {
								showTab(Integer.parseInt(value));
							} else if (name.equals("ANDROID")) {
								processExtensionCommand(value);
							} else if (name.equals("SETLBLS")) {
								setTabLabels(value.split(","));
							} else if (name.equals("SETCLIP")) {
								setClipText(value);
							} else if (name.equals("GETCLIP")) {
								try {
									returnClipText(getClipText());
								} catch (Exception e) {
									returnClipText("");
								}
							} else if (name.equals("SDIALOG")) {
								String[] array = value.split(",");
								showDialog(Integer.parseInt(array[0]),
										array[1], array[2], array[3],
										Integer.parseInt(array[4]), array[5]);
							} else if (name.equals("HISTORY")) {
								vimtouch.setHistoryItem(
										Integer.parseInt(value.substring(0, 1)),
										value.substring(2));
							}
						}

                    }
                }
                Log.d(TAG, "The LocalSocketServer thread is going to stop !!!");
                if (mReceiver != null){
                    try {
                        mReceiver.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (mServerSocket != null){
                    try {
                        mServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } finally {
                try {
                    if (mServerSocket!= null) {
                        mServerSocket.close();
                    }
                    Log.i(TAG, "socket listener stopped");
                } catch (IOException e) {
                }
            }
        }

		public void stopServer() {
            if (mServerSocket != null) {
                try {
                    // mark thread as interrupted
                    interrupt();

                    // now send connect request to myself to trigger leaving accept()
                    LocalSocket ls = new LocalSocket();
                    ls.connect(mServerSocket.getLocalSocketAddress());
                    ls.close();
                } catch (IOException e) {
                    Log.e(TAG, "stopSocketServer failed", e);
                }
            }
        }

    }

	private static void processExtensionCommand(String value) {
		int space = value.indexOf(' ');
		if (-1 == space) {
			returnExtensionResult("");
			return;
		}
		String type = value.substring(0, space);
		String params = value.substring(space+1);
		// Log.i(TAG, "Extension command: "+type+" - "+params);
		returnExtensionResult(IntegrationManager.getInstance(vimtouch).process(
				type, params));
	}
}

