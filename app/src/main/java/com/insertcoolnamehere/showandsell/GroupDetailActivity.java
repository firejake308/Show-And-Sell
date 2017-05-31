package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.insertcoolnamehere.showandsell.logic.ResultCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.insertcoolnamehere.showandsell.logic.ResultCode.NO_INTERNET;
import static com.insertcoolnamehere.showandsell.logic.ResultCode.OTHER_FAILURE;
import static com.insertcoolnamehere.showandsell.logic.ResultCode.SUCCESS;

public class GroupDetailActivity extends AppCompatActivity implements BrowseFragment.OnListFragmentInteractionListener {

    public static final String EXTRA_NAME = "GROUP_NAME";
    public static final String EXTRA_ADDRESS = "GROUP_ADDRESS";
    public static final String EXTRA_LOCATION_DETAIL = "GROUP_LOCATION_DETAIL";
    public static final String EXTRA_RATING = "GROUP_RATING";
    public static final String EXTRA_ID = "GROUP_ID";

    private String mGroupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        // fetch string fields from extras
        String name = getIntent().getStringExtra(EXTRA_NAME);
        String address = getIntent().getStringExtra(EXTRA_ADDRESS);
        String locationDetail = getIntent().getStringExtra(EXTRA_LOCATION_DETAIL);
        double rating = getIntent().getDoubleExtra(EXTRA_RATING, 0.0);
        mGroupId = getIntent().getStringExtra(EXTRA_ID);

        // show group name
        setTitle(name);

        // populate group data displays
        TextView addressView = (TextView) findViewById(R.id.group_detail_address);
        addressView.setText(address);
        TextView locationDetailView = (TextView) findViewById(R.id.group_detail_extra_info);
        locationDetailView.setText(locationDetail);
        RatingBar ratingBar = (RatingBar) findViewById(R.id.rating_bar_display);
        ratingBar.setRating((float) rating);
        Log.d("GroupDetail", "rating "+rating);

        // show group items
        Fragment browseFrag = BrowseFragment.newInstance(2, false, mGroupId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.browse_frag_placeholder, browseFrag).commit();

        // scroll to top
        final ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_view);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_UP);
            }
        });

        // hook up send button to action listener
        Button sendButton = (Button) findViewById(R.id.button_send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptPostRating();
            }
        });

        // hook up donate button to action listener
        FloatingActionButton openDonateBtn = (FloatingActionButton) findViewById(R.id.open_donate_btn);
        final Context cxt = this;
        openDonateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToDonate = new Intent(cxt, DonateActivity.class);
                goToDonate.putExtra(DonateActivity.EXTRA_GROUP_ID, mGroupId);
                goToDonate.putExtra(DonateActivity.EXTRA_GROUP_NAME, getIntent().getStringExtra(EXTRA_NAME));
                startActivity(goToDonate);
            }
        });
    }

    private void attemptPostRating() {
        RatingBar ratingBar = (RatingBar) findViewById(R.id.rating_bar_entry);
        double userRating = ratingBar.getRating();
        new PostRatingTask(this, userRating).execute();
    }

    @Override
    public void onListFragmentInteraction(String itemId) {
        // start the ItemDetailActivity and tell it which item to show
        Intent showItemDetailIntent = new Intent(this, ItemDetailActivity.class);
        showItemDetailIntent.putExtra(ItemDetailActivity.ITEM_ID, itemId);
        showItemDetailIntent.putExtra(ItemDetailActivity.OWNER_POWERS, false);
        startActivity(showItemDetailIntent);
    }

    @Override
    public void openChooseGroup() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.OPEN_CHOOSE_GROUP, true);
        startActivity(intent);
    }

    private class PostRatingTask extends AsyncTask<Void, Integer, ResultCode> {

        private final String LOG_TAG = PostRatingTask.class.getSimpleName();

        private Activity mParent;
        private double rating;

        PostRatingTask(Activity parent, double rating) {
            mParent = parent;
            Log.d(LOG_TAG, "user rating = "+rating);
            this.rating = rating;
        }

        @Override
        public ResultCode doInBackground(Void... params) {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if(info == null || !info.isConnected()) {
                // if there is no network, inform user through a toast
                return ResultCode.NO_INTERNET;
            } else {
                // declare resources to close in finally clause
                HttpURLConnection conn = null;
                try {
                    SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
                    String userId = savedData.getString(getString(R.string.userId), "");
                    String password = savedData.getString(getString(R.string.prompt_password), "");

                    // form a URL to connect to
                    Uri.Builder builder = new Uri.Builder();
                    String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .scheme("http")
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("groups")
                            .appendPath("rategroup")
                            .appendQueryParameter("id", mGroupId)
                            .appendQueryParameter("rating", ((int)rating)+"")
                            .appendQueryParameter("userId", userId)
                            .appendQueryParameter("password", password)
                            .build().toString();
                    URL url = new URL(uri);

                    // form connection
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(false);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setChunkedStreamingMode(0);
                    conn.connect();

                    // see if post was a success
                    int responseCode = conn.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 200) {
                        Log.d(LOG_TAG, "Post was success");
                        return SUCCESS;
                    } else if(responseCode == 449) {
                        Log.d(LOG_TAG, "Group may have been deleted, Post failure");
                        return OTHER_FAILURE;
                    } else {
                        Log.e(LOG_TAG, "response Code = "+responseCode);
                    }
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, "Bad URL", e);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error opening URL connection (probably?)", e);
                } finally {
                    conn.disconnect();
                }
            }

            // in case of failure
            return OTHER_FAILURE;
        }

        @Override
        public void onPostExecute(ResultCode result) {
            if(result == SUCCESS) {
                Toast.makeText(mParent, R.string.rating_success, Toast.LENGTH_SHORT).show();
            } else if (result == NO_INTERNET) {
                Toast.makeText(mParent, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG, "No connection available");
            } else {
                Toast.makeText(mParent, R.string.error_rating, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
