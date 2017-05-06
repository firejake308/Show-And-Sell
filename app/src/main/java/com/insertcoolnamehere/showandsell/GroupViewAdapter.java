package com.insertcoolnamehere.showandsell;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.insertcoolnamehere.showandsell.logic.Group;

import java.util.ArrayList;

/**
 * Adapter to populate group selection list
 */
public class GroupViewAdapter extends RecyclerView.Adapter<GroupViewAdapter.ViewHolder> {

    private ArrayList<Group> groupsToShow;
    private ChooseGroupFragment.OnChooseGroupListener mListener;
    private NewGroupListener mNewGroupListener;
    private Group currentGroup;

    public GroupViewAdapter(ArrayList<Group> list, ChooseGroupFragment.OnChooseGroupListener listener, NewGroupListener listener2) {
        groupsToShow = list;
        mListener = listener;
        mNewGroupListener = listener2;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_group, parent, false);
        return new ViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        viewHolder.mGroup = groupsToShow.get(position);

        // populate text fields
        viewHolder.mNameView.setText(viewHolder.mGroup.getName());
        viewHolder.mAddressView.setText(viewHolder.mGroup.getPickupAddress());

        // link to group detail
        viewHolder.mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onChooseGroup(viewHolder.mGroup);
            }
        });

        viewHolder.mFavoriteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Animation shrink = AnimationUtils.loadAnimation(viewHolder.mRootView.getContext(), R.anim.shrink);
                //viewHolder.mRootView.startAnimation(shrink);
                viewHolder.mRootView.invalidate();
                mNewGroupListener.setNewGroup(viewHolder.mGroup);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupsToShow.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View mRootView;
        TextView mNameView;
        TextView mAddressView;
        ImageView mFavoriteView;
        Group mGroup;

        ViewHolder(View view) {
            super(view);

            mRootView = view;
            mNameView = (TextView) view.findViewById(R.id.group_name);
            mAddressView = (TextView) view.findViewById(R.id.group_address);
            mFavoriteView = (ImageView) view.findViewById(R.id.favorite_group);
        }
    }

    public interface NewGroupListener {
        void setNewGroup(Group group);
    }
}
