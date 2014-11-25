package com.ap.androidltest.fragment;


import android.app.Fragment;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ap.androidltest.R;
import com.ap.androidltest.activity.DetailsActivity;
import com.ap.androidltest.activity.MainActivity;
import com.ap.androidltest.widget.GalleryLayoutManager;
import com.ap.androidltest.widget.decoration.InsetDecoration;
import com.bumptech.glide.Glide;


/**
 * A simple {@link android.app.Fragment} subclass.
 */
public class GalleryFragment extends Fragment {

    private static final String TAG = GalleryFragment.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private ImageView mPressedImageView;
    private int mTotalScrollY = 0;

    public GalleryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recycle_view, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new GalleryLayoutManager();
        mRecyclerView.setLayoutManager(layoutManager);
        InsetDecoration dd = new InsetDecoration(50);
        mRecyclerView.addItemDecoration(dd);

        // specify an adapter (see also next example)
        MyAdapter adapter = new MyAdapter(new String[]{"String 1", "String 2", "String 3", "String 4",
                "String 5", "String 6", "String 7", "String 8", "String 9", "String 10", "String 11", "String 12"});
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setOnScrollListener(new OnScrollListener());
        return view;
    }

    private boolean isLandscape() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Rect rect = new Rect();
        display.getRectSize(rect);
        return rect.width() > rect.height();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRecyclerView.setOnScrollListener(new OnScrollListener());
    }

    private void openDetailsForCard(final int imageId, final String title) {
        Palette.generateAsync(BitmapFactory.decodeResource(getResources(), imageId),
                new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        Palette.Swatch vibrant =
                                palette.getVibrantSwatch();
                        if (vibrant != null) {
                            startDetailsActivity(imageId, title, vibrant.getTitleTextColor(), vibrant.getRgb());
                        }
                    }
                });


    }

    private void startDetailsActivity(int imageId, String title, int titleColor, int titleBkgColor) {
        Intent intent = new Intent(getActivity(), DetailsActivity.class);
        intent.putExtra(DetailsActivity.IMAGE_RESOURCE_ID, imageId);
        intent.putExtra(DetailsActivity.CARD_TITLE, title);
        intent.putExtra(DetailsActivity.TITLE_TEXT_COLOR, titleColor);
        intent.putExtra(DetailsActivity.TITLE_BKG_COLOR, titleBkgColor);
        String transitionName = getString(R.string.transition_image);
        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                        mPressedImageView,   // The view which starts the transition
                        transitionName    // The transitionName of the view we’re transitioning to
                );
        ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
    }

    public static interface IViewHolderListener {
        void onCardClick(int position);

        void onFirstButtonClick(int position);

        void onSecondButtonClick(int position);
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private final int[] images = new int[]{R.drawable.img1, R.drawable.img2, R.drawable.img3, R.drawable.img4, R.drawable.img5};
        private String[] mDataset;

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(String[] myDataset) {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recycler_view, parent, false);
            // set the view's size, margins, paddings and layout parameters

            //noinspection UnnecessaryLocalVariable
            ViewHolder holder = new ViewHolder(v, new IViewHolderListener() {
                @Override
                public void onCardClick(int position) {
                    openDetailsForCard(images[position % 5], mDataset[position]);
                }

                @Override
                public void onFirstButtonClick(int position) {
                    Log.d(TAG, "First button clicked for position: " + position);
                }

                @Override
                public void onSecondButtonClick(int position) {
                    Log.d(TAG, "Second button clicked for position: " + position);
                }
            });
            return holder;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.setPosition(position);
            holder.titleText.setText(mDataset[position]);
            holder.button1.setText("Button 1");
            holder.button2.setText("Button 2");
            Glide.with(GalleryFragment.this)
                    .load(images[position % 5])
                    .fitCenter()
                    .into(holder.image);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.length;
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            public TextView titleText;
            public Button button1;
            public Button button2;
            public ImageView image;
            // each data item is just a string in this case
            public CardView cardView;
            private IViewHolderListener mListener;
            private int mPosition;

            public ViewHolder(View v, IViewHolderListener listener) {
                super(v);
                cardView = (CardView) v.findViewById(R.id.card_view);
                titleText = (TextView) v.findViewById(R.id.info_text);
                button1 = (Button) v.findViewById(R.id.card_button_1);
                button1.setTextColor(getResources().getColor(R.color.primary_dark));
                button2 = (Button) v.findViewById(R.id.card_button_2);
                button2.setTextColor(getResources().getColor(R.color.primary));
                image = (ImageView) v.findViewById(R.id.info_image);

                mListener = listener;
                image.setOnClickListener(this);
                button1.setOnClickListener(this);
                button2.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.card_button_1:
                        mListener.onFirstButtonClick(mPosition);
                        break;
                    case R.id.card_button_2:
                        mListener.onSecondButtonClick(mPosition);
                        break;
                    case R.id.info_image:
                        mPressedImageView = (ImageView) v;
                        mListener.onCardClick(mPosition);
                        break;
                }
            }

            public void setPosition(int position) {
                mPosition = position;
            }
        }
    }

    private class OnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (isLandscape()) return;
            if ((mTotalScrollY > 0 && dy < 0) || (mTotalScrollY < 0 && dy > 0)) mTotalScrollY = 0;
            mTotalScrollY += dy;
            if (Math.signum(mTotalScrollY) < 0 && Math.abs(mTotalScrollY) > 250)
                ((MainActivity) getActivity()).onToolBarShowOrHide(true);
            else if (Math.signum(mTotalScrollY) > 0 && Math.abs(mTotalScrollY) > 1000)
                ((MainActivity) getActivity()).onToolBarShowOrHide(false);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }
    }
}
