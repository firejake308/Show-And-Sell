package com.insertcoolnamehere.showandsell;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
        emailEntry = (EditText) findViewById(R.id.last_name_entry);
        usernameEntry = (EditText) findViewById(R.id.username_entry);
        passwordEntry = (EditText) findViewById(R.id.username_entry);
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
        String password2 = passwordEntry.getText().toString();

        // first, validate the email
        if(!email.contains("@") || !email.contains(".")) {
            cancel = true;
            emailEntry.setError("");
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
        else if(password1 != password2) {
            cancel = true;
            passwordEntry.setError(getString(R.string.error_password_match));
            focusView = passwordEntry;
        }

        if(cancel) {
            // cancel and inform user of any errors
            focusView.requestFocus();
        } else {
            mAuthTask = new CreateAccountTask(firstName, lastName, email, username, password1);
            mAuthTask.execute();
        }
    }

    public class CreateAccountTask extends AsyncTask<Void, Void, Boolean> {
        private String firstName;
        private String lastName;
        private String email;
        private String username;
        private String password;

        CreateAccountTask(String fn, String ln, String email, String un, String pw) {
            this.firstName = fn;
            this.lastName = ln;
            this.email = email;
            this.username = un;
            this.password = pw;
        }

        protected Boolean doInBackground(Void... Params) {
            return false;
        }
    }
}
