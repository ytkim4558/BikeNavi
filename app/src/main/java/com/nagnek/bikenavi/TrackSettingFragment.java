/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nagnek.bikenavi.customview.DelayAutoCompleteTextView;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.util.NagneUtil;

public class TrackSettingFragment extends Fragment implements RecentTrackListFragment.OnTrackSelectedListener {
    static final int SEARCH_INTEREST_POINT = 1; // 장소 검색 request code
    private static final String TAG = TrackSettingFragment.class.getSimpleName();
    DelayAutoCompleteTextView start_point, dest_point;
    POI start_poi, dest_poi;
    TrackPagerAdapter trackPagerAdapter;
    Track track = null;
    private SQLiteHandler db;   // sqlite

    public TrackSettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_track_setting, container, false);

        // Get the ViewPager and set it's RecentPOIPagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
        trackPagerAdapter = new TrackPagerAdapter(getChildFragmentManager(), getActivity());
        viewPager.setAdapter(trackPagerAdapter);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        // Iterate over all tabs and set the custom view
        for (int i = 0; i < tabLayout.getTabCount(); ++i) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setCustomView(trackPagerAdapter.getTabView(i));
            } else {
                Log.d(TAG, "tab이 null이네?");
            }
        }

        long start = System.currentTimeMillis();
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(getActivity().getApplicationContext());

        final TextInputLayout ti_start = (TextInputLayout) rootView.findViewById(R.id.ti_start_point);
        final TextInputLayout ti_dest = (TextInputLayout) rootView.findViewById(R.id.ti_dest_point);
        /**
         * 출발지나 도착지 입력창을 클릭하면 검색 액티비티로 넘어간다.
         */
        start_point = (DelayAutoCompleteTextView) rootView.findViewById(R.id.start_point);
        start_point.setKeyListener(null);
        start_point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Performing stop of activity that is not resumed: {com.nagnek.bikenavi/com.nagnek.bikenavi.MainActivity} 에러 방지
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getContext(), SearchActivity.class);
                        intent.putExtra(getResources().getString(R.string.name_purpose_search_point), "출발");
                        intent.putExtra(getResources().getString(R.string.current_point_text_for_transition), start_point.getText().toString());
                        // 화면전환 애니메이션을 생성한다. 트랜지션 이름은 양쪽 액티비티에 선언되어야한다.
                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity(),
                                start_point, start_point.getTransitionName());
                        getActivity().startActivityForResult(intent, SEARCH_INTEREST_POINT, options.toBundle());
                    }
                }, 300);
            }
        });

        dest_point = (DelayAutoCompleteTextView) rootView.findViewById(R.id.dest_point);
        dest_point.setKeyListener(null);
        dest_point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getActivity(), SearchActivity.class);
                        intent.putExtra(getResources().getString(R.string.name_purpose_search_point), "도착");
                        intent.putExtra(getResources().getString(R.string.current_point_text_for_transition), dest_point.getText().toString());
                        // 화면전환 애니메이션을 생성한다. 트랜지션 이름은 양쪽 액티비티에 선언되어야한다.
                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity(),
                                dest_point, dest_point.getTransitionName());
                        getActivity().startActivityForResult(intent, SEARCH_INTEREST_POINT, options.toBundle());
                    }
                }, 300);
            }
        });
        return rootView;
    }

    @Override
    public void onRecentTrackSelected(Track track) {
        start_point.setText(track.start_poi.name);
        dest_point.setText(track.dest_poi.name);
        db.updateLastUsedAtTrack(track);
        reactionSearchResult();
    }

    // 출발지 도착지 설정할때마다 불러옴.
    // 둘다 설정한 경우는 경로를 탐색하고 한곳만 설정한 경우는 해당 좌표를 표시한다.
    void reactionSearchResult() {
        // start 지점과 도착지점 모두 설정되었으면 경로를 찾는다.
        if (!start_point.getText().toString().equals("") && !dest_point.getText().toString().equals("")) {
            redirectTrackActivity();
        }
    }


    void redirectTrackActivity() {
        Intent intent = new Intent(getContext(), TrackActivity.class);
        intent.putExtra(NagneUtil.getStringFromResources(getActivity().getApplicationContext(), R.string.start_point_text_for_transition), start_point.getText().toString());
        intent.putExtra(NagneUtil.getStringFromResources(getActivity().getApplicationContext(), R.string.dest_point_text_for_transition), dest_point.getText().toString());
        intent.putExtra(NagneUtil.getStringFromResources(getActivity().getApplicationContext(), R.string.current_track_for_transition), track);
        startActivity(intent);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "왓나?");
        Log.d(TAG, "requestCode = " + requestCode + " But, I want this requestCode: " + SEARCH_INTEREST_POINT);
        if (requestCode == SEARCH_INTEREST_POINT) { // 장소검색 요청한게 돌아온 경우
            Log.d(TAG, "SEARCH_INTEREST_POINT_TRACK_SETTING_FRAGMENT");
            if (resultCode == Activity.RESULT_OK) {// 장소 검색 결과 리턴
                String purposePoint = data.getStringExtra(NagneUtil.getStringFromResources(getActivity(), R.string.name_purpose_search_point));
                Log.d(TAG, "장소입력한 곳은? " + purposePoint);
                String selectPoint = data.getStringExtra(NagneUtil.getStringFromResources(getActivity(), R.string.select_poi_name_for_transition));
                String address = data.getStringExtra(NagneUtil.getStringFromResources(getActivity(), R.string.select_poi_address_for_transition));
                if (purposePoint.equals("출발")) {
                    start_point.setText(selectPoint);
                    start_poi = (POI) data.getSerializableExtra(NagneUtil.getStringFromResources(getActivity(), R.string.current_point_poi_for_transition));
                } else if (purposePoint.equals("도착")) {
                    dest_point.setText(selectPoint);
                    dest_poi = (POI) data.getSerializableExtra(NagneUtil.getStringFromResources(getActivity(), R.string.current_point_poi_for_transition));
                } else {
                    Log.d(TAG, "purposePoint에 값이 없나? 아님 이상한가?");
                }
                if (start_poi != null && dest_poi != null) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Track track = new Track();
                            track.start_poi = start_poi;
                            track.dest_poi = dest_poi;
                            if (db.checkIfTrackExists(track)) {
                                db.updateLastUsedAtTrack(track);
                            } else {
                                db.addTrack(track);
                            }
                            if (trackPagerAdapter.recentTrackFragment != null) {
                                trackPagerAdapter.recentTrackFragment.addOrUpdateTrack(track);
                            }

                        }
                    });
                    thread.start();
                }
                reactionSearchResult();
            }
        }
    }
}
