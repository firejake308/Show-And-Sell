package com.insertcoolnamehere.showandsell;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.insertcoolnamehere.showandsell.BrowseFragment.OnListFragmentInteractionListener;
import com.insertcoolnamehere.showandsell.logic.Item;

import java.util.List;
import java.util.Locale;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Item} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class FullItemRecyclerViewAdapter extends RecyclerView.Adapter<FullItemRecyclerViewAdapter.ViewHolder> {

    private final List<Item> mItems;
    private final OnListFragmentInteractionListener mListener;

    private ViewGroup mParent;

    FullItemRecyclerViewAdapter(List<Item> items, OnListFragmentInteractionListener listener) {
        mItems = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_item, parent, false);
        mParent = parent;
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mItems.get(position);
        holder.mNameView.setText(mItems.get(position).getName());
        holder.mPriceView.setText(String.format(Locale.ENGLISH, "$%.2f", mItems.get(position).getPrice()));
        holder.mConditionView.setText(mItems.get(position).getCondition());
        holder.mThumbnailView.setImageBitmap(mItems.get(position).getPic());
        holder.mThumbnailView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem.getGuid());
                }
            }
        });

        // grey out unapproved items
        if(!holder.mItem.isApproved()) {
            holder.mView.setAlpha(0.5f);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mNameView;
        public final TextView mPriceView;
        public final TextView mConditionView;
        public final ImageView mThumbnailView;
        public Item mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = (TextView) view.findViewById(R.id.item_name);
            mPriceView = (TextView) view.findViewById(R.id.item_price);
            mConditionView = (TextView) view.findViewById(R.id.item_condition);
            mThumbnailView = (ImageView) view.findViewById(R.id.item_picture);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
    }
}
