package com.ap.androidltest.widget;

import android.graphics.PointF;
import android.support.v7.widget.LinearSmoothScroller;
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

    /* First (top-left) position visible at any point */
    private int mFirstVisiblePosition;
    /* Consistent size applied to all child views */
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;
    /* Metrics for the visible window of our data */
    private int mVisibleColumnCount;
    /* Flag to force current scroll offsets to be ignored on re-layout */
    private boolean mForceClearOffsets;
    private int mFirstItemOffset, mLastItemOffset;

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
            mFirstItemOffset = getCenteredItemOffset();
            mLastItemOffset = 0;

            detachAndScrapView(scrap, recycler);
        }

        //Always update the visible row/column counts
        updateWindowSizing();

        int childLeft;
        int childTop;
        if (getChildCount() == 0) { //First or empty layout
            /*
             * Reset the visible and scroll positions
             */
            mFirstVisiblePosition = 0;
            childLeft = childTop = 0;
        } else if (getVisibleChildCount() > getItemCount()) {
            //Data set is too small to scroll fully, just reset position
            mFirstVisiblePosition = 0;
            childLeft = childTop = 0;
        } else { //Adapter data set changes
            /*
             * Keep the existing initial position, and save off
             * the current scrolled offset.
             */
//            final View leftChild = getChildAt(0);
            if (mForceClearOffsets) {
                childLeft = childTop = 0;
                mForceClearOffsets = false;
            } else {
                childLeft = mFirstItemOffset;
                childTop = 0;
            }

            /*
             * Adjust the visible position if out of bounds in the
             * new layout. This occurs when the new item count in an adapter
             * is much smaller than it was before, and you are scrolled to
             * a location where no items would exist.
             */
            int lastVisiblePosition = positionOfIndex(getVisibleChildCount() - 1);
            if (lastVisiblePosition >= getItemCount()) {
                lastVisiblePosition = (getItemCount() - 1);
                int lastColumn = mVisibleColumnCount - 1;

                //Adjust to align the last position in the bottom-right
                mFirstVisiblePosition = Math.max(lastVisiblePosition - lastColumn, 0);

                childLeft = getHorizontalSpace() - (mDecoratedChildWidth * mVisibleColumnCount);
                childTop = getVerticalSpace() - mDecoratedChildHeight;

                //Correct cases where shifting to the bottom-right overscrolls the top-left
                // This happens on data sets too small to scroll in a direction.
                if (getFirstVisibleColumn() == 0) {
                    childLeft = Math.min(childLeft, 0);
                }
            }
        }

        //Clear all attached views into the recycle bin
        detachAndScrapAttachedViews(recycler);

        //Fill the grid for the initial layout of views
        fillGrid(DIRECTION_NONE, childLeft, childTop, recycler);
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

    /*
     * You must override this method if you would like to support external calls
     * to shift the view to a given adapter position. In our implementation, this
     * is the same as doing a fresh layout with the given position as the top-left
     * (or first visible), so we simply set that value and trigger onLayoutChildren()
     */
    @Override
    public void scrollToPosition(int position) {
        if (position >= getItemCount()) {
            Log.e(TAG, "Cannot scroll to " + position + ", item count is " + getItemCount());
            return;
        }

        //Ignore current scroll offset, snap to top-left
        mForceClearOffsets = true;
        //Set requested position as first visible
        mFirstVisiblePosition = position;
        //Trigger a new view layout
        requestLayout();
    }

    /*
     * You must override this method if you would like to support external calls
     * to animate a change to a new adapter position. The framework provides a
     * helper scroller implementation (LinearSmoothScroller), which we leverage
     * to do the animation calculations.
     */
    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, final int position) {
        if (position >= getItemCount()) {
            Log.e(TAG, "Cannot scroll to " + position + ", item count is " + getItemCount());
            return;
        }

        /*
         * LinearSmoothScroller's default behavior is to scroll the contents until
         * the child is fully visible. It will snap to the top-left or bottom-right
         * of the parent depending on whether the direction of travel was positive
         * or negative.
         */
        LinearSmoothScroller scroller = new LinearSmoothScroller(recyclerView.getContext()) {
            /*
             * LinearSmoothScroller, at a minimum, just need to know the vector
             * (x/y distance) to travel in order to get from the current positioning
             * to the target.
             */
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                final int rowOffset = getGlobalRowOfPosition(targetPosition)
                        - getGlobalRowOfPosition(mFirstVisiblePosition);
                final int columnOffset = getGlobalColumnOfPosition(targetPosition)
                        - getGlobalColumnOfPosition(mFirstVisiblePosition);

                return new PointF(columnOffset * mDecoratedChildWidth, rowOffset * mDecoratedChildHeight);
            }
        };
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
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
//        Log.d(TAG, "getWidth() = " + getWidth());

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
        else if (mLastItemOffset > getCenteredItemOffset())
            mLastItemOffset = getCenteredItemOffset();


        //Optimize the case where the entire data set is too small to scroll
        int viewSpan = getDecoratedRight(bottomView) - getDecoratedLeft(topView);
        if (viewSpan <= getHorizontalSpace()) {
            //We cannot scroll in either direction
            return 0;
        }

//        Log.d(TAG, "First item offset: " + mFirstItemOffset + "   getCenteredItemOffset() = " + getCenteredItemOffset());
//        Log.d(TAG, "Last item offset: " + mLastItemOffset + "   getCenteredItemOffset() = " + getCenteredItemOffset());

        int delta;
        boolean leftBoundReached = getFirstVisibleColumn() == 0 && mFirstItemOffset >= getCenteredItemOffset();
        boolean rightBoundReached = getLastVisibleColumn() >= getTotalColumnCount() && mLastItemOffset >= getCenteredItemOffset();

        if (dx > 0) { // Contents are scrolling left
//            Log.d(TAG, "Contents are scrolling left, rightBoundReached = " + rightBoundReached);
            //Check right bound
            if (rightBoundReached) {
                //If we've reached the last column, enforce limits
                int rightOffset = (getHorizontalSpace() - getCenteredItemOffset()) - getDecoratedRight(bottomView);
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

    /**
     * Private Helpers and Metrics Accessors
     */

    /* Return the overall column index of this position in the global layout */
    private int getGlobalColumnOfPosition(int position) {
        return position % getTotalColumnCount();
    }

    /* Return the overall row index of this position in the global layout */
    private int getGlobalRowOfPosition(int position) {
        return position / getTotalColumnCount();
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
        return getWidth() - getPaddingRight() - getPaddingLeft() - mFirstItemOffset;
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    private int getCenteredItemOffset() {
        return (getWidth() - getPaddingRight() - getPaddingLeft() - mDecoratedChildWidth) / 2;
    }
}
