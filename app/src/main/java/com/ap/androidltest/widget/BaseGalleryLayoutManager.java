package com.ap.androidltest.widget;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

/**
 * Created by AP on 05/12/14.
 * Base gallery layout manager class
 */
public abstract class BaseGalleryLayoutManager extends RecyclerView.LayoutManager {

    /* Fill Direction Constants */
    protected static final int DIRECTION_NONE = -1;
    protected static final int DIRECTION_START = 0;
    protected static final int DIRECTION_END = 1;
    protected static final int NOT_SET = Integer.MIN_VALUE;
    /* Flag to force current scroll offsets to be ignored on re-layout */
//    private int mFirstItemOffset = NOT_SET, mLastItemOffset = NOT_SET;
    protected int mPendingCenteredPosition = NOT_SET;
    private static final String TAG = BaseGalleryLayoutManager.class.getSimpleName();
    /* First (top-left) position visible at any point */
    protected int mFirstVisiblePosition = 0;
    /* Consistent size applied to all child views */
    protected int mDecoratedChildWidth;
    protected int mDecoratedChildHeight;
    /* Metrics for the visible window of our data */
    protected int mVisibleColumnCount;
    protected boolean mShowItemsInLoop;
    private float mMinScale = 0.8f;
    private float mMinAlpha = 1.0f;
    private float mMaxZ = 0.0f;
    private int mScaleStartDivider = 4;

    public abstract int getOffsetToItem(int position);

    protected abstract int getProperPosition(int position);

    @Override
    public Parcelable onSaveInstanceState() {
        final SavedState state = new SavedState(SavedState.EMPTY_STATE);
        state.centerItemPosition = getCurrentCenteredPosition();
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState pendingState = (SavedState) state;
        mPendingCenteredPosition = pendingState.centerItemPosition;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        //Completely scrap the existing layout
        removeAllViews();
    }

    /*
     * Rather than continuously checking how many views we can fit
     * based on scroll offsets, we simplify the math by computing the
     * visible grid as what will initially fit on screen, plus one.
     */
    protected void updateWindowSizing() {
        mVisibleColumnCount = (getHorizontalSpace() / mDecoratedChildWidth) + 1;
        if (getHorizontalSpace() % mDecoratedChildWidth > 0) {
            mVisibleColumnCount++;
        }

        //Allow minimum value for small data sets
        if (mVisibleColumnCount > getTotalColumnCount()) {
            mVisibleColumnCount = getTotalColumnCount();
        }
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, final int position) {
        Log.e(TAG, "Method smoothScrollToPosition is not supported. Use smoothScrollBy");
    }

    /*
     * Use this method to tell the RecyclerView if scrolling is even possible
     * in the horizontal direction.
     */
    @Override
    public boolean canScrollHorizontally() {
        //We do allow scrolling
        return true;
    }

    /*
     * Use this method to tell the RecyclerView if scrolling is even possible
     * in the vertical direction.
     */
    @Override
    public boolean canScrollVertically() {
        //We do allow scrolling
        return false;
    }

    /*
     * We must override this method to provide the default layout
     * parameters that each child view will receive when added.
     */
    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    /*
     * This is a helper method used by RecyclerView to determine
     * if a specific child view can be returned.
     */
    @Override
    public View findViewByPosition(int position) {
        for (int i = 0; i < getChildCount(); i++) {
            if (positionOfIndex(i) == position) {
                return getChildAt(i);
            }
        }

        return null;
    }

    public int getCurrentCenteredPosition() {
        View child;
        for (int i = 0; i < getChildCount(); i++) {
            child = getChildAt(i);
            if (getDecoratedRight(child) > getHorizontalSpace() / 2) {
                return positionOfIndex(i);
            }
        }

        if (mPendingCenteredPosition != NOT_SET) return mPendingCenteredPosition;
        return 0;
    }

    public void setMinimumScale(float minScale) {
        if (minScale < 0) minScale = 0;
        else if (minScale > 1.0f) minScale = 1.0f;
        mMinScale = minScale;
        scaleAllItems();
    }

    public void setMinimumAlpha(float minAlpha) {
        if (minAlpha < 0) minAlpha = 0;
        else if (minAlpha > 1.0f) minAlpha = 1.0f;
        mMinAlpha = minAlpha;
        scaleAllItems();
    }

    public void setMaxZ(float maxZ) {
        if (maxZ < 0) maxZ = 0;
        mMaxZ = maxZ;
        scaleAllItems();
    }

    public void setScaleStartDivider(int scaleStartDivider) {
        if (scaleStartDivider < 1) scaleStartDivider = 1;
        mScaleStartDivider = scaleStartDivider;
        scaleAllItems();
    }

    protected void scaleAllItems() {
        for (int i = 0; i < getVisibleChildCount(); i++) {
            View child = getChildAt(i);
            if (child == null) continue;
            float scale = getScaleForChild(child, false);
            float alpha = scale;
            float translationZScale = scale;
            if (scale < mMinScale) scale = mMinScale;
            if (alpha < mMinAlpha) alpha = mMinAlpha;
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setAlpha(alpha);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                child.setTranslationZ(mMaxZ * translationZScale);
            } else {
                ViewCompat.setTranslationZ(child, mMaxZ * translationZScale);
            }
        }
    }

    protected float getScaleForChild(View child, boolean considerMin) {
        int left = getDecoratedLeft(child);
        int right = getDecoratedRight(child);
        float center = (float) (left + (right - left) / 2);
        float halfScreen = (float) getWidth() / 2;
        float distanceFromCenter = Math.abs(halfScreen - center) / mScaleStartDivider;
        float scale = 1 - distanceFromCenter / halfScreen;
        if (scale < 0) scale = 0;
        if (considerMin && scale < mMinScale) scale = mMinScale;
        return scale;
    }

    /**
     * Mapping between child view indices and adapter data
     * positions helps fill the proper views during scrolling.
     *
     * @param childIndex index of the current child
     * @return position
     */
    protected int positionOfIndex(int childIndex) {
        return getProperPosition(mFirstVisiblePosition + childIndex % mVisibleColumnCount);
    }

    protected int getVisibleChildCount() {
        return mVisibleColumnCount;
    }

    protected int getTotalColumnCount() {
        return getItemCount();
    }

    protected int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    protected int getCenteredItemOffset() {
        return (getHorizontalSpace() - mDecoratedChildWidth) / 2;
    }

    public boolean isShowItemsInLoop() {
        return mShowItemsInLoop;
    }

    public void setShowItemsInLoop(boolean show) {
        mPendingCenteredPosition = getCurrentCenteredPosition();
        mShowItemsInLoop = show;
        //Completely scrap the existing layout
        removeAllViews();
        requestLayout();
    }

    protected static class SavedState implements Parcelable {
        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        protected static final SavedState EMPTY_STATE = new SavedState();
        public int centerItemPosition;

        private SavedState() {
        }

        protected SavedState(Parcelable superState) {
            if (superState == null) {
                throw new IllegalArgumentException("superState must not be null");
            }
        }

        protected SavedState(Parcel in) {
            centerItemPosition = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(centerItemPosition);
        }
    }

}
