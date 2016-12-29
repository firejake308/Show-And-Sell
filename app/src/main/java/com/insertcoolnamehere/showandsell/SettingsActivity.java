package com.insertcoolnamehere.showandsell;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.insertcoolnamehere.showandsell.logic.Item;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatActivity {
    // create settings activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        // add actions when buttons are pressed
        Button logout = (Button) findViewById(R.id.settings_logout);
        logout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });

        Button chooseGroup = (Button) findViewById(R.id.settings_choose_default_group);
        chooseGroup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                openChooseGroup();
            }
        });

        Button manageGroup = (Button) findViewById(R.id.settings_manage_group);
        manageGroup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    /**
     * Exit settings activity and open choose group activity
     */
    private void openChooseGroup() {
        //go to choose group activity
        Intent intent = new Intent(this, ChooseGroupActivity.class);
        startActivity(intent);
    }

    /**
    * Log out the current user and return to the login screen
    */
    private void logout() {
        // erase username from saved data
        SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = savedData.edit();
        editor.remove(getString(R.string.prompt_username));
        editor.remove(getString(R.string.prompt_password));
        editor.remove(getString(R.string.prompt_first_name));
        editor.remove(getString(R.string.prompt_last_name));
        editor.remove(getString(R.string.userId));
        // the next user to log in may or may not be a group owner
        editor.putBoolean(getString(R.string.group_owner_boolean), false);
        editor.apply();

        // when you log out, clear the items cache
        Item.clearItemsCache();

        // go back to login
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
