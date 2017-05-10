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

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */

public class BrowseFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final String ARG_GROUP_ID = "group-id";
    private int mColumnCount = 1;

    protected OnListFragmentInteractionListener mListener;
    private ArrayList<Item> mItemList;
    private RecyclerView.Adapter adapter;
    private AsyncTask mFetchItemsTask;

    private RecyclerView mRecyclerView;
    private View fragView;

    private String mGroupId;
    private int lastItemLoaded = 0;

    private boolean showAllGroups = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BrowseFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // if no items are present, fetch some from the server
        if(!Item.hasBrowseItems()) {
            updateItems();
        }
    }

    /**
     * Updates items when user swipes to refresh
     */
    public void onRefresh() {
        updateItems();
    }

    /**
     * Updates the <code>Item.allGroupsItems</code> array with fresh items from the server
     */
    public void updateItems() {
        // fetch items from server
        if(mFetchItemsTask == null) {
            // if there is a group selected, turn back on recycler view and hide error
            View errorView = fragView.findViewById(R.id.error_no_group);
            if(errorView.getVisibility() == View.VISIBLE) {
                errorView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }

            // show progress bar
            showProgress(true);

            // before clearing items list, record last item index
            lastItemLoaded = mItemList.size();

            // find all items in that group and update the list
            mFetchItemsTask = new FetchItemsTask(getActivity(), mGroupId).execute();
        }
    }

    @Override
    public void onResume() {
        super.onResume(); // always have to call super

        // update items list
        updateItems();
    }

    public void onPause() {
        super.onPause();
    }

    /**
     * Creates a new instance with the given number of columns in the grid layout
     * @param columnCount number of columns in the grid
     * @param showAllGroups whether or not this should show items from all groups
     * @return newly created fragment
     */
    @SuppressWarnings("unused")
    public static BrowseFragment newInstance(int columnCount, boolean showAllGroups, String groupId) {
        BrowseFragment fragment = new BrowseFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putString(ARG_GROUP_ID, groupId);
        fragment.showAllGroups = showAllGroups;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the number of columns in the grid layout
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            mGroupId = getArguments().getString(ARG_GROUP_ID);
        }

        if (showAllGroups)
            mItemList = Item.allGroupsItems;
        else {
            mItemList = Item.currentGroupItems;
            mItemList.clear();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);

        // create a reference to the fragment for later use
        fragView = view;

        // identify views and initialize
        View recyclerView = view.findViewById(R.id.list);
        SwipeRefreshLayout swiper = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        swiper.setOnRefreshListener(this);
        swiper.setColorSchemeResources(R.color.colorAccent);

        // Set the layout and adapter for the RecyclerView
        if (recyclerView instanceof RecyclerView) {
            Context context = recyclerView.getContext();
            mRecyclerView = (RecyclerView) recyclerView;
            if (mColumnCount <= 1) {
                // use a linear layout for a single-column view
                mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                // use a bottom-stacking staggered grid layout for multiple columns
                GridLayoutManager layoutManager = new GridLayoutManager(context, mColumnCount);
                layoutManager.setReverseLayout(showAllGroups);
                mRecyclerView.setLayoutManager(layoutManager);
            }
            // Show several details for bookmarks, but only picture and price for browse items
            if(isBookmark())
                adapter = new FullItemRecyclerViewAdapter(Item.bookmarkedItems, mListener);
            else
                adapter = new SummaryItemRecyclerViewAdapter(mItemList, mListener);
            mRecyclerView.setAdapter(adapter);
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            // hook up parent activity as listener
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
        /**
         * Reacts to the user tapping an item in the Browse list
         * @param itemId id of selected item
         */
        void onListFragmentInteraction(String itemId);

        /**
         * Opens the ChooseGroupFragment when the user clicks on the link that is shown in the
         * BrowseFragment when no group is selected
         */
        void openChooseGroup();
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

    /**
     * Helper method that returns the appropriate API Call as a URL for the FetchItemsTask
     * @param id group ID
     * @return API call as a URL
     * @throws MalformedURLException
     */
    protected URL getAPICall(String id) throws MalformedURLException {
        // construct the URL to fetch a user
        Uri.Builder  builder = new Uri.Builder();
        if (showAllGroups) {
            builder.scheme("http")
                    .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                    .appendPath("showandsell")
                    .appendPath("api")
                    .appendPath("items")
                    .appendPath("allapprovedinrange")
                    .appendQueryParameter("start", "" + lastItemLoaded)
                    .appendQueryParameter("end", "" + (lastItemLoaded + 6))
                    .build();
        } else {
            builder.scheme("http")
                    .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                    .appendPath("showandsell")
                    .appendPath("api")
                    .appendPath("items")
                    .appendPath("approvedinrange")
                    .appendQueryParameter("groupId", id)
                    .appendQueryParameter("start", "" + lastItemLoaded)
                    .appendQueryParameter("end", "" + (lastItemLoaded + 6))
                    .build();
        }
        Log.d("BrowseFragment", builder.toString());
        return new URL(builder.toString());
    }

    /**
     * Equivalent to <code>this instanceof BookmarksFragment</code>
     * @return whether or not this is a bookmark fragment
     */
    protected boolean isBookmark() {
        return false;
    }

    /**
     * Task that asynchronously queries the server for a fresh list of items
     */
    private class FetchItemsTask extends AsyncTask<Void, Integer, Integer> {
        /**
         * The Activity within which this AsyncTask runs
         */
        private Activity mParent;
        private String mGroupId;

        private final String LOG_TAG = FetchItemsTask.class.getSimpleName();

        // result codes for possible outcomes of the doInBackground method
        private final int NO_ITEMS = 3;
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

            if(info == null || !info.isConnected()) {
                // if there is no network, inform user through a toast
                mParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mParent, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
                    }
                });
                return NO_INERNET;
            } else {
                try {
                    // connect to the URL and open the reader
                    urlConnection = (HttpURLConnection) getAPICall(mGroupId).openConnection();
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // obtain status code
                    responseCode = urlConnection.getResponseCode();
                    Log.d(LOG_TAG, "response code: "+responseCode);
                    if(responseCode == 200) {
                        // read response to get user data from server
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String line = "";
                        String responseBody = "";
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

                            // create a new Item and initialize it with all of the characteristics
                            // provided by the server
                            int itemType;
                            if (isBookmark())
                                itemType = Item.BOOKMARK;
                            else if (showAllGroups)
                                itemType = Item.ALL;
                            else
                                itemType = Item.BROWSE;
                            Item item = new Item(itemJson.getString("ssItemId"), itemType);
                            item.setName(itemJson.getString("name"));
                            item.setPrice(itemJson.getDouble("price"));
                            item.setCondition(itemJson.getString("condition"));
                            item.setDescription(itemJson.getString("description"));
                            byte[] imgBytes = Base64.decode(itemJson.getString("thumbnail"), Base64.NO_PADDING);
                            item.setPic(BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length));
                            item.setApproved(itemJson.getBoolean("approved"));
                            item.setOwnerId(itemJson.getString("ownerId"));
                            item.setGroupId(itemJson.getString("groupId"));
                        }

                        return SUCCESS;
                    } else if (responseCode == 404) {
                        return NO_ITEMS;
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
            } else if (result == OTHER_FAILURE){
                Log.e(LOG_TAG, "It appears that the task failed :(");
            }

            // no matter what happens, show user that we're done trying here
            showProgress(false);
            mFetchItemsTask = null;
        }
    }
}