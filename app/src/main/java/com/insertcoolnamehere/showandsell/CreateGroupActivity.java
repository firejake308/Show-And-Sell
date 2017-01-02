package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Patrick on 02/01/2017.
 * This activity will allow an account holder to create a group
 */

public class CreateGroupActivity extends AppCompatActivity {

    // references to UI views
    private EditText groupNameEntry;
    private EditText locationNameEntry;
    private EditText extraLocationDataEntry;
    private Button   createGroupButton;

    // reference to ASyncTask
    private CreateGroupTask mAuthTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // find all the views
        groupNameEntry = (EditText) findViewById(R.id.group_name_entry);
        locationNameEntry = (EditText) findViewById(R.id.location_name_entry);
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
        String locationName = locationNameEntry.getText().toString();
        String extraLocationData = extraLocationDataEntry.getText().toString();

        // verify that the group name is long enough
        if(groupName.length() < 4) {
            cancel = true;
            groupNameEntry.setError("Group name must be at least 4 characters");
            focusView = groupNameEntry;
        }
        // verify that the location is long enough
        else if(locationName.length() < 4) {
            cancel = true;
            locationNameEntry.setError("Location must be at least 4 characters");
            focusView = locationNameEntry;
        }

        if(cancel) {
            // cancel and inform user of any errors
            focusView.requestFocus();
        } else {
            mAuthTask = new CreateGroupTask(this, groupName, locationName, extraLocationData);
            mAuthTask.execute();
        }
    }

    public class CreateGroupTask extends AsyncTask<Void, Void, Boolean> {

        private final String LOG_TAG = CreateGroupActivity.class.getSimpleName();

        private Activity mParent;

        private String groupName;
        private String locationName;
        private String extraLocationData;

        CreateGroupTask(Activity parent, String gn, String ln, String eld) {
            this.mParent = parent;

            this.groupName = gn;
            this.locationName = ln;
            this.extraLocationData = eld;
        }

        protected Boolean doInBackground(Void... Params) {
            String uri = new Uri.Builder().scheme("http")
                    .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                    .appendPath("showandsell")
                    .appendPath("api")
                    .appendPath("groups")
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

                    // TODO check this formally with Brayden when API finished
                    // format group input as JSON
                    String body = "";
                    JSONObject groupData = new JSONObject();
                    JSONObject group = new JSONObject();
                    group.put("name", groupName);
                    group.put("admin", GUID);
                    group.put("location", locationName);
                    group.put("extra_location_data", extraLocationData);
                    groupData.put("group", group);
                    groupData.put("password", password);
                    body = String.valueOf(groupData);

                    // send JSON to Cloud Server
                    out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
                    out.write(body);
                    out.flush();

                    // see if post was a success
                    int responseCode = connection.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 201) {
                        return true;
                    } else if(responseCode == 449) {
                        return false;
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

            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(success) {
                // launch main activity so user can begin browsing
                Intent intent = new Intent(mParent, MainActivity.class);
                startActivity(intent);
            } else {
                // alert user that they had a duplicate username or email
                groupNameEntry.setError(getString(R.string.error_un_group_name_duplicate));
                groupNameEntry.requestFocus();
            }
        }
    }
}