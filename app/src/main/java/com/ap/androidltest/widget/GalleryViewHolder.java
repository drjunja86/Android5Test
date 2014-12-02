package com.ap.androidltest.widget;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Created by AP on 02/12/14.
 * Class should be used as default ViewHolder for GalleryRecycler
 */
public class GalleryViewHolder extends RecyclerView.ViewHolder {

    final View.OnClickListener mListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            GalleryRecyclerView grv = mGalleryRecyclerView.get();
            if (grv != null) {
                grv.onItemClick(v, getPosition());
            }
        }

    };
    WeakReference<GalleryRecyclerView> mGalleryRecyclerView;

    public GalleryViewHolder(View itemView, GalleryRecyclerView galleryRecyclerView) {
        super(itemView);
        mGalleryRecyclerView = new WeakReference<>(galleryRecyclerView);
        if (itemView != null) {
            itemView.setOnClickListener(mListener);
        }
    }


}
