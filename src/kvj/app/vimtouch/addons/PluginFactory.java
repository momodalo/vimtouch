package kvj.app.vimtouch.addons;

import android.content.Context;
import android.util.AttributeSet;
import java.util.ArrayList;

public class PluginFactory extends AddOnsFactory <PluginAddOn> {
    private static final String TAG = "PluginFactory";
    private static final String XML_ASSETS_ATTRIBUTE = "pluginAssetName";
    private static final String XML_MD5_ATTRIBUTE = "pluginAssetMd5";

    private static final PluginFactory msInstance;
    static
    {
        msInstance = new PluginFactory();
    }
    private PluginFactory () {
        super(TAG, "net.momodalo.app.vimtouch.PLUGIN", "net.momodalo.app.vimtouch.plugin", "Plugins", "Plugin");
    }

	protected PluginAddOn createConcreteAddOn(Context context, String prefId, int nameId,
			String description, int sortIndex, AttributeSet attrs){
        String assets = attrs.getAttributeValue(null, XML_ASSETS_ATTRIBUTE);
        String md5 = attrs.getAttributeValue(null, XML_MD5_ATTRIBUTE);
        return new PluginAddOn(context, prefId, nameId, description, sortIndex, assets, md5);
    }

    public static ArrayList<PluginAddOn> getAllPlugins(Context context) {
        return msInstance.getAllAddOns(context);
    }

    public static PluginAddOn getPluginById(String id, Context context){
        return msInstance.getAddOnById(id,context);
    }
}
