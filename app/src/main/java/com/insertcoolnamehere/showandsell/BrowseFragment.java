package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Arrays;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */

public class BrowseFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;

    private OnListFragmentInteractionListener mListener;
    private BrowseItemRecyclerViewAdapter adapter;
    private AsyncTask mFetchItemsTask;

    private RecyclerView mRecyclerView;
    private View fragView;

    private String lastGroupId;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BrowseFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(!Item.hasItems()) {
            updateItems();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_browse, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            updateItems();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Update items when user swipes to refresh
     */
    public void onRefresh() {
        updateItems();
    }

    public void updateItems() {
        // fetch items from server
        if(mFetchItemsTask == null) {
            SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
            String groupId = sharedPref.getString(getString(R.string.saved_group_id), null);
            lastGroupId = groupId;

            if(groupId == null) {
                // direct user to choose a group if they haven't done so yet
                View view = fragView.findViewById(R.id.error_no_group);
                view.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);

                // link text view to choose group activity
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.openChooseGroup();
                    }
                });
            } else {
                // if there is a group selected, turn back on recycler view and hide error
                View errorView = fragView.findViewById(R.id.error_no_group);
                if(errorView.getVisibility() == View.VISIBLE) {
                    errorView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                }

                // show progress bar
                showProgress(true);

                // find all items in that group and update the list
                mFetchItemsTask = new FetchItemsTask(getActivity(), groupId).execute();
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume(); // always have to call super

        // update in-fragment lastGroupId to match persistent storage
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        lastGroupId = prefs.getString(getString(R.string.last_group_loaded), "NULL");

        // if group id has changed, we must refresh
        SharedPreferences savedData = getActivity().getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        if(!lastGroupId.equals(savedData.getString(getString(R.string.saved_group_id), "NULL"))) {
            updateItems();
        }
    }

    public void onPause() {
        super.onPause();

        // save last group loaded
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.last_group_loaded), lastGroupId);
        editor.apply();
    }

    @SuppressWarnings("unused")
    public static BrowseFragment newInstance(int columnCount) {
        BrowseFragment fragment = new BrowseFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);

        fragView = view;
        View recyclerView = view.findViewById(R.id.list);
        SwipeRefreshLayout swiper = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        swiper.setOnRefreshListener(this);
        swiper.setColorSchemeResources(R.color.colorAccent);

        // Set the adapter
        if (recyclerView instanceof RecyclerView) {
            Context context = recyclerView.getContext();
            mRecyclerView = (RecyclerView) recyclerView;
            if (mColumnCount <= 1) {
                mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                mRecyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            adapter = new BrowseItemRecyclerViewAdapter(isBookmark()?Item.bookmarkedItems:Item.itemsToShow, mListener);
            mRecyclerView.setAdapter(adapter);
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
          throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(String itemId);
        void openChooseGroup();
        void setGroupOwner(boolean isOwner);
    }

    /**
     * Shows the progress UI and hides the RecyclerView
     */
    private void showProgress(final boolean show) {
        final SwipeRefreshLayout swiper = (SwipeRefreshLayout) fragView.findViewById(R.id.swiperefresh);
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

    protected URL getAPICall(String id) throws MalformedURLException {
        // construct the URL to fetch a user
        Uri.Builder  builder = new Uri.Builder();
        builder.scheme("http")
                .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                .appendPath("showandsell")
                .appendPath("api")
                .appendPath("items")
                .appendQueryParameter("groupId", id)
                .build();
        return new URL(builder.toString());
    }

    protected boolean isBookmark() {
        return false;
    }

    // insert an AsyncTask here, using the ones in LoginActivity or DonateFragment as a reference
    private class FetchItemsTask extends AsyncTask<Void, Integer, Integer> {
        /**
         * The Activity within which this AsyncTask runs
         */
        private Activity mParent;
        private String mGroupId;

        private final String LOG_TAG = FetchItemsTask.class.getSimpleName();
        private final int NO_INERNET = 2;
        private final int SUCCESS = 1;
        private final int OTHER_FAILURE = 0;

        public FetchItemsTask(Activity parent, String groupId) {
            mParent = parent;
            mGroupId = groupId;
        }

        protected Integer doInBackground(Void... urls) {
            // im gonna copy paste the networking code from Login here
            // variables that we will have to close in try loop
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // the unparsed JSON response from the server
            int responseCode = -1;

            // cancel if task is detached from activity
            if(getActivity() == null)
                return OTHER_FAILURE;
            // check for internet connection
            ConnectivityManager manager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();

            //TODO: for Android 6.0+, request internet permission

            if(info == null || !info.isConnected()) {
                // if there is no network, inform user through a toast
                mParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mParent, "No connection available. Try again later.", Toast.LENGTH_SHORT).show();
                        Log.d(LOG_TAG, "No connection available");
                    }
                });
                return NO_INERNET;
            } else {
                try {
                    // first determine if the user is a group owner or not
                    URL url = new URL(new Uri.Builder().scheme("http")
                            .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("groups")
                            .appendPath(mGroupId)
                            .build().toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String line = "";
                    String responseBody = "";
                    while((line = reader.readLine()) != null) {
                        responseBody += line + '\n';
                    }

                    // parse response as JSON
                    JSONObject group = new JSONObject(responseBody);
                    String ownerId = group.getString("admin");
                    SharedPreferences savedData = getActivity().getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
                    String myId = savedData.getString(getString(R.string.userId), "NULL");
                    if (ownerId.equals(myId)) {
                        mListener.setGroupOwner(true);
                    } else {
                        mListener.setGroupOwner(false);
                    }

                    urlConnection.disconnect();

                    // connect to the URL and open the reader
                    urlConnection = (HttpURLConnection) getAPICall(mGroupId).openConnection();
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

                            // special for bookmarks
                            if(isBookmark()) {
                                itemJson = itemJson.getJSONObject("item");
                            }

                            Item item = new Item(itemJson.getString("ssItemId"), isBookmark());
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
            if(result == SUCCESS) {
                adapter.notifyDataSetChanged();
                showProgress(false);
                for(Item item: Item.itemsToShow) {
                    Log.d(LOG_TAG, item.toString()+item.isApproved());
                }
                mFetchItemsTask = null;
            } else {
                Log.e(LOG_TAG, "It appears that the task failed :(");
                showProgress(false);
                mFetchItemsTask = null;
            }
        }
    }
}