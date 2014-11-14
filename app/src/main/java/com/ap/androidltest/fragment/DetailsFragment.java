package com.ap.androidltest.fragment;


import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ap.androidltest.R;
import com.ap.androidltest.util.UIUtils;
import com.ap.androidltest.widget.ObservableScrollView;


/**
 * A simple {@link Fragment} subclass.
 */
public class DetailsFragment extends Fragment implements ObservableScrollView.Callbacks {

    private ObservableScrollView mScrollView;
    private int mPhotoHeightPixels, mHeaderHeightPixels, mAddButtonHeightPixels;
//    private LinearLayout mHeaderBox;
    private ImageView mAddButton;
    private float mMaxHeaderElevation;
    private float mFABElevation;
    private FrameLayout mDetails, mImageContainer;

    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            recomputePhotoAndScrollingMetrics();
        }
    };
    private int mImageId;
    private String mCardTitle;
    private Toolbar mToolbar;
    private int mTitleTextColor, mTitleBackgroundColor;

    public DetailsFragment() {
        // Required empty public constructor
    }

    public static DetailsFragment getInstance(int imageId, String cardTitle, int titleColor, int bkgColor) {
        DetailsFragment df = new DetailsFragment();
        df.mImageId = imageId;
        df.mCardTitle = cardTitle;
        df.mTitleTextColor = titleColor;
        df.mTitleBackgroundColor = bkgColor;
        return df;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details, container, false);

        mPhotoHeightPixels = getResources().getDimensionPixelSize(R.dimen.all_image_height);
        mHeaderHeightPixels = getResources().getDimensionPixelSize(R.dimen.all_bar_height);
        mAddButtonHeightPixels = getResources().getDimensionPixelSize(R.dimen.all_add_button_size);
        mFABElevation = getResources().getDimensionPixelSize(R.dimen.fab_elevation);
        mMaxHeaderElevation = getResources().getDimensionPixelSize(
                R.dimen.session_detail_max_header_elevation);

        mToolbar = (Toolbar) view.findViewById(R.id.all_toolbar);

//        mHeaderBox = (LinearLayout) view.findViewById(R.id.all_line);
        mAddButton = (ImageView) view.findViewById(R.id.all_add_button);
        ImageView image = (ImageView) view.findViewById(R.id.all_image);
        mImageContainer = (FrameLayout) view.findViewById(R.id.all_image_container);
//        TextView text = (TextView) view.findViewById(R.id.all_text);
//        TextView title = (TextView) view.findViewById(R.id.all_title);
        mDetails = (FrameLayout) view.findViewById(R.id.all_details);

        Bitmap bm = BitmapFactory.decodeResource(getResources(), mImageId);
        image.setImageBitmap(bm);
//        title.setText(getString(R.string.all_title) + ": " + mCardTitle);
        mToolbar.setTitle(getString(R.string.all_title) + ": " + mCardTitle);
        mToolbar.setSubtitle(getString(R.string.all_subtitle));
        mToolbar.setNavigationIcon(R.drawable.ic_up);
        mToolbar.setTitleTextColor(mTitleTextColor);
        mToolbar.setSubtitleTextColor(mTitleTextColor);
        mToolbar.setBackgroundColor(mTitleBackgroundColor);


        mScrollView = (ObservableScrollView) view.findViewById(R.id.all_scroll_view);
        mScrollView.addCallbacks(this);
        // Inflate the layout for this fragment
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(DetailsFragment.class.getSimpleName(), "On plus button pressed");
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mToolbar.setNavigationOnClickListener(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScrollView == null) {
            return;
        }

        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            //noinspection deprecation
            vto.removeGlobalOnLayoutListener(mGlobalLayoutListener);
        }
    }

    private void recomputePhotoAndScrollingMetrics() {
        ViewGroup.LayoutParams lp;
        lp = mImageContainer.getLayoutParams();
        if (lp.height != mPhotoHeightPixels) {
            lp.height = mPhotoHeightPixels;
            mImageContainer.setLayoutParams(lp);
        }

        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)
                mDetails.getLayoutParams();
        if (mlp.topMargin != mHeaderHeightPixels + mPhotoHeightPixels) {
            mlp.topMargin = mHeaderHeightPixels + mPhotoHeightPixels;
            mDetails.setLayoutParams(mlp);
        }

        onScrollChanged(0, 0); // trigger scroll handling
    }

    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        // Reposition the header bar -- it's normally anchored to the top of the content,
        // but locks to the top of the screen on scroll
        int scrollY = mScrollView.getScrollY();

        float newTop = Math.max(mPhotoHeightPixels, scrollY);
        mToolbar.setTranslationY(newTop);
        mAddButton.setTranslationY(newTop + mHeaderHeightPixels - mAddButtonHeightPixels / 2);

        float gapFillProgress = 1;
        if (mPhotoHeightPixels != 0) {
            gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
                    0,
                    mPhotoHeightPixels), 0), 1);
        }

        ViewCompat.setElevation(mToolbar, gapFillProgress * mMaxHeaderElevation);
        ViewCompat.setElevation(mAddButton, gapFillProgress * mMaxHeaderElevation
                + mFABElevation);

        // Move background photo (parallax effect)
        mImageContainer.setTranslationY(scrollY * 0.5f);
    }


}
