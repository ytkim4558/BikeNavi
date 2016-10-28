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

import com.nagnek.bikenavi.helper.SQLiteHandler;

public class RecentPOIFragment extends Fragment implements RecentPOIListener {
    private static final String TAG = RecentPOIFragment.class.getSimpleName();
    OnPoiSelectedListener mCallback;
    SQLiteHandler db;
    RecentPOIListAdapter adapter;
    RecyclerView rv;
    public RecentPOIFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_recent, container, false);

        db = SQLiteHandler.getInstance(getActivity().getApplicationContext());

        rv = (RecyclerView) rootView.findViewById(R.id.recenet_search_recyclerView);
        rv.setHasFixedSize(true);
        adapter = new RecentPOIListAdapter(getActivity(), db.getAllPOI(), this);
        rv.setAdapter(adapter);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity().getApplicationContext());
        rv.setLayoutManager(llm);

        try {
            mCallback = (OnPoiSelectedListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + "must implement OnPoiSelectedListener");
        }

        return rootView;
    }

    @Override
    public void latLngToDelete(String latLng) {
        db.deletePOIRow(latLng);
        Log.d(TAG, "latLng : " + latLng);
    }

    @Override
    public void poiClickToSet(POI poi) {
        mCallback.onRecentPOISelected(poi);
    }

    // 액티비티는 항상 이 인터페이스를 구현 해야한다
    public interface OnPoiSelectedListener {
        void onRecentPOISelected(POI poi);
    }
}
