package com.insertcoolnamehere.showandsell;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple {@link DialogFragment} subclass that confirms that a group manager actually wants to
 * delete an item
 */
public class ConfirmDeleteFragment extends DialogFragment {

    private OnConfirmDeleteListener mListener;

    public ConfirmDeleteFragment setListener(OnConfirmDeleteListener listener) {
        mListener = listener;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // use the builder to construct a confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.confirm_delete);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // continue with deleting item
                mListener.onConfirmDelete();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do...nothing!
            }
        });
        return builder.create();
    }

    /**
     * Listener that deals with actual deletion of items
     */
    public interface OnConfirmDeleteListener {
        /**
         * Once the user confirms the deletion, sends the request to remove the item from the server
         */
        void onConfirmDelete();
    }
}
