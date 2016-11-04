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

import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.time.Time;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2016-10-21.
 * 최근 장소 어댑터
 */

public class POIRecentListAdapter extends RecyclerView.Adapter<POIRecentListAdapter.RecentPOIListViewHolder> {

    LayoutInflater inflater;
    private Context context;
    private List<POI> recentPOIList = new ArrayList<>();
    private POIListener POIListener;
    private SQLiteHandler db;   // sqlite

    // Provide a suitable constructor (depends on the kind of dataset)
    public POIRecentListAdapter(Context context, List<POI> poiList, POIListener POIListener) {

        this.context = context;
        this.recentPOIList = poiList;
        this.POIListener = POIListener;
        inflater = LayoutInflater.from(context);
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(context.getApplicationContext());
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecentPOIListViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item_poi, parent, false);
        // set the view's size, margins, paddings and layout parameters
        RecentPOIListViewHolder vh = new RecentPOIListViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecentPOIListViewHolder holder, final int position) {
        POI currentPOI = recentPOIList.get(position);
        holder.iv_delete.setTag(position);
        holder.poi_name.setText(currentPOI.name);
        holder.poi_address.setText(currentPOI.address);
        if (currentPOI.last_used_at != null) {
            // 로그인해서 서버에서 정보를 받아와서 last_used_at이 적혀진 경우
            holder.poi_last_used_at.setText(Time.formatTimeString(currentPOI.last_used_at));
        } else {
            holder.poi_last_used_at.setText(Time.formatTimeString(db.getLastUsedAtUsingPOI(currentPOI.latLng)));
        }
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int select_position = (Integer) v.getTag();
                POIListener.poiClickToSet(recentPOIList.get(select_position));
            }
        });

        holder.iv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int delete_position = (Integer) v.getTag();
                POIListener.latLngToDelete(recentPOIList.get(delete_position).latLng);
                recentPOIList.remove(delete_position);
                notifyItemRemoved(delete_position);
                //this line below gives you the animation and also updates the
                //list items after the deleted item
                notifyItemRangeChanged(delete_position, getItemCount());
            }
        });
    }

    @Override
    public int getItemCount() {
        return recentPOIList.size();
    }

    class RecentPOIListViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_delete;
        CardView card_view;
        TextView poi_name; //poiName
        TextView poi_address; //poiAddress
        TextView poi_last_used_at; //poiLastUsedAt

        public RecentPOIListViewHolder(View v) {
            super(v);

            card_view = (CardView) v.findViewById(R.id.card_view);
            poi_name = (TextView) v.findViewById(R.id.text_poi_name);
            poi_address = (TextView) v.findViewById(R.id.text_poi_address);
            iv_delete = (ImageView) v.findViewById(R.id.iv_delete);
            poi_last_used_at = (TextView) v.findViewById(R.id.text_poi_last_used_at);
        }
    }
}
