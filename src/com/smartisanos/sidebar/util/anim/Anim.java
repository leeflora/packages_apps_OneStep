package com.smartisanos.sidebar.util.anim;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.view.View;

import com.smartisanos.sidebar.util.LOG;

import java.util.ArrayList;
import java.util.List;

import android.os.Process;

public class Anim {
    private static final LOG log = LOG.getInstance(Anim.class);
    private static final boolean DBG_ANIM = true;
    static {
        if (!DBG_ANIM) {log.close();}
    }

    public final static int QUAD_IN      = AnimInterpolator.QUAD_IN;
    public final static int QUAD_OUT     = AnimInterpolator.QUAD_OUT;
    public final static int QUAD_IN_OUT  = AnimInterpolator.QUAD_IN_OUT;

    public final static int CIRC_IN      = AnimInterpolator.CIRC_IN;
    public final static int CIRC_OUT     = AnimInterpolator.CIRC_OUT;
    public final static int CIRC_IN_OUT  = AnimInterpolator.CIRC_IN_OUT;

    public final static int CUBIC_IN     = AnimInterpolator.CUBIC_IN;
    public final static int CUBIC_OUT    = AnimInterpolator.CUBIC_OUT;
    public final static int CUBIC_IN_OUT = AnimInterpolator.CUBIC_IN_OUT;

    public final static int QUART_IN     = AnimInterpolator.QUART_IN;
    public final static int QUART_OUT    = AnimInterpolator.QUART_OUT;
    public final static int QUART_IN_OUT = AnimInterpolator.QUART_IN_OUT;

    public final static int QUINT_IN     = AnimInterpolator.QUINT_IN;
    public final static int QUINT_OUT    = AnimInterpolator.QUINT_OUT;
    public final static int QUINT_IN_OUT = AnimInterpolator.QUINT_IN_OUT;


    //Android anim name
    private static final String TRANSLATE_X = "translationX";
    private static final String TRANSLATE_Y = "translationY";
    private static final String ROTATION    = "rotation";
    private static final String ROTATION_X  = "rotationX";
    private static final String ROTATION_Y  = "rotationY";
    private static final String SCALE_X     = "scaleX";
    private static final String SCALE_Y     = "scaleY";
    private static final String ALHPA       = "alpha";

    public static final int TRANSLATE = 1001;
    public static final int ROTATE    = 1002;
    public static final int SCALE     = 1003;

    private int pid;

    private Object mView;
    private int animType;
    private int duration;
    private int mInOut;
    private Vector3f mFrom;
    private Vector3f mTo;

    private AnimListener mListener;
    private AnimatorSet mAnimationSet;

    private List<Animator> mAnimList;

    public Anim(View view, int type, int time, int easeInOut, Vector3f from, Vector3f to) {
        if (type != TRANSLATE
                && type != ROTATE
                && type != SCALE) {
            throw new IllegalArgumentException("error anim type ["+type+"]");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("lose from or to");
        }
        pid = Process.myTid();
        log.error("pid ["+pid+"]");
        mView = view;
        animType = type;
        duration = time;
        mInOut = easeInOut;
        mFrom = from;
        mTo = to;
        switch (animType) {
            case TRANSLATE : {
                initTranslate(mFrom, mTo);
                break;
            }
            case ROTATE : {
                break;
            }
            case SCALE : {
                break;
            }
        }
    }

    private void initTranslate(Vector3f from, Vector3f to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("something is null ["+from+"]["+to+"]");
        }
        log.error("initTranslate X ["+from.x+"] => ["+to.x+"], Y ["+from.y+"] => ["+to.y+"]");
        mAnimList = new ArrayList<Animator>();
        if (from.x != to.x) {
            ObjectAnimator animatorX = ObjectAnimator.ofFloat(mView, TRANSLATE_X, from.x, to.x);
            mAnimList.add(animatorX);
        }
        if (from.y != to.y) {
            ObjectAnimator animatorY = ObjectAnimator.ofFloat(mView, TRANSLATE_Y, from.y, to.y);
            mAnimList.add(animatorY);
        }
    }

    public void start() {
        if (mAnimList == null || mAnimList.size() == 0) {
//            throw new IllegalArgumentException("anim size is 0");
            log.error("anim array is empty !");
            return;
        }
        AnimInterpolator.Interpolator interpolator = new AnimInterpolator.Interpolator(mInOut);
        AnimatorSetListener listener = new AnimatorSetListener();
        mAnimationSet = new AnimatorSet();
        mAnimationSet.playTogether(mAnimList);
        mAnimationSet.setDuration(duration);
        mAnimationSet.setInterpolator(interpolator);
        mAnimationSet.addListener(listener);
        mAnimationSet.start();
    }

    private class AnimatorSetListener implements Animator.AnimatorListener {
        private long startTime;
        private long endTime;

        @Override
        public void onAnimationStart(Animator animator) {
            startTime = System.currentTimeMillis();
            if (mListener != null) {
                mListener.onStart();
            }
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            endTime = System.currentTimeMillis();
            if (mListener != null) {
                mListener.onComplete();
            }
//            log.error("anim spend time ["+(endTime - startTime)+"]");
        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    }

    public void cancel() {
        if (mAnimationSet == null) {
            return;
        }
        if (mAnimationSet.isRunning()) {
            mAnimationSet.cancel();
        }
    }

    public void setListener(AnimListener l) {
        mListener = l;
    }
}