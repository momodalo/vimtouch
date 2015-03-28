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

package net.momodalo.app.vimtouch;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.ClipboardManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.TermSession;

import net.momodalo.app.vimtouch.addons.PluginAddOn;
import net.momodalo.app.vimtouch.addons.PluginFactory;
import net.momodalo.app.vimtouch.compat.AndroidCompat;
import kvj.app.vimtouch.ext.manager.IntegrationManager;
import kvj.app.vimtouch.ext.manager.impl.InputExtension;
import kvj.app.vimtouch.ext.manager.impl.QuickbarExtension;
import kvj.app.vimtouch.ext.manager.impl.ToastExtension;


/**
 * A terminal emulator activity.
 */

public class VimTouch extends ActionBarActivity implements
                                                OnItemSelectedListener,
                                                ActionBar.OnNavigationListener {

    /**
     * Set to true to add debugging code and logging.
     */
    public static final boolean DEBUG = false;

    /**
     * Set to true to log each character received from the remote process to the android log, which
     * makes it easier to debug some kinds of problems with emulating escape sequences and control
     * codes.
     */
    public static final boolean LOG_CHARACTERS_FLAG = DEBUG && true;

    /**
     * Set to true to log unknown escape sequences.
     */
    public static final boolean LOG_UNKNOWN_ESCAPE_SEQUENCES = DEBUG && true;

    public static final int REQUEST_INSTALL = 0;
    public static final int REQUEST_OPEN = 1;
    public static final int REQUEST_BACKUP = 2;
    public static final int REQUEST_VRZ = 3;

    /**
     * The tag we use when logging, so that our messages can be distinguished from other messages in
     * the log. Public because it's used by several classes.
     */
    public static final String LOG_TAG = "VimTouch";

    /* Sliding Menu interface */
    public interface SlidingMenuInterface {

        public boolean onOptionsItemSelected(MenuItem item);

        public boolean onNavigationItemSelected(int pos, long id);

        public Fragment getFragment();
    }

    ;
    private SlidingMenuInterface mMenu = null;

    /**
     * Our main view. Displays the emulated terminal screen.
     */
    private TermView mEmulatorView;
    private VimTermSession mSession;
    private VimSettings mSettings;
    private LinearLayout mMainLayout;
    private DrawerLayout mDrawerLayout = null;
    private ActionBarDrawerToggle actionBarDrawerToggle = null;

    private LinearLayout mButtonBarLayout;
    private View mButtonBar;
    private View mTopButtonBar;
    private View mBottomButtonBar;
    private View mLeftButtonBar;
    private View mRightButtonBar;
    private TextView mButtons[];
    private Spinner mTabSpinner;
    private Menu mOptionMenu = null;
    private String[] mVimTabs = null;
    private int mVimCurTab = -1;
    private String mOpenCommand = "tabnew";
    private ArrayAdapter<CharSequence> mTabAdapter;
    private String mLastDir = null;
    private boolean mFullscreen = false;
    private final static int QUICK_BUTTON_SIZE = 9;

    private int mControlKeyId = 0;

    private static final String TERM_FD = "TERM_FD";

    private final static String DEFAULT_SHELL = "/system/bin/sh -";
    private String mShell;

    private final static String DEFAULT_INITIAL_COMMAND =
        "export PATH=/data/local/bin:$PATH";

    private static final String KEY_QUICKBAR = "quickbarContents";

    private String mInitialCommand;

    private SharedPreferences mPrefs;

    private String mUrl = null;
    private long mEnqueue = -1;
    private DownloadManager mDM;
    private int mScreenWidth;

    private Intent TSIntent;
    private VimTermService mService;

    private ArrayList<String> quickbarContents = null;
    private ServiceConnection mTSConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            VimTermService.TSBinder binder = (VimTermService.TSBinder) service;
            mService = binder.getService();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    };

    private int mapControlChar(int result) {
        if (result >= 'a' && result <= 'z') {
            result = (char) (result - 'a' + '\001');
        } else if (result >= 'A' && result <= 'Z') {
            result = (char) (result - 'A' + '\001');
        }
        return result;
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            TextView textview = (TextView) v;
            final String cmdstr = (String) textview
                .getTag(R.id.quickbar_button_keys);
            if (cmdstr.charAt(0) == ':') {
                if (cmdstr.length() > 1) {
                    Exec.doCommand(cmdstr.substring(1, cmdstr.length()));
                } else {
                    if (Exec.isInsertMode()) {
                        mSession.write(27);
                    }
                    mSession.write(cmdstr);
                }
            } else if (cmdstr.startsWith("<ctrl+")) {
                mSession.write(mapControlChar((int) cmdstr.charAt(6)));
                //Exec.doCommand(cmd.subSequence(1,cmd.length()).toString());
            } else {
                parseKeys(mSession, cmdstr);
            }
            Exec.updateScreen();
            mEmulatorView.lateCheckInserted();
        }

    };
    View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            showCmdHistory();
            return true;
        }
    };

    private String getIntentUrl(Intent intent) {
        if (intent == null || intent.getScheme() == null) {
            return null;
        }
        String url = null;
        if (intent.getScheme().equals("file")) {
            url = intent.getData().getPath();
        } else if (intent.getScheme().equals("content")) {

            String tmpPath = "tmp";
            try {
                InputStream attachment = getContentResolver().openInputStream(intent.getData());

                if (attachment.available() > 50000) {
                    tmpPath = "";
                    new AlertDialog.Builder(this).
                        setTitle(R.string.dialog_title_content_error).
                        setMessage(R.string.message_content_too_big).
                        show();
                    throw new IOException("file too big");
                }

                String attachmentFileName = "NoFile";
                if (intent != null && intent.getData() != null) {
                    Cursor c = getContentResolver().query(intent.getData(), null, null, null, null);
                    c.moveToFirst();
                    final int
                        fileNameColumnId =
                        c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (fileNameColumnId >= 0) {
                        attachmentFileName = c.getString(fileNameColumnId);
                    }
                }

                String str[] = attachmentFileName.split("\\.");

                File outFile;
                if (str.length >= 2) {
                    outFile =
                        File.createTempFile(str[0], "." + str[1], getDir(tmpPath, MODE_PRIVATE));
                } else {
                    outFile =
                        File.createTempFile(attachmentFileName, "", getDir(tmpPath, MODE_PRIVATE));
                }
                tmpPath = outFile.getAbsolutePath();

                FileOutputStream f = new FileOutputStream(outFile);

                byte[] buffer = new byte[1024];
                while (attachment.read(buffer) > 0) {
                    f.write(buffer);
                }
                f.close();
            } catch (Exception e) {
                tmpPath = null;
                Log.e(VimTouch.LOG_TAG, e.toString());
            }

            url = tmpPath;
        }
        return url;
    }

    protected void parseKeys(VimTermSession session, String cmdstr) {
        int from = 0;
        int to = 0;
        while (to < cmdstr.length()) {
            if (cmdstr.charAt(to) == '<') {
                // Begin sequence?
                int end = cmdstr.indexOf('>', to);
                if (-1 != end && end > to + 1) {
                    // Have something in between
                    // Write what we have
                    session.write(cmdstr.substring(from, to));
                    if (cmdstr.substring(to + 1, to + 3).equals("C-")) {
                        // Ctrl+...
                        session.write(mapControlChar(cmdstr.charAt(to + 3)));
                    } else {
                        // Special char
                        int code = mapSpecialChar(cmdstr.substring(to + 1, end));
                        if (-2 == code) {
                            // Skip
                        } else if (-1 == code) {
                            Log.e(LOG_TAG,
                                  "Unrecognized char: "
                                  + cmdstr.substring(to + 1, end));
                        } else {
                            session.write(code);
                        }
                    }
                    // Process next after seq
                    from = to = end + 1;
                    if (from >= cmdstr.length()) {
                        // No more chars
                        return;
                    }
                    continue;
                }
            }
            to++;
        }
        if (to > from) {
            session.write(cmdstr.substring(from, to));
        }
    }

    private int mapSpecialChar(String code) {
        if (code.equals("BS")) {
            return 8;
        }
        if (code.equals("Tab")) {
            return 9;
        }
        if (code.equals("CR")) {
            return 13;
        }
        if (code.equals("Esc")) {
            return 27;
        }
        if (code.equals("Space")) {
            return 32;
        }
        if (code.equals("lt")) {
            return '<';
        }
        if (code.equals("Bslash")) {
            return 92;
        }
        if (code.equals("Bar")) {
            return 124;
        }
        if (code.equals("Del")) {
            return 127;
        }
        if (code.equals("kOn")) {
            // Keyboard ON
            showIme();
            return -2;
        }
        if (code.equals("kOff")) {
            // Keyboard OFF
            hideIme();
            return -2;
        }
        return -1;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle icicle) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings = new VimSettings(getResources(), mPrefs);

        if (mSettings.getDarkTheme()) {
            setTheme(R.style.VimDarkTheme);
        } else {
            setTheme(R.style.VimTheme);
        }

        super.onCreate(icicle);
        Log.e(VimTouch.LOG_TAG, "onCreate");

        if (mSettings.getFullscreen() != (
            (getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN)
            != 0)) {
            doToggleFullscreen();
        }

        mUrl = getIntentUrl(getIntent());

        TSIntent = new Intent(this, VimTermService.class);
        startService(TSIntent);

        if (!bindService(TSIntent, mTSConnection, BIND_AUTO_CREATE)) {
            Log.w(VimTouch.LOG_TAG, "bind to service failed!");
        }

        if (Integer.valueOf(android.os.Build.VERSION.SDK) < 11) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        /* setup sliding menu */
        setContentView(R.layout.term_activity);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer);

        //getSlidingMenu().attachToActivity(this, SlidingMenu.SLIDING_CONTENT);

        mMenu = new FileListMenu(this);

        setSlidingMenuFragment(mMenu.getFragment());

        mTopButtonBar = findViewById(R.id.top_bar);
        mBottomButtonBar = findViewById(R.id.bottom_bar);
        mLeftButtonBar = findViewById(R.id.left_bar);
        mRightButtonBar = findViewById(R.id.right_bar);
        mButtonBar = mTopButtonBar;

        mButtonBarLayout = (LinearLayout) findViewById(R.id.button_bar_layout);
        if (AndroidCompat.SDK >= 11) {
            mButtonBarLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        }
        /*
        TextView button = (TextView)getLayoutInflater().inflate(R.layout.quickbutton, (ViewGroup)mButtonBarLayout, false);
        button.setText(R.string.title_keyboard);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){ doToggleSoftKeyboard(); }
        });
        */

        ActionBar actionBar = getSupportActionBar();
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.app_vimtouch, R.drawable.app_vimtouch, R.drawable.app_vimtouch);
        mTabSpinner = (Spinner) findViewById(R.id.tab_spinner);
        mTabSpinner.setVisibility(View.GONE);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayUseLogoEnabled(true);
        mTabAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        mTabAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        actionBar.setListNavigationCallbacks(mTabAdapter, this);

        mMainLayout = (LinearLayout) findViewById(R.id.main_layout);

        createExtensions();

        if (checkVimRuntime()) {
            startEmulator();
        }

        Exec.vimtouch = this;
    }

    private void createExtensions() {
        IntegrationManager.getInstance(this).addExtension(
            new ToastExtension(this));
        IntegrationManager.getInstance(this).addExtension(
            new QuickbarExtension(this));
        IntegrationManager.getInstance(this).addExtension(
            new InputExtension(this));
    }

    public void onDestroy() {
        super.onDestroy();
        unbindService(mTSConnection);
        stopService(TSIntent);
        mService = null;
        mTSConnection = null;
        IntegrationManager.getInstance(this).stop();

        System.runFinalizersOnExit(true);

        System.exit(0);
    }

    private TermView createEmulatorView(VimTermSession session) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        return new TermView(this, session, metrics);
    }

    private boolean checkPlugins() {
        // check plugins which not installed yet first
        ArrayList<PluginAddOn> plugins = PluginFactory.getAllPlugins(getApplicationContext());
        for (PluginAddOn addon : plugins) {
            if (!addon.isInstalled(getApplicationContext())) {
                Intent intent = new Intent(getApplicationContext(), InstallProgress.class);
                intent.setData(Uri.parse("plugin://" + addon.getId()));
                startActivityForResult(intent, REQUEST_INSTALL);
                return false;
            }
        }

        return true;
    }

    private void startEmulator() {
        String appPath = getApplicationContext().getFilesDir().getPath();
        String nativePath = getApplicationInfo().nativeLibraryDir;
        mSession = new VimTermSession(appPath, nativePath, getApplicationContext().getFilesDir().getAbsolutePath(), mUrl, mSettings, "");
        mSession.setFinishCallback(new TermSession.FinishCallback() {
            @Override
            public void onSessionFinish(TermSession session) {
                hideIme();
                finish();
            }
        });
        mEmulatorView = createEmulatorView(mSession);
        mMainLayout.addView(mEmulatorView);

        mEmulatorView.updateSize(true);
        //Exec.updateScreen();
        mUrl = null;
    }

    public String getQuickbarFile() {
        return getApplicationContext().getFilesDir() + "/vim/quickbar";
    }

    private boolean checkVimRuntime() {
        if (InstallProgress.isInstalled(this)) {
            return checkPlugins();
        }

        // check default package
        /* FIXME: we won't change default runtime for long time , but it's better to re-check again later.
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            if(mSettings.getLastVersionCode() == info.versionCode)
                return checkPlugins();
        } catch (PackageManager.NameNotFoundException e) {
        }
        */

        Intent intent = new Intent(getApplicationContext(), InstallProgress.class);
        startActivityForResult(intent, REQUEST_INSTALL);

        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_INSTALL) {
            if (checkVimRuntime()) {
                PackageInfo info;
                SharedPreferences.Editor editor = mPrefs.edit();

                try {
                    info =
                        getPackageManager()
                            .getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
                    editor.putLong(VimSettings.LASTVERSION_KEY, info.versionCode);
                    editor.commit();
                } catch (Exception e) {
                }

                startEmulator();
                updatePrefs();
                mEmulatorView.onResume();
            }
        } else if (requestCode == REQUEST_OPEN) {
            /*
            if (resultCode == Activity.RESULT_OK) {
                String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
                mUrl = filePath;
            }
            */
        }
    }

    @Override
    public void onStop() {
        Log.e(VimTouch.LOG_TAG, "on stop.");
        mSettings.writePrefs(mPrefs);
        /*
        if (mTermFd != null) {
            try{
                //mSession.write((":q!\r").getBytes("UTF-8"));
            }catch (Exception e){
            }
            Exec.close(mTermFd);
            mTermFd = null;
        }
        */
        super.onStop();
    }

    private void restart() {
        startActivity(getIntent());
        finish();
    }

    public void write(String data) {
        mSession.write(data);
    }

    public void write(int data) {
        mSession.write(data);
    }

    private TextView addQuickbarButton(String text) {
        TextView
            button =
            (TextView) getLayoutInflater()
                .inflate(R.layout.quickbutton, (ViewGroup) mButtonBarLayout, false);
        button.setOnClickListener(mClickListener);
        button.setOnLongClickListener(mLongClickListener);
        mButtonBarLayout.addView((View) button);
        return button;
    }

    private void defaultButtons(boolean force) {
        File file = new File(getQuickbarFile());

        if (!force && file.exists()) {
            return;
        }

        try {
            BufferedInputStream
                is =
                new BufferedInputStream(getResources().openRawResource(R.raw.quickbar));
            FileWriter fout = new FileWriter(file);
            while (is.available() > 0) {
                fout.write(is.read());
            }
            fout.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "install default quickbar", e);
        }

    }

    public void setCustomButtons(List<String> buttons) {
        quickbarContents = new ArrayList<String>(buttons);
        setButtons(buttons);
    }

    private void setButtons(List<String> buttons) {
        int index = 0;
        for (String line : buttons) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            TextView textview = null;
            String caption = line;
            String keys = line;
            int spaceIndex = line.lastIndexOf(' ');
            if (-1 != spaceIndex) {
                // Have caption
                keys = line.substring(0, spaceIndex);
                caption = line.substring(spaceIndex + 1);
            }
            if (index < mButtonBarLayout.getChildCount()) {
                textview = (TextView) mButtonBarLayout
                    .getChildAt(index);
                textview.setVisibility(View.VISIBLE);
            } else {
                textview = addQuickbarButton(line);
            }
            textview.setText(caption);
            textview.setTag(R.id.quickbar_button_keys, keys);
            index++;
        }
        for (int i = index; i < mButtonBarLayout.getChildCount(); i++) {
            mButtonBarLayout.getChildAt(i).setVisibility(View.GONE);
        }
    }

    public void updateButtons() {
        Log.i(LOG_TAG, "updateButtons");
        quickbarContents = null;
        defaultButtons(false);
        try {
            FileReader fileReader = new FileReader(getQuickbarFile());
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = null;
            List<String> buttons = new ArrayList<String>();
            while ((line = bufferedReader.readLine()) != null) {
                buttons.add(line);
            }
            bufferedReader.close();
            setButtons(buttons);
        } catch (IOException e) {
        }
    }

    private void updatePrefs() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ColorScheme colorScheme = new ColorScheme(mSettings.getColorScheme());

        if (mSession.getSuRoot() != mSettings.getSuRoot()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_need_restart)
                .setMessage(R.string.message_need_restart)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
            builder.create().show();
        }

        mEmulatorView.setDensity(metrics);
        mEmulatorView.updatePrefs(mSettings);

        mSession.updatePrefs(mSettings);

        mButtonBar.setVisibility(View.GONE);
        ((ViewGroup) mButtonBar).removeView(mButtonBarLayout);

        if (null == quickbarContents) {
            updateButtons();
        }

        int pos = mSettings.getQuickbarPosition();

        switch (pos) {
            case 1:
                mButtonBar = mBottomButtonBar;
                mButtonBarLayout.setOrientation(LinearLayout.HORIZONTAL);
                break;
            case 2:
                mButtonBar = mLeftButtonBar;
                mButtonBarLayout.setOrientation(LinearLayout.VERTICAL);
                break;
            case 3:
                mButtonBar = mRightButtonBar;
                mButtonBarLayout.setOrientation(LinearLayout.VERTICAL);
                break;
            case 0:
            default:
                mButtonBar = mTopButtonBar;
                mButtonBarLayout.setOrientation(LinearLayout.HORIZONTAL);
                break;
        }
        ((ViewGroup) mButtonBar).addView(mButtonBarLayout);
        if (mSettings.getQuickbarShow()) {
            mButtonBar.setVisibility(View.VISIBLE);
        } else {
            mButtonBar.setVisibility(View.GONE);
        }

    }

    @Override
    public void onResume() {
        Log.e(VimTouch.LOG_TAG, "on resume.");
        super.onResume();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings.readPrefs(mPrefs);
        if (mEmulatorView != null) {
            updatePrefs();
            mEmulatorView.onResume();

            if (mUrl != null) {
                Exec.doCommand("tabnew " + mUrl);
                Exec.updateScreen();
                mUrl = null;
            }
        }
    }

    @Override
    public void onPause() {
        Log.e(VimTouch.LOG_TAG, "on pause.");
        super.onPause();
        if (mEmulatorView != null) {
            hideIme();
            mEmulatorView.onPause();
        }
    }

    public void hideIme() {
        if (mEmulatorView == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager)
            getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEmulatorView.getWindowToken(), 0);
    }

    public void showIme() {
        if (mEmulatorView == null) {
            return;
        }
        InputMethodManager
            imm =
            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEmulatorView, InputMethodManager.SHOW_FORCED);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.e(VimTouch.LOG_TAG, "on new intent.");
        String url = getIntentUrl(intent);
        if (mSession == null) {
            mUrl = url;
            startEmulator();
            return;
        }
        if (url == mUrl || url == "") {
            return;
        }

        Bundle extras = intent.getExtras();
        int
            opentype =
            extras == null ? VimFileActivity.FILE_TABNEW
                           : extras.getInt(VimFileActivity.OPEN_TYPE, VimFileActivity.FILE_TABNEW);
        String opencmd;
        switch (opentype) {
            case VimFileActivity.FILE_NEW:
                opencmd = "new";
                break;
            case VimFileActivity.FILE_VNEW:
                opencmd = "vnew";
                break;
            case VimFileActivity.FILE_TABNEW:
            default:
                opencmd = "tabnew";
                break;
        }
        String old = mOpenCommand;
        mOpenCommand = opencmd;
        openNewFile(url);
        mOpenCommand = old;
        mUrl = null;
    }

    public void openNewFile(String url) {
        Exec.doCommand(mOpenCommand + " " + url.replaceAll(" ", "\\\\ "));
        Exec.updateScreen();
        if (null != mDrawerLayout) {
            mDrawerLayout.closeDrawers();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.e(VimTouch.LOG_TAG, "on configuration changed");
        if (mEmulatorView != null) {
            mEmulatorView.updateSize(true);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE), 500);
        }
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    private static final int MSG_DIALOG = 1;
    private static final int MSG_UPDATE = 2;
    private static final int MSG_SYNCCLIP = 3;
    private static final int MSG_SETCLIP = 4;
    private static final int MSG_SETCURTAB = 5;
    private static final int MSG_SETTABS = 6;
    private static final int MSG_SHOWTAB = 7;

    private class DialogObj {

        public int type;
        public String title;
        public String message;
        public String buttons;
        public int def_button;
        public String textfield;
    }

    ;

    static class MsgHandler extends Handler {

        private final WeakReference<VimTouch> mActivity;

        MsgHandler(VimTouch activity) {
            mActivity = new WeakReference<VimTouch>(activity);
        }

        ClipboardManager clipMgr(Activity activity) {
            Context ctx = activity.getApplicationContext();
            String svc = Context.CLIPBOARD_SERVICE;
            return (ClipboardManager) ctx.getSystemService(svc);
        }

        @Override
        public void handleMessage(Message msg) {
            VimTouch activity = mActivity.get();

            if (activity == null) {
                super.handleMessage(msg);
                return;
            }

            switch (msg.what) {
                case MSG_UPDATE:
                    Exec.updateScreen();
                    break;
                case MSG_DIALOG:
                    DialogObj obj = (DialogObj) msg.obj;
                    activity.showDialog(obj.type, obj.title, obj.message,
                                        obj.buttons, obj.def_button,
                                        obj.textfield);
                    break;
                case MSG_SYNCCLIP:
                    if (clipMgr(activity).getText() == null) {
                        activity.mClipText = "";
                    } else {
                        activity.mClipText = clipMgr(activity).getText().toString();
                    }
                    break;
                case MSG_SETCLIP:
                    activity.mClipText = (String) msg.obj;
                    clipMgr(activity).setText(activity.mClipText);
                    break;
                case MSG_SETCURTAB:
                    int n = (int) msg.arg1;
                    activity.setVimCurTab(n);
                case MSG_SETTABS:
                    String[] array = (String[]) msg.obj;
                    if (array == null) {
                        break;
                    }
                    activity.setVimTabs(array);
                    activity.setTabLabels(array);
                    break;
                case MSG_SHOWTAB:
                    int s = (int) msg.arg1;
                    activity.showTab(s);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private MsgHandler mHandler = new MsgHandler(this);

    public void nativeShowDialog(int type, String title, String message, String buttons,
                                 int def_button, String textfield) {
        DialogObj obj = new DialogObj();
        obj.type = type;
        obj.title = title;
        obj.message = message;
        obj.buttons = buttons;
        obj.def_button = def_button;
        obj.textfield = textfield;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_DIALOG, obj));
    }

    private String mClipText;

    public void nativeSyncClipText() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SYNCCLIP));
    }

    public String getClipText() {
        return mClipText;
    }

    public void nativeSetClipText(String text) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SETCLIP, text));
    }

    private AlertDialog.Builder actionBuilder;

    private void showDialog(int type, String title, String message, String buttons, int def_button,
                            String textfield) {
        buttons = buttons.replaceAll("&", "");
        String button_array[] = buttons.split("\n");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
            .setTitle(title)
            .setPositiveButton(button_array[def_button], new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Exec.resultDialogDefaultState();
                }
            })
            .setNegativeButton("More", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    AlertDialog actdialog = actionBuilder.create();
                    actdialog.show();
                }
            });
               /*
        switch(type) {
            case 1:
                break;
            case 2:
                break;
            default:
                dialog = null;
        }
        */

        AlertDialog dialog = builder.create();
        dialog.show();

        actionBuilder = new AlertDialog.Builder(VimTouch.this);
        actionBuilder.setTitle(title)
            .setCancelable(false)
            .setItems(button_array, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog2, int item) {
                    Exec.resultDialogState(item + 1);
                }
            });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private boolean doToggleFullscreen() {
        boolean ret = false;
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if ((attrs.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) {
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            ret = true;
        }
        mSettings.setFullscreen(ret);
        getWindow().setAttributes(attrs);
        mFullscreen = ret;
        return ret;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_fullscreen).setChecked(mFullscreen);
        menu.findItem(R.id.menu_keys).setChecked(mSettings.getQuickbarShow());
        mOptionMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_preferences) {
            doPreferences();
        } else if (id == R.id.menu_fullscreen) {
            item.setChecked(doToggleFullscreen());
        } else if (id == R.id.menu_vimrc) {
            Exec.doCommand("tabnew ~/.vimrc");
            Exec.updateScreen();
        } else if (id == R.id.menu_quickbar) {
            Exec.doCommand("tabnew " + getQuickbarFile());
            Exec.updateScreen();
        } else if (id == R.id.menu_toggle_soft_keyboard) {
            doToggleSoftKeyboard();
        } else if (id == R.id.menu_ESC) {
            mSession.write(27);
            Exec.updateScreen();
            mEmulatorView.lateCheckInserted();
        } else if (id == R.id.menu_quit) {
            Exec.doCommand("q!");
            Exec.updateScreen();
        } else if (id == R.id.menu_ime_composing) {
            item.setChecked(mEmulatorView.toggleIMEComposing());
        } else if (id == R.id.menu_extra_downloads) {
            Intent search = new Intent(Intent.ACTION_VIEW);
            search.setData(Uri.parse("market://search?q=VimTouch"));
            search.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(search);
        /*
        } else if (id == R.id.menu_full_vim_runtime)  {
            downloadFullRuntime();
        */
        } else if (id == R.id.menu_new) {
            item.setChecked(true);
            mOpenCommand = "new";
        } else if (id == R.id.menu_vnew) {
            item.setChecked(true);
            mOpenCommand = "vnew";
        } else if (id == R.id.menu_tabnew) {
            item.setChecked(true);
            mOpenCommand = "tabnew";
        } else if (id == R.id.menu_diff) {
            item.setChecked(true);
            mOpenCommand = "vert diffsplit";
        } else if (id == R.id.menu_save) {
            Exec.doCommand("w");
        } else if (id == R.id.menu_backup) {
            Intent intent = new Intent(getApplicationContext(), InstallProgress.class);
            intent.setData(Uri.parse("backup://" + Environment.getExternalStorageDirectory() + "/"
                                     + "VimTouchBackup.vrz"));
            startActivityForResult(intent, REQUEST_BACKUP);
        } else if (id == R.id.menu_keys) {
            if (mButtonBarLayout.isShown()) {
                mSettings.setQuickbarShow(false);
                item.setChecked(false);
                mButtonBar.setVisibility(View.GONE);
            } else {
                mSettings.setQuickbarShow(true);
                item.setChecked(true);
                mButtonBar.setVisibility(View.VISIBLE);
            }
        }
        actionBarDrawerToggle.onOptionsItemSelected(item);
        return super.onOptionsItemSelected(item);
    }

    private void doPreferences() {
        startActivity(new Intent(this, VimTouchPreferences.class));
    }

    private void doCopyAll() {
        ClipboardManager clip = (ClipboardManager)
            getSystemService(Context.CLIPBOARD_SERVICE);
        clip.setText(Exec.getCurrBuffer());
    }

    private void doPaste() {
        ClipboardManager clip = (ClipboardManager)
            getSystemService(Context.CLIPBOARD_SERVICE);
        CharSequence paste = clip.getText();
        mSession.write(paste.toString());
    }

    private void doToggleSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)
            getSystemService(Context.INPUT_METHOD_SERVICE);

        // force leave fullscreen first
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if ((attrs.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) {
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attrs);
            mFullscreen = false;
            mOptionMenu.findItem(R.id.menu_fullscreen).setChecked(mFullscreen);
        }
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

    }

    public ArrayAdapter<CharSequence> getTabAdapter() {
        return mTabAdapter;
    }

    public void setVimTabs(String[] array) {
        mVimTabs = array;
    }

    public void setTabLabels(String[] array) {
        if (array == null) {
            return;
        }
        mTabAdapter.clear();
        for (String str : array) {
            mTabAdapter.add(str);
        }
        mTabAdapter.notifyDataSetChanged();
    }

    public void setCurTab(int n) {
        if (n < 0) {
            showTab(0);
            return;
        }

        getSupportActionBar().setSelectedNavigationItem(n);
    }

    public void setVimCurTab(int n) {
        mVimCurTab = n;
        setCurTab(n);
    }

    public void showTab(int n) {
        if (n <= 0) {
            mVimCurTab = -1;
        }
        //mTabSpinner.setVisibility(n>0?View.VISIBLE:View.GONE);
        getSupportActionBar().setNavigationMode(
            n > 0 ? ActionBar.NAVIGATION_MODE_LIST
                  : ActionBar.NAVIGATION_MODE_STANDARD);
    }

    public void nativeSetCurTab(int n) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SETCURTAB, n, 0));
    }

    public void nativeShowTab(int n) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SHOWTAB, n, 0));
    }

    public void nativeSetTabs(String[] array) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SETTABS, array));
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Exec.setTab(pos);
    }

    public boolean onNavigationItemSelected(int pos, long id) {
        Exec.setTab(pos);
        return true;
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

    TextView mHistoryButtons[] = new TextView[10];

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void showCmdHistory() {

        final Dialog dialog = new Dialog(this, R.style.DialogSlideAnim);

        // Setting dialogview
        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);

        window.setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        dialog.setTitle(null);
        dialog.setContentView(R.layout.hist_list);
        dialog.setCancelable(true);

        LinearLayout layout = (LinearLayout) dialog.findViewById(R.id.hist_layout);
        if (AndroidCompat.SDK >= 11) {
            layout.setShowDividers(
                LinearLayout.SHOW_DIVIDER_BEGINNING | LinearLayout.SHOW_DIVIDER_MIDDLE
                | LinearLayout.SHOW_DIVIDER_END);
        }
        LayoutParams params = layout.getLayoutParams();
        params.width = mScreenWidth;
        layout.setLayoutParams(params);

        LayoutInflater inflater = LayoutInflater.from(this);
        boolean exists = false;

        for (int i = 0; i < 10; i++) {
            TextView button = (TextView) inflater.inflate(R.layout.histbutton, layout, false);
            //String cmd = Exec.getCmdHistory(i);
            //if(cmd.length() == 0) break;
            //exists = true;
            //button.setText(":"+cmd);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    TextView text = (TextView) v;
                    CharSequence cmd = text.getText();
                    if (cmd.length() > 1) {
                        ;
                    }
                    Exec.doCommand(cmd.subSequence(1, cmd.length()).toString());
                    dialog.dismiss();
                }
            });
            layout.addView((View) button);
            button.setVisibility(View.GONE);
            mHistoryButtons[i] = button;
        }
        Exec.getHistory();

        //if(exists)
        dialog.show();
    }

    public void setHistoryItem(int i, String text) {
        TextView button = mHistoryButtons[i];
        if (button == null) {
            return;
        }

        button.setText(":" + text);
        button.setVisibility(View.VISIBLE);
    }

    void setSlidingMenuFragment(Fragment frag) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.menu_frame, frag)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit();
    }
}
