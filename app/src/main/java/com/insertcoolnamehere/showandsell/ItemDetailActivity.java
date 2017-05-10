package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceActivity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.interfaces.BraintreeListener;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;
import com.insertcoolnamehere.showandsell.logic.Item;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.tweetcomposer.ComposerActivity;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Locale;

import io.fabric.sdk.android.Fabric;

public class ItemDetailActivity extends AppCompatActivity implements BraintreeListener {

    public static final String ITEM_ID = "ITEM_ID";
    public static final String OWNER_POWERS = "OWNER_POWERS";
    private static final int REQUEST_CODE = 9801;

    /**
     * This is the particular Item object whose data are displayed in this activity
     */
    protected Item mItem;

    private ArrayList<Message> mComments = new ArrayList<>();
    private CommentAdapter<Message> mAdapter;
    private AsyncTask mFetchCommentsTask;

    private String mToken;
    private String mItemGroupName = null;
    private String mGroupId = null;
    private String mGroupAddress = null;
    private String mGroupLocationDetail = null;
    private double mGroupRating = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // transparent status bar
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // set the layout of the activity
        setContentView(this instanceof ManageItemActivity?R.layout.activity_manage_item:R.layout.activity_item_detail);

        // get item data from intent
        Intent startingIntent = getIntent();
        Uri data = startingIntent.getData();

        if (data == null) {
            // open from app
            mItem = Item.getItem(startingIntent.getStringExtra(ITEM_ID));

            // set text and images for the activity view
            ImageView itemImage = (ImageView) findViewById(R.id.item_detail_image);
            itemImage.setImageBitmap(mItem.getPic());

            // open full image on click
            itemImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFullImage();
                }
            });

            // alternative item title
            CollapsingToolbarLayout toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
            toolbarLayout.setTitle(mItem.getName());

            // populate item detail fields
            TextView itemPrice = (TextView) findViewById(R.id.item_detail_price);
            itemPrice.setText(String.format(Locale.ENGLISH, "%.2f", mItem.getPrice()));
            TextView itemCondition = (TextView) findViewById(R.id.item_detail_condition);
            itemCondition.setText(mItem.getCondition());
            TextView itemDescription = (TextView) findViewById(R.id.item_detail_description);
            itemDescription.setText(mItem.getDescription());
            TextView itemGroup = (TextView) findViewById(R.id.item_detail_group_name);
            itemGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openGroupDetail();
                }
            });
            new FetchGroupTask(this).execute();

            // set up comments list view
            ListView listView = (ListView) findViewById(R.id.item_comments);
            mAdapter = new CommentAdapter<>(this, R.layout.text_view_comment_right, mComments);
            listView.setAdapter(mAdapter);
            if (mFetchCommentsTask == null) {
                new FetchCommentsTask(this).execute();
            }
        } else {
            new FetchItemTask(this, data.toString().split("://")[1]).execute();
            Log.d("ItemDetailActivity", "uri="+data);
            showProgress(true);
            return;
        }

        // set up post comment button
        final ImageButton postComment = (ImageButton) findViewById(R.id.btn_send_message);
        final EditText commentEntry = (EditText) findViewById(R.id.enter_comment);
        postComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptPostComment(commentEntry.getText().toString());
                commentEntry.setText("");
            }
        });

        // set up buy button
        FloatingActionButton buyButton = (FloatingActionButton) findViewById(R.id.item_detail_buy);
        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiatePurchase();
            }
        });

        // set up toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set up Twitter
        TwitterAuthConfig authConfig = new TwitterAuthConfig("TRASsuHji6yAXISCkDdUyABKy", "Oz3kxcNGJfM9QKfkfRETavcJEPdtovHFhnPtDmD56b5hjdRAyR");
        Fabric.with(this, new TwitterCore(authConfig), new TweetComposer());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_item_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (menuItem.getItemId() == R.id.action_share) {
            TweetComposer.Builder builder = new TweetComposer.Builder(this)
                .text(String.format(getString(R.string.tweet_pattern), mItem.getPrice(), mItem.getName(), mItem.getGuid()));
            builder.show();
            return true;
        } else if(menuItem.getItemId() == R.id.action_bookmark) {
            attemptPostBookmark();
            return true;
        }else {
            return super.onOptionsItemSelected(menuItem);
        }
    }

    private void finishCreation() {
        // set text and images for the activity view
        ImageView itemImage = (ImageView) findViewById(R.id.item_detail_image);
        itemImage.setImageBitmap(mItem.getPic());

        // open full image on click
        itemImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFullImage();
            }
        });

        // alternative item title
        CollapsingToolbarLayout toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        toolbarLayout.setTitle(mItem.getName());

        // populate item detail fields
        TextView itemPrice = (TextView) findViewById(R.id.item_detail_price);
        itemPrice.setText(String.format(Locale.ENGLISH, "%.2f", mItem.getPrice()));
        TextView itemCondition = (TextView) findViewById(R.id.item_detail_condition);
        itemCondition.setText(mItem.getCondition());
        TextView itemDescription = (TextView) findViewById(R.id.item_detail_description);
        itemDescription.setText(mItem.getDescription());
        TextView itemGroup = (TextView) findViewById(R.id.item_detail_group_name);
        itemGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGroupDetail();
            }
        });
        new FetchGroupTask(this).execute();

        // set up comments list view
        ListView listView = (ListView) findViewById(R.id.item_comments);
        mAdapter = new CommentAdapter<>(this, R.layout.text_view_comment_right, mComments);
        listView.setAdapter(mAdapter);
        if (mFetchCommentsTask == null) {
            new FetchCommentsTask(this).execute();
        }
    }

    private void openGroupDetail() {
        Intent openGroupIntent = new Intent(this, GroupDetailActivity.class);
        openGroupIntent.putExtra(GroupDetailActivity.EXTRA_NAME, mItemGroupName);
        openGroupIntent.putExtra(GroupDetailActivity.EXTRA_ADDRESS, mGroupAddress);
        openGroupIntent.putExtra(GroupDetailActivity.EXTRA_LOCATION_DETAIL, mGroupLocationDetail);
        openGroupIntent.putExtra(GroupDetailActivity.EXTRA_RATING, mGroupRating);
        openGroupIntent.putExtra(GroupDetailActivity.EXTRA_ID, mGroupId);
        startActivity(openGroupIntent);
    }

    private void openFullImage() {
        Intent intent = new Intent(this, FullImageActivity.class);
        intent.putExtra(ITEM_ID, mItem.getGuid());
        startActivity(intent);
    }
    private void initiatePurchase() {
        new GetClientTokenTask(this).execute();
    }
    private void finishPurchase() {
        DropInRequest dropInRequest = new DropInRequest()
                .clientToken(mToken);
        startActivityForResult(dropInRequest.getIntent(this), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                new PurchaseItemTask(this, result.getPaymentMethodNonce(), mToken).execute();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // the user canceled
            } else {
                // handle errors here, an exception may be available in
                Log.e("ItemDetailActivity", ""+data.getSerializableExtra(DropInActivity.EXTRA_ERROR));
            }
        }
    }

    /**
     * Initiates an Async Task that will attempt to post a bookmark for this user and this item to
     * the server.
     */
    private void attemptPostBookmark() {
        new PostBookmarkTask(this).execute();
    }

    private void attemptPostComment(String messageBody) {
        // add comment to comment list for now
        new PostMessageTask(this, messageBody).execute();
        SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
        String displayName = savedData.getString(getString(R.string.prompt_first_name),"")+" "+savedData.getString(getString(R.string.prompt_last_name), "");
        String id = savedData.getString(getString(R.string.userId), "NULL");
        mComments.add(new Message(displayName, id, messageBody, "9/99/99/9999 9:99:99 PM"));
    }

    private void adjustListViewSize() {
        ListView listView = (ListView) findViewById(R.id.item_comments);
        int lvHeight = 0;
        for (int i = 0; i < mComments.size(); i++) {
            View item = mAdapter.getView(i, null, listView);
            item.measure(0, 0);
            lvHeight += item.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = lvHeight;
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    /**
     * Turns the progress spinner on/off
     * @param isLoading whether or not to show the progress spinner
     */
    protected void showProgress(boolean isLoading) {
        View actual = findViewById(R.id.items_actual_details);
        View image = findViewById(R.id.item_detail_image);
        View button = findViewById(R.id.item_detail_buy);
        ProgressBar loading = (ProgressBar) findViewById(R.id.progress_bar);

        if(isLoading) {
            // hide actual details
            actual.setVisibility(View.GONE);
            image.setVisibility(View.GONE);
            button.setVisibility(View.GONE);

            // show progress bar
            loading.setVisibility(View.VISIBLE);
        } else {
            // show actual details
            actual.setVisibility(View.VISIBLE);
            image.setVisibility(View.VISIBLE);
            button.setVisibility(View.VISIBLE);

            // hide progress bar
            loading.setVisibility(View.GONE);
        }
    }

    /**
     * Serves as the bridge between the comment data and the comment list view
     * @param <T> Message or a subclass of it
     */
    private class CommentAdapter<T extends Message> extends ArrayAdapter{
        /**
         * Indicates that a comment will be displayed on the left side of the screen, where
         * messages from the seller and/or group owner will appear.
         */
        private final int LEFT_COMMENT = 0;
        /**
         * Indicates that a comment will be displayed on the right side of the screen, where
         * messages from potential buyers will appear.
         */
        private final int RIGHT_COMMENT = 1;

        /**
         * ArrayList representing all comments to show in this screen
         */
        private ArrayList<Message> mList;

        /**
         * Creates an adapter that fills the comment area of the screen with the comments
         * contained in list
         * @param context the Activity in which this class is being used
         * @param layout effectively ignored
         * @param list the list of comments to display
         */
        private CommentAdapter(Context context, int layout, ArrayList<Message> list) {
            super(context, layout, list);

            mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        /**
         * Determines whether the comment at a given position should be displayed on the left or
         * right side of the screen.
         * @param position position of comment in list
         * @return LEFT_COMMENT or RIGHT_COMMENT
         */
        @Override
        public int getItemViewType(int position) {
            SharedPreferences savedData = getSharedPreferences(getString(R.string.saved_data_file_key), Context.MODE_PRIVATE);
            String userId = savedData.getString(getString(R.string.userId), "NULL");
            if (userId.equals(mList.get(position).getUserId()))
                return RIGHT_COMMENT;
            else
                return LEFT_COMMENT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // see if a View has already been created
            if(convertView == null) {
                if(getItemViewType(position) == LEFT_COMMENT) {
                    // inflate the layout of the left-side comment
                    // make owner's comments golden
                    View rootView;
                    if(mItem.getOwnerId().equals(mList.get(position).getUserId())) {
                        Log.d("CommentAdapter", "found an owner comment");
                        rootView = getLayoutInflater().inflate(R.layout.text_view_comment_owner, parent, false);
                    } else {
                        rootView = getLayoutInflater().inflate(R.layout.text_view_comment_left, parent, false);
                    }
                    Log.d("CommentAdapter", "owner id: "+mItem.getOwnerId());
                    Log.d("CommentAdapter", "poster id: "+mList.get(position).getUserId());


                    // identify the TextViews of interest
                    TextView comment = (TextView) rootView.findViewById(R.id.textView_comment_left);
                    TextView poster = (TextView) rootView.findViewById(R.id.textView_poster_name);

                    // Get the comment from the list and set the text of the TextViews
                    comment.setText(mList.get(position).getBody());
                    poster.setText(mList.get(position).getUserName());
                    Log.d("CommentAdapter", "list: "+mList.get(position).getUserName());
                    Log.d("CommentAdapter", "view: "+poster.getText().toString());

                    // return the inflated layout
                    return rootView;
                } else {
                    View rootView = getLayoutInflater().inflate(R.layout.text_view_comment_right, parent, false);
                    TextView comment = (TextView) rootView.findViewById(R.id.textView_comment_right);
                    TextView poster = (TextView) rootView.findViewById(R.id.textView_poster_name);
                    comment.setText(mList.get(position).getBody());
                    poster.setText(mList.get(position).getUserName());
                    return rootView;
                }
            } else {
                if(getItemViewType(position) == LEFT_COMMENT) {
                    TextView comment = (TextView) convertView.findViewById(R.id.textView_comment_left);
                    comment.setText(mList.get(position).getBody());

                    TextView poster = (TextView) convertView.findViewById(R.id.textView_poster_name);
                    poster.setText(mList.get(position).getUserName());

                    // make owner's comments golden
                    if(mItem.getOwnerId().equals(mComments.get(position).getUserId())) {
                        comment.setBackground(getDrawable(R.drawable.owner_comment_left));
                        Log.d("CommentAdapter", "found an owner comment");
                    }
                    return convertView;
                } else {
                    TextView comment = (TextView) convertView.findViewById(R.id.textView_comment_right);
                    comment.setText(mList.get(position).getBody());

                    TextView poster = (TextView) convertView.findViewById(R.id.textView_poster_name);
                    poster.setText(mList.get(position).getUserName());
                    return convertView;
                }
            }
        }
    }

    /**
     * Changes the approved value of the displayed item on the server.
     */
    protected class ApproveItemTask extends AsyncTask<Void, Integer, Integer> {
        private static final String LOG_TAG = "ApproveItemTask";
        private static final int SUCCESS = 0;
        private static final int NO_INTERNET = 1;
        private static final int OTHER_FAILURE = 2;

        private Activity mParent;
        private boolean doApprove;
        private String newPrice;
        private String newDescription;

        /**
         * Creates a new ApproveTask to approve/disapprove an item
         * @param parent the parent activity to which the AsyncTask is bound
         * @param doApprove whether or not to approve the item
         */
        ApproveItemTask(Activity parent, String price, String description, boolean doApprove) {
            mParent = parent;
            this.doApprove = doApprove;
            newPrice = price;
            newDescription = description;
        }

        @Override
        protected Integer doInBackground(Void... urls) {
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
                    String pw = savedData.getString(getString(R.string.prompt_password), "");

                    // form a URL to connect to
                    Uri.Builder builder = new Uri.Builder();
                    String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .scheme("http")
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("items")
                            .appendPath("update")
                            .appendQueryParameter("id", mItem.getGuid())
                            .appendQueryParameter("adminPassword", pw).build().toString();
                    URL url = new URL(uri);

                    // form connection
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("PUT");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setChunkedStreamingMode(0);
                    conn.connect();

                    // update new values for item
                    mItem.setPrice(Double.parseDouble(newPrice));
                    mItem.setDescription(newDescription);

                    // get values for item
                    String name = mItem.getName();
                    String condition = mItem.getCondition();
                    Bitmap itemBitmap = mItem.getPic();

                    // scale down image and convert to base64
                    Log.d(LOG_TAG, "is itemBitmap null: "+(itemBitmap == null));
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    itemBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    String thumbnail = Base64.encodeToString(byteArray, Base64.NO_WRAP);

                    // convert item to JSON
                    JSONObject item = new JSONObject();
                    item.put("newName", name);
                    item.put("newPrice", newPrice);
                    item.put("newCondition", condition);
                    item.put("newDescription", newDescription);
                    item.put("approved", doApprove);
                    item.put("newThumbnail", thumbnail);
                    String body = item.toString();

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
                    } else if(responseCode == 449 || responseCode == 401) {
                        Log.d(LOG_TAG, "Post failure");
                        return OTHER_FAILURE;
                    } else {
                        Log.e(LOG_TAG, "response Code = "+responseCode);
                        return OTHER_FAILURE;
                    }
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, "Bad URL", e);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error opening URL connection (probably?)", e);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error forming JSON", e);
                } finally {
                    conn.disconnect();
                    try {
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
            if(result == SUCCESS) {
                // turn off progress spinner in case the user comes back
                showProgress(false);

                // return to previous activity
                Intent goHomeIntent = new Intent(mParent, ManageGroupActivity.class);
                startActivity(goHomeIntent);
            } else if (result == OTHER_FAILURE) {
                showProgress(false);

                // alert user that something has gone horribly wrong
                Toast.makeText(mParent, "Congratulations! You found a bug! " +
                        "Please report it to the devs", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class PostBookmarkTask extends AsyncTask<Void, Integer, Integer> {
        private static final int SUCCESS = 0;
        private static final int NO_INTERNET = 1;
        private static final int OTHER_FAILURE = 2;
        private static final int ALREADY_EXISTS = 3;
        private final String LOG_TAG = PostBookmarkTask.class.getSimpleName();

        private final Activity mParent;

        PostBookmarkTask(Activity parent) {
            mParent = parent;
        }

        @Override
        protected Integer doInBackground(Void... params) {
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
                    String un = savedData.getString(getString(R.string.userId), "");

                    // form a URL to connect to
                    Uri.Builder builder = new Uri.Builder();
                    String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .scheme("http")
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("bookmarks")
                            .appendPath("create")
                            .appendQueryParameter("userId", un)
                            .appendQueryParameter("itemId", mItem.getGuid()).build().toString();
                    URL url = new URL(uri);

                    // form connection
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(false);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setChunkedStreamingMode(0);
                    conn.connect();

                    // see if post was a success
                    int responseCode = conn.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 200) {
                        Log.d(LOG_TAG, "Post was success");
                        return SUCCESS;
                    } else if(responseCode == 409) {
                        Log.d(LOG_TAG, "Bookmark already exists");
                        return ALREADY_EXISTS;
                    } else if(responseCode == 449) {
                        Log.d(LOG_TAG, "Post failure");
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
        protected void onPostExecute(Integer result) {
            // inform user of success
            if(result == SUCCESS) {
                Toast.makeText(mParent, "Added bookmark!", Toast.LENGTH_SHORT).show();
                showProgress(false);
            }
            else if(result == OTHER_FAILURE)
                Toast.makeText(mParent, "Bookmark failed :(", Toast.LENGTH_SHORT).show();

            // no matter what, turn off progress spinner
            showProgress(false);
        }
    }

    protected class RejectItemTask extends AsyncTask<Void, Integer, Integer> {
        private static final String LOG_TAG = "RejectItemTask";
        private static final int SUCCESS = 0;
        private static final int NO_INTERNET = 1;
        private static final int OTHER_FAILURE = 2;

        private Activity mParent;
        private boolean doApprove;

        RejectItemTask(Activity parent) {
            mParent = parent;
        }

        @Override
        protected Integer doInBackground(Void... urls) {
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
                    String pw = savedData.getString(getString(R.string.prompt_password), "");

                    // form a URL to connect to
                    Uri.Builder builder = new Uri.Builder();
                    String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .scheme("http")
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("items")
                            .appendPath("delete")
                            .appendQueryParameter("id", mItem.getGuid())
                            .appendQueryParameter("password", pw).build().toString();
                    URL url = new URL(uri);

                    // form connection
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("DELETE");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setChunkedStreamingMode(0);
                    conn.connect();

                    // get values for item
                    String name = mItem.getName();
                    String price = ""+mItem.getPrice();
                    String condition = mItem.getCondition();
                    String description = mItem.getDescription();
                    Bitmap itemBitmap = mItem.getPic();
                    // see if post was a success
                    int responseCode = conn.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 200) {
                        Log.d(LOG_TAG, "Post was success");
                        return SUCCESS;
                    } else if(responseCode == 449) {
                        Log.d(LOG_TAG, "Post failure");
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
        protected void onPostExecute(Integer result) {
            if(result == SUCCESS) {
                // turn off progress spinner in case user comes back
                showProgress(false);

                // return to previous activity
                Intent goHomeIntent = new Intent(mParent, ManageGroupActivity.class);
                startActivity(goHomeIntent);
            }
        }
    }
    private class GetClientTokenTask extends AsyncTask<Void, Void, Integer> {
        private static final int SUCCESS = 0;
        private static final int NO_INTERNET = 1;
        private static final int OTHER_FAILURE = 2;
        private final String LOG_TAG = GetClientTokenTask.class.getSimpleName();

        private final Activity mParent;

        GetClientTokenTask(Activity parent) {
            mParent = parent;
        }

        @Override
        protected Integer doInBackground(Void... params) {
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
        protected void onPostExecute(Integer result) {
            if(result == SUCCESS) {
                showProgress(false);
                finishPurchase();
            }
            else
                Toast.makeText(mParent, "An unknown error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
            showProgress(false);
        }

    }

    private class PurchaseItemTask extends AsyncTask<Void, Void, Integer> {
        private static final int SUCCESS = 0;
        private static final int NO_INTERNET = 1;
        private static final int OTHER_FAILURE = 2;
        private final String LOG_TAG = PurchaseItemTask.class.getSimpleName();

        private final Activity mParent;
        private final PaymentMethodNonce mNonce;
        private final String mToken;

        PurchaseItemTask(Activity parent, PaymentMethodNonce nonce, String token) {
            mParent = parent;
            mNonce = nonce;
            mToken = token;
        }

        @Override
        protected Integer doInBackground(Void... params) {
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
                            .appendPath("items")
                            .appendPath("buyitem")
                            .appendQueryParameter("id", mItem.getGuid())
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
                    purchaseInfo.put("amount", mItem.getPrice());
                    String body = purchaseInfo.toString();
                    out.write(body.getBytes());
                    out.flush();

                    // see if post was a success
                    int responseCode = conn.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 200) {
                        Log.d(LOG_TAG, "Item bought! You will receive an email shortly.");
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
        protected void onPostExecute(Integer result) {
            if(result == SUCCESS) {
                Toast.makeText(mParent, "Item bought!", Toast.LENGTH_SHORT).show();
                showProgress(false);
            }
            else
                Toast.makeText(mParent, "Purchase failed :(", Toast.LENGTH_SHORT).show();
            showProgress(false);
        }
    }

    private class PostMessageTask extends AsyncTask<Void, Void, Integer> {
        private static final int SUCCESS = 0;
        private static final int NO_INTERNET = 1;
        private static final int OTHER_FAILURE = 2;
        private final String LOG_TAG = PostMessageTask.class.getSimpleName();

        private final Activity mParent;
        private String mMessage;

        PostMessageTask(Activity parent, String messageBody) {
            mParent = parent;
            mMessage = messageBody;
        }

        @Override
        protected Integer doInBackground(Void... params) {
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
                    Log.d(LOG_TAG, "posting user: "+un);

                    // form a URL to connect to
                    Uri.Builder builder = new Uri.Builder();
                    String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .scheme("http")
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("chat")
                            .appendPath("create")
                            .appendQueryParameter("posterId", un)
                            .appendQueryParameter("password", pw).build().toString();
                    URL url = new URL(uri);

                    // form connection
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(false);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setChunkedStreamingMode(0);
                    conn.connect();

                    // form JSON
                    JSONObject message = new JSONObject();
                    message.put("itemId", mItem.getGuid());
                    message.put("body", mMessage);
                    Log.d(LOG_TAG, message.toString());

                    out = new BufferedOutputStream(conn.getOutputStream());
                    out.write(message.toString().getBytes());
                    out.flush();

                    // see if post was a success
                    int responseCode = conn.getResponseCode();
                    Log.d(LOG_TAG, "Response Code from Cloud Server: "+responseCode);

                    if(responseCode == 200) {
                        Log.d(LOG_TAG, "Post was success");

                        return SUCCESS;
                    } else if(responseCode == 404) {
                        Log.d(LOG_TAG, "Post failure");
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
                    Log.e(LOG_TAG, "Error formatting JSON");
                } finally {
                    conn.disconnect();
                }
            }

            // in case of failure
            return OTHER_FAILURE;
        }
        @Override
        protected void onPostExecute(Integer result) {
            if(result == SUCCESS) {
                new FetchCommentsTask(mParent).execute();
                showProgress(false);
            }
            else {
                showProgress(false);
            }
        }
    }

    private class FetchCommentsTask extends AsyncTask<Void, Void, Integer> {
        /**
         * The Activity within which this AsyncTask runs
         */
        private Activity mParent;

        private final String LOG_TAG = FetchCommentsTask.class.getSimpleName();
        private final int NO_COMMENTS = 3;
        private final int NO_INERNET = 2;
        private final int SUCCESS = 1;
        private final int OTHER_FAILURE = 0;

        private ArrayList<Message> mServerComments = new ArrayList<>();

        FetchCommentsTask(Activity parent) {
            mParent = parent;
        }

        protected Integer doInBackground(Void... urls) {
            // variables that we will have to close in try loop
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // the unparsed JSON response from the server
            int responseCode = -1;

            // check for internet connection
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
                return NO_INERNET;
            } else {
                try {
                    // connect to the URL and open the reader
                    Uri.Builder  builder = new Uri.Builder();
                    builder.scheme("http")
                            .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("chat")
                            .appendPath("messages")
                            .appendQueryParameter("itemId", mItem.getGuid())
                            .build();
                    URL url = new URL(builder.toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // obtain status code
                    responseCode = urlConnection.getResponseCode();
                    Log.d(LOG_TAG, "response code="+responseCode);
                    if(responseCode == 200) {
                        // read response to get user data from server
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String line = "";
                        String responseBody = "";
                        while((line = reader.readLine()) != null) {
                            responseBody += line + '\n';
                        }

                        // parse response as JSON
                        JSONArray items = new JSONArray(responseBody);

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject itemJson = items.getJSONObject(i);

                            Log.d(LOG_TAG, itemJson.toString());
                            Message msg = new Message(itemJson.getString("posterName"), itemJson.getString("posterId"), itemJson.getString("body"), itemJson.getString("datePosted"));
                            mServerComments.add(msg);
                        }

                        Log.d(LOG_TAG, "Before");
                        for(Message m: mServerComments) {
                            Log.d(LOG_TAG, m.time);
                        }

                        // bubble sort comments
                        for (int bigkey = 1; bigkey < mServerComments.size(); bigkey++) {
                            int smallkey = bigkey;
                            while(mServerComments.get(smallkey-1).compareTo(mServerComments.get(smallkey)) < 0) {
                                // swap element at key with element to its left
                                Message keyEl = mServerComments.get(smallkey);
                                mServerComments.set(smallkey, mServerComments.get(smallkey - 1));
                                mServerComments.set(smallkey - 1, keyEl);
                                smallkey--;
                                if(smallkey == 0)
                                    break;
                            }
                        }

                        Log.d(LOG_TAG, "After");
                        for(Message m: mServerComments) {
                            Log.d(LOG_TAG, m.time);
                        }

                        return SUCCESS;
                    } else if (responseCode == 404) {
                        return NO_COMMENTS;
                    } else {
                        return OTHER_FAILURE;
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error getting response from server", e);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error parsing JSON", e);
                } finally {
                    // release system resources
                    if(urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if(reader != null) {
                        try {
                            reader.close();
                        } catch(IOException e) {
                            Log.e(LOG_TAG, "Error closing input stream", e);
                        }
                    }
                }
            }

            // if anything goes wrong, return the other failure code
            return OTHER_FAILURE;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result == SUCCESS) {
                mComments.clear();
                mComments.addAll(mServerComments);
                mAdapter.notifyDataSetChanged();
                adjustListViewSize();
                showProgress(false);
                mFetchCommentsTask = null;
            } else if (result == NO_COMMENTS) {
                showProgress(false);
                mFetchCommentsTask = null;
            } else if (result == OTHER_FAILURE){
                Log.e(LOG_TAG, "It appears that the task failed :(");
                showProgress(false);
                mFetchCommentsTask = null;
            }
        }
    }

    private class FetchItemTask extends AsyncTask<Void, Integer, Integer> {
        /**
         * The Activity within which this AsyncTask runs
         */
        private Activity mParent;
        private String mItemId;

        private final String LOG_TAG = FetchItemTask.class.getSimpleName();
        private final int NOT_FOUND = 3;
        private final int NO_INERNET = 2;
        private final int SUCCESS = 1;
        private final int OTHER_FAILURE = 0;

        public FetchItemTask(Activity parent, String itemId) {
            mParent = parent;
            mItemId = itemId;
        }

        protected Integer doInBackground(Void... urls) {
            // im gonna copy paste the networking code from Login here
            // variables that we will have to close in try loop
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // the unparsed JSON response from the server
            int responseCode = -1;

            // cancel if task is detached from activity
            // check for internet connection
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
                return NO_INERNET;
            } else {
                try {
                    String location = new Uri.Builder().scheme("http")
                            .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .appendEncodedPath("showandsell")
                            .appendPath("api")
                            .appendPath("items")
                            .appendPath("item")
                            .appendQueryParameter("id", mItemId).build().toString();
                    URL url = new URL(location);

                    // connect to the URL and open the reader
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // obtain status code
                    responseCode = urlConnection.getResponseCode();
                    Log.d(LOG_TAG, "response code="+responseCode);
                    if(responseCode == 200) {
                        // read response to get user data from server
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String line = "";
                        String responseBody = "";
                        while((line = reader.readLine()) != null) {
                            responseBody += line + '\n';
                        }

                        // parse response as JSON
                        JSONObject itemJson = new JSONObject(responseBody);

                        mItem = new Item(itemJson.getString("ssItemId"), Item.BOOKMARK);
                        Log.d(LOG_TAG, "Server contains item #"+mItem.getGuid());
                        mItem.setName(itemJson.getString("name"));
                        mItem.setPrice(itemJson.getDouble("price"));
                        mItem.setCondition(itemJson.getString("condition"));
                        mItem.setDescription(itemJson.getString("description"));
                        byte[] imgBytes = Base64.decode(itemJson.getString("thumbnail"), Base64.NO_PADDING);
                        mItem.setPic(BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length));
                        mItem.setApproved(itemJson.getBoolean("approved"));
                        mItem.setOwnerId(itemJson.getString("ownerId"));
                        Log.d(LOG_TAG, "Item # "+mItem+" is approved? "+mItem.isApproved());

                        return SUCCESS;
                    } else if (responseCode == 404) {
                        return NOT_FOUND;
                    } else {
                        return OTHER_FAILURE;
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error getting response from server", e);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error parsing JSON", e);
                } finally {
                    // release system resources
                    if(urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if(reader != null) {
                        try {
                            reader.close();
                        } catch(IOException e) {
                            Log.e(LOG_TAG, "Error closing input stream", e);
                        }
                    }
                }
            }

            // if anything goes wrong, return the other failure code
            return OTHER_FAILURE;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == SUCCESS) {
                finishCreation();
            } else if(result == OTHER_FAILURE) {
                Log.e(LOG_TAG, "It appears that the task failed :(");
            }

            // no matter what happens, show user that we're done trying here
            showProgress(false);
        }
    }

    private class FetchGroupTask extends AsyncTask<Void, Void, Integer> {
        // instance variables
        private Activity mParent;

        // result codes
        private final int SUCCESS = 0;
        private final int NO_INTERNET = 1;
        private final int OTHER_FAILURE = 2;
        // for use in debug logs
        private final String LOG_TAG = "FetchGroupTask";

        FetchGroupTask(Activity parent) {
            mParent = parent;
        }

        public Integer doInBackground(Void... params) {
            // variables that we will have to close in try loop
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // the unparsed JSON response from the server
            int responseCode = -1;

            // check for internet connection
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
                try {
                    // connect to the URL and open the reader
                    Uri.Builder  builder = new Uri.Builder();
                    builder.scheme("http")
                            .encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("groups")
                            .appendPath("group")
                            .appendQueryParameter("id", mItem.getGroupId())
                            .build();
                    URL url = new URL(builder.toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // obtain status code
                    responseCode = urlConnection.getResponseCode();
                    Log.d(LOG_TAG, "response code="+responseCode);
                    if(responseCode == 200) {
                        // read response to get user data from server
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String line = "";
                        String responseBody = "";
                        while((line = reader.readLine()) != null) {
                            responseBody += line + '\n';
                        }

                        // parse response as JSON
                        JSONObject itemJson = new JSONObject(responseBody);

                        Log.d(LOG_TAG, itemJson.toString());
                        mItemGroupName = itemJson.getString("name");
                        mGroupId = itemJson.getString("ssGroupId");
                        mGroupAddress = itemJson.getString("address");
                        mGroupLocationDetail = itemJson.getString("locationDetail");
                        mGroupRating = itemJson.getDouble("rating");

                        return SUCCESS;
                    } else {
                        return OTHER_FAILURE;
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error getting response from server", e);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error parsing JSON", e);
                } finally {
                    // release system resources
                    if(urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if(reader != null) {
                        try {
                            reader.close();
                        } catch(IOException e) {
                            Log.e(LOG_TAG, "Error closing input stream", e);
                        }
                    }
                }
            }

            // if anything goes wrong, return the other failure code
            return OTHER_FAILURE;
        }

        @Override
        public void onPostExecute(Integer result) {
            if (result == SUCCESS) {
                TextView itemGroup = (TextView) findViewById(R.id.item_detail_group_name);
                itemGroup.setText(mItemGroupName);
            } else {
                TextView itemGroup = (TextView) findViewById(R.id.item_detail_group_name);
                itemGroup.setText(R.string.error_group_not_found);
            }
        }
    }

    /**
     * Represents a message in comments
     */
    private class Message implements Comparable<Message>{
        private String userName;
        private String userId;
        private String body;
        private String time;

        Message(String userName, String userId, String body, String time) {
            this.userName = userName;
            this.userId = userId;
            this.body = body;
            this.time = time;
        }

        public String getUserName() {
            return userName;
        }

        public String getBody() {
            return body;
        }

        public String getUserId() {
            return userId;
        }

        public int compareTo(Message other) {
            String[] myTime = time.split("[ \\/:]");
            String[] theirTime = other.time.split("[ \\/:]");
            
            int myMonth = Integer.parseInt(myTime[0]);
            int myDay = Integer.parseInt(myTime[1]);
            int myYear = Integer.parseInt(myTime[2]);
            int myHour = Integer.parseInt(myTime[3]);
            int myMinute = Integer.parseInt(myTime[4]);
            int mySecond = Integer.parseInt(myTime[5]);
            String myM = myTime[6];

            int theirMonth = Integer.parseInt(theirTime[0]);
            int theirDay = Integer.parseInt(theirTime[1]);
            int theirYear = Integer.parseInt(theirTime[2]);
            int theirHour = Integer.parseInt(theirTime[3]);
            int theirMinute = Integer.parseInt(theirTime[4]);
            int theirSecond = Integer.parseInt(theirTime[5]);
            String theirM = theirTime[6];
            
            if(myYear != theirYear)
                return myYear-theirYear;
            else if(myMonth != theirMonth)
                return myMonth-theirMonth;
            else if(myDay != theirDay)
                return myDay-theirDay;
            else if (!myM.equals(theirM))
                return myM.compareTo(theirM);
            else if(myHour != theirHour)
                return myHour-theirHour;
            else if(myMinute != theirMinute)
                return myMinute-theirMinute;
            else if(mySecond != theirSecond)
                return mySecond-theirSecond;
            else
                return 0;
        }
    }
}