/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by user on 2016-10-21.
 */

public class RecentPOIAdapter extends RecyclerView.Adapter<RecentPOIAdapter.RecentPOIViewHolder> {
    private String[] mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class RecentPOIViewHolder extends RecyclerView.ViewHolder {
        public CardView mCardView;
        public TextView mTextView;
        public RecentPOIViewHolder(View v) {
            super(v);

            mCardView = (CardView) v.findViewById(R.id.card_view);
            mTextView = (TextView) v.findViewById(R.id.text_view);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecentPOIAdapter(String[] myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecentPOIAdapter.RecentPOIViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item, parent, false);
        // set the view's size, margins, paddings and layout parameters
        RecentPOIViewHolder vh = new RecentPOIViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecentPOIViewHolder holder, int position) {
        holder.mTextView.setText(mDataset[position]);
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
