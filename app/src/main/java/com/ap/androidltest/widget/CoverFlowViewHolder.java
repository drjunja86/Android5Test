package com.ap.androidltest.widget;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Created by AP on 02/12/14.
 * Class should be used as default ViewHolder for GalleryRecycler
 */
public class CoverFlowViewHolder extends RecyclerView.ViewHolder {

    final View.OnClickListener mListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            CoverFlowRecyclerView grv = mGalleryRecyclerView.get();
            if (grv != null) grv.onItemClick(v, getPosition(), false);
        }

    };

    final View.OnLongClickListener mLongListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            CoverFlowRecyclerView grv = mGalleryRecyclerView.get();
            return grv != null && grv.onItemClick(v, getPosition(), true);
        }
    };

    WeakReference<CoverFlowRecyclerView> mGalleryRecyclerView;

    public CoverFlowViewHolder(View itemView, CoverFlowRecyclerView galleryRecyclerView) {
        super(itemView);
        mGalleryRecyclerView = new WeakReference<>(galleryRecyclerView);
        if (itemView != null) {
            itemView.setOnClickListener(mListener);
            itemView.setOnLongClickListener(mLongListener);
        }
    }


}
