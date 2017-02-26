package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.insertcoolnamehere.showandsell.logic.Item;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements DonateFragment.OnDonationListener, BrowseFragment.OnListFragmentInteractionListener{

    private static final int REQUEST_IMAGE_CAPTURE = 0;

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
        mViewPager.setCurrentItem(0);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_browse);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_bookmarks);

        FloatingActionButton openDonateBtn = (FloatingActionButton) findViewById(R.id.openDonateBtn);
        final Context cxt = this;
        openDonateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToDonate = new Intent(cxt, DonateActivity.class); // TODO reset
                startActivity(goToDonate);
            }
        });
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_IMAGE_CAPTURE) {
            try {
                // when the camera app returns a picture to us
                Bundle extras = data.getExtras();
                itemPic = (Bitmap) extras.get("data");
                Log.d(LOG_TAG, "When camera takes picture, height is: "+itemPic.getHeight());
                imageTakenYet = true;

                // update text of button
                Button takePicBtn = (Button) findViewById(R.id.upload_img_btn);
                takePicBtn.setText(getString(R.string.prompt_picture_taken));
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "The user didn't actually take a picture");
                Button donateBtn = (Button) findViewById(R.id.donate_btn);
                imageTakenYet = false;
            }
        }
    }

    @Deprecated
    public void onDonation(String condition) {

    }

    public void onOpenBookmark(String itemId) {

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
        Intent intent = new Intent(this, ChooseGroupActivity.class);
        startActivity(intent);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch(position) {
                case 0:
                    // browse tab
                    return BrowseFragment.newInstance(2);
                case 1:
                    // bookmarks tab
                    return new BookmarksFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }/*

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "BROWSE";
                case 1:
                    return "BOOKMARKS";
            }
            return null;
        }*/
    }
}