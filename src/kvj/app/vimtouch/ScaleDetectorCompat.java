package kvj.app.vimtouch;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by kvorobyev on 11/26/14.
 */
@TargetApi(Build.VERSION_CODES.FROYO)
public class ScaleDetectorCompat implements ScaleGestureDetector.OnScaleGestureListener {

    private final TermView view;
    private final ScaleGestureDetector scaleDetector;

    public ScaleDetectorCompat(TermView view) {
        this.view = view;
        this.scaleDetector = new ScaleGestureDetector(view.getContext(), this);
    }

    public boolean onScale(ScaleGestureDetector detector) {
        float span = detector.getCurrentSpan()/view.mScaleSpan;
        view.setScale(span, span, 0.0f, detector.getFocusY());
        view.invalidate();
        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector detector){
        view.setZoom(false);

        view.mScaleSpan = detector.getCurrentSpan();
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector){
        float span = detector.getCurrentSpan()/view.mScaleSpan;
        view.setScale(1.0f, 1.0f, 0.0f, 0.0f);
        view.mScaleSpan = -1.0f;
        int size = (int)(view.mSettings.getFontSize()*span);
        if (size < 2) size = 2;
        view.mSettings.setFontSize(size);
        view.updatePrefs(view.mSettings);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return scaleDetector.onTouchEvent(ev);
    }
}
