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
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnOpenBookmarkListener} interface
 * to handle interaction events.
 * Use the {@link BookmarksFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BookmarksFragment extends BrowseFragment {

    private OnOpenBookmarkListener mListener;

    public BookmarksFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment BookmarksFragment.
     */
    public static BookmarksFragment newInstance(String param1, String param2) {
        BookmarksFragment fragment = new BookmarksFragment();
        return fragment;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(String itemId) {
        if (mListener != null) {
            mListener.onOpenBookmark(itemId);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnOpenBookmarkListener) {
            mListener = (OnOpenBookmarkListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnOpenBookmarkListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    protected URL getAPICall(String id) throws MalformedURLException {
        // get user id and pass
        if(getActivity() == null) {
            return null;
        } else {
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
                    .appendQueryParameter("userId", userId)
                    .appendQueryParameter("password", pw)
                    .build();
            return new URL(builder.toString());
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnOpenBookmarkListener {
        void onOpenBookmark(String itemId);
    }
}
