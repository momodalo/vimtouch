package kvj.app.vimtouch;

import com.lazydroid.autoupdateapk.AutoUpdateApk;

import org.kvj.bravo7.ApplicationContext;

/**
 * Created by kvorobyev on 11/18/14.
 */
public class VimTouchApp extends ApplicationContext {

    private AutoUpdateApk autoUpdateApk = null;

    @Override
    protected void init() {
        autoUpdateApk = new AutoUpdateApk(this);
        autoUpdateApk.setUpdateInterval(AutoUpdateApk.DAYS);
    }
}
