package com.ttwishing.stretchablelistview.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class StretchableView extends RelativeLayout implements StretchableListView.StretchListener {

    private ListAdapter listAdapter;

    private final View view;
    private int minHeight;
    private int which = 0;  //0:top 1:bottom

    private int fillHeight;
    private boolean isStretching;

    public StretchableView(Context context) {
        this(context, null);
    }

    public StretchableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StretchableView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StretchableView);
        this.minHeight = a.getDimensionPixelSize(R.styleable.StretchableView_minHeight, 0);
        this.which = a.getInteger(R.styleable.StretchableView_which, 0);
        a.recycle();

        this.view = new View(context);
        addView(this.view, -1, this.minHeight);
    }

    public StretchableView(Context context, int minHeight, int which) {
        super(context);
        this.minHeight = minHeight;
        this.which = which;
        this.view = new View(context);
        addView(this.view, -1, minHeight);
    }

    private final DataSetObserver dataSetObserver = new DataSetObserver() {

        @Override
        public void onChanged() {
            notifyDataSetChanged(false);
        }

        @Override
        public void onInvalidated() {
            notifyDataSetChanged(false);
        }
    };

    protected void setAdapter(ListAdapter listAdapter) {
        if (listAdapter != null)
            listAdapter.unregisterDataSetObserver(this.dataSetObserver);

        if (listAdapter != null)
            listAdapter.registerDataSetObserver(this.dataSetObserver);

        this.listAdapter = listAdapter;
    }

    //计算可见高度
    private int calVisibleHeight() {
        ViewParent parent = getParent();
        if (parent instanceof ListView) {
            ListView listView = (ListView) parent;
            boolean isStackFromBottom = listView.isStackFromBottom(); //false 内容从底部开始填充,此场景都是从头开始
            if ((this.which != 0 || !isStackFromBottom) && (this.which != 1 || isStackFromBottom)) { // (bottom || 从头填充) && (top || 从底填充)
                return Math.max(listView.getHeight() - calListViewContentHeight(listView, this), 0);
            }
        }
        return 0;
    }

    //计算ListView的内容高度,除StretchableView高度外
    protected static int calListViewContentHeight(ListView listView, StretchableView view) {
        int count = listView.getChildCount();
        if (count > 0) {
            int firstTop = listView.getChildAt(0).getTop();
            int lastBottom = listView.getChildAt(count - 1).getBottom();

            //view是否可见
            boolean visible = listView.indexOfChild(view) >= 0;
            if (visible) {
                Log.d("kurt_test", "calListViewContentHeight: " + lastBottom + " - " + firstTop + " - " + view.getHeight() + ", " + listView.getHeight());
                return lastBottom - firstTop - view.getHeight();
            } else {
                Log.d("kurt_test", "calListViewContentHeight: " + lastBottom + " - " + firstTop + ", " + listView.getHeight());
                return lastBottom - firstTop;
            }
        }
        return 0;
    }

    private void notifyDataSetChanged(boolean shouldLayout) {
        this.fillHeight = -1;
        this.isStretching = false;
        setStretchedHeight(0, shouldLayout);
    }

    protected void resetHeight() {
        if (!this.isStretching) {
            this.fillHeight = calVisibleHeight();
            setStretchedHeight(0, true);
        }
    }

    private void setStretchedHeight(int stretch, boolean requestLayout) {
        ViewGroup.LayoutParams lp = this.view.getLayoutParams();
        if (lp != null) {
            int oldHeight = lp.height;
            lp.height = Math.max(this.minHeight, this.fillHeight) + Math.abs(stretch);
            if (requestLayout && oldHeight != lp.height) {
                this.view.requestLayout();
            }
        }
    }

    @Override
    public void onStretchHeightChanged(StretchableListView observableListView, int lastStretch, int stretch, boolean paramBoolean) {
        if (lastStretch < 0 && this.which == 0) { //header
            setStretchedHeight(stretch, true);
        }
        if (lastStretch > 0 && this.which == 1) { //bottom
            setStretchedHeight(stretch, true);
        }

    }

    @Override
    public void onStretchReleaseComplete(StretchableListView observableListView, int lastStretch, boolean paramBoolean) {
        this.isStretching = false;
    }

    @Override
    public void onStretchStart(StretchableListView observableListView, int lastStretch, int stretch, boolean paramBoolean) {
        this.isStretching = true;
    }

    @Override
    public void onStretchReleaseStart(StretchableListView observableListView, int stretch, boolean paramBoolean) {

    }
}
