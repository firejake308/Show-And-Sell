package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.insertcoolnamehere.showandsell.logic.Group;
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


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChooseGroupFragment.OnChooseGroupListener} interface
 * to handle interaction events.
 */
public class ChooseGroupFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GroupViewAdapter.NewGroupListener {

    private OnChooseGroupListener mListener;
    private GroupViewAdapter mAdapter;
    private String groupName;
    private FrameLayout currentGroupLayout;
    private GoogleApiClient mGoogleApiClient;
    private FetchGroupsTask mAuthTask;
    private View rootView;
    private RecyclerView groupRecyclerView;

    private double latitude = 0;
    private double longitude = 0;

    private final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 9801;

    public ChooseGroupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_choose_group, container, false);

        updateGroups();

        // add group buttons to list
        groupRecyclerView = (RecyclerView) rootView.findViewById(R.id.list_of_groups);
        mAdapter = new GroupViewAdapter(Group.unselectedGroups, mListener, this);
        groupRecyclerView.setAdapter(mAdapter);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        return rootView;
    }

    private void updateCurrentGroupView(LayoutInflater inflater) {
        // update current group view
        SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        String groupId = sharedPref.getString(getString(R.string.saved_group_id), "NO GROUP SELECTED");
        Group currGroup = Group.getGroup(groupId);

        updateCurrentGroupView(inflater, currGroup);
    }

    private void updateCurrentGroupView(LayoutInflater inflater, final Group currGroup) {
        currentGroupLayout = (FrameLayout) rootView.findViewById(R.id.current_group);
        View currentGroupView;
        if(this.currentGroupLayout.getChildCount() < 1) {
            currentGroupView = inflater.inflate(R.layout.list_item_group, this.currentGroupLayout, false);
            this.currentGroupLayout.addView(currentGroupView);
        } else {
            currentGroupView = currentGroupLayout.getChildAt(0);
        }

        // link to current group
        currentGroupLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onChooseGroup(currGroup);
            }
        });

        TextView nameView = (TextView) currentGroupView.findViewById(R.id.group_name);
        TextView addressView = (TextView) currentGroupView.findViewById(R.id.group_address);
        ImageView favoriteView = (ImageView) currentGroupView.findViewById(R.id.favorite_group);

        try {
            nameView.setText(currGroup.getName());
            addressView.setText(currGroup.getPickupAddress());
        } catch (NullPointerException e) {
            nameView.setText("NO GROUP SELECTED");
            addressView.setText("NO GROUP SELECTED");
        }
        if (Build.VERSION.SDK_INT >= 21)
            favoriteView.setImageDrawable(getContext().getDrawable(R.drawable.ic_favorite_filled));
        else
            getResources().getDrawable(R.drawable.ic_favorite_filled);

        // remove current group from list of available groups
        Group.unselectedGroups.clear();
        Group.unselectedGroups.addAll(Group.availableGroups);
        Group.unselectedGroups.remove(currGroup);
        mAdapter.notifyDataSetChanged();
    }

    private void updateGroups() {
        try {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        } catch(Exception e) {
            Log.e("ChooseGroupFragment", "Error creating Google Services API client");
        }
    }

    public void onConnected(Bundle bundle) {
        // create a LocationRequest
        LocationRequest request = LocationRequest.create();
        request.setNumUpdates(1);
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        try {
            // get permission for location from the user
            if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
            while (ContextCompat.checkSelfPermission(getContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {try{Thread.sleep(500);}catch(Exception e){}}

            // once we have permission, send the request for location
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);
        } catch (SecurityException se){Log.e("ChooseGroupFragment", "User Denied Permission");}
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // do literally nothing
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e("ChooseGroupFragment", "The connection failed");
        mAuthTask = new FetchGroupsTask(getActivity(), latitude, longitude);
    }

    @Override
    public void onLocationChanged(Location location) {
        // update latitude and longitude
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        // update groupRecyclerView
        mAuthTask = new FetchGroupsTask(getActivity(), latitude, longitude);
        mAuthTask.execute();
    }

    /**
     * Changes the user's primary group when they select a new one
    */
    public void setNewGroup(Group group) {
        // clear browse group items, because that has changed
        Item.allGroupsItems.clear();

        // update group name and id in the saved data
        SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.saved_group_id), group.getId());
        editor.putString(getString(R.string.saved_group_name), group.getName());
        // also, user may or may not be the owner of this group, so we'll set that to false and let
        // the FetchManagedItemsTask figure it out
        editor.putBoolean(getString(R.string.group_owner_boolean), false);
        editor.commit();

        // update current group view
        updateCurrentGroupView(getActivity().getLayoutInflater(), group);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnChooseGroupListener) {
            mListener = (OnChooseGroupListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
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
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnChooseGroupListener {
        void onChooseGroup(Group group);
    }

    /**
     * Determines the appropriate URL for the API call in FetchGroupsTask based on the availability
     * of location data
     * @param lat latitude of user's current position
     * @param lon longitude of user's current position
     * @return API call as URL
     * @throws MalformedURLException
     */
    protected URL getAPICall(double lat, double lon) throws MalformedURLException {
        // construct the URL to fetch groupRecyclerView, all if no location data
        if (lat == 0 && lon == 0) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                    .appendPath("showandsell")
                    .appendPath("api")
                    .appendPath("groups")
                    .appendPath("allgroups")
                    .build();
            Log.d("FetchGroupsTask", builder.toString());
            return new URL(builder.toString());
        }
        // construct URL to fetch groupRecyclerView in a 20 mile radius if location input
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
                .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                .appendPath("showandsell")
                .appendPath("api")
                .appendPath("groups")
                .appendPath("closestgroups")
                .appendQueryParameter("n", "10")
                .appendQueryParameter("latitude", ""+lat)
                .appendQueryParameter("longitude", ""+lon)
                .build();
        return new URL(builder.toString());
    }

    private class FetchGroupsTask extends AsyncTask<Void, Integer, Integer> {
        /**
         * The Activity within which this AsyncTask runs
         */
        private Activity mParent;
        private double lat;
        private double lon;

        /**
         * Tag to identify this task in debug and error logs
         */
        private final String LOG_TAG = FetchGroupsTask.class.getSimpleName();

        // result codes
        private final int NO_INTERNET = 2;
        private final int SUCCESS = 1;
        private final int OTHER_FAILURE = 0;

        FetchGroupsTask(Activity parent, double latitude, double longitude) {
            mParent = parent;
            lat = latitude;
            lon = longitude;
        }

        protected Integer doInBackground(Void... urls) {
            // variables that we will have to close in try loop
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // the raw, un-parsed JSON response from the server
            int responseCode;

            // check for internet connection
            ConnectivityManager manager = (ConnectivityManager) mParent.getSystemService(Context.CONNECTIVITY_SERVICE);
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
                    // connect to the URL and open the reader
                    urlConnection = (HttpURLConnection) getAPICall(lat, lon).openConnection();
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // obtain status code
                    responseCode = urlConnection.getResponseCode();
                    Log.d(LOG_TAG, "response code = "+responseCode);
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
                        Group.clearGroups();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject itemJson = items.getJSONObject(i);

                            // add group to list
                            new Group(itemJson.getString("name"), itemJson.getString("ssGroupId"),
                                    itemJson.getString("address"), itemJson.getString("locationDetail"),
                                    itemJson.getDouble("rating"));
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

            // stop the progress spinner
            View progressView = rootView.findViewById(R.id.choose_group_progress);
            progressView.setVisibility(View.GONE);
            Log.d(LOG_TAG, "available groups data set changed: "+Group.availableGroups.size());

            // update current group view
            updateCurrentGroupView(getActivity().getLayoutInflater());
        }
    }
}
