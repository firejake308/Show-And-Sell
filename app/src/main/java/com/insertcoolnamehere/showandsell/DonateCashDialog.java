package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Dialog to accept cash donations to groups
 */

public class DonateCashDialog extends DialogFragment {
    private OnDonateCashListener mListener;

    public DonateCashDialog setListener(OnDonateCashListener listener) {
        mListener = listener;
        return this;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_donate_cash, null);
        builder.setView(dialogView);
        final Activity activity = getActivity();
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // initiate donate
                EditText amountEntry = (EditText) dialogView.findViewById(R.id.donation_amount_entry);
                mListener.onDonateCash(Double.parseDouble(amountEntry.getText().toString()));
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });

        return builder.create();
    }

    public interface OnDonateCashListener {
        void onDonateCash(double amount);
    }
}
