package com.ap.androidltest.activity;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ap.androidltest.R;
import com.ap.androidltest.fragment.GalleryFragment;
import com.ap.androidltest.fragment.RecycleViewFragment;


public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private static final String KEY_TOOLBAR_OFFSET_Y = "KEY_TOOLBAR_OFFSET_Y";
    private static final boolean DRAWER_OVER_TOOL_BAR = true;
    private ActionBarDrawerToggle mToggle;
    private ListView mDrawerList;
    private Toolbar mToolbar;
    private int mToolbarHeight;
    private int mToolbarOffsetY = 0;
    private boolean mToolbarShown;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DRAWER_OVER_TOOL_BAR) setContentView(R.layout.activity_main_drawer_over_toolbar);
        else setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        mToolbarHeight = getResources().getDimensionPixelSize(R.dimen.tool_bar_height);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        mToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mToggle);

        mDrawerList = (ListView) findViewById(R.id.navigation_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new String[]{"Screen 1", "Screen 2", "Screen 3"}));

        if (savedInstanceState == null) {
            setFragment(1);
        }
        else mToolbarOffsetY = savedInstanceState.getInt(KEY_TOOLBAR_OFFSET_Y);
        onToolBarShowOrHide(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_TOOLBAR_OFFSET_Y, mToolbarOffsetY);
        super.onSaveInstanceState(outState);
    }

    public void onToolBarShowOrHide(boolean show) {
        if (mToolbarShown == show) return;
        mToolbarShown = show;
        mToolbar.animate()
                .alpha(show?1:0)
                .y(show?0:-mToolbarHeight)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator());
        ViewGroup.MarginLayoutParams mlp =
                (ViewGroup.MarginLayoutParams) mDrawerList.getLayoutParams();
        //noinspection PointlessBooleanExpression,ConstantConditions
        mlp.topMargin = ((show && !DRAWER_OVER_TOOL_BAR) ? mToolbarHeight : 0);
        mDrawerList.setLayoutParams(mlp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mToggle.onOptionsItemSelected(item)) return true;
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDrawerList.setOnItemClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDrawerList.setOnItemClickListener(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setFragment(position);
    }

    private void setFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new RecycleViewFragment();
                break;
            case 1:
                fragment = new GalleryFragment();
                break;
            default:
                fragment = new RecycleViewFragment();
                break;
        }

        getFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();

        mDrawerLayout.closeDrawers();
    }
}
