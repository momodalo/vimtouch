package kvj.app.vimtouch.addons;

import android.content.Context;

public class RuntimeAddOn extends AddOnImpl {
    private String mAssets;
    private static final String TYPE = "runtime";
    public RuntimeAddOn(Context packageContext, String id, int nameResId, String description, int sortIndex, String assets, String md5){
        super(packageContext, id, nameResId, description, sortIndex, md5);
        mAssets = assets;
        mType = "runtime";
    }

    public String getAssetName() {
        return mAssets;
    }

}
