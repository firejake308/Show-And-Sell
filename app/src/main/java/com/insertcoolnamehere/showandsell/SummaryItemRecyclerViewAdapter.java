package com.insertcoolnamehere.showandsell;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.insertcoolnamehere.showandsell.logic.Item;

import java.util.List;
import java.util.Locale;

public class SummaryItemRecyclerViewAdapter extends RecyclerView.Adapter<SummaryItemRecyclerViewAdapter.ViewHolder> {

    private final List<Item> mItems;
    private final BrowseFragment.OnListFragmentInteractionListener mListener;

    private ViewGroup mParent;

    public SummaryItemRecyclerViewAdapter(List<Item> items, BrowseFragment.OnListFragmentInteractionListener listener) {
        mItems = items;
        mListener = listener;
    }

    @Override
    public SummaryItemRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.summary_item, parent, false);
        mParent = parent;
        return new SummaryItemRecyclerViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SummaryItemRecyclerViewAdapter.ViewHolder holder, int position) {
        holder.mItem = mItems.get(position);
        holder.mPriceView.setText(String.format(Locale.ENGLISH, "$%.2f", mItems.get(position).getPrice()));
        holder.mThumbnailView.setImageBitmap(mItems.get(position).getPic());

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
        public final TextView mPriceView;
        public final ImageView mThumbnailView;
        public Item mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mPriceView = (TextView) view.findViewById(R.id.item_price);
            mThumbnailView = (ImageView) view.findViewById(R.id.item_picture);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mItem.getName() + "'";
        }
    }
}
