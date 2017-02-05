package com.insertcoolnamehere.showandsell;

import android.database.DataSetObserver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DonateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        ListView steps = (ListView) findViewById(R.id.steps_listview);
        ListAdapter adapter = new StepAdapter();
        steps.setAdapter(adapter);
    }

    private class StepAdapter implements ListAdapter {
        /**
         * The total number of steps displayed by this adapter
         */
        private final int STEP_COUNT = 5;
        /**
         * The currently active step
         */
        private int mCurrentStep = 0;

        public void registerDataSetObserver (DataSetObserver observer){}
        public void unregisterDataSetObserver (DataSetObserver observer) {}
        public boolean hasStableIds() {return true;}
        public int getViewTypeCount() {return 1;}
        public boolean isEmpty() {return false;}
        public boolean isEnabled(int id) {return true;}
        public boolean areAllItemsEnabled() {return true;}
        public int getCount() {return STEP_COUNT;}

        public View getView (int position, View convertView, ViewGroup parent) {
            View finalProduct;
            if (convertView == null) {
                // inflate XML
                finalProduct = getLayoutInflater().inflate(R.layout.fragment_step_inactive, parent, false);

                // configure content
                TextView stepNumberView = (TextView) finalProduct.findViewById(R.id.step_number_view);
                stepNumberView.setText(""+(position+1));

                TextView stepLabel = (TextView) finalProduct.findViewById(R.id.primary_step_label);
                switch(position) {
                    case 0:
                        stepLabel.setText(R.string.step_take_pic_label);
                        break;
                    case 1:
                        stepLabel.setText(R.string.step_title_label);
                        break;
                    case 2:
                        stepLabel.setText(R.string.step_details_label);
                        break;
                    case 3:
                        stepLabel.setText(R.string.step_price_label);
                        break;
                    case 4:
                        stepLabel.setText(R.string.step_condition_label);
                        break;
                }

                // remove connector line for last element
                if(position == STEP_COUNT - 1) {
                    View connector = finalProduct.findViewById(R.id.step_connector);
                    connector.setVisibility(View.GONE);
                }
            } else {
                finalProduct = convertView;
            }
            return finalProduct;
        }

        public int getItemViewType(int position) {
            return AdapterView.ITEM_VIEW_TYPE_IGNORE;
        }

        public long getItemId(int position) {return position;}

        public Object getItem(int position) {return position;}

    }
}
