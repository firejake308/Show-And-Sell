package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Geocoder;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.insertcoolnamehere.showandsell.logic.CryptoTool;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import static android.content.ContentValues.TAG;

public class CreateAccountActivity extends AppCompatActivity implements OnConnectionFailedListener{

    // references to UI views
    private EditText firstNameEntry;
    private EditText lastNameEntry;
    private EditText emailEntry;
    private EditText passwordEntry;
    private EditText confirmPwEntry;
    private Button createAccountButton;

    private String firstName;
    private String lastName;
    private String email;
    private String password1;
    private String password2;
    private String userId;

    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;

    // reference to AsyncTask
    private CreateAccountTask mAuthTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // find all the views
        firstNameEntry = (EditText) findViewById(R.id.first_name_entry);
        lastNameEntry = (EditText) findViewById(R.id.last_name_entry);
        emailEntry = (EditText) findViewById(R.id.email_entry);
        passwordEntry = (EditText) findViewById(R.id.password_entry);
        confirmPwEntry = (EditText) findViewById(R.id.confirm_password);
        createAccountButton = (Button) findViewById(R.id.create_account_btn);
        View signInButton = findViewById(R.id.create_account_with_google_btn);

        // hook up action listeners
        confirmPwEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == R.id.create_account || actionId == EditorInfo.IME_NULL) {
                    attemptCreateAccount();
                    return true;
                }
                return false;
            }
        });
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptCreateAccount();
            }
        });
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptCreateAccountWithGoogle();
            }
        });
    }

    private void attemptCreateAccount() {
        // for reporting errors
        boolean cancel = false;
        View focusView = null;

        // fetch values from EditTexts
        firstName = firstNameEntry.getText().toString();
        lastName = lastNameEntry.getText().toString();
        email = emailEntry.getText().toString();
        password1 = passwordEntry.getText().toString();
        password2 = confirmPwEntry.getText().toString();

        // first, validate the email
        if(!email.contains("@") || !email.contains(".")) {
            cancel = true;
            emailEntry.setError("Invalid email");
            focusView = emailEntry;
        }
        // verify that the password is long enough
        else if(password1.length() < 4) {
            cancel = true;
            passwordEntry.setError("Password must be at least 4 characters");
            focusView = passwordEntry;
        }

        // next, check that the passwords match
        else if(!password1.equals(password2)) {
            cancel = true;

            passwordEntry.setError(getString(R.string.error_password_match));
            focusView = passwordEntry;
        }

        if(cancel) {
            // cancel and inform user of any errors
            focusView.requestFocus();
        } else {
            // remember that the user didn't sign in with google
            SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = savedData.edit();
            editor.putBoolean(getString(R.string.is_google_user), false);
            editor.apply();
            mAuthTask = new CreateAccountTask(this, firstName, lastName, email, CryptoTool.encrypt(password1));
            mAuthTask.execute();
        }
    }

    /**
     * Attempts to sign in the user using google
     */
    private void attemptCreateAccountWithGoogle() {
        // Configure sign-in to request the user's ID, email address, and basic profile. ID and
        // basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        if (mGoogleApiClient == null) {
            // Build a GoogleApiClient with access to GoogleSignIn.API and the options above.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
        }

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            int statusCode = result.getStatus().getStatusCode();
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            try {
                GoogleSignInAccount acct = result.getSignInAccount();
                String email = acct.getEmail();
                String firstName = acct.getGivenName();
                String lastName = acct.getFamilyName();
                String password = acct.getId();
                mAuthTask = new CreateAccountTask(this, firstName, lastName, email, CryptoTool.encrypt(password));
                mAuthTask.execute();

                // remember that the user signed in with google
                SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = savedData.edit();
                editor.putBoolean(getString(R.string.is_google_user), true);
                editor.apply();
            } catch (NullPointerException e) {
                Log.e("LoginActivity", "null exception");
            }
        } else {
            // Signed out, show unauthenticated UI.
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e("LoginActivity", "Connection to Google API Client failed");
    }

    public class CreateAccountTask extends AsyncTask<Void, Void, Boolean> {

        private final String LOG_TAG = CreateAccountTask.class.getSimpleName();

        private Activity mParent;

        private String firstName;
        private String lastName;
        private String email;
        private String password;

        CreateAccountTask(Activity parent, String fn, String ln, String email, String pw) {
            this.mParent = parent;

            this.firstName = fn;
            this.lastName = ln;
            this.email = email;
            this.password = pw;
        }

        protected Boolean doInBackground(Void... Params) {
            String uri = new Uri.Builder().scheme("http")
                    .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                    .appendPath("showandsell")
                    .appendPath("api")
                    .appendPath("users")
                    .appendPath("create")
                    .build().toString();
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
            } else {
                HttpURLConnection connection = null;
                BufferedWriter out = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL(uri);

                    // open connection and configure for output
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(15000);
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setChunkedStreamingMode(0);
                    connection.connect();

                    // format user input as JSON
                    String body = "";
                    JSONObject user = new JSONObject();
                    user.put("password", password);
                    user.put("firstName", firstName);
                    user.put("lastName", lastName);
                    user.put("email", email);
                    user.put("groupId", "");
                    body = String.valueOf(user);

                    // send JSON to Cloud Server
                    out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
                    out.write(body);
                    out.flush();

                    // see if post was a success
                    int responseCode = connection.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 200) {
                        // read response to get user data from server
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String line = "";
                        String responseBody = "";
                        while((line = reader.readLine()) != null) {
                            responseBody += line + '\n';
                        }

                        JSONObject userObj = new JSONObject(responseBody);
                        Log.d(LOG_TAG, userObj.toString());
                        userId = userObj.getString("ssUserId");
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
                            reader.close();
                        } catch(Exception e) {
                            Log.e(LOG_TAG, "Couldn't close out or reader stream", e);
                        }

                    }
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(success) {
                // store data for later use
                SharedPreferences savedData = mParent.getSharedPreferences(getString(R.string.saved_data_file_key),
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = savedData.edit();
                editor.putString(getString(R.string.prompt_email), email);
                editor.putString(getString(R.string.prompt_password), password);
                editor.putString(getString(R.string.prompt_first_name), firstName);
                editor.putString(getString(R.string.prompt_last_name), lastName);
                editor.putString(getString(R.string.userId), userId);
                editor.apply();

                // launch tutorial activity so user can figure out how app works
                Intent intent = new Intent(mParent, TutorialActivity.class);
                startActivity(intent);
            } else {
                // alert user that they had a duplicate email
                emailEntry.setError(getString(R.string.error_email_duplicate));
                emailEntry.requestFocus();
            }
        }
    }

    // TODO remove this unused class if @Patrick says we don't need it
    private class DataLongOperationAsyncTask extends AsyncTask<String, Void, String[]> {
        private String address = "8116 Island Park Court";

        @Override
        protected String[] doInBackground(String... params) {
            String response;
            try {
                response = getLatLongByURL("http://maps.google.com/maps/api/geocode/json?address="+address+"&sensor=false");
                Log.d("response",""+response);
                return new String[]{response};
            } catch (Exception e) {
                return new String[]{"error"};
            }
        }

        @Override
        protected void onPostExecute(String... result) {
            try {
                JSONObject jsonObject = new JSONObject(result[0]);

                double lng = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lng");

                double lat = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lat");

                Log.d("latitude", "" + lat);
                Log.d("longitude", "" + lng);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    public String getLatLongByURL(String requestURL) {
        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else {
                response = "";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}
