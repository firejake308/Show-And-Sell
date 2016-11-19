package com.insertcoolnamehere.showandsell;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.insertcoolnamehere.showandsell.logic.Item;

public class ItemDetailActivity extends AppCompatActivity {

    public static final String ITEM_ID = "ITEM_ID";

    private Item mItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the layout of the activity
        setContentView(R.layout.activity_item_detail);

        // get item data from intent
        mItem = (Item) Item.getItem(getIntent().getStringExtra(ITEM_ID));
    }
}
