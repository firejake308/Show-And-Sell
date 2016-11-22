package com.insertcoolnamehere.showandsell;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;

import com.insertcoolnamehere.showandsell.dummy.DummyContent;
import com.insertcoolnamehere.showandsell.logic.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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

        // set up comments list view
        ListView listView = (ListView) findViewById(R.id.item_comments);
        ArrayList<String> list = new ArrayList<String>();
        ListIterator<DummyContent.DummyItem> iter = DummyContent.ITEMS.listIterator();
        while(iter.hasNext()) {
            DummyContent.DummyItem item = iter.next();
            list.add(item.content);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.text_view_comment, R.id.textView_comment, list);
        listView.setAdapter(adapter);
    }
}
