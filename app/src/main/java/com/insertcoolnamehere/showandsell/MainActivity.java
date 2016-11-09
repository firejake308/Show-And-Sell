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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements DonateFragment.OnDonationListener, BookmarksFragment.OnOpenBookmarkListener{

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

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
            return true;
        } else if(id == R.id.action_logout) {
            logout();
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
            // when the camera app returns a picture to us
            Bundle extras = data.getExtras();
            itemPic = (Bitmap) extras.get("data");
        }
    }

    public void onDonation() {
        EditText descriptionEntry = (EditText) findViewById(R.id.item_description_entry);
        EditText detailsEntry = (EditText) findViewById(R.id.item_details_entry);
        EditText priceEntry = (EditText) findViewById(R.id.item_price_entry);
        Spinner conditionEntry = (Spinner) findViewById(R.id.item_condition_entry);

        String description = descriptionEntry.getText().toString();
        String deets = detailsEntry.getText().toString();
        String price = priceEntry.getText().toString();
        String condition = "Used"; // TODO make this actually work

        new UploadItemTask(this, description, price, condition, deets, itemPic).execute();
    }

    public void onOpenBookmark(String itemId) {

    }

    /**
     * Log out the current user and return to the login screen
     */
    private void logout() {
        // erase username from saved data
        SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = savedData.edit();
        editor.remove(getString(R.string.prompt_username));
        editor.remove(getString(R.string.prompt_password));
        editor.remove(getString(R.string.prompt_first_name));
        editor.remove(getString(R.string.prompt_last_name));
        editor.remove(getString(R.string.userId));
        editor.commit();

        // go back to login
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private Activity mParent;

        public PlaceholderFragment() {

        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_browse, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);

            // get username from saved data
            SharedPreferences savedData = getActivity().getSharedPreferences(getString(R.string.saved_data_file_key),
                    Context.MODE_PRIVATE);
            String firstName = savedData.getString(getString(R.string.prompt_first_name), "undefined");

            textView.setText(getString(R.string.section_format, firstName, getArguments().getInt(ARG_SECTION_NUMBER, 0)));
            return rootView;
        }
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
                    return PlaceholderFragment.newInstance(1);
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
                    String groupId = "12162f04-587f-4ca6-a80d-91e1cc58ffaa"; // TODO let user choose group
                    SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
                    String userId = savedData.getString(getString(R.string.userId), "");
                    String name = mName;
                    String price = mPrice;
                    String condition = mCondition;
                    String description = mDescription;

                    // scale down image and convert to base64
                    Bitmap bmp = Bitmap.createScaledBitmap(mBitmap, 250, 250, false);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 75, stream);
                    byte[] byteArray = stream.toByteArray();
                    String thumbnail = Base64.encodeToString(byteArray,Base64.DEFAULT);

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
    }
}
