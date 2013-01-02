package net.momodalo.app.vimtouch.addons;

import android.content.Context;
import android.util.AttributeSet;
import java.util.ArrayList;

public class RuntimeFactory extends AddOnsFactory <RuntimeAddOn> {
    private static final String TAG = "RuntimeFactory";
    private static final String XML_ASSETS_ATTRIBUTE = "runtimeAssetName";
    private static final String XML_MD5_ATTRIBUTE = "runtimeAssetMd5";

    private static final RuntimeFactory msInstance;
    static
    {
        msInstance = new RuntimeFactory();
    }
    private RuntimeFactory () {
        super(TAG, "net.momodalo.app.vimtouch.RUNTIME", "net.momodalo.app.vimtouch.runtime", "Runtimes", "Runtime");
    }

	protected RuntimeAddOn createConcreteAddOn(Context context, String prefId, int nameId,
			String description, int sortIndex, AttributeSet attrs){
        String assets = attrs.getAttributeValue(null, XML_ASSETS_ATTRIBUTE);
        String md5 = attrs.getAttributeValue(null, XML_MD5_ATTRIBUTE);
        return new RuntimeAddOn(context, prefId, nameId, description, sortIndex, assets, md5);
    }

    public static ArrayList<RuntimeAddOn> getAllRuntimes(Context context) {
        return msInstance.getAllAddOns(context);
    }

    public static RuntimeAddOn getRuntimeById(String id, Context context){
        return msInstance.getAddOnById(id,context);
    }
}
