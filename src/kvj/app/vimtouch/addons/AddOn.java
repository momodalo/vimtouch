package kvj.app.vimtouch.addons;

import android.content.Context;

public interface AddOn {
	public static final int INVALID_RES_ID = 0;
	
	String getId();
	int getNameResId();
	String getName();
    String getDescription();
    Context getPackageContext();
    int getSortIndex();
}
