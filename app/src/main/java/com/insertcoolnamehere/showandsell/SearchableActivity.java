package com.insertcoolnamehere.showandsell;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.insertcoolnamehere.showandsell.logic.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class SearchableActivity extends AppCompatActivity {
    private ArrayList<Item> mResults;
    private SearchResultsAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);

        // set adapter for list view
        ListView listView = (ListView) findViewById(R.id.search_results_list);
        mResults = new ArrayList<Item>();
        mAdapter = new SearchResultsAdapter(this, R.id.search_results_list, mResults);
        listView.setAdapter(mAdapter);

        // Get the intent, verify the action, and get the query
        Intent intent = getIntent();
        if(intent.getAction().equals(Intent.ACTION_SEARCH)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            search(query);
        }
    }

    /**
     * Searches for an item that matches the query, either in its name or description
     * @param query the value the user entered into the search bar
     */
    private void search(String query) {
        ArrayList<Item> items = Item.getItemsList();
        ListIterator<Item> iter = items.listIterator();
        while(iter.hasNext()) {
            Item curr = iter.next();
            if (curr.getName().toLowerCase().contains(query.toLowerCase())) {
                mResults.add(curr);
            } else if (curr.getDescription().toLowerCase().contains(query.toLowerCase())) {
                mResults.add(curr);
            }
        }

        displayResults(mResults);
    }

    private void displayResults(ArrayList<Item> list) {
        mAdapter.notifyDataSetChanged();
    }

    public class SearchResultsAdapter<T extends Item> extends ArrayAdapter<Item> {
        public SearchResultsAdapter(Context context, int resource, ArrayList<Item> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            View view;
            if(convertView == null) {
                // inflate a view of an item and its details
                view = getLayoutInflater().inflate(R.layout.fragment_item, parent, false);
            } else {
                view = convertView;
            }

            // populate the view
            TextView nameView = (TextView) view.findViewById(R.id.item_name);
            TextView descriptionView = (TextView) view.findViewById(R.id.item_description);
            TextView priceView = (TextView) view.findViewById(R.id.item_price);
            TextView conditionView = (TextView) view.findViewById(R.id.item_condition);
            ImageView imageView = (ImageView) view.findViewById(R.id.item_picture);

            Item item = getItem(position);
            nameView.setText(item.getName());
            descriptionView.setText(item.getDescription());
            priceView.setText(String.format("$%.2f", item.getPrice()));
            conditionView.setText(item.getCondition());
            imageView.setImageBitmap(item.getPic());

            return view;
        }
    }
}
