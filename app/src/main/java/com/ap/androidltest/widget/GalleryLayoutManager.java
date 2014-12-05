package com.ap.androidltest.widget;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

/**
 * Created by AP on 19/11/14.
 * Layout manager for RecycleView to simulate
 */
public class GalleryLayoutManager extends BaseGalleryLayoutManager {

    private static final String TAG = GalleryLayoutManager.class.getSimpleName();
    /* Flag to force current scroll offsets to be ignored on re-layout */
    private int mFirstItemOffset = NOT_SET, mLastItemOffset = NOT_SET;

    public GalleryLayoutManager() {
        Log.d(TAG, "Initializing GalleryLayoutManager");
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

        if (isCoverFlow()) onLayoutChildrenCoverFlow(recycler, state);
        else onLayoutChildrenGallery(recycler, state);

        scaleAllItems();
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

        if (isCoverFlow()) return scrollHorizontallyCoverFlowBy(dx, recycler, state);
        return scrollHorizontallyGalleryBy(dx, recycler, state);
    }

    /**
     * Calculates offset to the item with position
     *
     * @param position position of the desired item
     * @return offset in pixels
     */
    public int getOffsetToItem(int position) {
        if (isCoverFlow()) return getOffsetToItemCoverFlow(position);
        return getOffsetToItemGallery(position);
    }

    @Override
    protected int getProperPosition(int position) {
        if (isCoverFlow()) return getProperPositionCoverFlow(position);
        return getProperPositionGallery(position);
    }

    private void onLayoutChildrenCoverFlow(RecyclerView.Recycler recycler, @SuppressWarnings("UnusedParameters") RecyclerView.State state) {
        int childLeft;
        int childTop = 0;

        int centeredChild = Math.round((float) getVisibleChildCount() / 2.0f) - 1;
        if (mPendingCenteredPosition != NOT_SET) {
            //situation when there are should be items which go out of screen before the centered item
            mFirstVisiblePosition = mPendingCenteredPosition - centeredChild;
            childLeft = getCenteredItemOffset() - centeredChild * mDecoratedChildWidth;
            mPendingCenteredPosition = NOT_SET;
        } else if (getChildCount() == 0) {
            mFirstVisiblePosition = getProperPosition(mFirstVisiblePosition - centeredChild);
            childLeft = getCenteredItemOffset() - centeredChild * mDecoratedChildWidth;
        } else { //Adapter data set changes
            /*
             * Keep the existing initial position, and save off
             * the current scrolled offset.
             */
            childLeft = getDecoratedLeft(getChildAt(0)) - getPaddingLeft();
        }

        //Clear all attached views into the recycle bin
        detachAndScrapAttachedViews(recycler);

        //Fill the grid for the initial layout of views
        fillGridCoverFlow(DIRECTION_NONE, childLeft, childTop, recycler);
    }

    private void onLayoutChildrenGallery(RecyclerView.Recycler recycler, @SuppressWarnings("UnusedParameters") RecyclerView.State state) {
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
        fillGridGallery(DIRECTION_NONE, childLeft, childTop, recycler);
    }

    private void fillGridGallery(int direction, int emptyLeft, int emptyTop, RecyclerView.Recycler recycler) {
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

    private void fillGridCoverFlow(int direction, int emptyLeft, int emptyTop, RecyclerView.Recycler recycler) {
        mFirstVisiblePosition = getProperPosition(mFirstVisiblePosition);
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

        mFirstVisiblePosition = getProperPosition(mFirstVisiblePosition);

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

    private int scrollHorizontallyGalleryBy(int dx, RecyclerView.Recycler recycler, @SuppressWarnings("UnusedParameters") RecyclerView.State state) {
        //Take leftmost measurements from the top-left child
        final View topView = getChildAt(0);
        //Take rightmost measurements from the top-right child
        int offsetIndex = 0;
        View bottomView = null;
        while (offsetIndex < mVisibleColumnCount && bottomView == null) {
            offsetIndex++;
            bottomView = getChildAt(mVisibleColumnCount - offsetIndex);
        }

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

        int delta;
        boolean leftBoundReached = getFirstVisibleColumn() == 0 &&
                mFirstItemOffset >= getCenteredItemOffset();
        boolean rightBoundReached = getLastVisibleColumn() >= getTotalColumnCount() &&
                mLastItemOffset >= getCenteredItemOffset() - getPaddingRight();

        if (dx > 0) { // Contents are scrolling left
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
            //Check left bound
            if (leftBoundReached) {
                int leftOffset = -getDecoratedLeft(topView) + getPaddingLeft() + getCenteredItemOffset();
                delta = Math.min(-dx, leftOffset);
            } else {
                delta = -dx;
            }
        }
        offsetChildrenHorizontal(delta);

        if (dx > 0) {
            if (getDecoratedRight(topView) < 0 && !rightBoundReached) {
                fillGridGallery(DIRECTION_END, mFirstItemOffset, 0, recycler);
            } else if (!rightBoundReached) {
                fillGridGallery(DIRECTION_NONE, mFirstItemOffset, 0, recycler);
            }
        } else {
            if (getDecoratedLeft(topView) > 0 && !leftBoundReached) {
                fillGridGallery(DIRECTION_START, mFirstItemOffset, 0, recycler);
            } else if (!leftBoundReached) {
                fillGridGallery(DIRECTION_NONE, mFirstItemOffset, 0, recycler);
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

    private int scrollHorizontallyCoverFlowBy(int dx, RecyclerView.Recycler recycler, @SuppressWarnings("UnusedParameters") RecyclerView.State state) {
        //Take leftmost measurements from the top-left child
        final View topView = getChildAt(0);

        offsetChildrenHorizontal(-dx);

        if (dx > 0) {
            if (getDecoratedRight(topView) < 0) {
                fillGridCoverFlow(DIRECTION_END, 0, 0, recycler);
            }
        } else {
            if (getDecoratedLeft(topView) > 0) {
                fillGridCoverFlow(DIRECTION_START, 0, 0, recycler);
            }
        }

        scaleAllItems();

        /*
         * Return value determines if a boundary has been reached
         * (for edge effects and flings). If returned value does not
         * match original delta (passed in), RecyclerView will draw
         * an edge effect.
         */
        return dx;
    }

    private int getOffsetToItemCoverFlow(int position) {
        if (position < 0 || position >= getTotalColumnCount()) return 0;
        View zeroChild = getChildAt(0);
        if (zeroChild == null) return 0;
        int left = getDecoratedLeft(zeroChild) - getPaddingLeft();
        int zeroChildPosition = positionOfIndex(0);
        int columnsOffset = position - zeroChildPosition;
        if (Math.abs(columnsOffset) > getItemCount() / 2)
            columnsOffset = getItemCount() + columnsOffset;
        int columnsOffsetWidth = columnsOffset * mDecoratedChildWidth;
        int offset = left - getCenteredItemOffset() + columnsOffsetWidth;
        return (offset >= -1 && offset <= 1) ? 0 : offset;
    }

    private int getOffsetToItemGallery(int position) {
        if (position < 0 || position >= getTotalColumnCount()) return 0;
        View zeroChild = getChildAt(0);
        if (zeroChild == null) return 0;
        int left = getDecoratedLeft(zeroChild) - getPaddingLeft();
        int zeroChildPosition = positionOfIndex(0);
        int columnsOffset = (position - zeroChildPosition) * mDecoratedChildWidth;
        int offset = left - getCenteredItemOffset() + columnsOffset;
        return (offset >= -1 && offset <= 1) ? 0 : offset;
    }

    private int getProperPositionGallery(int position) {
        return position;
    }

    private int getProperPositionCoverFlow(int position) {
        if (position < 0) position = getItemCount() + (position % getItemCount());
        else if (position >= getItemCount()) position = position % getItemCount();
        return position;
    }

    private int getFirstVisibleColumn() {
        return (mFirstVisiblePosition % getTotalColumnCount());
    }

    private int getLastVisibleColumn() {
        return getFirstVisibleColumn() + mVisibleColumnCount;
    }

    private boolean isCoverFlow() {
        return mShowItemsInLoop && getVisibleChildCount() < getItemCount();
    }
}
