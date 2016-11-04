/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.nagnek.bikenavi.helper.SQLiteHandler;

/**
 * Created by user on 2016-10-29.
 */

public class TrackBookmarkedListFragment extends Fragment implements TrackListListener {
    private static final String TAG = TrackBookmarkedListFragment.class.getSimpleName();
    TrackBookmarkedListFragment.OnTrackSelectedListener mCallback;
    SQLiteHandler db;
    TrackBookmarkedListAdapter adapter;
    RecyclerView rv;

    public TrackBookmarkedListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_bookmarked_track, container, false);
        Log.d(TAG, "inflater.inflate");
        db = SQLiteHandler.getInstance(getContext().getApplicationContext());
        Log.d(TAG, "SQLiteHandler.getInstance");

        rv = (RecyclerView) rootView.findViewById(R.id.recenet_search_recyclerView);
        rv.setHasFixedSize(true);

        Log.d(TAG, "rootView.findViewById(R.id.recenet_search_recyclerView");
        adapter = new TrackBookmarkedListAdapter(getContext().getApplicationContext(), db.getAllBookmarkedTrack(), this);
        rv.setAdapter(adapter);

        LinearLayoutManager llm = new LinearLayoutManager(getContext().getApplicationContext());
        rv.setLayoutManager(llm);

        try {
            mCallback = (TrackBookmarkedListFragment.OnTrackSelectedListener) getParentFragment();
            if (mCallback == null) {
                Log.d(TAG, "mCallback은 null이야 ㅠ");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString() + "must implement OnTrackSelectedListener");
        }

        return rootView;
    }

    @Override
    public void trackClickToDelete(Track track) {
        Gson gson = new Gson();
        Log.d(TAG, "delete track : " + gson.toJson(track));
        db.deleteBookmarkedTrackRow(track);
    }

    @Override
    public void trackClickToSet(Track track, int position) {
        Gson gson = new Gson();
        Log.d(TAG, "click track : " + gson.toJson(track));
        mCallback.onBookmarkedSelected(track);
        adapter.updateTrack(track, position);
    }

    public void addOrUpdateTrack(Track track) {
        if (db.checkIFBookmarkedTrackExists(track)) {
            adapter.refresh();
        } else {
            adapter.addTrack(track);
        }
    }

    // 부모 프래그먼트는 항상 이 인터페이스를 구현 해야한다
    public interface OnTrackSelectedListener {
        void onBookmarkedSelected(Track track);
    }
}