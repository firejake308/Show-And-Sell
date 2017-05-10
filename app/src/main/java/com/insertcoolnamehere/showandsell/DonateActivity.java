package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
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
import android.widget.Toast;

import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.insertcoolnamehere.showandsell.logic.ResultCode;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.insertcoolnamehere.showandsell.logic.ResultCode.NO_INTERNET;
import static com.insertcoolnamehere.showandsell.logic.ResultCode.OTHER_FAILURE;
import static com.insertcoolnamehere.showandsell.logic.ResultCode.SUCCESS;

public class DonateActivity extends AppCompatActivity implements DonateCashDialog.OnDonateCashListener {

    private static final int REQUEST_TAKE_PHOTO = 13;
    private static final int REQUEST_PAYMENT = 11;
    private static final String LOG_TAG = DonateActivity.class.getSimpleName();
    public static final String EXTRA_GROUP_ID = "GROUP_ID";
    public static final String EXTRA_GROUP_NAME = "GROUP_NAME";

    private ListView mListView;
    private StepAdapter mAdapter;

    private String mDescription;
    private String mDetails;
    private String mPrice;
    private String mCondition;
    private Bitmap mThumbnail;
    private Bitmap mImage;

    private String mCurrentPhotoPath;
    private String mToken;
    private double mAmount;

    private boolean donateAttempted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        donateAttempted = false;

        // initialize list view of steps
        mListView = (ListView) findViewById(R.id.steps_listview);
        mAdapter = new StepAdapter();
        mListView.setAdapter(mAdapter);

        // set the title to include the name of the group
        String groupName = getIntent().getStringExtra(EXTRA_GROUP_NAME);
        if (groupName != null) {
            setTitle(getString(R.string.donate_to_group_pattern) + " " + groupName);
        }

        // set up donate cash link
        View link = findViewById(R.id.donate_cash_link);
        final DonateCashDialog.OnDonateCashListener listener = this;
        link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DonateCashDialog().setListener(listener).show(getSupportFragmentManager(), "DonateCashDialog");
            }
        });
    }

    @Override
    public void onDonateCash(double amount) {
        // pop up payment dialog
        mAmount = amount;
        new GetClientTokenTask(this).execute();
    }

    private void donate() {
        Log.d(LOG_TAG, "Beginning to donate item");
        donateAttempted = true;
        // make sure the user has actually given us all the fields we asked for
        if(mDescription.length() == 0) {
            mAdapter.openItem(1);
            return;
        } else if(mDetails.length() == 0) {
            mAdapter.openItem(2);
            return;
        } else if(mPrice.length() == 0) {
            mAdapter.openItem(3);
            return;
        } else if(Double.parseDouble(mPrice) < 0.31){
            mAdapter.openItem(3);
        } else if(mImage == null) {
            mAdapter.openItem(0);
            Toast.makeText(this, "Please provide a picture", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(LOG_TAG, "Making progress");
        showProgress(true);
        new UploadItemTask(this, mDescription, mPrice, mCondition, mDetails, mImage).execute();
    }

    private void attemptTakePic() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // make sure that a camera exists to catch this intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // create file to hold image
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to create image file", e);
            }

            // continue if file creation was successful
            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(this, "com.insertcoolnamehere.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void initiatePurchase() {
        new GetClientTokenTask(this).execute();
    }
    private void finishPurchase() {
        DropInRequest dropInRequest = new DropInRequest()
                .clientToken(mToken);
        startActivityForResult(dropInRequest.getIntent(this), REQUEST_PAYMENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            // Get the dimensions of the View
            ImageView mImageView = (ImageView) findViewById(R.id.upload_img_btn);
            int targetW = mImageView.getWidth();
            int targetH = mImageView.getHeight();
            Log.d(LOG_TAG, "onResult targetW: "+targetW);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;
            if(bmOptions.outHeight <= 0)
                return;

            // Determine how much to scale down the image
            int scaleFactor = Math.max(photoW/targetW, photoH/targetH);
            Log.d(LOG_TAG, "onResult photoW: "+photoW);
            Log.d(LOG_TAG, "onResult SF: "+scaleFactor);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            mThumbnail = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            mImageView.setImageBitmap(mThumbnail);

            // get larger image for upload
            bmOptions.inSampleSize = 8;
            mImage = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        } else if (requestCode == REQUEST_PAYMENT) {
            if (resultCode == Activity.RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                new DonateMoneyTask(this, result.getPaymentMethodNonce(), mToken, mAmount).execute();
            } else if (resultCode != Activity.RESULT_CANCELED) {
                // handle errors here, an exception may be available in
                Log.e("ItemDetailActivity", ""+data.getSerializableExtra(DropInActivity.EXTRA_ERROR));
            }
        }
    }

    /**
     * Helper method that creates an image file where we can write full-size camera images
     * @return file where JPEG image will be stored
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH).format(new Date());
        String filename = "SANDS_ITEM_"+timestamp;
        //File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.d(LOG_TAG, storageDirectory.getAbsolutePath());
        File imgFile = File.createTempFile(filename, ".jpg", storageDirectory);
        mCurrentPhotoPath = imgFile.getAbsolutePath();
        return  imgFile;
    }

    /**
     * Shows a loading animation
     * @param loading whether or not to show the loading animation
     */
    private void showProgress(boolean loading) {
        View progressBar = findViewById(R.id.donate_progress_bar);
        View textView = findViewById(R.id.donate_cash_link);
        if (loading) {
            mListView.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            mListView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
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

            if (position < mCurrentStep) {
                // highlight comleted steps green
                View stepCircle = finalProduct.findViewById(R.id.step_number_view);
                ImageView stepCircleNew = (ImageView) finalProduct.findViewById(R.id.step_completed_view);
                stepCircleNew.setVisibility(View.VISIBLE);
            }

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

            // configure active step
            if(getItemViewType(position) == STEP_ACTIVE) {
                // activate button
                final Button nextStepBtn = (Button) finalProduct.findViewById(R.id.btn_next_step);
                nextStepBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText editText = (EditText) findViewById(R.id.item_description_entry);
                        if (editText != null) {
                            editText.getParent().clearChildFocus(editText);

                            // get input from edit text
                            if (mCurrentStep == 1)
                                mDescription = editText.getText().toString();
                            else if (mCurrentStep == 2)
                                mDetails = editText.getText().toString();
                            else if (mCurrentStep == 3) {
                                if (!TextUtils.isEmpty(editText.getText()))
                                    mPrice = String.format(Locale.ENGLISH, "%.2f", Double.parseDouble(editText.getText().toString()));
                            }
                        } else if (mCurrentStep == 4) {
                            donate();
                            return;
                        }
                        mCurrentStep++;
                        mListView.invalidateViews();
                    }
                });

                // change text to finished for last step
                if (position == STEP_COUNT -1 ) {
                    nextStepBtn.setText(getString(R.string.action_donate));
                } else {
                    nextStepBtn.setText(getString(R.string.next_step_btn_label));
                }

                // configure previous step button
                final Button prevStepBtn = (Button) finalProduct.findViewById(R.id.prev_step_btn);
                prevStepBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCurrentStep--;
                        View editText = findViewById(R.id.item_description_entry);
                        if (editText != null)
                            editText.getParent().clearChildFocus(editText);
                        mListView.invalidateViews();
                    }
                });

                // hide previous step button for first step
                if (mCurrentStep == 0)
                    prevStepBtn.setVisibility(View.GONE);

                // configure input view
                LinearLayout flex = (LinearLayout) finalProduct.findViewById(R.id.step_input_parent);
                switch(mCurrentStep) {
                    case 0:
                        // create an imageview
                        ImageView uploadImg = (ImageView) getLayoutInflater().inflate(R.layout.upload_img_view, flex, false);
                        uploadImg.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                attemptTakePic();
                            }
                        });
                        // set image to picture if already taken
                        if (mThumbnail != null) {
                            uploadImg.setImageBitmap(mThumbnail);
                        }

                        // create the explanatory textview
                        TextView instructions = new TextView(getApplicationContext());
                        instructions.setText(R.string.prompt_image_upload);
                        instructions.setTextColor(getColor(R.color.hintOrDisabledText));

                        flex.addView(instructions);
                        flex.addView(uploadImg);

                        // update connector length
                        View connector = finalProduct.findViewById(R.id.step_connector);
                        ViewGroup.LayoutParams params = connector.getLayoutParams();
                        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 190, getResources().getDisplayMetrics());
                        connector.setLayoutParams(params);
                        break;
                    case 1:
                        // inflate EditText for title entry
                        View titleEntry = getLayoutInflater().inflate(R.layout.item_title_entry, flex, false);
                        EditText editText = (EditText) titleEntry.findViewById(R.id.item_description_entry);
                        editText.setText(mDescription);
                        if (TextUtils.isEmpty(mDescription) && donateAttempted) {
                            editText.setError("Please enter a description");
                        }
                        flex.addView(titleEntry);
                        break;
                    case 2:
                        View detailsCustom = getLayoutInflater().inflate(R.layout.item_details_entry, flex, false);
                        editText = (EditText) detailsCustom.findViewById(R.id.item_description_entry);
                        editText.setText(mDetails);
                        if(TextUtils.isEmpty(mDetails) && donateAttempted)
                            editText.setError("Please provide details");
                        flex.addView(detailsCustom);
                        break;
                    case 3:
                        View priceCustom = getLayoutInflater().inflate(R.layout.item_price_entry, flex, false);
                        editText = (EditText) priceCustom.findViewById(R.id.item_description_entry);
                        editText.setText(mPrice);
                        if(TextUtils.isEmpty(mPrice) && donateAttempted)
                            editText.setError("Please enter a price");
                        else if (mPrice != null) {
                            if(Double.parseDouble(mPrice) < 0.31 && donateAttempted)
                                editText.setError("Price must be >$0.31");
                        }
                        flex.addView(priceCustom);
                        break;
                    case 4:
                        View conditionCustom = getLayoutInflater().inflate(R.layout.item_condition_entry, flex, false);
                        Spinner conditionEntry = (Spinner) conditionCustom.findViewById(R.id.item_condition_entry);
                        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(),
                                R.array.possible_conditions, android.R.layout.simple_spinner_item);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        conditionEntry.setAdapter(adapter);
                        conditionEntry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                TextView display = (TextView) parent.getChildAt(0);
                                display.setTextColor(Color.BLACK);
                                mCondition = parent.getItemAtPosition(position).toString();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
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

        /**
         * Opens the step at the specified position
         * @param position index of the step to be opened
         */
        public void openItem(int position) {
            mCurrentStep = position;
            mListView.invalidateViews();
        }

    }

    /**
     * Asynchronous task that uploads a donated item to the server on a separate thread, so that it
     * won't block the UI thread with networking work.
     */
    public class UploadItemTask extends AsyncTask<Bitmap, Void, Integer> {

        private static final String LOG_TAG = "UploadItemTask";
        private static final int SUCCESS = 0;
        private static final int NO_CONNECTION = 1;
        private static final int NO_GROUP = 2;
        private static final int OTHER_FAILURE = 3;

        private Activity mParent;
        private String mName;
        private String mPrice;
        private String mCondition;
        private String mDescription;
        private Bitmap mBitmap;

        UploadItemTask(Activity parent, String name, String price, String condition, String description, Bitmap bitmap) {
            mParent = parent;
            mName = name;
            mPrice = price;
            mCondition = condition;
            mDescription = description;
            mBitmap = bitmap;
        }

        @Override
        protected Integer doInBackground(Bitmap... bmpData) {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if(info == null || !info.isConnected()) {
                // if there is no network, inform user through a toast
                return NO_CONNECTION;
            } else {
                // declare resources to close in finally clause
                HttpURLConnection conn = null;
                BufferedOutputStream out = null;
                try {
                    // form a URL to connect to
                    Uri.Builder builder = new Uri.Builder();
                    String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .scheme("http")
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("items")
                            .appendPath("create").build().toString();
                    URL url = new URL(uri);

                    // form connection
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setChunkedStreamingMode(0);
                    conn.connect();

                    // get values for item
                    SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
                    String groupId = mParent.getIntent().getStringExtra(EXTRA_GROUP_ID);
                    String userId = savedData.getString(getString(R.string.userId), "");
                    String name = mName;
                    String price = mPrice;
                    String condition = mCondition;
                    String description = mDescription;

                    // require the user to have chosen a group to donate
                    if (groupId.equals("")) {
                        throw new IllegalArgumentException();
                    }

                    // scale down image and convert to base64
                    Log.d(LOG_TAG, "is mBitmap null: "+(mBitmap == null));
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    String thumbnail = Base64.encodeToString(byteArray,Base64.NO_WRAP);

                    // convert item to JSON
                    String body = "";
                    JSONObject item = new JSONObject();
                    item.put("groupId", groupId);
                    item.put("ownerId", userId);
                    item.put("name", name);
                    item.put("price", price);
                    item.put("condition", condition);
                    item.put("description", description);
                    item.put("thumbnail", thumbnail);
                    body = item.toString();
                    Log.d(LOG_TAG, body);

                    // write to output stream
                    out = new BufferedOutputStream(conn.getOutputStream());
                    out.write(body.getBytes());
                    out.flush();

                    // see if post was a success
                    int responseCode = conn.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 200) {
                        Log.d(LOG_TAG, "Post was success");
                        return SUCCESS;
                    } else if(responseCode == 449) {
                        Log.d(LOG_TAG, "Group may have been deleted, Post failure");
                        return OTHER_FAILURE;
                    } else {
                        Log.e(LOG_TAG, "response Code = "+responseCode);
                    }
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, "Bad URL", e);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error opening URL connection (probably?)", e);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error forming JSON", e);
                } catch(IllegalArgumentException e) {
                    Log.e(LOG_TAG, "No group chosen");
                    return NO_GROUP;
                } finally {
                    conn.disconnect();
                    try {
                        if (out != null)
                            out.close();
                    } catch(Exception e) {
                        Log.e(LOG_TAG, "Error closing output stream", e);
                    }
                }
            }

            // in case of failure
            return OTHER_FAILURE;
        }

        @Override
        protected void onPostExecute(Integer result) {
            // update text of button
            TextView takePicBtn = (TextView) findViewById(R.id.primary_step_label);
            if (takePicBtn != null)
                takePicBtn.setText(getString(R.string.prompt_image_upload));

            // go back to main activity
            if(result == SUCCESS) {
                Toast.makeText(mParent, R.string.successful_donation, Toast.LENGTH_SHORT).show();
                NavUtils.navigateUpFromSameTask(mParent);
            } else if (result == NO_CONNECTION) {
                Toast.makeText(mParent, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG, "No connection available");
            } else if (result == NO_GROUP) {
                Toast.makeText(mParent, "Please choose a group to donate to.", Toast.LENGTH_SHORT).show();
                showProgress(false);
                Intent chooseGroupIntent = new Intent(mParent, MainActivity.class);
                chooseGroupIntent.putExtra(MainActivity.OPEN_CHOOSE_GROUP, true);
                startActivity(chooseGroupIntent);
            } else {
                Toast.makeText(mParent, R.string.error_donate, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class GetClientTokenTask extends AsyncTask<Void, Void, ResultCode> {
        private final String LOG_TAG = GetClientTokenTask.class.getSimpleName();

        private final Activity mParent;

        GetClientTokenTask(Activity parent) {
            mParent = parent;
        }

        @Override
        protected ResultCode doInBackground(Void... params) {
            // check if we have an Internet connection
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();

            if(info == null || !info.isConnected()) {
                // if there is no network, inform user through a toast
                mParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mParent, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
                        Log.d(LOG_TAG, "No connection available");
                    }
                });
                return NO_INTERNET;
            } else {
                // declare resources to close in finally clause
                HttpURLConnection conn = null;
                try {
                    // get group owner password
                    SharedPreferences savedData = mParent.getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);

                    // form a URL to connect to
                    Uri.Builder builder = new Uri.Builder();
                    String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .scheme("http")
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("items")
                            .appendPath("paymenttoken").build().toString();
                    URL url = new URL(uri);

                    // form connection
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(false);
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setChunkedStreamingMode(0);
                    conn.connect();

                    // see if post was a success
                    int responseCode = conn.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 200) {
                        Log.d(LOG_TAG, "Token Acquired Successfully");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String responseBody = "";
                        String line;
                        while((line = reader.readLine()) != null) {
                            responseBody += line;
                        }
                        try {
                            // parse response as not JSON
                            mToken = responseBody;
                            Log.d(LOG_TAG, "mToken = "+mToken);
                        } catch (Exception e) {Log.e(LOG_TAG, "Error with JSON");}

                        return SUCCESS;
                    } else if(responseCode == 404) {
                        Log.d(LOG_TAG, "Token not acquired-failure");
                        return OTHER_FAILURE;
                    } else {
                        Log.e(LOG_TAG, "response Code = "+responseCode);
                        return OTHER_FAILURE;
                    }
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, "Bad URL", e);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error opening URL connection (probably?)", e);
                } finally {
                    conn.disconnect();
                }
            }

            // in case of failure
            return OTHER_FAILURE;
        }
        @Override
        protected void onPostExecute(ResultCode result) {
            if(result == SUCCESS)
                finishPurchase();
            else
                Toast.makeText(mParent, "An unknown error occurred. Please try again later.", Toast.LENGTH_SHORT).show();

            showProgress(false);
        }
    }

    private class DonateMoneyTask extends AsyncTask<Void, Void, ResultCode> {
        private final String LOG_TAG = DonateMoneyTask.class.getSimpleName();

        private final Activity mParent;
        private final PaymentMethodNonce mNonce;
        private final String mToken;
        private final double mAmount;

        DonateMoneyTask(Activity parent, PaymentMethodNonce nonce, String token, double amount) {
            mParent = parent;
            mNonce = nonce;
            mToken = token;
            mAmount = amount;
        }

        @Override
        protected ResultCode doInBackground(Void... params) {
            // check if we have an Internet connection
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();

            if(info == null || !info.isConnected()) {
                // if there is no network, inform user through a toast
                mParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mParent, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
                        Log.d(LOG_TAG, "No connection available");
                    }
                });
                return NO_INTERNET;
            } else {
                // declare resources to close in finally clause
                HttpURLConnection conn = null;
                BufferedOutputStream out = null;
                try {
                    // get group owner password
                    SharedPreferences savedData = mParent.getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
                    String un = savedData.getString(getString(R.string.userId), "");
                    String pw = savedData.getString(getString(R.string.prompt_password), "");

                    // form a URL to connect to
                    Uri.Builder builder = new Uri.Builder();
                    String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .scheme("http")
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("groups")
                            .appendPath("donatetogroup")
                            .appendQueryParameter("id", mParent.getIntent().getStringExtra(EXTRA_GROUP_ID))
                            .appendQueryParameter("userId", ""+un)
                            .appendQueryParameter("password", ""+pw)
                            .build().toString();
                    URL url = new URL(uri);
                    Log.d(LOG_TAG, url.toString());

                    // form connection
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setChunkedStreamingMode(0);
                    conn.connect();

                    // send JSON
                    out = new BufferedOutputStream(conn.getOutputStream());
                    JSONObject purchaseInfo = new JSONObject();
                    purchaseInfo.put("token", mToken);
                    purchaseInfo.put("paymentMethodNonce", mNonce);
                    purchaseInfo.put("amount", mAmount);
                    String body = purchaseInfo.toString();
                    out.write(body.getBytes());
                    out.flush();

                    // see if post was a success
                    int responseCode = conn.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 200) {
                        Log.d(LOG_TAG, "Donation sent successfully!");
                        return SUCCESS;
                    } else if(responseCode == 404) {
                        Log.d(LOG_TAG, "Purchase failed :(");
                        return OTHER_FAILURE;
                    } else {
                        Log.e(LOG_TAG, "response Code = "+responseCode);
                        return OTHER_FAILURE;
                    }
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, "Bad URL", e);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error opening URL connection (probably?)", e);
                } catch (JSONException e){
                    Log.e(LOG_TAG, "Error parsing JSON", e);
                }finally {
                    conn.disconnect();
                }
            }

            // in case of failure
            return OTHER_FAILURE;
        }
        @Override
        protected void onPostExecute(ResultCode result) {
            if(result == SUCCESS)
                Toast.makeText(mParent, R.string.successful_donation, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(mParent, R.string.error_donate, Toast.LENGTH_SHORT).show();

            showProgress(false);
        }
    }
}
