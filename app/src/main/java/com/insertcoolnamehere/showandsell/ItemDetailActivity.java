package com.insertcoolnamehere.showandsell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.insertcoolnamehere.showandsell.dummy.DummyContent;
import com.insertcoolnamehere.showandsell.logic.Item;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {

    public static final String ITEM_ID = "ITEM_ID";
    public static final String OWNER_POWERS = "OWNER_POWERS";

    private Item mItem;
    private boolean giveOwnerPowers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the layout of the activity
        setContentView(R.layout.activity_item_detail);

        // get item data from intent
        mItem = Item.getItem(getIntent().getStringExtra(ITEM_ID));
        giveOwnerPowers = getIntent().getBooleanExtra(OWNER_POWERS, false);

        // set text and images for the activity view
        ImageView itemImage = (ImageView) findViewById(R.id.item_detail_image);
        itemImage.setImageBitmap(mItem.getPic());

        TextView itemName = (TextView) findViewById(R.id.item_detail_name);
        itemName.setText(mItem.getName());
        TextView itemPrice = (TextView) findViewById(R.id.item_detail_price);
        itemPrice.setText(String.format(Locale.ENGLISH, "$%.2f", mItem.getPrice()));
        TextView itemCondition = (TextView) findViewById(R.id.item_detail_condition);
        itemCondition.setText(mItem.getCondition());
        TextView itemDescription = (TextView) findViewById(R.id.item_detail_description);
        itemDescription.setText(mItem.getDescription());

        FloatingActionButton approveBtn = (FloatingActionButton) findViewById(R.id.btn_approve);
        final Activity parentForTask = this;
        if(giveOwnerPowers) {
            // show reject button if group owner
            FloatingActionButton rejectButton = (FloatingActionButton) findViewById(R.id.btn_reject);
            rejectButton.setVisibility(View.VISIBLE);
            rejectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new RejectItemTask(parentForTask).execute();
                }
            });

            // show approve button if group owner and item needs approval
            if (!mItem.isApproved()) {
                approveBtn.setVisibility(View.VISIBLE);
                approveBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new ApproveItemTask(parentForTask, true).execute();
                    }
                });
            }
        }

        // set up comments list view
        ListView listView = (ListView) findViewById(R.id.item_comments);
        ArrayList<String> list = new ArrayList<>();
        ListIterator<DummyContent.DummyItem> iter = DummyContent.ITEMS.listIterator();
        while(iter.hasNext()) {
            DummyContent.DummyItem item = iter.next();
            list.add(item.content);
        }
        CommentAdapter<String> adapter = new CommentAdapter<>(this, R.layout.text_view_comment_right, list);
        listView.setAdapter(adapter);

        // set up buy button
        Button buyButton = (Button) findViewById(R.id.item_detail_buy);
        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiatePurchase();
            }
        });
    }

    private void initiatePurchase() {
        // TODO get client token
        attemptPostBookmark();
    }

    private void attemptPostBookmark() {
        new PostBookmarkTask(this).execute();
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

    private class ApproveItemTask extends AsyncTask<Void, Integer, Integer> {
        private static final String LOG_TAG = "ApproveItemTask";
        private static final int SUCCESS = 0;
        private static final int NO_INTERNET = 1;
        private static final int OTHER_FAILURE = 2;

        private Activity mParent;
        private boolean doApprove;

        ApproveItemTask(Activity parent, boolean doApprove) {
            mParent = parent;
            this.doApprove = doApprove;
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
                        Toast.makeText(mParent, "No connection available. Try again later.", Toast.LENGTH_SHORT).show();
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
                            .appendEncodedPath(mItem.getGuid())
                            .appendQueryParameter("groupOwnerPassword", pw).build().toString();
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

                    // get values for item
                    String name = mItem.getName();
                    String price = ""+mItem.getPrice();
                    String condition = mItem.getCondition();
                    String description = mItem.getDescription();
                    Bitmap itemBitmap = mItem.getPic();

                    // scale down image and convert to base64
                    Log.d(LOG_TAG, "is itemBitmap null: "+(itemBitmap == null));
                    double scaleFactor = Math.max(itemBitmap.getWidth()/250.0, itemBitmap.getHeight()/250.0);
                    Bitmap bmp = Bitmap.createScaledBitmap(itemBitmap, (int)(itemBitmap.getWidth()/scaleFactor),(int)(itemBitmap.getHeight()/scaleFactor), false);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    String thumbnail = Base64.encodeToString(byteArray, Base64.NO_WRAP);

                    // convert item to JSON
                    JSONObject item = new JSONObject();
                    item.put("newName", name);
                    item.put("newPrice", price);
                    item.put("newCondition", condition);
                    item.put("newDescription", description);
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
                // return to previous activity
                Intent goHomeIntent = new Intent(mParent, MainActivity.class);
                startActivity(goHomeIntent);
            }
        }
    }

    private class PostBookmarkTask extends AsyncTask<Void, Integer, Integer> {
        private static final int SUCCESS = 0;
        private static final int NO_INTERNET = 1;
        private static final int OTHER_FAILURE = 2;
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
                        Toast.makeText(mParent, "No connection available. Try again later.", Toast.LENGTH_SHORT).show();
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
                    String un = savedData.getString(getString(R.string.prompt_username), "");

                    // form a URL to connect to
                    Uri.Builder builder = new Uri.Builder();
                    String uri = builder.encodedAuthority(LoginActivity.CLOUD_SERVER_IP)
                            .scheme("http")
                            .appendPath("showandsell")
                            .appendPath("api")
                            .appendPath("bookmarks").build().toString();
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
                    String itemId = mItem.getGuid();

                    // convert item to JSON
                    JSONObject bookmark = new JSONObject();
                    bookmark.put("userId", un);
                    bookmark.put("itemId", itemId);
                    String body = bookmark.toString();
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
                Toast.makeText(mParent, "Item bought!", Toast.LENGTH_SHORT).show();
                new ApproveItemTask(mParent, false).execute();
            }
            else
                Toast.makeText(mParent, "Item purchase failed :(", Toast.LENGTH_SHORT).show();
        }
    }

    private class RejectItemTask extends AsyncTask<Void, Integer, Integer> {
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
                        Toast.makeText(mParent, "No connection available. Try again later.", Toast.LENGTH_SHORT).show();
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
                            .appendEncodedPath(mItem.getGuid())
                            .appendQueryParameter("ownerPassword", pw).build().toString();
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
                // return to previous activity
                Intent goHomeIntent = new Intent(mParent, MainActivity.class);
                startActivity(goHomeIntent);
            }
        }
    }
}
