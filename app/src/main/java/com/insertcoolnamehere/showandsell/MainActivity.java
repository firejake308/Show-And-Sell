package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;

import com.insertcoolnamehere.showandsell.logic.Group;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity implements BrowseFragment.OnListFragmentInteractionListener, ChooseGroupFragment.OnChooseGroupListener{

    private static final int REQUEST_IMAGE_CAPTURE = 0;
    public static final String OPEN_CHOOSE_GROUP = "OPEN_CHOOSE_GROUP";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private Bitmap itemPic;
    private boolean imageTakenYet = false;

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private boolean isGroupOwner = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(1);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_browse);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_bookmarks_dark);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_group);

        // highlight the selected tab
        final Activity activity  = this;
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        tab.setIcon(R.drawable.ic_bookmarks);
                        activity.setTitle(R.string.bookmark_fragment_title);
                        break;
                    case 1:
                        tab.setIcon(R.drawable.ic_browse);
                        activity.setTitle(R.string.browse_fragment_title);
                        break;
                    case 2:
                        tab.setIcon(R.drawable.ic_group_light);
                        activity.setTitle(R.string.choose_group_fragment_title);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        tab.setIcon(R.drawable.ic_bookmarks_dark);
                        break;
                    case 1:
                        tab.setIcon(R.drawable.ic_browse_dark);
                        break;
                    case 2:
                        tab.setIcon(R.drawable.ic_group);
                        break;
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // jump to donate
        if (getIntent().getBooleanExtra(OPEN_CHOOSE_GROUP, false))
            mViewPager.setCurrentItem(2);
    }

    @Override
    public void onResume() {
        super.onResume();

        // check if still group owner and set private boolean to reflect that
        // User will still be group owner unless user logs out or changes group
        SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        isGroupOwner = savedData.getBoolean(getString(R.string.group_owner_boolean), false);
        Log.d(LOG_TAG, "when resumed, isGroupOwner: "+isGroupOwner);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if(imageTakenYet) {
            Log.d(LOG_TAG, "I saved the image like a good boy");
            double scaleFactor = 1;
            Bitmap bmp = Bitmap.createScaledBitmap(itemPic, (int) (itemPic.getWidth() / scaleFactor), (int) (itemPic.getHeight() / scaleFactor), false);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            savedInstanceState.putByteArray(getString(R.string.item_image), byteArray);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        byte[] byteArray = savedInstanceState.getByteArray(getString(R.string.item_image));
        if(byteArray != null) {
            itemPic = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageTakenYet = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // save group owner status
        SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = savedData.edit();
        editor.putBoolean(getString(R.string.group_owner_boolean), isGroupOwner);
        editor.commit();
        Log.d(LOG_TAG, "when paused, isGroupOwner: "+isGroupOwner);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent showSettingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(showSettingsIntent);
            return true;
        } else if(id == R.id.action_search) {
            onSearchRequested();
        }

        return super.onOptionsItemSelected(item);
    }

    public void takePic() {
        Intent startCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(startCameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(startCameraIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Deprecated
    public void onDonation(String condition) {

    }

    public void onOpenBookmark(String itemId) {

    }

    @Override
    public void onChooseGroup(Group group) {
        // get group details
        String name = group.getName();
        String address = group.getPickupAddress();
        String extraInfo = group.getExtraInfo();
        String id = group.getId();
        double rating = group.getRating();

        // send details through intent
        Intent intent = new Intent(this, GroupDetailActivity.class);
        intent.putExtra(GroupDetailActivity.EXTRA_NAME, name);
        intent.putExtra(GroupDetailActivity.EXTRA_ADDRESS, address);
        intent.putExtra(GroupDetailActivity.EXTRA_LOCATION_DETAIL, extraInfo);
        intent.putExtra(GroupDetailActivity.EXTRA_RATING, rating);
        intent.putExtra(GroupDetailActivity.EXTRA_ID, id);
        startActivity(intent);
    }

    @Override
    public void onListFragmentInteraction(String itemId) {
        // start the ItemDetailActivity and tell it which item to show
        Intent showItemDetailIntent = new Intent(this, ItemDetailActivity.class);
        showItemDetailIntent.putExtra(ItemDetailActivity.ITEM_ID, itemId);
        showItemDetailIntent.putExtra(ItemDetailActivity.OWNER_POWERS, isGroupOwner);
        startActivity(showItemDetailIntent);
    }

    @Override
    public void openChooseGroup() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.OPEN_CHOOSE_GROUP, true);
        startActivity(intent);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            switch(position) {
                case 0:
                    // browse tab
                    return new BookmarksFragment();
                case 1:
                    // bookmarks tab
                    return BrowseFragment.newInstance(2, true, null);
                case 2:
                    // group tab
                    return new ChooseGroupFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }
}