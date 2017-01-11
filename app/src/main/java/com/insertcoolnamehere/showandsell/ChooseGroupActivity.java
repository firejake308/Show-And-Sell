package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Patrick on 11/20/2016.
 * This activity allows the user to choose and switch groups
 */

public class ChooseGroupActivity extends AppCompatActivity {
    // data for activity
    private String groupName = "";
    private List<String> groupTexts = new ArrayList<>();
    private List<String> groupIds   = new ArrayList<>();
    private Button currentGroup;
    private AsyncTask mFetchGroupsTask;
    private ArrayAdapter<String> mAdapter;

    // creates the choose group activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_group);

        updateGroups();

        // update current group text
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        groupName = sharedPref.getString(getString(R.string.saved_group_name), "No Group Selected");
        currentGroup = (Button) findViewById(R.id.current_group);
        currentGroup.setText(groupName);

        // add group buttons to list
        ListView groups = (ListView) findViewById(R.id.list_of_groups);
        mAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                groupTexts);
        groups.setAdapter(mAdapter);
        groups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // set new group
                setNewGroup(position);
                // update group texts and ids for the list
                updateGroups();
            }
        });
    }

    /**
     * This method will update groups for refreshing and onCreate
     * 11/23/2016
     */
    private void updateGroups() {
        // update group texts and ids for the list
        mFetchGroupsTask = new FetchGroupsTask(this).execute();
    }

    /**
     * This method will change the user's group when a new one is selected
     * 11/23/2016
     */
    private void setNewGroup(int position) {
        // get new group id and name
        String currentGroupId = groupIds.get(position);
        String currentGroupName = groupTexts.get(position);

        // update text in current group button
        currentGroup.setText(currentGroupName);

        // clear browse group items, because that has changed
        Item.browseGroupItems.clear();

        Log.d("ChooseGroupActivity", "curr grp id: "+currentGroupId);

        // update group name and id in the app
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.saved_group_id), currentGroupId);
        editor.putString(getString(R.string.saved_group_name), currentGroupName);
        // also, user may or may not be the owner of this group, so we'll set that to false and let
        // the FetchItemsTask figure it out
        editor.putBoolean(getString(R.string.group_owner_boolean), false);
        editor.commit();
    }

    // get URL for groups
    protected URL getAPICall() throws MalformedURLException {
        // construct the URL to fetch a user
        Uri.Builder  builder = new Uri.Builder();
        builder.scheme("http")
                .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                .appendPath("showandsell")
                .appendPath("api")
                .appendPath("groups")
                .appendPath("allgroups")
                .build();
        return new URL(builder.toString());
    }

    // insert an AsyncTask here, using the ones in LoginActivity or DonateFragment or BrowseFragment as a reference
    private class FetchGroupsTask extends AsyncTask<Void, Integer, Integer> {
        /**
         * The Activity within which this AsyncTask runs
         */
        private Activity mParent;

        private final String LOG_TAG = ChooseGroupActivity.FetchGroupsTask.class.getSimpleName();
        private final int NO_INTERNET = 2;
        private final int SUCCESS = 1;
        private final int OTHER_FAILURE = 0;

        FetchGroupsTask(Activity parent) {
            mParent = parent;
        }

        protected Integer doInBackground(Void... urls) {
            // im gonna copy paste the networking code from Login here
            // variables that we will have to close in try loop
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;


            // the unparsed JSON response from the server
            int responseCode;

            // check for internet connection
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
                return NO_INTERNET;
            } else {
                try {
                    // connect to the URL and open the reader
                    urlConnection = (HttpURLConnection) getAPICall().openConnection();
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // obtain status code
                    responseCode = urlConnection.getResponseCode();
                    if(responseCode == 200) {
                        // read response to get user data from server
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String responseBody = "";
                        String line;
                        while((line = reader.readLine()) != null) {
                            responseBody += line + '\n';
                        }

                        // parse response as JSON
                        JSONArray items = new JSONArray(responseBody);
                        groupTexts.clear();
                        groupIds.clear();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject itemJson = items.getJSONObject(i);

                            groupTexts.add(itemJson.getString("name"));
                            groupIds.add(itemJson.getString("ssGroupId"));
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
            // update list view
            mAdapter.notifyDataSetChanged();
            Log.d(LOG_TAG, "data set changed");
        }
    }
}
