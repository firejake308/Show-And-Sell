package com.insertcoolnamehere.showandsell;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String CLOUD_SERVER_IP = "68.248.214.70:8080";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if user is already logged in, go straight to main activity
        SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key),
                Context.MODE_PRIVATE);
        if(savedData.contains(getString(R.string.prompt_username))) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_login);
        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button mSignUpButton = (Button) findViewById(R.id.sign_up_button);
        mSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent createAccountIntent = new Intent(v.getContext(), CreateAccountActivity.class);
                startActivity(createAccountIntent);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid password, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        // Check that a password was provided
        if(TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(this, username, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Integer> {
        private final String LOG_TAG = UserLoginTask.class.getSimpleName();

        private final Activity mParent;
        private final String mUsername;
        private final String mPassword;
        private String firstName;
        private String lastName;
        private String userId;

        UserLoginTask(Activity parent, String username, String password) {
            mParent = parent;
            mUsername = username;
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
                return 2;
            } else {
                try {
                    // construct the URL to fetch a user
                    Uri.Builder  builder = new Uri.Builder();
                    builder.scheme("http")
                            .encodedAuthority(CLOUD_SERVER_IP)
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("users")
                            .appendQueryParameter("username", mUsername)
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
                    if(responseCode == 200) {
                        // read response to get user data from server
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String line = "";
                        String responseBody = "";
                        while((line = reader.readLine()) != null) {
                            responseBody += line + '\n';
                        }

                        // parse response as JSON
                        JSONArray array = new JSONArray(responseBody);
                        JSONObject user = array.getJSONObject(0);
                        firstName = user.getString("firstName");
                        lastName = user.getString("lastName");
                        userId = user.getString("ssUserId");

                        return 1;
                    } else {
                        return 0;
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

            // if anything goes wrong, don't let them log in and act like the password was wrong
            return 0;
        }

        @Override
        protected void onPostExecute(final Integer success) {
            mAuthTask = null;
            showProgress(false);

            if (success == 1) {
                // store username for later use
                SharedPreferences savedData = mParent.getSharedPreferences(getString(R.string.saved_data_file_key),
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = savedData.edit();
                editor.putString(getString(R.string.prompt_username), mUsername);
                editor.putString(getString(R.string.prompt_password), mPassword);
                editor.putString(getString(R.string.prompt_first_name), firstName);
                editor.putString(getString(R.string.prompt_last_name), lastName);
                editor.putString(getString(R.string.userId), userId);
                editor.commit();

                // clear text boxes so they're empty when user logs out
                mUsernameView.setText("");
                mPasswordView.setText("");

                // launch main activity so user can begin browsing
                Intent intent = new Intent(mParent, MainActivity.class);
                startActivity(intent);
                finish();
            } else if (success == 0){
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            } else if (success == 2) {
                // this means there was no internet connection, so we will just
                // wait for the user to turn on internet again
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

