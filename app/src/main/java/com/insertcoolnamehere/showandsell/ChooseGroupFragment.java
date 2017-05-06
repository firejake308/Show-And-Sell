package com.insertcoolnamehere.showandsell;

import android.*;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
import java.util.StringTokenizer;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChooseGroupFragment.OnChooseGroupListener} interface
 * to handle interaction events.
 */
public class ChooseGroupFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private OnChooseGroupListener mListener;
    private String groupName;
    private Button currentGroup;
    private ArrayAdapter<String> mAdapter;
    private ArrayList<String> groupTexts = new ArrayList<>();
    private ArrayList<String> groupIds = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;
    private FetchGroupsTask mAuthTask;
    private View rootView;

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

        // update current group text
        SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        groupName = sharedPref.getString(getString(R.string.saved_group_name), "No Group Selected");
        currentGroup = (Button) rootView.findViewById(R.id.current_group);
        currentGroup.setText(groupName);

        // add group buttons to list
        ListView groups = (ListView) rootView.findViewById(R.id.list_of_groups);
        mAdapter = new ArrayAdapter<>(
                getContext(),
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

        return rootView;
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
        } catch (SecurityException se){Log.e("ChooseGroupActivity", "User Denied Permission");}
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // do literally nothing
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e("ChooseGroupActivity", "I'm a failure at life and i should kill myself");
        mAuthTask = new FetchGroupsTask(getActivity(), latitude, longitude);
    }

    @Override
    public void onLocationChanged(Location location) {
        // update latitude and longitude
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        // update groups
        mAuthTask = new FetchGroupsTask(getActivity(), latitude, longitude);
        mAuthTask.execute();
    }

    /**
     * Changes the user's primary group when they select a new one
     */
    private void setNewGroup(int position) {
        // get new group id and name
        String currentGroupId = groupIds.get(position);
        String currentGroupName = groupTexts.get(position);

        // update text in current group button
        currentGroup.setText(currentGroupName);

        // clear browse group items, because that has changed
        Item.browseGroupItems.clear();

        // update group name and id in the saved data
        SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.saved_group_id), currentGroupId);
        editor.putString(getString(R.string.saved_group_name), currentGroupName);
        // also, user may or may not be the owner of this group, so we'll set that to false and let
        // the FetchManagedItemsTask figure it out
        editor.putBoolean(getString(R.string.group_owner_boolean), false);
        editor.commit();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onChooseGroup(uri.toString());
        }
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
        // TODO: Update argument type and name
        void onChooseGroup(String group);
    }

    /**
     * Determines the appropriate URL for the API call in FetchGroupsTask based on the availability
     * of location data
     * @param lat latitiude of user's current position
     * @param lon longitude of user's current position
     * @return API call as URL
     * @throws MalformedURLException
     */
    protected URL getAPICall(double lat, double lon) throws MalformedURLException {
        // construct the URL to fetch groups, all if no location data
        if (lat == 0 && lon == 0) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                    .appendPath("showandsell")
                    .appendPath("api")
                    .appendPath("groups")
                    .appendPath("allgroups")
                    .build();
            Log.d("REACHED", "REACHED");
            return new URL(builder.toString());
        }
        // construct URL to fetch groups in a 20 mile radius if location input
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
                        Toast.makeText(mParent, "No connection available. Try again later.", Toast.LENGTH_SHORT).show();
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

                            // update list of group names to display and corresponding IDs
                            groupTexts.add(itemJson.getString("name")+" \u2014 "+itemJson.getString("address"));
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

            // stop the progress spinner
            View progressView = rootView.findViewById(R.id.choose_group_progress);
            progressView.setVisibility(View.GONE);
            Log.d(LOG_TAG, "available groups data set changed");
        }
    }
}
