package com.insertcoolnamehere.showandsell;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * A fragment from where the user can browse all of their bookmarks
 */
public class BookmarksFragment extends BrowseFragment {

    public BookmarksFragment() {
        // Required empty public constructor
    }

    /**
     * Helper method that returns the appropriate API Call as a URL for the FetchItemsTask
     * @param id group ID
     * @return API call as a URL
     * @throws MalformedURLException
     */
    @Override
    protected URL getAPICall(String id) throws MalformedURLException {
        if(getActivity() == null) {
            return null;
        } else {
            // get user id and password
            SharedPreferences savedData = getActivity().getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
            String userId = savedData.getString(getString(R.string.userId), "NULL");
            String pw = savedData.getString(getString(R.string.prompt_password), "NULL");

            // construct the URL to fetch bookmarks
            Uri.Builder  builder = new Uri.Builder();
            builder.scheme("http")
                    .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                    .appendPath("showandsell")
                    .appendPath("api")
                    .appendPath("bookmarks")
                    .appendPath("bookmarks")
                    .appendQueryParameter("userId", userId)
                    .appendQueryParameter("password", pw)
                    .build();
            Log.d("BookmarksFragment", "URL: "+builder.toString());
            return new URL(builder.toString());
        }
    }

    /**
     * Equivalent to <code>this instanceof BookmarksFragment</code>
     * @return
     */
    @Override
    protected boolean isBookmark() {
        return true;
    }
}
