package com.ap.androidltest.widget.decoration;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * ItemDecoration implementation that applies an inset margin
 * around each child of the RecyclerView. The inset value is controlled
 * by a dimension resource.
 * Author: devunwired
 * Edited by: AP
 */
public class InsetDecoration extends RecyclerView.ItemDecoration {

    private int mVerticalInsets, mHorizontalInsets;

    public InsetDecoration(int horizontalInsets, int verticalInsets) {
        mHorizontalInsets = horizontalInsets;
        mVerticalInsets = verticalInsets;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        //We can supply forced insets for each item view here in the Rect
        outRect.set(mHorizontalInsets, mVerticalInsets, mHorizontalInsets, mVerticalInsets);
    }
}
