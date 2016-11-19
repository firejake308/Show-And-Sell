package com.insertcoolnamehere.showandsell;

import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.insertcoolnamehere.showandsell.BrowseFragment.OnListFragmentInteractionListener;
import com.insertcoolnamehere.showandsell.dummy.DummyContent.DummyItem;
import com.insertcoolnamehere.showandsell.logic.Item;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class BrowseItemRecyclerViewAdapter extends RecyclerView.Adapter<BrowseItemRecyclerViewAdapter.ViewHolder> {

    private final List<Item> mItems;
    private final OnListFragmentInteractionListener mListener;

    public BrowseItemRecyclerViewAdapter(List<Item> items, OnListFragmentInteractionListener listener) {
        mItems = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mItems.get(position);
        holder.mNameView.setText(mItems.get(position).getName());
        holder.mDescriptionView.setText(mItems.get(position).getDescription());
        holder.mPriceView.setText("$"+mItems.get(position).getPrice());
        holder.mConditionView.setText(mItems.get(position).getCondition());
        holder.mThumbnailView.setImageBitmap(mItems.get(position).getPic());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mNameView;
        public final TextView mDescriptionView;
        public final TextView mPriceView;
        public final TextView mConditionView;
        public final ImageView mThumbnailView;
        public Item mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = (TextView) view.findViewById(R.id.item_name);
            mDescriptionView = (TextView) view.findViewById(R.id.item_description);
            mPriceView = (TextView) view.findViewById(R.id.item_price);
            mConditionView = (TextView) view.findViewById(R.id.item_condition);
            mThumbnailView = (ImageView) view.findViewById(R.id.item_picture);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mDescriptionView.getText() + "'";
        }
    }
}