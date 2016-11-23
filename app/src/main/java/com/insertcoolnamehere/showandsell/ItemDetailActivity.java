package com.insertcoolnamehere.showandsell;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;

import com.insertcoolnamehere.showandsell.dummy.DummyContent;
import com.insertcoolnamehere.showandsell.logic.Item;

import java.util.ArrayList;
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
        CommentAdapter<String> adapter = new CommentAdapter<String>(this, R.layout.text_view_comment_right, list);
        listView.setAdapter(adapter);
    }

    private class CommentAdapter<T extends String> extends ArrayAdapter<String> {
        private final int LEFT_COMMENT = 0;
        private final int RIGHT_COMMENT = 1;

        private ArrayList<String> mList;

        private CommentAdapter(Context context, int layout, ArrayList<String> list) {
            super(context, layout, list);

            mList = list;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return position % 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                if(getItemViewType(position) == LEFT_COMMENT) {
                    Log.d("CommentAdapter", "creating left comment");
                    View rootView = getLayoutInflater().inflate(R.layout.text_view_comment_left, parent, false);
                    TextView comment = (TextView) rootView.findViewById(R.id.textView_comment_left);
                    comment.setText(mList.get(position));
                    return rootView;
                } else {
                    View rootView = getLayoutInflater().inflate(R.layout.text_view_comment_right, parent, false);
                    TextView comment = (TextView) rootView.findViewById(R.id.textView_comment_right);
                    comment.setText(mList.get(position));
                    return rootView;
                }
            } else {
                if(getItemViewType(position) == LEFT_COMMENT) {
                    Log.d("CommentAdapter", "creating left comment");
                    TextView comment = (TextView) convertView.findViewById(R.id.textView_comment_left);
                    comment.setText(mList.get(position));
                    return convertView;
                } else {
                    TextView comment = (TextView) convertView.findViewById(R.id.textView_comment_right);
                    comment.setText(mList.get(position));
                    return convertView;
                }
            }
        }
    }
}
