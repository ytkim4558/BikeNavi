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
import com.nagnek.bikenavi.helper.SessionManager;
import com.nagnek.bikenavi.time.Time;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2016-10-27.
 */

public class TrackListOfRecentUsedAdapter extends RecyclerView.Adapter<TrackListOfRecentUsedAdapter.RecentTrackListViewHolder> {
    private static final String TAG = TrackListOfRecentUsedAdapter.class.getSimpleName();
    Context context;
    List<Track> recentTrackList = new ArrayList<>();
    LayoutInflater inflater;
    TrackListListener trackListListener;
    private SQLiteHandler db;   // sqlite
    private SessionManager session; // 로그인했는지 확인용 변수

    // Provide a suitable constructor (depends on the kind of dataset)
    public TrackListOfRecentUsedAdapter(Context context, List<Track> trackList, TrackListListener trackListListener) {

        this.context = context;
        this.recentTrackList = trackList;
        this.trackListListener = trackListListener;
        inflater = LayoutInflater.from(context);
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(context.getApplicationContext());
        // Session manager
        session = new SessionManager(context.getApplicationContext());
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public TrackListOfRecentUsedAdapter(Context context, List<Track> trackList) {

        this.context = context;
        this.recentTrackList = trackList;
        this.trackListListener = (TrackListListener) context;
        inflater = LayoutInflater.from(context);

    }

    // Create new views (invoked by the layout manager)
    @Override
    public TrackListOfRecentUsedAdapter.RecentTrackListViewHolder onCreateViewHolder(ViewGroup parent,
                                                                                     int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item_recent_track, parent, false);
        // set the view's size, margins, paddings and layout parameters
        TrackListOfRecentUsedAdapter.RecentTrackListViewHolder vh = new TrackListOfRecentUsedAdapter.RecentTrackListViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(TrackListOfRecentUsedAdapter.RecentTrackListViewHolder holder, final int position) {

        holder.iv_delete.setTag(position);
        List<POI> stopIDList = recentTrackList.get(position).stop_poi_list;
        if (stopIDList == null) {
            // 경유지가 없으므로 시작장소 -> 도착장소로 표시
            holder.track_log.setText(recentTrackList.get(position).startPOI.name + " ➤ " + recentTrackList.get(position).destPOI.name);
        } else {
            // 경유지마다 전부 표시
            // 트랙 경로들을 -> 로 묶어서 보여줌
            String track_list = recentTrackList.get(position).startPOI.name;

            for (POI poi : stopIDList) {
                track_list += (" ➤ " + poi.name);
            }
            track_list += recentTrackList.get(position).destPOI.name;
            holder.track_log.setText(track_list);
        }
        if (session.isSessionLoggedIn()) {
            holder.track_last_used_at.setText(Time.formatTimeString(recentTrackList.get(position).last_used_at));
        } else {
            holder.track_last_used_at.setText(Time.formatTimeString(db.getLastUsedAtUsingTrack(recentTrackList.get(position))));
        }
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
        if (session.isSessionLoggedIn()) {
        } else {
            this.recentTrackList = db.getAllLocalUserTrack();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    class RecentTrackListViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_delete;
        CardView card_view;
        TextView track_log; // 트랙 로그. 예 ) 출발지 -> 도착지 또는 출발지 -> 경유지1 -> 경유지2 -> 경유지3 -> .. -> 도착지
        TextView track_last_used_at; //trackLastUsedAt

        public RecentTrackListViewHolder(View v) {
            super(v);

            card_view = (CardView) v.findViewById(R.id.card_view);
            track_log = (TextView) v.findViewById(R.id.text_track_log);
            iv_delete = (ImageView) v.findViewById(R.id.iv_delete);
            track_last_used_at = (TextView) v.findViewById(R.id.text_poi_last_used_at);
        }
    }
}
