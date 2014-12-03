package com.ap.androidltest.widget;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

/**
 * Created by AP on 19/11/14.
 * Layout manager for RecycleView to simulate
 */
public class GalleryLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = GalleryLayoutManager.class.getSimpleName();

    /* Fill Direction Constants */
    private static final int DIRECTION_NONE = -1;
    private static final int DIRECTION_START = 0;
    private static final int DIRECTION_END = 1;
    private static final int NOT_SET = Integer.MIN_VALUE;
    /* Flag to force current scroll offsets to be ignored on re-layout */
    private int mFirstItemOffset = NOT_SET, mLastItemOffset = NOT_SET;
    private int mPendingCenteredPosition = NOT_SET;
    /* First (top-left) position visible at any point */
    private int mFirstVisiblePosition = 0;
    /* Consistent size applied to all child views */
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;
    /* Metrics for the visible window of our data */
    private int mVisibleColumnCount;
    private float mMinScale = 0.8f;
    private float mMinAlpha = 1.0f;
    private float mMaxZ = 0.0f;

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

    /*
     * This method is your initial call from the framework. You will receive it when you
     * need to start laying out the initial set of views. This method will not be called
     * repeatedly, so don't rely on it to continually process changes during user
     * interaction.
     *
     * This method will be called when the data set in the adapter changes, so it can be
     * used to update a layout based on a new item count.
     */
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //We have nothing to show for an empty data set but clear any existing views
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        if (getChildCount() == 0) { //First or empty layout
            //Scrap measure one child
            View scrap = recycler.getViewForPosition(0);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);

            /*
             * We make some assumptions in this code based on every child
             * view being the same size (i.e. a uniform grid). This allows
             * us to compute the following values up front because they
             * won't change.
             */
            mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
            mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);
            if (mFirstItemOffset == NOT_SET)
                mFirstItemOffset = getCenteredItemOffset();
            if (mLastItemOffset == NOT_SET)
                mLastItemOffset = 0;

            detachAndScrapView(scrap, recycler);
        }

        //Always update the visible row/column counts
        updateWindowSizing();


        int childLeft;
        int childTop = 0;

        if (mPendingCenteredPosition != NOT_SET) {
            int centeredChild = Math.round((float) getVisibleChildCount() / 2.0f) - 1;
            if (mPendingCenteredPosition <= centeredChild) {
                //situation when most left item should have padding from left
                mFirstItemOffset = getCenteredItemOffset() - mPendingCenteredPosition * mDecoratedChildWidth;
                mFirstVisiblePosition = 0;
                childLeft = mFirstItemOffset;
            } else {
                //situation when there are should be items which go out of screen before the centered item
                mFirstItemOffset = 0;
                mFirstVisiblePosition = mPendingCenteredPosition - centeredChild;
                childLeft = getCenteredItemOffset() - centeredChild * mDecoratedChildWidth;
            }
            mPendingCenteredPosition = NOT_SET;
        } else if (getChildCount() == 0) { //First or empty layout
            if (mFirstVisiblePosition == 0 && mFirstItemOffset > 0) childLeft = mFirstItemOffset;
            else childLeft = 0;
        } else if (getVisibleChildCount() > getItemCount()) {
            //Data set is too small to scroll fully, just reset position
            mFirstVisiblePosition = 0;
            childLeft = 0;
        } else { //Adapter data set changes
            /*
             * Keep the existing initial position, and save off
             * the current scrolled offset.
             */
            if (mFirstItemOffset > 0) childLeft = mFirstItemOffset;
            else childLeft = getDecoratedLeft(getChildAt(0)) - getPaddingLeft();
        }

        //Clear all attached views into the recycle bin
        detachAndScrapAttachedViews(recycler);

        //Fill the grid for the initial layout of views
        fillGrid(DIRECTION_NONE, childLeft, childTop, recycler);
        scaleAllItems();
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
    private void updateWindowSizing() {
        mVisibleColumnCount = (getHorizontalSpace() / mDecoratedChildWidth) + 1;
        if (getHorizontalSpace() % mDecoratedChildWidth > 0) {
            mVisibleColumnCount++;
        }

        //Allow minimum value for small data sets
        if (mVisibleColumnCount > getTotalColumnCount()) {
            mVisibleColumnCount = getTotalColumnCount();
        }
    }

    private void fillGrid(int direction, RecyclerView.Recycler recycler) {
        fillGrid(direction, mFirstItemOffset, 0, recycler);
    }

    private void fillGrid(int direction, int emptyLeft, int emptyTop, RecyclerView.Recycler recycler) {
        if (mFirstVisiblePosition < 0) mFirstVisiblePosition = 0;
        if (mFirstVisiblePosition >= getItemCount()) mFirstVisiblePosition = (getItemCount() - 1);

        /*
         * First, we will detach all existing views from the layout.
         * detachView() is a lightweight operation that we can use to
         * quickly reorder views without a full add/remove.
         */
        SparseArray<View> viewCache = new SparseArray<>(getChildCount());
        int startLeftOffset = getPaddingLeft() + emptyLeft;
        int startTopOffset = getPaddingTop() + emptyTop;
        if (getChildCount() != 0) {
            final View topView = getChildAt(0);
            startLeftOffset = getDecoratedLeft(topView);
            startTopOffset = getDecoratedTop(topView);
            switch (direction) {
                case DIRECTION_START:
                    startLeftOffset -= mDecoratedChildWidth;
                    break;
                case DIRECTION_END:
                    startLeftOffset += mDecoratedChildWidth;
                    break;
            }

            //Cache all views by their existing position, before updating counts
            for (int i = 0; i < getChildCount(); i++) {
                int position = positionOfIndex(i);
                final View child = getChildAt(i);
                viewCache.put(position, child);
            }

            //Temporarily detach all views.
            // Views we still need will be added back at the proper index.
            for (int i = 0; i < viewCache.size(); i++) {
                detachView(viewCache.valueAt(i));
            }
        }

        /*
         * Next, we advance the visible position based on the fill direction.
         * DIRECTION_NONE doesn't advance the position in any direction.
         */
        switch (direction) {
            case DIRECTION_START:
                mFirstVisiblePosition--;
                break;
            case DIRECTION_END:
                mFirstVisiblePosition++;
                break;
        }

        if (mFirstVisiblePosition < 0) mFirstVisiblePosition = 0;

        /*
         * Next, we supply the grid of items that are deemed visible.
         * If these items were previously there, they will simple be
         * re-attached. New views that must be created are obtained
         * from the Recycler and added.
         */
        int leftOffset = startLeftOffset;
        int topOffset = startTopOffset;

        for (int i = 0; i < getVisibleChildCount(); i++) {
            int nextPosition = positionOfIndex(i);

            if (nextPosition >= getItemCount()) {
                //Item space beyond the data set, don't attempt to add a view
                continue;
            }

            //Layout this position
            View view = viewCache.get(nextPosition);
            if (view == null) {
                /*
                 * The Recycler will give us either a newly constructed view,
                 * or a recycled view it has on-hand. In either case, the
                 * view will already be fully bound to the data by the
                 * adapter for us.
                 */
                view = recycler.getViewForPosition(nextPosition);
                addView(view);

                /*
                 * It is prudent to measure/layout each new view we
                 * receive from the Recycler. We don't have to do
                 * this for views we are just re-arranging.
                 */
                measureChildWithMargins(view, 0, 0);
                layoutDecorated(view, leftOffset, topOffset,
                        leftOffset + mDecoratedChildWidth,
                        topOffset + mDecoratedChildHeight);
            } else {
                //Re-attach the cached view at its new index
                attachView(view);
                viewCache.remove(nextPosition);
            }

            if (i % mVisibleColumnCount == (mVisibleColumnCount - 1)) {
                leftOffset = startLeftOffset;
                topOffset += mDecoratedChildHeight;
                //If we wrapped without setting the column count, we've reached it
            } else {
                leftOffset += mDecoratedChildWidth;
            }
        }

        /*
         * Finally, we ask the Recycler to scrap and store any views
         * that we did not re-attach. These are views that are not currently
         * necessary because they are no longer visible.
         */
        for (int i = 0; i < viewCache.size(); i++) {
            recycler.recycleView(viewCache.valueAt(i));
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
     * This method describes how far RecyclerView thinks the contents should scroll horizontally.
     * You are responsible for verifying edge boundaries, and determining if this scroll
     * event somehow requires that new views be added or old views get recycled.
     */
    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        //Take leftmost measurements from the top-left child
        final View topView = getChildAt(0);
        //Take rightmost measurements from the top-right child
        int offsetIndex = 0;
        View bottomView = null;
        while (offsetIndex < mVisibleColumnCount && bottomView == null) {
            offsetIndex++;
            bottomView = getChildAt(mVisibleColumnCount - offsetIndex);
        }
//        Log.d(TAG, "dx = " + dx);
//        Log.d(TAG, "getDecoratedLeft(topView) = " + getDecoratedLeft(topView));
//        Log.d(TAG, "getDecoratedRight(bottomView) = " + getDecoratedRight(bottomView));

        if (getFirstVisibleColumn() == 0) mFirstItemOffset = getDecoratedLeft(topView);
        else mFirstItemOffset = 0;
        if (mFirstItemOffset < 0) mFirstItemOffset = 0;
        else if (mFirstItemOffset > getCenteredItemOffset())
            mFirstItemOffset = getCenteredItemOffset();

//        Log.d(TAG, "getLastVisibleColumn() = " + getLastVisibleColumn() + "   getTotalColumnCount() = " + getTotalColumnCount());
        if (getLastVisibleColumn() >= getTotalColumnCount())
            mLastItemOffset = getHorizontalSpace() - getDecoratedRight(bottomView);
        else mLastItemOffset = 0;
        if (mLastItemOffset < 0) mLastItemOffset = 0;
        else if (mLastItemOffset > getCenteredItemOffset() - getPaddingRight())
            mLastItemOffset = getCenteredItemOffset() - getPaddingRight();


//        Log.d(TAG, "First item offset: " + mFirstItemOffset + "   getCenteredItemOffset() = " + getCenteredItemOffset());
//        Log.d(TAG, "Last item offset: " + mLastItemOffset + "   getCenteredItemOffset() = " + getCenteredItemOffset());

        int delta;
        boolean leftBoundReached = getFirstVisibleColumn() == 0 &&
                mFirstItemOffset >= getCenteredItemOffset();
        boolean rightBoundReached = getLastVisibleColumn() >= getTotalColumnCount() &&
                mLastItemOffset >= getCenteredItemOffset() - getPaddingRight();

        if (dx > 0) { // Contents are scrolling left
//            Log.d(TAG, "Contents are scrolling left, rightBoundReached = " + rightBoundReached);
            //Check right bound
            if (rightBoundReached) {
                //If we've reached the last column, enforce limits
                int rightOffset = (getHorizontalSpace() - getCenteredItemOffset()) - getDecoratedRight(bottomView) + getPaddingRight();
                delta = Math.max(-dx, rightOffset);
            } else {
                //No limits while the last column isn't visible
                delta = -dx;
            }
        } else { // Contents are scrolling right
//            Log.d(TAG, "Contents are scrolling right, leftBoundReached = " + leftBoundReached);
            //Check left bound
            if (leftBoundReached) {
                int leftOffset = -getDecoratedLeft(topView) + getPaddingLeft() + getCenteredItemOffset();
                delta = Math.min(-dx, leftOffset);
            } else {
                delta = -dx;
            }
        }
//        Log.d(TAG, "Delta = " + delta);

        offsetChildrenHorizontal(delta);

        if (dx > 0) {
            if (getDecoratedRight(topView) < 0 && !rightBoundReached) {
                fillGrid(DIRECTION_END, recycler);
            } else if (!rightBoundReached) {
                fillGrid(DIRECTION_NONE, recycler);
            }
        } else {
            if (getDecoratedLeft(topView) > 0 && !leftBoundReached) {
                fillGrid(DIRECTION_START, recycler);
            } else if (!leftBoundReached) {
                fillGrid(DIRECTION_NONE, recycler);
            }
        }

        scaleAllItems();

        /*
         * Return value determines if a boundary has been reached
         * (for edge effects and flings). If returned value does not
         * match original delta (passed in), RecyclerView will draw
         * an edge effect.
         */
        return -delta;
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
        return -1;
    }

    public int getOffsetToItem(int position) {
        if (position < 0 || position >= getTotalColumnCount()) return 0;
        View zeroChild = getChildAt(0);
        if (zeroChild == null) return 0;
        int left = getDecoratedLeft(zeroChild) - getPaddingLeft();
        int zeroChildPosition = positionOfIndex(0);
        int columnsOffset = (position - zeroChildPosition) * mDecoratedChildWidth;
        int offset = left - getCenteredItemOffset() + columnsOffset;
        return (offset >= -1 && offset <= 1) ? 0 : offset;
    }

    private void scaleAllItems() {
        for (int i = 0; i < getVisibleChildCount(); i++) {
            View child = getChildAt(i);
            if (child == null) continue;
            int left = getDecoratedLeft(child);
            int right = getDecoratedRight(child);
            float center = (float) (left + (right - left) / 2);
            float halfScreen = (float) getWidth() / 2;
            float distanceFromCenter = Math.abs(halfScreen - center) / 2; // divided by 4 to start resizing earlier
            float scale = 1 - distanceFromCenter / halfScreen;
            if (scale < 0) scale = 0;
            float alpha = scale;
            float translationZScale = scale;
            if (scale < mMinScale) scale = mMinScale;
            if (alpha < mMinAlpha) alpha = mMinAlpha;
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setAlpha(alpha);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                child.setTranslationZ(mMaxZ * translationZScale);
            } else if (child instanceof CardView) {
                ((CardView) child).setCardElevation(
                        ((CardView) child).getMaxCardElevation() * translationZScale);
            }
        }
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

    /*
     * Mapping between child view indices and adapter data
     * positions helps fill the proper views during scrolling.
     */
    private int positionOfIndex(int childIndex) {
        int row = childIndex / mVisibleColumnCount;
        int column = childIndex % mVisibleColumnCount;

        return mFirstVisiblePosition + (row * getTotalColumnCount()) + column;
    }

    private int getFirstVisibleColumn() {
        return (mFirstVisiblePosition % getTotalColumnCount());
    }

    private int getLastVisibleColumn() {
        return getFirstVisibleColumn() + mVisibleColumnCount;
    }

    private int getVisibleChildCount() {
        return mVisibleColumnCount;
    }

    private int getTotalColumnCount() {
        return getItemCount();
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    private int getCenteredItemOffset() {
        return (getHorizontalSpace() - mDecoratedChildWidth) / 2;
    }


    protected static class SavedState implements Parcelable {
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
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
        private int centerItemPosition;

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
