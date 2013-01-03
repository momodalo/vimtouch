package net.momodalo.app.vimtouch.addons;

import android.content.Context;
import java.io.File;

public abstract class AddOnImpl implements AddOn {

	private final String mId;
    private final int mNameResId;
    private final String mDescription;
    private final Context mPackageContext;
    private final int mSortIndex;
    protected String mType;
    
    protected AddOnImpl(Context packageContext, String id, int nameResId, String description, int sortIndex)
    {
    	mId = id;
    	mNameResId = nameResId;
    	mDescription = description;
    	mPackageContext = packageContext;
    	mSortIndex = sortIndex;
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

    protected void initTypeDir(Context context) {
        File dir = new File(context.getFilesDir()+"/installed/"+mType);
        if(!dir.exists()) dir.mkdirs();
    }

}
