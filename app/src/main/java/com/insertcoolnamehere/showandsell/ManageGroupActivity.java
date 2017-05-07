package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiActivity;
import com.insertcoolnamehere.showandsell.logic.Item;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class ManageGroupActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, BrowseFragment.OnListFragmentInteractionListener {

    private static final String LOG_TAG = ManageGroupActivity.class.getSimpleName();

    private FullItemRecyclerViewAdapter adapter;
    private AsyncTask mFetchItemsTask;

    private int mLastItemLoaded;

    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_manage_group);

        View recyclerView = findViewById(R.id.list);
        SwipeRefreshLayout swiper = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swiper.setOnRefreshListener(this);
        swiper.setColorSchemeResources(R.color.colorAccent);

        // Set the adapter
        if (recyclerView instanceof RecyclerView) {
            Context context = recyclerView.getContext();
            mRecyclerView = (RecyclerView) recyclerView;
            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new FullItemRecyclerViewAdapter(Item.managedGroupItems, this);
            mRecyclerView.setAdapter(adapter);
        }

        View errorView = findViewById(R.id.error_not_group_owner);
        errorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCreateGroup();
            }
        });


        updateItems();
    }

    @Override
    public void onRefresh() {
        updateItems();
    }

    public void updateItems() {
        // fetch items from server
        if(mFetchItemsTask == null) {
            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
            String groupId = sharedPref.getString(getString(R.string.saved_group_id), null);

            if (groupId != null) {
                // show progress spinner
                showProgress(true);

                // record index of last item loaded
                mLastItemLoaded = Item.managedGroupItems.size();

                // clear managed group items
                Item.clearManagedItems();

                // fetch all items in the group that this user owns
                mFetchItemsTask = new FetchManagedItemsTask(this).execute();
            }
        }
    }

    /**
     * Shows the progress spinner
     */
    private void showProgress(final boolean show) {
        final SwipeRefreshLayout swiper = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        if(show) {
            swiper.post(new Runnable() {
                @Override
                public void run() {
                    swiper.setRefreshing(true);
                }
            });
        } else {
            swiper.post(new Runnable() {
                @Override
                public void run() {
                    swiper.setRefreshing(false);
                }
            });
        }
    }

    @Override
    public void onListFragmentInteraction(String itemId) {
        // start the ItemDetailActivity and tell it which item to show
        Log.d(LOG_TAG, "inside the onListFragmentInteraction for: "+itemId);
        Intent showItemDetailIntent = new Intent(this, ManageItemActivity.class);
        showItemDetailIntent.putExtra(ItemDetailActivity.ITEM_ID, itemId);
        showItemDetailIntent.putExtra(ItemDetailActivity.OWNER_POWERS, true);
        startActivity(showItemDetailIntent);
    }

    @Override
    public void openChooseGroup() {
        Log.e(LOG_TAG, "Someone tried to abuse me!");
    }

    private void openCreateGroup() {
        Intent intent = new Intent(this, CreateGroupActivity.class);
        startActivity(intent);
    }

    protected URL getAPICall(String id) throws MalformedURLException {
        // construct the URL to fetch a user
        Uri.Builder  builder = new Uri.Builder();
        builder.scheme("http")
                .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                .appendPath("showandsell")
                .appendPath("api")
                .appendPath("items")
                .appendPath("items")
                //.appendPath("itemsinrange")
                .appendQueryParameter("groupId", id)
                //.appendQueryParameter("start", ""+mLastItemLoaded)
                //.appendQueryParameter("end", ""+(mLastItemLoaded+5))
                .build();
        Log.d(LOG_TAG, "Manage group fetch items request: "+builder.toString());
        return new URL(builder.toString());
    }

    private class FetchManagedItemsTask extends AsyncTask<Void, Integer, Integer> {
        /**
         * The Activity within which this AsyncTask runs
         */
        private Activity mParent;

        private final String LOG_TAG = FetchManagedItemsTask.class.getSimpleName();
        private final int NOT_GROUP_OWNER = 3;
        private final int NO_INERNET = 2;
        private final int SUCCESS = 1;
        private final int OTHER_FAILURE = 0;

        FetchManagedItemsTask(Activity parent) {
            mParent = parent;
        }

        protected Integer doInBackground(Void... urls) {
            // variables that we will have to close in try loop
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // the unparsed JSON response from the server
            int responseCode = -1;

            // cancel if task is detached from activity
            if(getApplicationContext() == null)
                return OTHER_FAILURE;
            // check for internet connection
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();

            if(info == null || !info.isConnected()) {
                // if there is no network, inform user through a toast
                mParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mParent, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
                        Log.d(LOG_TAG, "No connection available");
                    }
                });
                return NO_INERNET;
            } else {
                try {
                    //we need the user's user id from saved data
                    SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
                    String userId = savedData.getString(getString(R.string.userId), "NULL");

                    Log.d(LOG_TAG, "Started forming url");
                    // first determine if the user is a group owner or not
                    URL url = new URL(new Uri.Builder().scheme("http")
                            .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("groups")
                            .appendPath("groupwithadmin")
                            .appendQueryParameter("adminId", userId)
                            .build().toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    responseCode = urlConnection.getResponseCode();

                    String groupId="";
                    String line = "";
                    String responseBody = "";
                    if (responseCode == 200) {
                        // parse response as JSON
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        while ((line = reader.readLine()) != null) {
                            responseBody += line + '\n';
                        }

                        JSONObject group = new JSONObject(responseBody);
                        groupId = group.getString("ssGroupId");
                        Log.d(LOG_TAG, "This guy owns group: " + groupId);
                    } else if (responseCode == 404) {
                        return NOT_GROUP_OWNER;
                    }

                    Log.d(LOG_TAG, "finished first api call in manage group");
                    urlConnection.disconnect();

                    // connect to the URL and open the reader
                    urlConnection = (HttpURLConnection) getAPICall(groupId).openConnection();
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // obtain status code
                    responseCode = urlConnection.getResponseCode();
                    if(responseCode == 200) {
                        // read response to get user data from server
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        line = "";
                        responseBody = "";
                        while((line = reader.readLine()) != null) {
                            responseBody += line + '\n';
                        }

                        // parse response as JSON
                        JSONArray items = new JSONArray(responseBody);

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject itemJson = items.getJSONObject(i);

                            Item item = new Item(itemJson.getString("ssItemId"), Item.MANAGE);
                            Log.d(LOG_TAG, "Server contains item #"+item.getGuid());
                            item.setName(itemJson.getString("name"));
                            item.setPrice(itemJson.getDouble("price"));
                            item.setCondition(itemJson.getString("condition"));
                            item.setDescription(itemJson.getString("description"));
                            byte[] imgBytes = Base64.decode(itemJson.getString("thumbnail"), Base64.NO_PADDING);
                            item.setPic(BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length));
                            item.setApproved(itemJson.getBoolean("approved"));
                            Log.d(LOG_TAG, "Item # "+item+" is approved? "+item.isApproved());
                        }

                        return SUCCESS;
                    } else {
                        return OTHER_FAILURE;
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error getting response from server", e);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error parsing JSON", e);
                } finally {
                    // release system resources
                    if(urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if(reader != null) {
                        try {
                            reader.close();
                        } catch(IOException e) {
                            Log.e(LOG_TAG, "Error closing input stream", e);
                        }
                    }
                }
            }

            // if anything goes wrong, return the other failure code
            return OTHER_FAILURE;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == SUCCESS) {
                adapter.notifyDataSetChanged();
                showProgress(false);
                for(Item item: Item.managedGroupItems) {
                    Log.d(LOG_TAG, item.toString()+item.isApproved());
                }

                // if there is a group selected, turn back on recycler view and hide error
                View errorView = findViewById(R.id.error_not_group_owner);
                if(errorView.getVisibility() == View.VISIBLE) {
                    errorView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                }

                mFetchItemsTask = null;
            } else if (result == NOT_GROUP_OWNER) {
                // stop that circle
                showProgress(false);

                // direct user to create a group if they haven't done so yet
                View view = findViewById(R.id.error_not_group_owner);
                view.setVisibility(View.VISIBLE);
                Log.d(LOG_TAG, "showing error view");
                mRecyclerView.setVisibility(View.GONE);
            } else {
                Log.e(LOG_TAG, "It appears that the task failed :(");
                showProgress(false);
                mFetchItemsTask = null;
            }
        }
    }
}
