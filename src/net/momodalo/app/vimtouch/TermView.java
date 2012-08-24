package net.momodalo.app.vimtouch;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.graphics.Canvas;
import android.os.Handler;

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

    private static final int FLING_REFRESH_PERIOD = 50;
    private static final int SCREEN_CHECK_PERIOD = 1000;
    private static final int CURSOR_BLINK_PERIOD = 1000;

    public TermView(Context context, TermSession session, DisplayMetrics metrics) {
        super(context, session, metrics);
        mSession = session;
        mGestureDetector = new GestureDetector(this);
        mScaleDetector = new ScaleGestureDetector(context, this);
    }

    public void updatePrefs(TermSettings settings, ColorScheme scheme) {
        if (scheme == null) {
            scheme = new ColorScheme(settings.getColorScheme());
        }

        setTextSize(settings.getFontSize());
        setCursorStyle(settings.getCursorStyle(), settings.getCursorBlink());
        setUseCookedIME(settings.useCookedIME());
        setColorScheme(scheme);
        setBackKeyCharacter(settings.getBackKeyCharacter());
        setControlKeyCode(settings.getControlKeyCode());
        setFnKeyCode(settings.getFnKeyCode());
    }

    public void updatePrefs(TermSettings settings) {
        updatePrefs(settings, null);
    }

    // Begin GestureDetector.OnGestureListener methods
    /**
     * Our message handler class. Implements a periodic callback.
     */
    private final Handler mHandler = new Handler();

    public void onLongPress(MotionEvent e) {
        setZoom(true);
        invalidate();
    }

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

    float mScaleSpan = (float)-1.0;
    float mPreviousSpan = (float)-1.0;
    

    public boolean onScale(ScaleGestureDetector detector) {
        try{
            float span = detector.getCurrentSpan();
            if(mScaleSpan > span){
                float diff = mPreviousSpan - span;
                if( diff > getCharacterHeight()) {
                    String line = Integer.toString((int)(diff/getCharacterHeight()));
                    mSession.write(line+"dd");
                    mPreviousSpan = span;
                }else if (-diff > getCharacterHeight() ){
                    String line = Integer.toString((int)(-diff/getCharacterHeight()));
                    mSession.write(line+"u");
                    mPreviousSpan = span;
                }
            }else{
                float diff = span - mPreviousSpan;
                if( diff > getCharacterHeight()) {
                    String line = Integer.toString((int)(diff/getCharacterHeight()));
                    mSession.write(line+"p");
                    mPreviousSpan = span;
                }else if (-diff > getCharacterHeight() ){
                    String line = Integer.toString((int)(-diff/getCharacterHeight()));
                    mSession.write(line+"u");
                    mPreviousSpan = span;
                }
            }
        } catch (Exception e){
        }
        Exec.updateScreen();
        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector detector){
        mScaleSpan = detector.getCurrentSpan();;
        mPreviousSpan = mScaleSpan;
        setZoom(false);
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector){
        /*
        try{
            mSession.write('z');
            if(mScaleSpan > detector.getCurrentSpan())
                mSession.write('c');
            else
                mSession.write('o');
            mTermOut.flush();
        } catch (IOException e){
        }
        Exec.updateScreen();
        */
        mScaleSpan = (float)-1.0;
    }

    private float mVelocity = 0;

    private Runnable mFlingRun = new Runnable() {
        public void run() {
            Exec.scrollBy((int)mVelocity);
            Exec.updateScreen();
            if(mVelocity > 0){
                mVelocity -= mVelocity>2?2:mVelocity;
            }else{
                mVelocity -= mVelocity<-2?-2:mVelocity;
            }
            if(mVelocity == 0) return;
            mHandler.postDelayed(this, FLING_REFRESH_PERIOD);
        }
    };

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        mVelocity = -velocityY/(5*getCharacterHeight());
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
        Exec.updateScreen();
        return true;
    }

    // End GestureDetector.OnGestureListener methods
    float mLastY = -1;
    float mLastY2 = -1;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float y = ev.getY();
        float x = ev.getX();
        int action = ev.getAction();
        int fingers = ev.getPointerCount();
        if(action == MotionEvent.ACTION_DOWN && fingers == 1){
        }else if (action == MotionEvent.ACTION_MOVE && fingers == 1 && mScaleSpan < 0.0){
            if(mLastY == -1){
                mLastY = y;
            } else if(mLastY != -1 && Math.abs(y-mLastY) > getCharacterHeight() && !getZoom()){
                Exec.scrollBy((int)((mLastY - y)/getCharacterHeight()));
                mLastY = y;
            }
            Exec.moveCursor( (int)(y/getCharacterHeight()), (int)(x/getCharacterWidth()));
            Exec.updateScreen();
        }else if(action == MotionEvent.ACTION_UP){
            mLastY = -1;
            setZoom(false);
            invalidate();
        }
        if (mScaleSpan < 0.0) 
            mGestureDetector.onTouchEvent(ev);
        return mScaleDetector.onTouchEvent(ev);
    }
}
