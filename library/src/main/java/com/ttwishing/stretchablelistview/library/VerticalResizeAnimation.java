package com.ttwishing.stretchablelistview.library;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class VerticalResizeAnimation implements Runnable {

    private static final String TAG = VerticalResizeAnimation.class.getCanonicalName();

    private boolean isScrolling = false;
    private Scroller mScroller;
    private int endY;

    private int min;
    private int max;
    private View view;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    public VerticalResizeAnimation() {
        this(null);
    }

    public VerticalResizeAnimation(View view) {
        this(view, new DecelerateInterpolator(App.getInstance(), null));
    }

    public VerticalResizeAnimation(View view, Interpolator interpolator) {
        this.mScroller = new Scroller(App.getInstance(), interpolator);
        this.view = view;
    }

    //配置滑动参数,其后执行start()方法,实现真正的滑动
    public void configScroll(int startY, int endY, int duration) {
        this.mScroller.startScroll(0, startY, 0, endY - startY, duration);
        this.min = Math.min(startY, endY);
        this.max = Math.max(startY, endY);
        this.endY = endY;
    }

    public void start() {
        isScrolling = true;
        mHandler.post(this);
    }

    public void complete() {
    }


    public void cancel() {
        this.mScroller.forceFinished(true);
    }

    public boolean isScrolling() {
        return this.isScrolling;
    }

    @Override
    public void run() {
        if (mScroller.computeScrollOffset()) {
            //滑动中
            int currentY = mScroller.getCurrY();
            if (currentY < min) {
                currentY = min;
            } else if (currentY > max) {
                currentY = max;
            }
            setHeight(currentY);
            mHandler.post(this);
        } else {
            //滑动完成
            isScrolling = false;
            setHeight(endY);
            complete();
        }
    }

    protected void setHeight(int height) {
        if (view != null) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.height = height;
                view.requestLayout();
            }
        }
    }
}
