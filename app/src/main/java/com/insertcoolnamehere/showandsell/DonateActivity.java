package com.insertcoolnamehere.showandsell;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.DataSetObserver;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class DonateActivity extends AppCompatActivity {

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        mListView = (ListView) findViewById(R.id.steps_listview);
        BaseAdapter adapter = new StepAdapter();
        mListView.setAdapter(adapter);
    }

    private class StepAdapter extends BaseAdapter {
        /**
         * The total number of steps displayed by this adapter
         */
        private final int STEP_COUNT = 5;

        private final int STEP_ACTIVE = 1;

        private final int STEP_INACTIVE = 0;
        /**
         * The currently active step
         */
        private int mCurrentStep = 0;

        public void registerDataSetObserver (DataSetObserver observer){}
        public void unregisterDataSetObserver (DataSetObserver observer) {}
        public boolean hasStableIds() {return true;}
        public int getViewTypeCount() {return 2;}
        public boolean isEmpty() {return false;}
        public boolean isEnabled(int id) {return true;}
        public boolean areAllItemsEnabled() {return true;}
        public int getCount() {return STEP_COUNT;}

        public View getView (int position, final View convertView, ViewGroup parent) {
            final View finalProduct;
            // inflate XML based on whether step is active or inactive
            if (getItemViewType(position) == STEP_INACTIVE)
                finalProduct = getLayoutInflater().inflate(R.layout.fragment_step_inactive, parent, false);
            else
                finalProduct = getLayoutInflater().inflate(R.layout.fragment_step_active, parent, false);

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

            // activate button for active step
            if(getItemViewType(position) == STEP_ACTIVE) {
                final Button nextStepBtn = (Button) finalProduct.findViewById(R.id.btn_next_step);
                nextStepBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCurrentStep++;
                        View editText = findViewById(R.id.item_description_entry);
                        if (editText != null)
                            editText.getParent().clearChildFocus(editText);
                        mListView.invalidateViews();
                    }
                });

                // configure input view
                LinearLayout flex = (LinearLayout) finalProduct.findViewById(R.id.step_input_parent);
                switch(mCurrentStep) {
                    case 0:
                        // create an imageview
                        ImageView uploadImg = (ImageView) getLayoutInflater().inflate(R.layout.upload_img_view, flex, false);

                        // create the explanatory textview
                        TextView instructions = new TextView(getApplicationContext());
                        instructions.setText(R.string.prompt_confirm_password);
                        instructions.setTextColor(getColor(R.color.hintOrDisabledText));

                        flex.addView(instructions);
                        flex.addView(uploadImg);

                        // TODO update connector length
                        //View connector = finalProduct.findViewById(R.id.step_connector);
                        //connector.setMinimumHeight(184);
                        break;
                    case 1:
                        // inflate EditText for title entry
                        View titleEntry = getLayoutInflater().inflate(R.layout.item_title_entry, flex, false);
                        flex.addView(titleEntry);
                        break;
                    case 2:
                        View detailsCustom = getLayoutInflater().inflate(R.layout.item_details_entry, flex, false);
                        flex.addView(detailsCustom);
                        break;
                    case 3:
                        View priceCustom = getLayoutInflater().inflate(R.layout.item_price_entry, flex, false);
                        flex.addView(priceCustom);
                        break;
                    case 4:
                        View conditionCustom = getLayoutInflater().inflate(R.layout.item_condition_entry, flex, false);
                        Spinner conditionEntry = (Spinner) conditionCustom.findViewById(R.id.item_condition_entry);
                        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(),
                                R.array.possible_conditions, android.R.layout.simple_spinner_item);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        conditionEntry.setAdapter(adapter);
                        flex.addView(conditionCustom);
                        break;
                }
            }

            // remove connector line for last step
            if(position == STEP_COUNT - 1) {
                View connector = finalProduct.findViewById(R.id.step_connector);
                connector.setVisibility(View.GONE);
            }
            return finalProduct;
        }

        public int getItemViewType(int position) {
            if (position == mCurrentStep)
                return STEP_ACTIVE;
            else
                return STEP_INACTIVE;
        }

        public long getItemId(int position) {return position;}

        public Object getItem(int position) {return position;}

    }
}
