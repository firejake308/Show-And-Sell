package com.insertcoolnamehere.showandsell;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * A variation of the ItemDetailActivity specifically for use when managing a group
 */

public class ManageItemActivity extends ItemDetailActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_manage_item, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_approve) {
            if (!mItem.isApproved()) {
                new ApproveItemTask(this, true).execute();
                showProgress(true);
            }
            return true;
        } else if (item.getItemId() == R.id.action_reject) {
            new RejectItemTask(this).execute();
            showProgress(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
