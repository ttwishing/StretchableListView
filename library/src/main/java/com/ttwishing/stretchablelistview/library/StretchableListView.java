package com.ttwishing.stretchablelistview.library;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.ttwishing.stretchablelistview.library.util.ListUtils;

public class StretchableListView extends ListView {

    private static final String TAG = StretchableListView.class.getSimpleName();

    private float lastMotionY = -1.0F;
    private int lastStretch = 0;
    private boolean unStretched;

    private final Set<StretchableView> headerViewSet = new HashSet<>();
    private final Set<StretchableView> footerViewSet = new HashSet<>();

    private AbsListView.OnScrollListener customScrollListener;//用户定义
    private StretchListener stretchListener;//用户定义

    private DefaultGestureListener gestureListener = new DefaultGestureListener();
    private GestureDetector gestureDetector = new GestureDetector(getContext().getApplicationContext(), gestureListener);

    private ListVerticalResizeAnimation resizeAnimation = new ListVerticalResizeAnimation();

    private final DataSetObserver dataSetObserver = new DataSetObserver() {

        @Override
        public void onChanged() {
            resetStretch();
        }

        @Override
        public void onInvalidated() {
            resetStretch();
        }
    };

    private final AbsListView.OnScrollListener mainOnScrollListener = new AbsListView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            Log.d("MainScroll", "onScrollStateChanged: scrollState = " + scrollState + ", lastStretchHeight = " + lastStretch);
            if (resizeAnimation.isScrolling()) {
                return;
            }
            if (scrollState == SCROLL_STATE_IDLE) {
                //滑动空闲
                resetGestureListener();
            } else if (scrollState == SCROLL_STATE_TOUCH_SCROLL && lastStretch != 0) {
                restoreFromStretch(lastStretch);
                lastMotionY = -1.0f;
                lastStretch = 0;
            }

            if (customScrollListener != null) {
                customScrollListener.onScrollStateChanged(view, scrollState);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (customScrollListener != null) {
                customScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            }
        }
    };

    public StretchableListView(Context context) {
        super(context);
        init();
    }

    public StretchableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StretchableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        super.setOnScrollListener(mainOnScrollListener);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        for (StretchableView view : headerViewSet) {
            view.resetHeight();
        }
        for (StretchableView view : footerViewSet) {
            view.resetHeight();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void setOnScrollListener(AbsListView.OnScrollListener onScrollListener) {
        this.customScrollListener = onScrollListener;
        super.setOnScrollListener(mainOnScrollListener);
    }

    public void setStretchListener(StretchListener listener) {
        this.stretchListener = listener;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        for (StretchableView view : headerViewSet) {
            view.setAdapter(adapter);
        }

        for (StretchableView view : footerViewSet) {
            view.setAdapter(adapter);
        }

        ListAdapter listAdapter = getAdapter();
        if (listAdapter != null)
            listAdapter.unregisterDataSetObserver(dataSetObserver);

        if (listAdapter != null)
            listAdapter.registerDataSetObserver(dataSetObserver);
        super.setAdapter(adapter);

    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        resetStretch();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            resetGestureListener();
        }

        this.gestureDetector.onTouchEvent(ev);

        boolean isStretching = false; //包括拉伸和拉伸后的还原
        if (action == MotionEvent.ACTION_DOWN) {
            for (StretchableView view : headerViewSet) {
                view.requestLayout();
            }
            for (StretchableView view : footerViewSet) {
                view.requestLayout();
            }
        } else if (action == MotionEvent.ACTION_MOVE && ev.getPointerCount() == 1) {
            //是否顶部stretch拉伸或还原
            boolean isStretchUp = false;
            if ((getFirstVisiblePosition() == 0 && this.lastStretch < 0) || (isFirstItemVisible() && gestureListener.getScrollDirection() == ListUtils.ScrollDirection.UP)) {
                isStretchUp = true;
            }

            //是否底部stretch拉伸或还原
            boolean isStretchDown = false;
            if ((getLastVisiblePosition() == getCount() - 1 && this.lastStretch > 0) || (isLastItemVisible() && gestureListener.getScrollDirection() == ListUtils.ScrollDirection.DOWN)) {
                isStretchDown = true;
            }

            Log.d("onTouchEvent", "isStretchUp = " + isStretchUp+", "+isStretchDown);

            if (isStretchUp || isStretchDown) {
                this.resizeAnimation.cancel();

                //是否是滑动的起始
                boolean firstStretch = this.lastMotionY < 0;

                int oldStretchHeight = 0;
                if (!firstStretch) {
                    oldStretchHeight = this.lastStretch;
                }

                //确定滑动高度
                int stretchHeight = 0;
                if (!firstStretch) {
                    stretchHeight = (int) (this.lastMotionY - ev.getY()) / 2;
                }

                boolean unStretched = (oldStretchHeight < 0 && stretchHeight >= 0) || (oldStretchHeight > 0 && stretchHeight <= 0);
                if (firstStretch || unStretched) {
                    this.unStretched = true;
                }

                if (firstStretch) {
                    this.lastMotionY = ev.getY();
                }
                if (unStretched) {
                    restoreFromStretch(oldStretchHeight);
                    if (stretchHeight < 0 && isStretchUp) {
                        oldStretchHeight = -1;
                        isStretching = true;
                    } else if (stretchHeight > 0 && isStretchDown) {
                        oldStretchHeight = 1;
                        isStretching = true;
                    } else {
                        isStretching = true;
                        stretchHeight = 0;
                        oldStretchHeight = 0;
                    }
                } else if (oldStretchHeight != stretchHeight) {
                    isStretching = true;
                } else {
                    isStretching = false;
                }

                Log.d("onTouchEvent", "isStretching = " + isStretching);

                this.lastStretch = stretchHeight;
                if (isStretching) {
                    if (this.unStretched) {
                        this.unStretched = false;
                        if (this.stretchListener != null) {
                            this.stretchListener.onStretchStart(this, oldStretchHeight, stretchHeight, true);
                        }
                    }

                    if (this.stretchListener != null) {
                        this.stretchListener.onStretchHeightChanged(this, oldStretchHeight, stretchHeight, true);
                    }
                    if (isStretchDown) {
                        setSelectionFromTop(-1 + getCount(), -getHeight(), false);
                    }
                }
                isStretching = true;
            } else {
                if (this.lastStretch != 0) {
                    isStretching = true;
                } else {
                    isStretching = false;
                }
                this.lastStretch = 0;
                this.lastMotionY = -1;
            }
        } else {
            //将拉伸还原
            if (this.lastStretch != 0) {
                releaseStretch(this.lastStretch);
                isStretching = true;
            } else {
                isStretching = false;
            }
            this.lastStretch = 0;
            this.lastMotionY = -1;
        }
        if (isStretching) {
            return true;
        } else {
            return super.onTouchEvent(ev);
        }
    }

    public void setSelectionFromTop(int position, int y, boolean bool) {
        if (bool) {
            setSelectionFromTop(position, y);
        } else {
            super.setSelectionFromTop(position, y);
        }
    }

    @Override
    public void setSelectionFromTop(int position, int y) {
        int firstVisiblePosition = getFirstVisiblePosition();

        int top = 0;
        View firstChild = getChildAt(0);
        if (firstChild != null) {
            top = firstChild.getTop();
        }

        int height = 100;
        if (firstChild != null) {
            height = firstChild.getHeight();
        }

        if ((position * 100 + y / 100.0F) < (firstVisiblePosition * 100 + 100.0F * top / height)) {
            gestureListener.setScrollDirection(ListUtils.ScrollDirection.UP);
        } else {
            gestureListener.setScrollDirection(ListUtils.ScrollDirection.DOWN);
        }
        super.setSelectionFromTop(position, y);
    }

    @Override
    public void addHeaderView(View v) {
        if (v instanceof StretchableView) {
            headerViewSet.add((StretchableView) v);
        }
        super.addHeaderView(v);
    }

    @Override
    public boolean removeHeaderView(View v) {
        boolean result = super.removeHeaderView(v);
        if (result && v instanceof StretchableView) {
            StretchableView stretchableView = (StretchableView) v;
            stretchableView.setAdapter(null);
            headerViewSet.remove(stretchableView);
        }
        return result;
    }

    @Override
    public void addFooterView(View v) {
        if (v instanceof StretchableView)
            footerViewSet.add((StretchableView) v);
        super.addFooterView(v);
    }

    @Override
    public boolean removeFooterView(View v) {
        boolean result = super.removeFooterView(v);
        if (result && v instanceof StretchableView) {
            StretchableView stretchableView = (StretchableView) v;
            stretchableView.setAdapter(null);
            footerViewSet.remove(stretchableView);
        }
        return result;
    }

    public boolean isFirstOrLastVisible() {
        if (isFirstItemVisible() || isLastItemVisible()) {
            return true;
        }
        return false;
    }

    public boolean isFirstItemVisible() {
        if (getChildCount() == 0)
            return true;
        int firstVisiblePosition = getFirstVisiblePosition();
        int firstPaddingTop = getChildAt(0).getTop() - getListPaddingTop();
        return firstVisiblePosition == 0 && firstPaddingTop >= 0;
    }

    public boolean isLastItemVisible() {
        if (getChildCount() == 0)
            return true;

        int lastPaddingBottom = getHeight() - getPaddingBottom();
        int lastBottom = getChildAt(-1 + getChildCount()).getBottom();
        //最后一个可见item为最后一个 &&
        return getLastVisiblePosition() == getCount() - 1 && lastBottom <= lastPaddingBottom;
    }

    //从当前位置释放
    private void releaseStretch(int stretch) {
        if (stretchListener != null)
            stretchListener.onStretchReleaseStart(this, stretch, true);

        if (stretch > 0) {
            gestureListener.setScrollDirection(ListUtils.ScrollDirection.UP);
        } else {
            gestureListener.setScrollDirection(ListUtils.ScrollDirection.DOWN);
        }
        this.resizeAnimation.configScroll(stretch, 0, 250);
        this.resizeAnimation.start();
    }

    private void resetStretch() {
        if (this.lastStretch != 0)
            restoreFromStretch(this.lastStretch);
        this.lastStretch = 0;
        this.lastMotionY = -1.0F;
    }

    private void restoreFromStretch(int lastStretch) {
        if (this.stretchListener != null) {
            this.stretchListener.onStretchHeightChanged(this, lastStretch, 0, true);
            this.stretchListener.onStretchReleaseComplete(this, lastStretch, true);
        }
    }

    public void resetGestureListener() {
        gestureListener.reset();
    }

    public interface StretchListener {
        //拉伸程度变化
        void onStretchHeightChanged(StretchableListView listView, int lastStretch, int stretch, boolean force);

        //开始拉伸
        void onStretchStart(StretchableListView listView, int lastStretch, int stretch, boolean force);

        //从当前height开始释放/还原
        void onStretchReleaseStart(StretchableListView listView, int stretch, boolean force);

        //拉伸还原完毕
        void onStretchReleaseComplete(StretchableListView listView, int lastStretch, boolean force);

    }

    class ListVerticalResizeAnimation extends VerticalResizeAnimation {

        int lastHeight;

        @Override
        public void complete() {
            Log.d("VerticalResize", "complete: mLastHeight " + lastHeight);
            super.complete();
            if (stretchListener != null) {
                stretchListener.onStretchReleaseComplete(StretchableListView.this, lastHeight, true);
            }

            //同时传递ScrollState为Idle
            mainOnScrollListener.onScrollStateChanged(StretchableListView.this, OnScrollListener.SCROLL_STATE_IDLE);
        }

        @Override
        protected void setHeight(int height) {
            Log.d("VerticalResize", "setHeight: height=" + height + ", mLastHeight " + lastHeight);

            super.setHeight(height);

            if (stretchListener != null && lastStretch != height) {
                stretchListener.onStretchHeightChanged(StretchableListView.this, lastStretch, height, true);
            }
            lastStretch = height;
            if (height != 0) {
                lastHeight = height;
            }

            int firstVisibleItem = StretchableListView.this.getFirstVisiblePosition();
            int lastVisibleItemCount = StretchableListView.this.getLastVisiblePosition();
            int totalItemCount = StretchableListView.this.getCount();

            //同时传递当前是在scroll
            mainOnScrollListener.onScroll(StretchableListView.this, firstVisibleItem, lastVisibleItemCount - firstVisibleItem + 1, totalItemCount);
        }
    }

    /**
     * 确定滑动方向与距离ßß
     */
    static class DefaultGestureListener extends GestureDetector.SimpleOnGestureListener {
        private final Context context = App.getInstance();
        private final int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        // 手势为下滑：为正 手势上滑：为负
        private float scrolledDistance = 0.0F;
        private ListUtils.ScrollDirection scrollDirection = ListUtils.ScrollDirection.DOWN;
        private boolean hasReset = false;

        /**
         * 快递点击
         *
         * @param e ACTION_UP
         * @return
         */
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d("GestureListener", "onSingleTapUp: " + e.getAction());

            reset();
            return super.onSingleTapUp(e);
        }

        /**
         * 点击时
         *
         * @param e ACTION_DOWN
         * @return
         */
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d("GestureListener", "onDown: " + e.getAction());
            reset();
            return super.onDown(e);
        }

        /**
         * @param e1        最初的ACTION_DOWN事件
         * @param e2        最后一个ACTION_MOVE事件
         * @param velocityX
         * @param velocityY
         * @return
         */
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d("GestureListener", "onFling: velocityY = " + velocityY);
            boolean isVertical = false;
            if (Math.abs(velocityY) > Math.abs(velocityX)) {
                //偏纵向滑动
                isVertical = true;
            }
            this.scrollDirection = ListUtils.ScrollDirection.fromVelocity(scrollDirection, velocityX, velocityY, isVertical);

            //手势下滑为正; 手势上滑为负
            this.scrolledDistance = -velocityY;
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        /**
         * @param e1        最初的ACTION_DOWN事件
         * @param e2        ACTION_UP事件
         * @param distanceX
         * @param distanceY
         * @return
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d("GestureListener", "onScroll: scrolledDistance = " + scrolledDistance + ", distanceY = " + distanceY + ", touchSlop = " + touchSlop);

            if (this.hasReset) {
                this.hasReset = false;
                return false;
            }
            float scrolledDistance = Math.abs(distanceY);

            //手势上滑 || 手抛下滑 || 滑动距离不足
            if ((this.scrolledDistance >= 0.0F && distanceY > 0.0F) || (this.scrolledDistance <= 0.0F && distanceY < 0.0F) || scrolledDistance < this.touchSlop) {
                this.scrolledDistance = distanceY + this.scrolledDistance;
            } else {
                this.scrolledDistance = 0;
            }
            if (distanceY < 0.0F) { //手势为向下没去
                this.scrollDirection = ListUtils.ScrollDirection.UP;
            } else {
                this.scrollDirection = ListUtils.ScrollDirection.DOWN;
            }
            Log.d("GestureListener", "scrollDirection = "+scrollDirection);
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        public float getScrolledDistance() {
            return this.scrolledDistance;
        }

        public ListUtils.ScrollDirection getScrollDirection() {
            return this.scrollDirection;
        }

        public void setScrollDirection(ListUtils.ScrollDirection direction) {
            this.scrollDirection = direction;
        }

        public void reset() {
            this.scrolledDistance = 0.0F;
            this.hasReset = true;
        }
    }
}
