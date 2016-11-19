package com.insertcoolnamehere.showandsell;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;

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

        // set text and images for the activity view
        ImageView itemImage = (ImageView) findViewById(R.id.item_detail_image);
        itemImage.setImageBitmap(mItem.getPic());

        TextView itemName = (TextView) findViewById(R.id.item_detail_name);
        itemName.setText(mItem.getName());
        TextView itemPrice = (TextView) findViewById(R.id.item_detail_price);
        itemPrice.setText("" + mItem.getPrice());
        TextView itemCondition = (TextView) findViewById(R.id.item_detail_condition);
        itemCondition.setText(mItem.getCondition());
        TextView itemDescription = (TextView) findViewById(R.id.item_detail_description);
        itemDescription.setText(mItem.getDescription());
    }
}
