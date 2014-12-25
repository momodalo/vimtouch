package net.momodalo.app.vimtouch.addons;

import android.content.Context;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public abstract class AddOnImpl implements AddOn {

	private final String mId;
    private final int mNameResId;
    private final String mDescription;
    private final Context mPackageContext;
    private final int mSortIndex;
    protected String mType;
    private String mMd5;
    
    protected AddOnImpl(Context packageContext, String id, int nameResId, String description, int sortIndex, String md5)
    {
    	mId = id;
    	mNameResId = nameResId;
    	mDescription = description;
    	mPackageContext = packageContext;
    	mSortIndex = sortIndex;
        mMd5 = md5;
    }
    
	public final String getId() {
		return mId;
	}

	public final int getNameResId() {
		return mNameResId;
	}

	public final String getDescription() {
		return mDescription;
	}

	public final Context getPackageContext() {
		return mPackageContext;
	}

	public final int getSortIndex() {
		return mSortIndex;
	}

	public String getName() {
		return mPackageContext.getString(mNameResId);
	}

    protected String getTypeDir(Context context) {
        return context.getFilesDir()+"/installed/"+mType;
    }

    public void initTypeDir(Context context) {
        File dir = new File(context.getFilesDir()+"/installed/"+mType);
        if(!dir.exists()) dir.mkdirs();
    }

    protected String getMd5FileName(Context context) {
        return getTypeDir(context)+"/"+getId()+".md5";
    }

    public String getFileListName(Context context) {
        return getTypeDir(context)+"/"+getId()+".list";
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

    public String getAssetMd5() {
        return mMd5;
    }

}
