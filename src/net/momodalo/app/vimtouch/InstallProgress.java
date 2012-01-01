package net.momodalo.app.vimtouch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InstallProgress extends Activity {
    public static final String LOG_TAG = "VIM Installation";

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.installprogress);
        // Start lengthy operation in a background thread
        new Thread(new Runnable() {
            public void run() {
                installZip(R.raw.vim);
                installZip(R.raw.terminfo);

                File vimrc = new File(getApplicationContext().getFilesDir()+"/vim/vimrc");

                try{
                    BufferedInputStream is = new BufferedInputStream(getResources().openRawResource(R.raw.vimrc));
                    FileWriter fout = new FileWriter(vimrc);
                    while(is.available() > 0){
                        fout.write(is.read());
                    }
                    fout.close();
                } catch(Exception e) { 
                    Log.e(LOG_TAG, "install vimrc", e); 
                } 

                File tmp = new File(getApplicationContext().getFilesDir()+"/tmp");
                tmp.mkdir();

                finish();
            }
        }).start();
    }

    private void installZip(int resourceId) {
        try  { 
            String dirname = getApplicationContext().getFilesDir().getPath();
            InputStream is = this.getResources().openRawResource(resourceId);
            ZipInputStream zin = new ZipInputStream(is); 
            ZipEntry ze = null; 
            while ((ze = zin.getNextEntry()) != null) { 
                Log.e(LOG_TAG, "Unzipping " + ze.getName()); 

                if(ze.isDirectory()) { 
                    File file = new File(dirname+"/"+ze.getName());
                    if(!file.isDirectory())
                        file.mkdirs();
                } else { 
                    FileOutputStream fout = new FileOutputStream(dirname+"/"+ze.getName());
                    for (int c = zin.read(); c != -1; c = zin.read()) { 
                        fout.write(c); 
                    } 

                    zin.closeEntry(); 
                    fout.close(); 
                } 

            } 
            zin.close(); 
        } catch(Exception e) { 
            Log.e(LOG_TAG, "unzip", e); 
        } 
    }
}
