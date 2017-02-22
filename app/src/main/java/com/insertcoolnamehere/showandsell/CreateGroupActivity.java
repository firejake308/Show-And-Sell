package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

/**
 * Created by Patrick on 02/01/2017.
 * This activity will allow an account holder to create a group
 */

public class CreateGroupActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // references to UI views
    private EditText groupNameEntry;
    private EditText addressEntry;
    private EditText cityEntry;
    private EditText stateEntry;
    private EditText extraLocationDataEntry;
    private Button   createGroupButton;

    // reference to ASyncTask
    private CreateGroupTask mAuthTask;
    private Geocoder coder;
    private GoogleApiClient mGoogleApiClient;

    private String streetAddress = "";
    private double latitude = 0;
    private double longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // find all the views
        groupNameEntry = (EditText) findViewById(R.id.group_name_entry);
        addressEntry = (EditText) findViewById(R.id.location_name_entry);
        cityEntry = (EditText) findViewById(R.id.city_name_entry);
        stateEntry = (EditText) findViewById(R.id.state_name_entry);
        extraLocationDataEntry = (EditText) findViewById(R.id.extra_location_data_entry);
        createGroupButton = (Button) findViewById(R.id.create_group_btn);

        createGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptCreateGroup();
            }
        });
    }

    private void attemptCreateGroup() {
        // if the AsyncTask has already been created, then don't restart it

        // for reporting errors
        boolean cancel = false;
        View focusView = null;

        // fetch values from EditTexts
        String groupName = groupNameEntry.getText().toString();
        streetAddress = addressEntry.getText().toString() + ", " + cityEntry.getText().toString() + ", " + stateEntry.getText().toString();
        String extraLocationData = extraLocationDataEntry.getText().toString();

        try {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }catch(Exception e){Log.d("ERROR", "GEO");}

        // verify that the group name is long enough
        if(groupName.length() < 4) {
            cancel = true;
            groupNameEntry.setError("Group name must be at least 4 characters");
            focusView = groupNameEntry;
        }
        // verify that the location is long enough
        else if(streetAddress.length() < 4) {
            cancel = true;
            addressEntry.setError("Incorrect Address");
            focusView = addressEntry;
        }
        // verify geocoder connected
        else if (!mGoogleApiClient.isConnected()) {
            cancel = true;
            addressEntry.setError("Not connected to Google API");
            focusView = addressEntry;
        }

        if(cancel) {
            // cancel and inform user of any errors
            focusView.requestFocus();
        } else {
            mAuthTask = new CreateGroupTask(this, groupName, latitude, longitude, extraLocationData);
            mAuthTask.execute();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("CreateGroupActivity", "I'm connected!");
        List<Address> address;
        if (mGoogleApiClient.isConnected()) {
            try {
                coder = new Geocoder(this, Locale.getDefault());
                address = coder.getFromLocationName(streetAddress, 1);
                Address location = address.get(0);
                latitude  = location.getLatitude();
                longitude = location.getLongitude();
            } catch (IOException e) {
                Log.e("CreateGroupActivity", "Error geocoding address", e);
            }
        } else {
            Log.e("CreateGroupActivity", "Not conencted to Google API");
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // do literally nothing
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e("CreateGroupActivity", "I'm a failure at life and i should kill myslef");
    }

    public class CreateGroupTask extends AsyncTask<Void, Void, Integer> {

        private final String LOG_TAG = CreateGroupActivity.class.getSimpleName();

        private final int SUCCESS = 0;
        private final int NO_INTERNET = 1;
        private final int GROUP_NAME_TAKEN = 2;
        private final int OTHER_FAILURE = 4;

        private Activity mParent;

        private String groupName;
        private double latitude;
        private double longitude;
        private String extraLocationData;

        CreateGroupTask(Activity parent, String gn, double lat, double lon, String eld) {
            this.mParent = parent;

            this.groupName = gn;
            this.latitude = lat;
            this.longitude = lon;
            this.extraLocationData = eld;
        }

        protected Integer doInBackground(Void... Params) {
            String uri = new Uri.Builder().scheme("http")
                    .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                    .appendPath("showandsell")
                    .appendPath("api")
                    .appendPath("groups")
                    .appendPath("create")
                    .build().toString();
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
                HttpURLConnection connection = null;
                BufferedWriter out = null;
                try {
                    URL url = new URL(uri);

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(15000);
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setChunkedStreamingMode(0);
                    connection.connect();

                    SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key),
                            Context.MODE_PRIVATE);
                    String GUID = savedData.getString(getString(R.string.userId), "");
                    String password = savedData.getString(getString(R.string.prompt_password), "");

                    // format group input as JSON
                    String body = "";
                    JSONObject groupData = new JSONObject();
                    JSONObject group = new JSONObject();
                    group.put("name", groupName);
                    group.put("adminId", GUID);
                    group.put("latitude", latitude);
                    group.put("longitude", longitude);
                    group.put("locationDetail", extraLocationData);
                    groupData.put("group", group);
                    groupData.put("password", password);
                    body = String.valueOf(groupData);
                    Log.d(LOG_TAG, body);

                    // send JSON to Cloud Server
                    out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
                    out.write(body);
                    out.flush();

                    // see if post was a success
                    int responseCode = connection.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 200) {
                        return SUCCESS;
                    } else if (responseCode == 400){
                        return GROUP_NAME_TAKEN;
                    }else if(responseCode == 449) {
                        return OTHER_FAILURE;
                    } else {
                        Log.e(LOG_TAG, "response Code = "+responseCode);
                    }
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, "The URL was incorrectly formed");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Unidentified error in network operations while creating account");
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch(IOException e) {
                            Log.e(LOG_TAG, "Couldn't close out stream", e);
                        }

                    }
                }
            }

            return OTHER_FAILURE;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result == SUCCESS) {
                // launch main activity so user can begin browsing
                Intent intent = new Intent(mParent, MainActivity.class);
                startActivity(intent);
            } else if (result == GROUP_NAME_TAKEN){
                // alert user that they had a duplicate email
                groupNameEntry.setError(getString(R.string.error_un_group_name_duplicate));
                groupNameEntry.requestFocus();
            } else if (result == OTHER_FAILURE) {
                Toast.makeText(mParent, "Congratulations! You found a bug! " +
                        "Please report it to the devs", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
