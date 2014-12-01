package com.ap.androidltest.fragment;


import android.app.Fragment;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ap.androidltest.R;
import com.ap.androidltest.activity.DetailsActivity;
import com.ap.androidltest.widget.GalleryRecyclerView;
import com.bumptech.glide.Glide;


/**
 * A simple {@link android.app.Fragment} subclass.
 */
public class GalleryFragment extends Fragment implements GalleryRecyclerView.OnItemClickListener {

    private static final String TAG = GalleryFragment.class.getSimpleName();
    private final int[] images = new int[]{R.drawable.img1, R.drawable.img2, R.drawable.img3, R.drawable.img4, R.drawable.img5};
    private GalleryRecyclerView mRecyclerView;
    private ImageView mPressedImageView;
    private String[] mDataSet = new String[]{"String 1", "String 2", "String 3", "String 4",
            "String 5", "String 6", "String 7", "String 8", "String 9", "String 10", "String 11", "String 12"};

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
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        mRecyclerView = (GalleryRecyclerView) view.findViewById(R.id.recycler_view);
        // specify an adapter (see also next example)
        MyAdapter adapter = new MyAdapter();
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.getDefaultDecoration().setHorizontalInsets(
                getResources().getDimensionPixelSize(R.dimen.space_between_items));
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        mRecyclerView.setOnItemClickListener(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        mRecyclerView.setOnItemClickListener(this);
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

    @Override
    public void onItemClick(View view, int position) {
        mPressedImageView = (ImageView) view.findViewById(R.id.info_image);
        openDetailsForCard(images[position % 5], mDataSet[position]);
    }

    @Override
    public void onItemLongClick(View view, int position) {
        //action on long click
    }

    public static interface IViewHolderListener {
        void onFirstButtonClick(int position);
        void onSecondButtonClick(int position);
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

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
                public void onFirstButtonClick(int position) {
                    Log.d(TAG, "First button clicked for position: " + position);
                    if (position > 0)
                        mRecyclerView.smoothScrollToPosition(position - 1);
                }

                @Override
                public void onSecondButtonClick(int position) {
                    Log.d(TAG, "Second button clicked for position: " + position);
                    if (position < mDataSet.length - 1)
                        mRecyclerView.smoothScrollToPosition(position + 1);
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
            holder.titleText.setText(mDataSet[position]);
            holder.button1.setText(getText(R.string.description_prev));
            holder.button2.setText(getText(R.string.description_next));
            Glide.with(GalleryFragment.this)
                    .load(images[position % 5])
                    .fitCenter()
                    .into(holder.image);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataSet.length;
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
                button1.setOnClickListener(this);
                button2.setOnClickListener(this);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mRecyclerView.onItemClick(v, mPosition);
                    }
                });
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
                }
            }

            public void setPosition(int position) {
                mPosition = position;
            }
        }
    }
}
