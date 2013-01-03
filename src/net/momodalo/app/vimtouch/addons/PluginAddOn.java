package net.momodalo.app.vimtouch.addons;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class PluginAddOn extends AddOnImpl {
    private String mAssets;
    private String mMd5;
    public PluginAddOn(Context packageContext, String id, int nameResId, String description, int sortIndex, String assets, String md5){
        super(packageContext, id, nameResId, description, sortIndex);
        mAssets = assets;
        mMd5 = md5;
        mType = "plugin";
    }

    protected String getMd5FileName(Context context) {
        return getTypeDir(context)+"/"+getId()+".md5";
    }

    public boolean isInstalled(Context context){
        String name = getMd5FileName(context); 
        File md5file = new File(name);
        if(!md5file.exists()) return false;

        try{
            BufferedReader reader = new BufferedReader(new FileReader(md5file));

            String saved = reader.readLine();
            if(saved.equals(getAssetMd5())) return true;
        }catch(Exception e){
        }
        return false;
    }

    public void setInstalled( Context context, boolean installed){
        initTypeDir(context);

        String name = getMd5FileName(context); 
        File md5file = new File(name);
        if(installed){
            try{
                FileWriter fout = new FileWriter(md5file);
                fout.write(getAssetMd5());
                fout.close();
            }catch (Exception e){
            }
        }else{
            md5file.delete();
        }
    }

    public String getAssetName() {
        return mAssets;
    }

    public String getAssetMd5() {
        return mMd5;
    }

}
