package net.momodalo.app.vimtouch.addons;

import android.content.Context;

public class PluginAddOn extends AddOnImpl {
    private String mAssets;
    private String mMd5;
    public PluginAddOn(Context packageContext, String id, int nameResId, String description, int sortIndex, String assets, String md5){
        super(packageContext, id, nameResId, description, sortIndex, md5);
        mAssets = assets;
        mType = "plugin";
    }

    public String getAssetName() {
        return mAssets;
    }
}
