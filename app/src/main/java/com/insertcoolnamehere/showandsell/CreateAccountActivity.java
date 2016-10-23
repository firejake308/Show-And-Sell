package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
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

public class CreateAccountActivity extends AppCompatActivity {

    // references to UI views
    private EditText firstNameEntry;
    private EditText lastNameEntry;
    private EditText emailEntry;
    private EditText usernameEntry;
    private EditText passwordEntry;
    private EditText confirmPwEntry;
    private Button createAccountButton;

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
        usernameEntry = (EditText) findViewById(R.id.username_entry);
        passwordEntry = (EditText) findViewById(R.id.password_entry);
        confirmPwEntry = (EditText) findViewById(R.id.confirm_password);
        createAccountButton = (Button) findViewById(R.id.create_account_btn);

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
    }

    private void attemptCreateAccount() {
        // if the AsyncTask has already been created, then don't restart it

        // for reporting errors
        boolean cancel = false;
        View focusView = null;

        // fetch values from EditTexts
        String firstName = firstNameEntry.getText().toString();
        String lastName = lastNameEntry.getText().toString();
        String email = emailEntry.getText().toString();
        String username = usernameEntry.getText().toString();
        String password1 = passwordEntry.getText().toString();
        String password2 = confirmPwEntry.getText().toString();

        // first, validate the email
        if(!email.contains("@") || !email.contains(".")) {
            cancel = true;
            emailEntry.setError("Invalid email");
            focusView = emailEntry;
        }
        // verify that the username is long enough
        else if(username.length() < 4) {
            cancel = true;
            usernameEntry.setError("Username must be at least 4 characters");
            focusView = usernameEntry;
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
            mAuthTask = new CreateAccountTask(this, firstName, lastName, email, username, password1);
            mAuthTask.execute();
        }
    }

    public class CreateAccountTask extends AsyncTask<Void, Void, Boolean> {

        private final String LOG_TAG = CreateAccountTask.class.getSimpleName();

        private Activity mParent;

        private String firstName;
        private String lastName;
        private String email;
        private String username;
        private String password;

        CreateAccountTask(Activity parent, String fn, String ln, String email, String un, String pw) {
            this.mParent = parent;

            this.firstName = fn;
            this.lastName = ln;
            this.email = email;
            this.username = un;
            this.password = pw;
        }

        protected Boolean doInBackground(Void... Params) {
            String uri = new Uri.Builder().scheme("http")
                    .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                    .appendPath("showandsell")
                    .appendPath("api")
                    .appendPath("users")
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

                    // format user input as JSON
                    String body = "";
                    JSONObject user = new JSONObject();
                    user.put("username", username);
                    user.put("password", password);
                    user.put("firstName", firstName);
                    user.put("lastName", lastName);
                    user.put("email", email);
                    body = String.valueOf(user);

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
                usernameEntry.setError(getString(R.string.error_un_email_duplicate));
                usernameEntry.requestFocus();
            }
        }
    }
}
