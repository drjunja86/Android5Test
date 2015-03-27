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

    private static final String TAG = InsetDecoration.class.getSimpleName();
    private int mVerticalInsets, mHorizontalInsets;

    public InsetDecoration(int horizontalInsets, int verticalInsets) {
        mHorizontalInsets = horizontalInsets;
        mVerticalInsets = verticalInsets;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        float scaleX = view.getScaleX();
        //We can supply forced insets for each item view here in the Rect
        outRect.set(mHorizontalInsets, mVerticalInsets, mHorizontalInsets, mVerticalInsets);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setVerticalInsets(int verticalInsets) {
        mVerticalInsets = verticalInsets;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setHorizontalInsets(int horizontalInsets) {
        mHorizontalInsets = horizontalInsets;
    }
}
