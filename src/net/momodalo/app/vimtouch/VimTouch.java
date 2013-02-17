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

import java.util.ArrayList;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.ClipboardManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.TermSession;
import com.lamerman.FileDialog;

import net.momodalo.app.vimtouch.addons.RuntimeFactory;
import net.momodalo.app.vimtouch.addons.RuntimeAddOn;
import net.momodalo.app.vimtouch.addons.PluginFactory;
import net.momodalo.app.vimtouch.addons.PluginAddOn;

/**
 * A terminal emulator activity.
 */

public class VimTouch extends Activity implements OnItemSelectedListener {
    /**
     * Set to true to add debugging code and logging.
     */
    public static final boolean DEBUG = false;

    /**
     * Set to true to log each character received from the remote process to the
     * android log, which makes it easier to debug some kinds of problems with
     * emulating escape sequences and control codes.
     */
    public static final boolean LOG_CHARACTERS_FLAG = DEBUG && true;

    /**
     * Set to true to log unknown escape sequences.
     */
    public static final boolean LOG_UNKNOWN_ESCAPE_SEQUENCES = DEBUG && true;

    private static final int REQUEST_INSTALL = 0;
    private static final int REQUEST_OPEN = 1;

    /**
     * The tag we use when logging, so that our messages can be distinguished
     * from other messages in the log. Public because it's used by several
     * classes.
     */
    public static final String LOG_TAG = "VimTouch";

    /**
     * Our main view. Displays the emulated terminal screen.
     */
    private TermView mEmulatorView;
    private VimTermSession mSession;
    private VimSettings mSettings;
    private LinearLayout mMainLayout;

    private LinearLayout mButtonBarLayout;
    private View mButtonBar;
    private View mTopButtonBar;
    private View mBottomButtonBar;
    private View mLeftButtonBar;
    private View mRightButtonBar;
    private TextView mButtons[];
    private Spinner mTabSpinner;
    private ArrayAdapter<CharSequence> mTabAdapter;
    private final static int QUICK_BUTTON_SIZE=9;

    private int mControlKeyId = 0;

    private static final String TERM_FD = "TERM_FD";

    private final static String DEFAULT_SHELL = "/system/bin/sh -";
    private String mShell;

    private final static String DEFAULT_INITIAL_COMMAND =
        "export PATH=/data/local/bin:$PATH";
    private String mInitialCommand;

    private SharedPreferences mPrefs;

    private String mUrl = null;
    private long mEnqueue = -1;
    private DownloadManager mDM;

    View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v){
            TextView textview = (TextView)v;
            CharSequence cmd = textview.getText();
            if(cmd.charAt(0) == ':'){
                if(cmd.length() > 1){
                    Exec.doCommand(cmd.subSequence(1,cmd.length()).toString());
                }else{
                    if(Exec.isInsertMode())
                        mSession.write(27);
                    mSession.write(cmd.toString());
                }
            }else
                mSession.write(cmd.toString());
            Exec.updateScreen();
            mEmulatorView.lateCheckInserted();
        }
    };

    private String getIntentUrl(Intent intent){
        if(intent == null || intent.getScheme() == null) return null;
        String url = null;
        if(intent.getScheme().equals("file")) {
            url = intent.getData().getPath();
        }else if (intent.getScheme().equals("content")){
              
             String tmpPath = "tmp";
             try {
                InputStream attachment = getContentResolver().openInputStream(intent.getData());

                if(attachment.available() > 50000){
                    tmpPath = "";
                    new AlertDialog.Builder(this).
                        setTitle(R.string.dialog_title_content_error).
                        setMessage(R.string.message_content_too_big).
                        show();
                    throw new IOException("file too big");
                }

                String attachmentFileName = "NoFile";
                if (intent != null && intent.getData() != null) {
                    Cursor c = getContentResolver().query( intent.getData(), null, null, null, null);
                    c.moveToFirst();
                    final int fileNameColumnId = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (fileNameColumnId >= 0)
                        attachmentFileName = c.getString(fileNameColumnId);
                }

                String str[] = attachmentFileName.split("\\.");

                File outFile;
                if(str.length >= 2)
                    outFile=File.createTempFile(str[0], "."+str[1], getDir(tmpPath, MODE_PRIVATE));
                else
                    outFile=File.createTempFile(attachmentFileName,"", getDir(tmpPath, MODE_PRIVATE));
                tmpPath = outFile.getAbsolutePath();

                FileOutputStream f = new FileOutputStream(outFile);
                                                                          
                byte[] buffer = new byte[1024];
                while (attachment.read(buffer) > 0){
                    f.write(buffer);
                }
                f.close();
            }catch (Exception e){
                tmpPath = null;
                Log.e(VimTouch.LOG_TAG, e.toString());
            }

            url = tmpPath;
        }
        return url;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.e(VimTouch.LOG_TAG, "onCreate");

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings = new VimSettings(getResources(), mPrefs);

        mUrl = getIntentUrl(getIntent());

        if(Integer.valueOf(android.os.Build.VERSION.SDK) < 11)
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.term_activity);

        mTopButtonBar = findViewById(R.id.top_bar);
        mBottomButtonBar = findViewById(R.id.bottom_bar);
        mLeftButtonBar = findViewById(R.id.left_bar);
        mRightButtonBar = findViewById(R.id.right_bar);
        mButtonBar = mTopButtonBar;

        mButtonBarLayout = (LinearLayout) findViewById(R.id.button_bar_layout);
        TextView button = (TextView)getLayoutInflater().inflate(R.layout.quickbutton, (ViewGroup)mButtonBarLayout, false);
        button.setText(R.string.title_keyboard);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){ doToggleSoftKeyboard(); }
        });
        // add tab spinner
        mTabSpinner = (Spinner)getLayoutInflater().inflate(R.layout.tabspinner, (ViewGroup)mButtonBarLayout, false);
        mTabSpinner.setOnItemSelectedListener(this);
        mButtonBarLayout.addView((View)mTabSpinner);
        mTabAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        mTabAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTabSpinner.setAdapter(mTabAdapter);

        mMainLayout = (LinearLayout)findViewById(R.id.main_layout);

        if(checkVimRuntime())
            startEmulator();

        Exec.vimtouch = this;
    }

    public void onDestroy() {
        super.onDestroy();

        System.runFinalizersOnExit(true);

        System.exit(0);
    }

    private TermView createEmulatorView(VimTermSession session) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return new TermView(this, session, metrics);
    }

    private boolean checkPlugins() {
        // check plugins which not installed yet first
        ArrayList<PluginAddOn> plugins = PluginFactory.getAllPlugins(getApplicationContext());
        for (PluginAddOn addon: plugins){
            if(!addon.isInstalled(getApplicationContext())){
                Intent intent = new Intent(getApplicationContext(), InstallProgress.class);
                intent.setData(Uri.parse("plugin://"+addon.getId()));
                startActivityForResult(intent, REQUEST_INSTALL);
                return false;
            }
        }
        
        return true;
    }

    private void startEmulator() {
        String appPath = getApplicationContext().getFilesDir().getPath();
        mSession = new VimTermSession (appPath, mUrl, mSettings, "");
        mSession.setFinishCallback(new TermSession.FinishCallback () {
            @Override
            public void onSessionFinish(TermSession session)
            {
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
        return getApplicationContext().getFilesDir()+"/vim/quickbar";
    }
    
    private boolean checkVimRuntime(){
        if(InstallProgress.isInstalled(this))
            return checkPlugins();
        
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
        if(requestCode == REQUEST_INSTALL){
            if(checkVimRuntime()){
                PackageInfo info;
                SharedPreferences.Editor editor = mPrefs.edit();

                try {
                    info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
                    editor.putLong(VimSettings.LASTVERSION_KEY, info.versionCode);
                    editor.commit();
                }catch(Exception e){
                }

                startEmulator();
                updatePrefs();
                mEmulatorView.onResume();
            }
        }else if (requestCode == REQUEST_OPEN){
            if (resultCode == Activity.RESULT_OK) {
                String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
                mUrl = filePath;
            }
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

    private void write(String data) {
        mSession.write(data);
    }

    private void addQuickbarButton(String text) { 
        TextView button = (TextView)getLayoutInflater().inflate(R.layout.quickbutton, (ViewGroup)mButtonBarLayout, false);
        button.setText(text);
        button.setOnClickListener(mClickListener);
        mButtonBarLayout.addView((View)button);
    }

    private void defaultButtons(boolean force) {
        File file = new File(getQuickbarFile());

        if(!force && file.exists()) return;

        try{
            BufferedInputStream is = new BufferedInputStream(getResources().openRawResource(R.raw.quickbar));
            FileWriter fout = new FileWriter(file);
            while(is.available() > 0){
                fout.write(is.read());
            }
            fout.close();
        } catch(Exception e) { 
            Log.e(LOG_TAG, "install default quickbar", e); 
        } 

    }

    private void updateButtons(){
        defaultButtons(false);
        try {
            FileReader fileReader = new FileReader(getQuickbarFile());
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = null;
            int index = 1;
            while ((line = bufferedReader.readLine()) != null) {
                if(line.length() == 0)continue;
                if(index < mButtonBarLayout.getChildCount()){
                    TextView textview = (TextView)mButtonBarLayout.getChildAt(index);
                    textview.setText(line);
                }else{
                    addQuickbarButton(line);
                }
                index++;
            }
            bufferedReader.close();
        }catch (IOException e){
        }
    }

    private void updatePrefs() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ColorScheme colorScheme = new ColorScheme(mSettings.getColorScheme());

        mEmulatorView.setDensity(metrics);
        mEmulatorView.updatePrefs(mSettings);

        mSession.updatePrefs(mSettings);


        mButtonBar.setVisibility(View.GONE);
        ((ViewGroup)mButtonBar).removeView(mButtonBarLayout);

        updateButtons();
        
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
        ((ViewGroup)mButtonBar).addView(mButtonBarLayout);
        if(mSettings.getQuickbarShow())
            mButtonBar.setVisibility(View.VISIBLE);
        else
            mButtonBar.setVisibility(View.GONE);

        if(mSettings.getFullscreen() != ((getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN ) != 0)) {
            doToggleFullscreen();
        }
    }

    @Override
    public void onResume() {
        Log.e(VimTouch.LOG_TAG, "on resume.");
        super.onResume();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings.readPrefs(mPrefs);
        if(mEmulatorView != null){
            updatePrefs();
            mEmulatorView.onResume();

            if(mUrl != null){
                Exec.doCommand("tabnew "+mUrl);
                Exec.updateScreen();
                mUrl = null;
            }
        }
    }

    @Override
    public void onPause() {
        Log.e(VimTouch.LOG_TAG, "on pause.");
        super.onPause();
        if(mEmulatorView != null)
        {
            hideIme();
            mEmulatorView.onPause();
        }
    }

    public void hideIme() {
        if (mEmulatorView == null)
            return;

        InputMethodManager imm = (InputMethodManager)
            getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEmulatorView.getWindowToken(), 0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.e(VimTouch.LOG_TAG, "on new intent.");
        String url = getIntentUrl(intent);
        if(mSession == null){
            mUrl = url;
            startEmulator();
            return;
        }
        if(url == mUrl || url == "") return;
        
        Bundle extras = intent.getExtras();
        int opentype = extras==null?VimFileActivity.FILE_TABNEW:extras.getInt(VimFileActivity.OPEN_TYPE, VimFileActivity.FILE_TABNEW);
        String opencmd;
        switch(opentype){
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
        Exec.doCommand(opencmd+" "+url);
        Exec.updateScreen();
        mUrl = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.e(VimTouch.LOG_TAG, "on configuration changed");
        if(mEmulatorView != null){
            mEmulatorView.updateSize(true);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE), 500);
        }
    }

    private static final int MSG_DIALOG = 1;
    private static final int MSG_UPDATE = 2;
    private static final int MSG_SYNCCLIP = 3;
    private static final int MSG_SETCLIP = 4;
    private static final int MSG_SETCURTAB = 5;
    private static final int MSG_SETTABS = 6;
    private class DialogObj {
        public int type;
        public String title;
        public String message;
        public String buttons;
        public int def_button;
        public String textfield;
    };

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
                activity.realShowDialog(obj.type, obj.title, obj.message,
                                        obj.buttons, obj.def_button,
                                        obj.textfield);
                break;
            case MSG_SYNCCLIP:
                activity.mClipText = clipMgr(activity).getText().toString();
                break;
            case MSG_SETCLIP:
                activity.mClipText = (String)msg.obj;
                clipMgr(activity).setText(activity.mClipText);
                break;
            case MSG_SETCURTAB:
                int n = (int)msg.arg1;
                activity.realSetCurTab(n);
            case MSG_SETTABS:
                String[] array = (String[])msg.obj;
                if(array==null)
                    break;
                ArrayAdapter<CharSequence> adapter = activity.getTabAdapter();
                adapter.clear();
                for (String str: array){
                    adapter.add(str);
                }
                adapter.notifyDataSetChanged();
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    private MsgHandler mHandler = new MsgHandler(this);

    public void showDialog(int type, String title, String message, String buttons, int def_button, String textfield) {
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

    public void syncClipText() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SYNCCLIP));
    }

    public String getClipText() {
        return mClipText;
    }

    public void setClipText(String text) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SETCLIP, text));
    }

    private AlertDialog.Builder actionBuilder;

    private void realShowDialog(int type, String title, String message, String buttons, int def_button, String textfield) {
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
                            Exec.resultDialogState(item+1);
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
        if((attrs.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN ) != 0) {
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }else{
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN; 
            ret = true;
        }
        mSettings.setFullscreen(ret);
        getWindow().setAttributes(attrs); 
        return ret;
    }

    private void downloadFullRuntime() {

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                    DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    Query query = new Query();
                    query.setFilterById(mEnqueue);
                    Cursor c = mDM.query(query);
                    if (c.moveToFirst()) {
                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                            String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            Intent newintent = new Intent(getApplicationContext(), InstallProgress.class);
                            newintent.setData(Uri.parse(uriString));
                            startActivity(newintent);
                        }
                    }
                }
            }
        };
         
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
     
        mDM = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Request request = new Request(Uri.parse("https://github.com/downloads/momodalo/vimtouch/vim.vrz"));
        mEnqueue = mDM.enqueue(request);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_fullscreen).setChecked(mSettings.getFullscreen());
        menu.findItem(R.id.menu_keys).setChecked(mSettings.getQuickbarShow());
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
            Exec.doCommand("tabnew "+getQuickbarFile());
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
        } else if (id == R.id.menu_extra_downloads)  {
            Intent search = new Intent(Intent.ACTION_VIEW);
            search.setData(Uri.parse("market://search?q=VimTouch"));
            search.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(search);
        /*
        } else if (id == R.id.menu_full_vim_runtime)  {
            downloadFullRuntime();
        */
        }else if (id == R.id.menu_new) {
            Intent intent = new Intent(getBaseContext(), VimFileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(VimFileActivity.OPEN_TYPE, VimFileActivity.FILE_NEW);
            startActivity(intent);
        }else if (id == R.id.menu_vnew) {
            Intent intent = new Intent(getBaseContext(), VimFileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(VimFileActivity.OPEN_TYPE, VimFileActivity.FILE_VNEW);
            startActivity(intent);
        }else if (id == R.id.menu_tabnew) {
            Intent intent = new Intent(getBaseContext(), VimFileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(VimFileActivity.OPEN_TYPE, VimFileActivity.FILE_TABNEW);
            startActivity(intent);
        } else if (id == R.id.menu_save) {
            Exec.doCommand("w");
        } else if (id == R.id.menu_keys) {
            if(mButtonBarLayout.isShown()){
                mSettings.setQuickbarShow(false);
                item.setChecked(false);
                mButtonBar.setVisibility(View.GONE);
            }else{
                mSettings.setQuickbarShow(true);
                item.setChecked(true);
                mButtonBar.setVisibility(View.VISIBLE);
            }
        }
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
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

    }

    public ArrayAdapter<CharSequence> getTabAdapter(){
        return mTabAdapter;
    }

    public void realSetCurTab(int n){
        mTabSpinner.setSelection(n);
    }

    public void setCurTab(int n){
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SETCURTAB, n, 0));
    }

    public void setTabs(String[] array){
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SETTABS, array));
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Exec.setTab(pos);
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }
}
