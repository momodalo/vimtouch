package net.momodalo.app.vimtouch;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.net.Uri;
import android.content.SharedPreferences;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.math.BigInteger;

public class InstallProgress extends Activity {
    public static final String LOG_TAG = "VIM Installation";
    private Uri mUri;
    private ProgressBar mProgressBar;

    private void installDefaultRuntime() {
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream is = new DigestInputStream(getResources().openRawResource(R.raw.vim),md);
            installZip(is);


            // write md5 bytes
            File md5 = new File(getMD5Filename(this));
            FileWriter fout = new FileWriter(md5);

            BigInteger bi = new BigInteger(1, md.digest());
            String result = bi.toString(16);
            if (result.length() % 2 != 0) 
                result = "0"+result;
            Log.e(LOG_TAG, "compute md5 "+result);
            fout.write(result);
            fout.close();
        } catch(Exception e) { 
            Log.e(LOG_TAG, "install vim runtime or compute md5 error", e); 
        }

        installZip(getResources().openRawResource(R.raw.terminfo));

        installSysVimrc(this);

    }

    private static String getVimrc(Activity activity) {
        return activity.getApplicationContext().getFilesDir()+"/vim/vimrc";
    }

    private static String getMD5Filename( Activity activity) {
        return activity.getApplicationContext().getFilesDir()+"/vim.md5";
    }

    private static boolean checkMD5(Activity activity){
        File md5 = new File(getMD5Filename(activity));
        InputStream ris = activity.getResources().openRawResource(R.raw.vim);

        if(!md5.exists()) return false;

        // read md5 
        try{
            BufferedReader reader = new BufferedReader(new FileReader(md5));

            String saved = reader.readLine();
            if(saved.equals(activity.getResources().getString(R.string.vim_md5))) return true;
        }catch(Exception e){
        }

        return false;

    }

    public static boolean isInstalled(Activity activity){
        File vimrc = new File(getVimrc(activity));
        if(vimrc.exists()){
            // Compare size to make sure the sys vimrc doesn't change
            try{
                if(vimrc.getTotalSpace() != activity.getResources().openRawResource(R.raw.vimrc).available()){
                    installSysVimrc(activity);
                }
            }catch(Exception e){
                installSysVimrc(activity);
            }
            return checkMD5(activity);
        }
        
        return false;
    }

    public static void installSysVimrc(Activity activity) {

        File vimrc = new File(activity.getApplicationContext().getFilesDir()+"/vim/vimrc");

        try{
            BufferedInputStream is = new BufferedInputStream(activity.getResources().openRawResource(R.raw.vimrc));
            FileWriter fout = new FileWriter(vimrc);
            while(is.available() > 0){
                fout.write(is.read());
            }
            fout.close();
        } catch(Exception e) { 
            Log.e(LOG_TAG, "install vimrc", e); 
        } 

        File tmp = new File(activity.getApplicationContext().getFilesDir()+"/tmp");
        tmp.mkdir();
    }

    private void installLocalFile() {
        try {
            File file = new File(mUri.getPath());
            if(file.exists()){
                installZip(new FileInputStream(file));
            }
        }catch (Exception e){
            Log.e(LOG_TAG, "install " + mUri + " error " + e);
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        try {
            mUri = getIntent().getData();
        }catch (Exception e){
            mUri = null;
        }

        setContentView(R.layout.installprogress);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        // Start lengthy operation in a background thread
        new Thread(new Runnable() {
            public void run() {
            Log.e(LOG_TAG, "install " + mUri );
                if(mUri == null){
                    installDefaultRuntime();
                }else if (mUri.getScheme().equals("file")) {
                    installLocalFile();
                }else if (mUri.getScheme().equals("content")){
                    try{
                        InputStream attachment = getContentResolver().openInputStream(mUri);
                        installZip(attachment);
                    }catch(Exception e){
                    }
                }
              
                showNotification();
                finish();
            }
        }).start();
    }

    void showNotification() {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        CharSequence from = "VimTouch";
        CharSequence message = "Vim Runtime install finished";

        Notification notif = new Notification(R.drawable.app_vimtouch, message,
        System.currentTimeMillis());

        notif.setLatestEventInfo(this, from, message, null);
        notif.defaults = Notification.DEFAULT_ALL;

        nm.notify( 0, notif);
    }

    private void installZip(InputStream is) {
        try  { 
            String dirname = getApplicationContext().getFilesDir().getPath();
            ZipInputStream zin = new ZipInputStream(is); 
            ZipEntry ze = null; 
            while ((ze = zin.getNextEntry()) != null) { 
                Log.e(LOG_TAG, "Unzipping " + ze.getName()); 

                if(ze.isDirectory()) { 
                    File file = new File(dirname+"/"+ze.getName());
                    if(!file.isDirectory())
                        file.mkdirs();
                } else { 
                    int size;
                    byte[] buffer = new byte[2048];

                    FileOutputStream fout = new FileOutputStream(dirname+"/"+ze.getName());
                    BufferedOutputStream bufferOut = new BufferedOutputStream(fout, buffer.length);
                    while((size = zin.read(buffer, 0, buffer.length)) != -1) {
                        bufferOut.write(buffer, 0, size);
                    }

                    bufferOut.flush();
                    bufferOut.close(); 
                    zin.closeEntry(); 
                } 

            } 

            byte[] buf = new byte[2048];
            while(is.available() > 0){
                is.read(buf);
            }
            buf = null;

            zin.close(); 
        } catch(Exception e) { 
            Log.e(LOG_TAG, "unzip", e); 
        } 
    }
}
