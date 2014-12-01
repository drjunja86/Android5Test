package com.ap.androidltest.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.ap.androidltest.widget.decoration.InsetDecoration;

/**
 * Created by AP on 01/12/14.
 * This view helps to use RecyclerView as Gallery. By default it uses InsetDecoration as an item
 * decoration with offset equals to 50.
 */
public class GalleryRecyclerView extends RecyclerView {

    private static final String TAG = GalleryRecyclerView.class.getSimpleName();
    private InsetDecoration mDefaultDecoration;
    private GalleryLayoutManager mLayoutManager;
    private RecyclerView.OnScrollListener mScrollListener;
    private OnItemClickListener mItemClickListener;

    public GalleryRecyclerView(Context context) {
        this(context, null);
    }

    public GalleryRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GalleryRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
        setClickable(true);
    }

    private void init() {
        // use a linear layout manager
        mLayoutManager = new GalleryLayoutManager();
        mLayoutManager.setMinimumScale(0.7f);
        mLayoutManager.setMinimumAlpha(0.8f);
        setLayoutManager(mLayoutManager);
        mDefaultDecoration = new InsetDecoration(0, 50);
        addItemDecoration(mDefaultDecoration);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        adapter.getItemCount();
    }

    @Override
    public LayoutManager getLayoutManager() {
        return mLayoutManager;
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        if (layout instanceof GalleryLayoutManager) {
            mLayoutManager = (GalleryLayoutManager) layout;
        } else {
            Log.e(TAG, "You can set only GalleryLayoutManager or class which extends that");
        }
        super.setLayoutManager(layout);
    }

    public InsetDecoration getDefaultDecoration() {
        return mDefaultDecoration;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        super.setOnScrollListener(new OnScrollListener());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        super.setOnScrollListener(null);
    }

    @Override
    public void setOnScrollListener(RecyclerView.OnScrollListener listener) {
        mScrollListener = listener;
    }

    @Override
    public void smoothScrollToPosition(int position) {
        smoothScrollBy(mLayoutManager.getOffsetToItem(position), 0);
    }

    @Override
    public void scrollToPosition(int position) {
        scrollBy(mLayoutManager.getOffsetToItem(position), 0);
    }

    public void onItemClick(View view, int position) {
        int centeredPosition = mLayoutManager.getCurrentCenteredPosition();
        if (centeredPosition == position) {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(view, position);
            }
        } else {
            smoothScrollToPosition(position);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    @SuppressWarnings("UnusedDeclaration")
    public interface OnItemClickListener {
        public void onItemClick(View view, int position);

        public void onItemLongClick(View view, int position);
    }

    private class OnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (mScrollListener != null) mScrollListener.onScrolled(recyclerView, dx, dy);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (mScrollListener != null)
                mScrollListener.onScrollStateChanged(recyclerView, newState);
            if (RecyclerView.SCROLL_STATE_IDLE == newState) {
                int centeredItem = mLayoutManager.getCurrentCenteredPosition();
                smoothScrollBy(mLayoutManager.getOffsetToItem(centeredItem), 0);
            }
        }
    }
}
