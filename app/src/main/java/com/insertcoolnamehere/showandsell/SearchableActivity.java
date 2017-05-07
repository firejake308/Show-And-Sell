package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.insertcoolnamehere.showandsell.logic.Item;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

public class SearchableActivity extends AppCompatActivity {
    private ArrayList<Item> mResults;
    private SearchResultsAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);

        // set adapter for list view
        ListView listView = (ListView) findViewById(R.id.search_results_list);
        mResults = new ArrayList<Item>();
        mAdapter = new SearchResultsAdapter(this, R.id.search_results_list, mResults);
        listView.setAdapter(mAdapter);

        // add groups
        new FetchGroupsTask(this).execute();

        // Get the intent, verify the action, and get the query
        Intent intent = getIntent();
        if(intent.getAction().equals(Intent.ACTION_SEARCH)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            search(query);
        }
    }

    /**
     * Searches for an item that matches the query, either in its name or description
     * @param query the value the user entered into the search bar
     */
    private void search(String query) {
        ArrayList<Item> items = Item.getItemsList();
        ListIterator<Item> iter = items.listIterator();
        while(iter.hasNext()) {
            Item curr = iter.next();
            if (curr.getName().toLowerCase().contains(query.toLowerCase())) {
                mResults.add(curr);
            } else if (curr.getDescription().toLowerCase().contains(query.toLowerCase())) {
                mResults.add(curr);
            }
        }

        displayResults(mResults);
    }

    private void displayResults(ArrayList<Item> list) {
        mAdapter.notifyDataSetChanged();
    }

    public class SearchResultsAdapter<T extends Item> extends ArrayAdapter<Item> {
        private Activity context;
        private ArrayList<Item> objects;
        public SearchResultsAdapter(Activity context, int resource, ArrayList<Item> objects) {
            super(context, resource, objects);
            this.context = context;
            this.objects = objects;
        }

        @NonNull
        @Override
        public View getView (final int position, View convertView, @NonNull ViewGroup parent) {
            View view;
            if(convertView == null) {
                // inflate a view of an item and its details
                view = getLayoutInflater().inflate(R.layout.fragment_item, parent, false);
            } else {
                view = convertView;
            }

            // open item if clicked
            if(getItem(position).getPrice() != 0)
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, ItemDetailActivity.class);
                        intent.putExtra(ItemDetailActivity.ITEM_ID, objects.get(position).getGuid());
                        startActivity(intent);
                    }
                });
            else
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // update group name and id in the app
                        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(getString(R.string.saved_group_id), getItem(position).getGuid());
                        editor.putString(getString(R.string.saved_group_name), getItem(position).getName());
                        // also, user may or may not be the owner of this group, so we'll set that to false and let
                        // the FetchManagedItemsTask figure it out
                        editor.putBoolean(getString(R.string.group_owner_boolean), false);
                        editor.commit();

                        //

                        // go back where we came from
                        NavUtils.navigateUpFromSameTask(context);
                    }
                });

            // populate the view
            TextView nameView = (TextView) view.findViewById(R.id.item_name);
            TextView priceView = (TextView) view.findViewById(R.id.item_price);
            TextView conditionView = (TextView) view.findViewById(R.id.item_condition);
            ImageView imageView = (ImageView) view.findViewById(R.id.item_picture);

            Item item = getItem(position);
            nameView.setText(item.getName());
            if(item.getPrice() != 0) {
                priceView.setText(String.format(Locale.ENGLISH, "$%.2f", item.getPrice()));
                imageView.setImageBitmap(item.getPic());
            } else {
                priceView.setText(item.getDescription());
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageBitmap(item.getPic());
            }
            conditionView.setText(item.getCondition());

            return view;
        }
    }

    private class FetchGroupsTask extends AsyncTask<Void, Integer, Integer> {
        /**
         * The Activity within which this AsyncTask runs
         */
        private Activity mParent;

        private final String LOG_TAG = FetchGroupsTask.class.getSimpleName();
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
                        Toast.makeText(mParent, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
                    }
                });
                return NO_INTERNET;
            } else {
                try {
                    // construct URL to fetch groups in a 20 mile radius if location input
                    Uri.Builder builder = new Uri.Builder();
                    builder.scheme("http")
                            .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("groups")
                            .appendPath("search")
                            .appendQueryParameter("name", getIntent().getStringExtra(SearchManager.QUERY))
                            .build();
                    URL url = new URL(builder.toString());

                    // connect to the URL and open the reader
                    urlConnection = (HttpURLConnection) url.openConnection();
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

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject itemJson = items.getJSONObject(i);

                            // TODO
                            String groupId = itemJson.getString("ssGroupId");
                            Item groupAsItem = new Item(groupId, Item.OTHER);
                            groupAsItem.setName(itemJson.getString("name"));
                            groupAsItem.setDescription(itemJson.getString("address"));
                            groupAsItem.setCondition(itemJson.getString("locationDetail"));
                            groupAsItem.setApproved(true);
                            groupAsItem.setPrice(0);
                            groupAsItem.setPic(BitmapFactory.decodeResource(getResources(), R.drawable.ic_group));
                            mResults.add(groupAsItem);
                            Log.d(LOG_TAG, groupAsItem.toString());
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
            Log.d(LOG_TAG, "available groups data set changed");
        }
    }
}
