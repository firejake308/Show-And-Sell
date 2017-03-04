package com.insertcoolnamehere.showandsell;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * A variation of the ItemDetailActivity specifically for use when managing a group
 */

public class ManageItemActivity extends ItemDetailActivity implements ConfirmDeleteFragment.OnConfirmDeleteListener {
    private static final String LOG_TAG = ManageItemActivity.class.getSimpleName();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_manage_item, menu);
        if (mItem.isApproved()) {
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_save));
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_approve) {
            Log.d(LOG_TAG, "approve this item, sir!");
            TextView priceView = (TextView) findViewById(R.id.item_detail_price);
            String price = priceView.getText().toString();
            TextView detailsView = (TextView) findViewById(R.id.item_detail_description);
            String details = detailsView.getText().toString();
            new ApproveItemTask(this, price, details, true).execute();
            showProgress(true);
            return true;
        } else if (item.getItemId() == R.id.action_reject) {
            new ConfirmDeleteFragment().setListener(this).show(getFragmentManager(), "ConfirmDeleteFragment");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onConfirmDelete() {
        new RejectItemTask(this).execute();
        showProgress(true);
    }
}
