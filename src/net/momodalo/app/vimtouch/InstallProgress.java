package net.momodalo.app.vimtouch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InstallProgress extends Activity {
    public static final String LOG_TAG = "VIM Installation";
    private Uri mUri;
    private ProgressBar mProgressBar;

    private void installDefaultRuntime() {
        installZip(getResources().openRawResource(R.raw.vim));
        installZip(getResources().openRawResource(R.raw.terminfo));

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
                if(mUri == null){
                    installDefaultRuntime();
                }else if (mUri.getScheme() == "file") {
                    installLocalFile();
                }else if (mUri.getScheme() == "http") {
                }

                finish();
            }
        }).start();
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
            zin.close(); 
        } catch(Exception e) { 
            Log.e(LOG_TAG, "unzip", e); 
        } 
    }
}
