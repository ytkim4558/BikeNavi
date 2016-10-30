/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
 * Created by user on 2016-10-27.
 */

public class BookmarkedTrackListAdapter extends RecyclerView.Adapter<BookmarkedTrackListAdapter.BookmarkedTrackListViewHolder> {
    private static final String TAG = BookmarkedTrackListAdapter.class.getSimpleName();
    Context context;
    List<Track> recentTrackList = new ArrayList<>();
    LayoutInflater inflater;
    TrackListListener trackListListener;
    private SQLiteHandler db;   // sqlite

    // Provide a suitable constructor (depends on the kind of dataset)
    public BookmarkedTrackListAdapter(Context context, List<Track> trackList, TrackListListener trackListListener) {

        this.context = context;
        this.recentTrackList = trackList;
        this.trackListListener = trackListListener;
        inflater = LayoutInflater.from(context);
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(context.getApplicationContext());
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public BookmarkedTrackListAdapter(Context context, List<Track> trackList) {

        this.context = context;
        this.recentTrackList = trackList;
        this.trackListListener = (TrackListListener) context;
        inflater = LayoutInflater.from(context);

    }

    // Create new views (invoked by the layout manager)
    @Override
    public BookmarkedTrackListViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item_recent_track, parent, false);
        // set the view's size, margins, paddings and layout parameters
        BookmarkedTrackListViewHolder vh = new BookmarkedTrackListViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(BookmarkedTrackListViewHolder holder, final int position) {

        holder.iv_delete.setTag(position);
        List<POI> stopList = recentTrackList.get(position).stop_list;
        if (stopList == null) {
            // 경유지가 없으므로 시작장소 -> 도착장소로 표시
            if (recentTrackList.get(position).start_poi != null && recentTrackList.get(position).dest_poi != null) {
                holder.track_log.setText(recentTrackList.get(position).start_poi.name + " -> " + recentTrackList.get(position).dest_poi.name);
            }
        } else {
            // 경유지마다 전부 표시
            // 트랙 경로들을 -> 로 묶어서 보여줌
            String track_list = recentTrackList.get(position).start_poi.name;

            for (POI poi : stopList) {
                track_list += ("->" + poi.name);
            }
            track_list += recentTrackList.get(position).dest_poi.name;
            holder.track_log.setText(track_list);
        }
        holder.track_last_used_at.setText(Time.formatTimeString(db.getLastUsedAtUsingTrack(recentTrackList.get(position))));
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int select_position = (Integer) v.getTag();
                Log.d(TAG, "selected_position : " + select_position);
                trackListListener.trackClickToSet(recentTrackList.get(select_position), select_position);
            }
        });

        holder.iv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int delete_position = (Integer) v.getTag();
                trackListListener.trackClickToDelete(recentTrackList.get(delete_position));
                recentTrackList.remove(delete_position);
                notifyItemRemoved(delete_position);
                //this line below gives you the animation and also updates the
                //list items after the deleted item
                notifyItemRangeChanged(delete_position, getItemCount());
            }
        });
    }

    @Override
    public int getItemCount() {
        return recentTrackList.size();
    }

    void addTrack(Track track) {
        recentTrackList.add(track);
        notifyItemInserted(0);
        //this line below gives you the animation and also updates the
        //list items after the inserted item
        notifyItemRangeChanged(0, getItemCount());
    }

    void updateTrack(Track track, int position) {
        recentTrackList.remove(position);
        recentTrackList.add(0, track);
        notifyDataSetChanged();
    }

    void refresh() {
        this.recentTrackList = db.getAllTrack();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    class BookmarkedTrackListViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_delete;
        CardView card_view;
        TextView track_log; // 트랙 로그. 예 ) 출발지 -> 도착지 또는 출발지 -> 경유지1 -> 경유지2 -> 경유지3 -> .. -> 도착지
        TextView track_last_used_at; //trackLastUsedAt

        public BookmarkedTrackListViewHolder(View v) {
            super(v);

            card_view = (CardView) v.findViewById(R.id.card_view);
            track_log = (TextView) v.findViewById(R.id.text_track_log);
            iv_delete = (ImageView) v.findViewById(R.id.iv_delete);
            track_last_used_at = (TextView) v.findViewById(R.id.text_track_last_used_at);
        }
    }
}
