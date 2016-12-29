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
        mViewPager.setCurrentItem(1);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
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
            double scaleFactor = Math.max(itemPic.getWidth() / 250.0, itemPic.getHeight() / 250.0);
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
                imageTakenYet = true;

                // update text of button
                Button takePicBtn = (Button) findViewById(R.id.upload_img_btn);
                takePicBtn.setText(getString(R.string.prompt_picture_taken));
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "The user didn't actually take a picture");
                Button donateBtn = (Button) findViewById(R.id.donate_btn);
                donateBtn.setEnabled(false);
                imageTakenYet = false;
            }
        }
    }

    @Override
    public void onDonation(String condition) {
        // fetch user-entered data
        EditText descriptionEntry = (EditText) findViewById(R.id.item_description_entry);
        EditText detailsEntry = (EditText) findViewById(R.id.item_details_entry);
        EditText priceEntry = (EditText) findViewById(R.id.item_price_entry);

        String description = descriptionEntry.getText().toString();
        String deets = detailsEntry.getText().toString();
        String price = priceEntry.getText().toString();

        // make sure the user has actually given us all the fields we asked for
        Button donateBtn = (Button) findViewById(R.id.donate_btn);
        if(description.length() == 0) {
            descriptionEntry.setError("Please enter a description");
            donateBtn.setEnabled(false);
            return;
        } else if(deets.length() == 0) {
            detailsEntry.setError("Please provide details");
            donateBtn.setEnabled(false);
            return;
        } else if(price.length() == 0) {
            priceEntry.setError("Please enter a price");
            donateBtn.setEnabled(false);
            return;
        } else if(!imageTakenYet || itemPic == null) {
            Toast.makeText(this, "Please provide a picture", Toast.LENGTH_SHORT).show();
            donateBtn.setEnabled(false);
            return;
        } else {
            donateBtn.setEnabled(true);
        }

        // clear EditTexts
        descriptionEntry.setText("");
        detailsEntry.setText("");
        priceEntry.setText("");

        // no need to save an image anymore
        imageTakenYet = false;

        // go back to browse fragment
        mViewPager.setCurrentItem(1);

        new UploadItemTask(this, description, price, condition, deets, itemPic).execute();
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

    @Override
    public void setGroupOwner(boolean isOwner) {
        Item.setShowUnapproved(isOwner);
        isGroupOwner = isOwner;

        SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = savedData.edit();
        editor.putBoolean(getString(R.string.group_owner_boolean), isOwner);
        editor.apply();
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
                    // donate tab
                    return new DonateFragment();
                case 1:
                    // browse tab
                    return BrowseFragment.newInstance(2);
                case 2:
                    // bookmarks tab
                    return new BookmarksFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "DONATE";
                case 1:
                    return "BROWSE";
                case 2:
                    return "BOOKMARKS";
            }
            return null;
        }
    }

    public class UploadItemTask extends AsyncTask<Bitmap, Void, Boolean> {

        private static final String LOG_TAG = "UploadItemTask";

        private Activity mParent;
        private String mName;
        private String mPrice;
        private String mCondition;
        private String mDescription;
        private Bitmap mBitmap;

        public UploadItemTask(Activity parent, String name, String price, String condition, String description, Bitmap bitmap) {
            mParent = parent;
            mName = name;
            mPrice = price;
            mCondition = condition;
            mDescription = description;
            mBitmap = bitmap;
        }

        @Override
        protected Boolean doInBackground(Bitmap... bmpData) {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if(info == null || !info.isConnected()) {
                // if there is no network, inform user through a toast
                mParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mParent, "No connection available. Try again later.", Toast.LENGTH_SHORT).show();
                        Log.d(LOG_TAG, "No connection available");
                    }
                });
            } else {
                // declare resources to close in finally clause
                HttpURLConnection conn = null;
                BufferedOutputStream out = null;
                try {
                    // form a URL to connect to
                    Uri.Builder builder = new Uri.Builder();
                    String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .scheme("http")
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("items").build().toString();
                    URL url = new URL(uri);

                    // form connection
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setChunkedStreamingMode(0);
                    conn.connect();

                    // get values for item
                    SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
                    String groupId = savedData.getString(getString(R.string.saved_group_id), "d57f3c49-907d-4e6f-ab2c-2e76969b3447");
                    String userId = savedData.getString(getString(R.string.userId), "");
                    String name = mName;
                    String price = mPrice;
                    String condition = mCondition;
                    String description = mDescription;

                    // scale down image and convert to base64
                    Log.d(LOG_TAG, "is mBitmap null: "+(mBitmap == null));
                    double scaleFactor = Math.max(mBitmap.getWidth()/250.0, mBitmap.getHeight()/250.0);
                    Bitmap bmp = Bitmap.createScaledBitmap(mBitmap, (int)(mBitmap.getWidth()/scaleFactor),(int)(mBitmap.getHeight()/scaleFactor), false);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    String thumbnail = Base64.encodeToString(byteArray,Base64.NO_WRAP);

                    // convert item to JSON
                    String body = "";
                    JSONObject item = new JSONObject();
                    item.put("groupId", groupId);
                    item.put("ownerId", userId);
                    item.put("name", name);
                    item.put("price", price);
                    item.put("condition", condition);
                    item.put("description", description);
                    item.put("thumbnail", thumbnail);
                    body = item.toString();
                    Log.d(LOG_TAG, body);

                    // write to output stream
                    out = new BufferedOutputStream(conn.getOutputStream());
                    out.write(body.getBytes());
                    out.flush();

                    // see if post was a success
                    int responseCode = conn.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 201) {
                        Log.d(LOG_TAG, "Post was success");
                        return true;
                    } else if(responseCode == 449) {
                        Log.d(LOG_TAG, "Post failure");
                        return false;
                    } else {
                        Log.e(LOG_TAG, "response Code = "+responseCode);
                    }
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, "Bad URL", e);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error opening URL connection (probably?)", e);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error forming JSON", e);
                } finally {
                    conn.disconnect();
                    try {
                        out.close();
                    } catch(Exception e) {
                        Log.e(LOG_TAG, "Error closing output stream", e);
                    }
                }
            }

            // in case of failure
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // update text of button
            Button takePicBtn = (Button) findViewById(R.id.upload_img_btn);
            takePicBtn.setText(getString(R.string.prompt_image_upload));
        }
    }
}

