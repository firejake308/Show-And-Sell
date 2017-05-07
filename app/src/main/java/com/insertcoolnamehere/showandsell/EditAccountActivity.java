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

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.insertcoolnamehere.showandsell.logic.CryptoTool;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static android.content.ContentValues.TAG;

/**
 * Created by Patrick on 08/03/2017.
 *
 * The purpose of this activity is to allow the user to change account details
 */

public class EditAccountActivity extends AppCompatActivity {
    // references to UI views
    private EditText firstNameEntry;
    private EditText lastNameEntry;
    private EditText emailEntry;
    private EditText passwordEntry;
    private EditText confirmPwEntry;
    private Button updateAccountButton;

    private String firstName;
    private String lastName;
    private String email;
    private String password0;
    private String password1;
    private String password2;
    private String userId;

    // reference to AsyncTask
    private EditAccountActivity.EditAccountTask mAuthTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_account);

        SharedPreferences savedData = this.getSharedPreferences(getString(R.string.saved_data_file_key),
                Context.MODE_PRIVATE);
        String fn = savedData.getString(getString(R.string.prompt_first_name), "");
        String ln = savedData.getString(getString(R.string.prompt_last_name), "");
        String em = savedData.getString(getString(R.string.prompt_email), "");
        String pa = CryptoTool.decrypt(savedData.getString(getString(R.string.prompt_password), ""));
        String pa2 = CryptoTool.decrypt(savedData.getString(getString(R.string.prompt_password), ""));

        password0 = CryptoTool.encrypt(pa);

        // find all the views
        firstNameEntry = (EditText) findViewById(R.id.first_name_entry_edit);
        firstNameEntry.setText(fn);
        lastNameEntry = (EditText) findViewById(R.id.last_name_entry_edit);
        lastNameEntry.setText(ln);
        emailEntry = (EditText) findViewById(R.id.email_entry_edit);
        emailEntry.setText(em);
        passwordEntry = (EditText) findViewById(R.id.password_entry_edit);
        passwordEntry.setText(pa);
        confirmPwEntry = (EditText) findViewById(R.id.confirm_password_edit);
        confirmPwEntry.setText(pa2);
        updateAccountButton = (Button) findViewById(R.id.update_account_btn);

        // remember that the user signed in with google
        boolean isGoogleUser = savedData.getBoolean(getString(R.string.is_google_user), false);
        if (isGoogleUser) {
            passwordEntry.setEnabled(false);
            passwordEntry.setFocusable(false);
            confirmPwEntry.setEnabled(false);
            confirmPwEntry.setFocusable(false);
        }

        updateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptUpdateAccount();
            }
        });
    }

    private void attemptUpdateAccount() {
        // if the AsyncTask has already been created, then don't restart it

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

            mAuthTask = new EditAccountActivity.EditAccountTask(this, firstName, lastName, email, CryptoTool.encrypt(password1));
            mAuthTask.execute();
        }
    }

    public class EditAccountTask extends AsyncTask<Void, Void, Boolean> {

        private final String LOG_TAG = EditAccountActivity.EditAccountTask.class.getSimpleName();

        private Activity mParent;

        private String firstName;
        private String lastName;
        private String email;
        private String password;

        EditAccountTask(Activity parent, String fn, String ln, String email, String pw) {
            this.mParent = parent;

            this.firstName = fn;
            this.lastName = ln;
            this.email = email;
            this.password = pw;
        }

        protected Boolean doInBackground(Void... Params) {
            SharedPreferences savedData = mParent.getSharedPreferences(getString(R.string.saved_data_file_key),
                    Context.MODE_PRIVATE);
            userId = savedData.getString(getString(R.string.userId), "");

            Uri.Builder builder = new Uri.Builder();
            String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                    .scheme("http")
                    .appendPath("showandsell")
                    .appendPath("api")
                    .appendPath("users")
                    .appendPath("update")
                    .appendQueryParameter("id", userId)
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

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(15000);
                    connection.setRequestMethod("PUT");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setChunkedStreamingMode(0);
                    connection.connect();

                    String groupId = savedData.getString(getString(R.string.saved_group_id), "");

                    // format user input as JSON
                    String body = "";
                    JSONObject user = new JSONObject();
                    user.put("newEmail", email);
                    user.put("oldPassword", password0);
                    user.put("newPassword", password);
                    user.put("newFirstName", firstName);
                    user.put("newLastName", lastName);
                    user.put("newGroupId", groupId);
                    body = user.toString();

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
                editor.putString(getString(R.string.prompt_password), CryptoTool.encrypt(password));
                editor.putString(getString(R.string.prompt_first_name), firstName);
                editor.putString(getString(R.string.prompt_last_name), lastName);
                editor.putString(getString(R.string.userId), userId);
                editor.apply();

                // launch tutorial activity so user can figure out how app works

            } else {
                // alert user that they had a duplicate email
                emailEntry.setError(getString(R.string.error_email_duplicate));
                emailEntry.requestFocus();
            }
        }
    }
}
