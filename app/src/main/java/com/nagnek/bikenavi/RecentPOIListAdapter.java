/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2016-10-21.
 * 최근 장소 어댑터
 */

public class RecentPOIListAdapter extends RecyclerView.Adapter<RecentPOIListAdapter.RecentPOIListViewHolder> {

    Context context;
    List<POI> recentPOIList = new ArrayList<>();
    LayoutInflater inflater;
    Listener listener;

    class RecentPOIListViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_delete;
        CardView card_view;
        TextView poi_name; //poiName

        public RecentPOIListViewHolder(View v) {
            super(v);

            card_view = (CardView) v.findViewById(R.id.card_view);
            poi_name = (TextView) v.findViewById(R.id.text_poi_name);
            iv_delete = (ImageView) v.findViewById(R.id.iv_delete);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecentPOIListAdapter(Context context, List<POI> poiList, Listener listener) {

        this.context = context;
        this.recentPOIList = poiList;
        this.listener = listener;
        inflater = LayoutInflater.from(context);

    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecentPOIListAdapter(Context context, List<POI> poiList) {

        this.context = context;
        this.recentPOIList = poiList;
        this.listener = (Listener) context;
        inflater = LayoutInflater.from(context);

    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecentPOIListViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item, parent, false);
        // set the view's size, margins, paddings and layout parameters
        RecentPOIListViewHolder vh = new RecentPOIListViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecentPOIListViewHolder holder, final int position) {

        holder.iv_delete.setTag(position);
        holder.poi_name.setText(recentPOIList.get(position).name);

        holder.iv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.latLngToDelete(recentPOIList.get((Integer)v.getTag()).latLng);
                recentPOIList.remove(position);
                notifyItemRemoved(position);
                //this line below gives you the animation and also updates the
                //list items after the deleted item
                notifyItemRangeChanged(position, getItemCount());
            }
        });
    }

    @Override
    public int getItemCount() {
        return recentPOIList.size();
    }
}
