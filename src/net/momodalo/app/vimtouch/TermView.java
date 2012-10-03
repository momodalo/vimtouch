package net.momodalo.app.vimtouch;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.text.InputType;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;

import java.lang.Runnable;
import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

public class TermView extends EmulatorView implements
        GestureDetector.OnGestureListener,
        ScaleGestureDetector.OnScaleGestureListener {
    private TermSession mSession;
    private int mTopRow = 0; //we don't use termnial scroll
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleDetector;
    private boolean mSingleTapESC;
    private boolean mTouchGesture;
    private VimSettings mSettings;
    private boolean mInserted = false;
    private Runnable mCheckRunnable;
    private Handler mCheckHandler;
    private VimInputConnection mInputConnection = null;
    private boolean mIMEComposing = false;

    private static final int FLING_REFRESH_PERIOD = 100;
    private static final int SCREEN_CHECK_PERIOD = 1000;
    private static final int CURSOR_BLINK_PERIOD = 1000;

    public TermView(Context context, TermSession session, DisplayMetrics metrics) {
        super(context, session, metrics);
        mSession = session;
        mGestureDetector = new GestureDetector(this);
        mScaleDetector = new ScaleGestureDetector(context, this);

        mCheckRunnable = new Runnable() {
            public void run() {
                boolean b = Exec.isInsertMode();
                if (b != mInserted){
                    Context context = getContext();
                    InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.restartInput(TermView.this);
                    mInserted = b;
                }
            }
        };
        mCheckHandler = new Handler();
    }

    public ScaleGestureDetector getScaleDetector ()
    {
        return mScaleDetector;
    }

    public void updatePrefs(VimSettings settings, ColorScheme scheme) {
        if (scheme == null) {
            scheme = new ColorScheme(settings.getColorScheme());
        }

        setTextSize(settings.getFontSize());
        setCursorStyle(settings.getCursorStyle(), settings.getCursorBlink());
        setUseCookedIME(settings.useCookedIME());
        mIMEComposing = settings.useCookedIME();
        setColorScheme(scheme);
        setBackKeyCharacter(settings.getBackKeyCharacter());
        setControlKeyCode(settings.getControlKeyCode());
        setFnKeyCode(settings.getFnKeyCode());
        setZoomBottom(settings.getZoomBottom());
        mSingleTapESC = settings.getSingleTapESC();
        mTouchGesture = settings.getTouchGesture();

        mSettings = settings;
    }

    public void updatePrefs(VimSettings settings) {
        updatePrefs(settings, null);
    }

    // Begin GestureDetector.OnGestureListener methods
    /**
     * Our message handler class. Implements a periodic callback.
     */
    private final Handler mHandler = new Handler();

    public boolean onScroll(MotionEvent e1, MotionEvent e2,
            float distanceX, float distanceY) {
        /*
        distanceY += mScrollRemainder;
        int deltaRows = (int) (distanceY / getCharacterHeight());
        mScrollRemainder = distanceY - deltaRows * getCharacterHeight();
        mTopRow =
            Math.min(0, Math.max(-(mTranscriptScreen
                    .getActiveTranscriptRows()), mTopRow + deltaRows));
        */
        invalidate();
        return true;
    }

    float mScaleSpan = -1.0f;

    public boolean onScale(ScaleGestureDetector detector) {
        float span = detector.getCurrentSpan()/mScaleSpan;
        setScale(span, span, 0.0f, detector.getFocusY());
        invalidate();
        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector detector){
        setZoom(false);

        mScaleSpan = detector.getCurrentSpan();
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector){
        float span = detector.getCurrentSpan()/mScaleSpan;
        setScale(1.0f, 1.0f, 0.0f,0.0f);
        mScaleSpan = -1.0f;
        int size = (int)(mSettings.getFontSize()*span);
        if (size < 2) size = 2;
        mSettings.setFontSize(size);
        updatePrefs(mSettings);
    }

    public boolean onSingleTapUp(MotionEvent ev) {
        if(mSingleTapESC)mSession.write(27);
        return true;
    }

    private float mVelocity = 0;

    private Runnable mFlingRun = new Runnable() {
        public void run() {
            Exec.scrollBy((int)mVelocity);
            if(mVelocity > 0){
                mVelocity -= mVelocity>2?2:mVelocity;
            }else{
                mVelocity -= mVelocity<-2?-2:mVelocity;
            }
            if(mVelocity == 0){
                if(mInputConnection!=null)mInputConnection.notifyTextChange();
                return;
            }
            mHandler.postDelayed(this, FLING_REFRESH_PERIOD);
        }
    };

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        mVelocity = -velocityY/(10*getCharacterHeight());
        mHandler.postDelayed(mFlingRun, FLING_REFRESH_PERIOD);
        return true;
    }

    public boolean onDown(MotionEvent ev) {
        float y = ev.getY();
        float x = ev.getX();
        int fingers = ev.getPointerCount();
        mLastY = y;
        mHandler.removeCallbacks(mFlingRun);

        Exec.moveCursor( (int)(y/getCharacterHeight()), (int)(x/getCharacterWidth()));
        return true;
    }

    // End GestureDetector.OnGestureListener methods
    float mLastY = -1;
    float mLastX = -1;
    float mLastY2 = -1;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        lateCheckInserted();

        if(getSelectingText())
            return super.onTouchEvent(ev);
        float y = ev.getY();
        float x = ev.getX();
        int action = ev.getAction();
        int fingers = ev.getPointerCount();

        if(action == MotionEvent.ACTION_DOWN && fingers == 1){
            mLastY = y;
            mLastX = x;
        }else if (action == MotionEvent.ACTION_MOVE && fingers == 1 && mScaleSpan < 0.0){
            if(mLastX != -1 && Math.abs(x-mLastX) > getCharacterWidth() * 5 && !getZoom()){
                setZoom(true);
                mLastY = -1;
            } else if(mLastY != -1 && Math.abs(y-mLastY) > getCharacterHeight() && !getZoom()){
                if(mTouchGesture){
                    Exec.scrollBy((int)((mLastY - y)/getCharacterHeight()));
                    if(mInputConnection!=null)mInputConnection.notifyTextChange();
                }
                mLastY = y;
                mLastX = -1;
            }
            Exec.moveCursor( (int)(y/getCharacterHeight()), (int)(x/getCharacterWidth()));
        }else if(action == MotionEvent.ACTION_UP){
            mLastY = -1;
            mLastX = -1;
            setZoom(false);
            invalidate();
            if(mInputConnection!=null)mInputConnection.notifyTextChange();
        }
        if (mTouchGesture && mScaleSpan < 0.0) 
            mGestureDetector.onTouchEvent(ev);
        return mScaleDetector.onTouchEvent(ev);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean b = super.onKeyUp(keyCode,event);
        lateCheckInserted();
        return b;
    }

    public void lateCheckInserted(){
        //FIXME check the vim State change lately
        mCheckHandler.removeCallbacks(mCheckRunnable);
        mCheckHandler.postDelayed(mCheckRunnable, 500);
    }

    public InputConnection onCreateInputConnection (EditorInfo outAttrs) {
        if(!Exec.isInsertMode() || !mIMEComposing){
            mInputConnection = null;
            return super.onCreateInputConnection(outAttrs);
        }
        outAttrs.actionLabel = null;
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE;
        mInputConnection = new VimInputConnection(this);
        return mInputConnection;
    }

    public boolean toggleIMEComposing(){
        mIMEComposing = mIMEComposing?false:true;
        Context context = getContext();
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.restartInput(TermView.this);
        return mIMEComposing;
    }


    /*
    public boolean onCheckIsTextEditor () {
        return true;
    }*/
}
