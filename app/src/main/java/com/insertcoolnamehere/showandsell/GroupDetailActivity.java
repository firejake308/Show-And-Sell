package com.insertcoolnamehere.showandsell;

import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RatingBar;
import android.widget.TextView;

public class GroupDetailActivity extends AppCompatActivity {

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
        CollapsingToolbarLayout toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        if (name != null)
            toolbarLayout.setTitle(name);

        // populate group data displays
        TextView addressView = (TextView) findViewById(R.id.group_detail_address);
        addressView.setText(address);
        TextView locationDetailView = (TextView) findViewById(R.id.group_detail_extra_info);
        locationDetailView.setText(locationDetail);
        RatingBar ratingBar = (RatingBar) findViewById(R.id.rating_bar);
        ratingBar.setRating((float) rating);
        Log.d("GroupDetail", "rating "+rating);
    }
}
