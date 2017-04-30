package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.insertcoolnamehere.showandsell.logic.CryptoTool;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.insertcoolnamehere.showandsell.LoginActivity.CLOUD_SERVER_IP;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if user is already logged in, go straight to main activity
        SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key),
                Context.MODE_PRIVATE);
        if(savedData.contains(getString(R.string.prompt_email))) {
            // get email and password
            String email = savedData.getString(getString(R.string.prompt_email), "");
            String pw = savedData.getString(getString(R.string.prompt_password), "");
            new UserLoginTask(this, email, pw).execute();
        } else {
            // otherwise, take user to login activity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Integer> {
        private final String LOG_TAG = LoginActivity.UserLoginTask.class.getSimpleName();

        private final int SUCCESS = 0;
        private final int OTHER_FAILURE = 4;

        private final Activity mParent;
        private final String mEmail;
        private final String mPassword;
        private String firstName;
        private String lastName;
        private String userId;

        UserLoginTask(Activity parent, String email, String password) {
            mParent = parent;
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // variables that we will have to close in try loop
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // the unparsed JSON response from the server
            int responseCode = -1;

            // check for internet connection
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();

            if(info == null || !info.isConnected()) {
                // if there is no network, take user to login activity
                return OTHER_FAILURE;
            } else {
                try {
                    // construct the URL to fetch a user
                    Uri.Builder  builder = new Uri.Builder();
                    builder.scheme("http")
                            .encodedAuthority(CLOUD_SERVER_IP)
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("users")
                            .appendPath("userbyemail")
                            .appendQueryParameter("email", mEmail)
                            .appendQueryParameter("password", mPassword)
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
                    Log.d(LOG_TAG, "response code="+responseCode);
                    if(responseCode == 200) {
                        // read response to get user data from server
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String line = "";
                        String responseBody = "";
                        while((line = reader.readLine()) != null) {
                            responseBody += line + '\n';
                        }

                        // parse response as JSON
                        JSONObject user = new JSONObject(responseBody);
                        firstName = user.getString("firstName");
                        lastName = user.getString("lastName");
                        userId = user.getString("ssUserId");

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

            // if anything goes wrong, don't let them log in
            return OTHER_FAILURE;
        }

        @Override
        protected void onPostExecute(final Integer success) {
            if (success == SUCCESS) {
                // store data for later use
                SharedPreferences savedData = mParent.getSharedPreferences(getString(R.string.saved_data_file_key),
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = savedData.edit();
                editor.putString(getString(R.string.prompt_email), mEmail);
                editor.putString(getString(R.string.prompt_password), mPassword);
                editor.putString(getString(R.string.prompt_first_name), firstName);
                editor.putString(getString(R.string.prompt_last_name), lastName);
                editor.putString(getString(R.string.userId), userId);
                editor.apply();

                // launch main activity so user can begin browsing
                Intent intent = new Intent(mParent, MainActivity.class);
                startActivity(intent);
                finish();
            } else if (success == OTHER_FAILURE) {
                // let user login manually
                Intent intent = new Intent(mParent, LoginActivity.class);
                startActivity(intent);
                finish();
            }
            // if there was NO_INTERNET, just wait for the user to turn on internet
        }
    }
}
