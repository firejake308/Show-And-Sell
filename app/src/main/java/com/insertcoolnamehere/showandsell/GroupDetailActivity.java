package com.insertcoolnamehere.showandsell;

import android.content.Intent;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;

public class GroupDetailActivity extends AppCompatActivity implements BrowseFragment.OnListFragmentInteractionListener {

    public static final String EXTRA_NAME = "GROUP_NAME";
    public static final String EXTRA_ADDRESS = "GROUP_ADDRESS";
    public static final String EXTRA_LOCATION_DETAIL = "GROUP_LOCATION_DETAIL";
    public static final String EXTRA_RATING = "GROUP_RATING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        // fetch string fields from extras
        String name = getIntent().getStringExtra(EXTRA_NAME);
        String address = getIntent().getStringExtra(EXTRA_ADDRESS);
        String locationDetail = getIntent().getStringExtra(EXTRA_LOCATION_DETAIL);
        double rating = getIntent().getDoubleExtra(EXTRA_RATING, 0.0);

        // show group name
        setTitle(name);

        // populate group data displays
        TextView addressView = (TextView) findViewById(R.id.group_detail_address);
        addressView.setText(address);
        TextView locationDetailView = (TextView) findViewById(R.id.group_detail_extra_info);
        locationDetailView.setText(locationDetail);
        RatingBar ratingBar = (RatingBar) findViewById(R.id.rating_bar);
        ratingBar.setRating((float) rating);
        Log.d("GroupDetail", "rating "+rating);

        // show group items
        Fragment browseFrag = BrowseFragment.newInstance(2);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.browse_frag_placeholder, browseFrag).commit();
    }

    @Override
    public void onListFragmentInteraction(String itemId) {
        // start the ItemDetailActivity and tell it which item to show
        Intent showItemDetailIntent = new Intent(this, ItemDetailActivity.class);
        showItemDetailIntent.putExtra(ItemDetailActivity.ITEM_ID, itemId);
        showItemDetailIntent.putExtra(ItemDetailActivity.OWNER_POWERS, false);
        startActivity(showItemDetailIntent);
    }

    @Override
    public void openChooseGroup() {
        Intent intent = new Intent(this, ChooseGroupActivity.class);
        startActivity(intent);
    }
}
