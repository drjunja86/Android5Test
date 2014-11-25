package com.ap.androidltest.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.ap.androidltest.R;
import com.ap.androidltest.fragment.DetailsFragment;


public class DetailsActivity extends ActionBarActivity {

    public static final String IMAGE_RESOURCE_ID = "IMAGE_RESOURCE_ID";
    public static final String CARD_TITLE = "CARD_TITLE";
    public static final String TITLE_TEXT_COLOR = "TITLE_TEXT_COLOR";
    public static final String TITLE_BKG_COLOR = "TITLE_BKG_COLOR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        int imageId = getIntent().getIntExtra(IMAGE_RESOURCE_ID, -1);
        int titleTextColor = getIntent().getIntExtra(TITLE_TEXT_COLOR, -1);
        int titleBkgColor = getIntent().getIntExtra(TITLE_BKG_COLOR, -1);
        String cardTitle = getIntent().getStringExtra(CARD_TITLE);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, DetailsFragment.getInstance(imageId, cardTitle, titleTextColor, titleBkgColor))
                    .commit();
        }
    }
}
