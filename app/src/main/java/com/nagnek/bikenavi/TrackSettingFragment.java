/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.app.AppController;
import com.nagnek.bikenavi.customview.DelayAutoCompleteTextView;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;
import com.nagnek.bikenavi.util.NagneUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TrackSettingFragment extends Fragment implements TrackListOfRecentUsedFragment.OnTrackSelectedListener, TrackListOfBookmarkedFragment.OnTrackSelectedListener {
    static final int SEARCH_INTEREST_POINT = 1; // 장소 검색 request code
    private static final String TAG = TrackSettingFragment.class.getSimpleName();
    DelayAutoCompleteTextView start_point, dest_point;
    POI start_poi, dest_poi;
    TrackPagerAdapter trackPagerAdapter;
    Track track = null;
    private SQLiteHandler db;   // sqlite
    private SessionManager session; // 로그인했는지 확인용 변수

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

        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(getContext().getApplicationContext());

        // Session manager
        session = new SessionManager(getContext().getApplicationContext());

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_track_setting, container, false);

        // Get the ViewPager and set it's POIPagerAdapter so that it can display items
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
    public void onStart() {
        // 다른 액티비티로 갔다가 올때마다. 또는 화면이 다시 뿌려질때마다 경로 리스트 화면들 갱신
        if (trackPagerAdapter != null) {
            if (trackPagerAdapter.trackListOfRecentUsedFragment != null) {
                if (trackPagerAdapter.trackListOfRecentUsedFragment.adapter != null) {
                    // 최근 경로 리스트 갱신
                    if (session.isSessionLoggedIn()) {
                        try {
                            if (trackPagerAdapter.trackListOfRecentUsedFragment.trackList != null) {
                                trackPagerAdapter.trackListOfRecentUsedFragment.trackList.clear();
                                Log.d(TAG, "새로 최근 경로 목록 생성");
                            }
                            trackPagerAdapter.trackListOfRecentUsedFragment.requestCount = 1;
                            trackPagerAdapter.trackListOfRecentUsedFragment.getData();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        trackPagerAdapter.trackListOfRecentUsedFragment.adapter.refresh();
                    }
                }
            }
            if (trackPagerAdapter.trackListOfBookmarkedFragment != null) {
                if (trackPagerAdapter.trackListOfBookmarkedFragment.adapter != null) {
                    // 북마크 경로 리스트 갱신
                    if (session.isSessionLoggedIn()) {
                        try {
                            if (trackPagerAdapter.trackListOfBookmarkedFragment.trackList != null) {
                                trackPagerAdapter.trackListOfBookmarkedFragment.trackList.clear();
                                Log.d(TAG, "새로 북마크 경로 목록 생성");
                            }
                            trackPagerAdapter.trackListOfBookmarkedFragment.requestCount = 1;
                            trackPagerAdapter.trackListOfBookmarkedFragment.getData();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        trackPagerAdapter.trackListOfBookmarkedFragment.adapter.refresh();
                    }
                }
            }
        }
        super.onStart();
    }

    // 주의 : 북마크된 리스트를 눌러도 북마크 테이블의 사용시각은 갱신이 안됨. 최근 유저 사용시각만 갱신됨. 북마크의 경우 생성 시각 기준으로 정렬하기 때문에 고려 안함.
    @Override
    public void onRecentTrackSelected(Track track) {
        this.track = track;
        start_poi = track.startPOI;
        dest_poi = track.destPOI;
        start_point.setText(track.startPOI.name);
        dest_point.setText(track.destPOI.name);
        if (!session.isSessionLoggedIn()) {
            db.updateLastUsedAtUserTrack(track);
            db.updateLastUsedAtTrack(track);
            redirectTrackActivity();
        } else {
            addOrUpdateUserTrackToServer(track, AppConfig.ListType.RECENT);
        }
    }

    // 서버에 경로 저장.
    private void addOrUpdateUserTrackToServer(final Track track, final AppConfig.ListType listType) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_or_delete_track_to_table_bookmark_track";

        // track정보 와 유저정보를 내 서버(회원가입쪽으로)로 HTTP POST를 이용해 보낸다
        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_USER_TRACK_REGISTER_OR_UPDATE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "경로 추가 또는 수정의 Response: " + response);

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    Log.d(TAG, "response : " + response);
                    boolean error = jsonObject.getBoolean("error");

                    // Check for error node in json
                    if (!error) {

                        // 서버에 반영 성공했으니 경로 액티비티로 넘어가자.
                        redirectTrackActivity();

                    } else {
                        // 서버에 갱신 실패. Get the error message
                        String errorMsg = jsonObject.getString("error_msg");
                        Toast.makeText(getContext().getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getContext().getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError) {
                    Log.e(TAG, "Login Error: 서버가 응답하지 않습니다." + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getContext().getApplicationContext(),
                            "Login Error: 서버가 응답하지 않습니다.", Toast.LENGTH_LONG).show();
                } else if (error instanceof ServerError) {
                    Log.e(TAG, "서버 에러래" + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getContext().getApplicationContext(),
                            "Login Error: 서버 Error.", Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getContext().getApplicationContext(),
                            error.getMessage(), Toast.LENGTH_LONG).show();
                }

                Toast.makeText(getContext().getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                // 로그인 한 경우
                SQLiteHandler.UserType loginUserType = session.getUserType();
                HashMap<String, String> user = db.getLoginedUserDetails(loginUserType);

                switch (loginUserType) {
                    case BIKENAVI:
                        String email = user.get(SQLiteHandler.KEY_EMAIL);
                        params.put("email", email);

                        break;
                    case GOOGLE:
                        String googleemail = user.get(SQLiteHandler.KEY_GOOGLE_EMAIL);
                        params.put("googleemail", googleemail);
                        break;
                    case KAKAO:
                        String kakaoId = user.get(SQLiteHandler.KEY_KAKAO_ID);
                        params.put("kakaoid", kakaoId);
                        break;
                    case FACEBOOK:
                        String facebookId = user.get(SQLiteHandler.KEY_FACEBOOK_ID);
                        params.put("facebookid", facebookId);
                        break;
                }
                params.put("START_POI_LAT_LNG", track.startPOI.latLng);
                params.put("DEST_POI_LAT_LNG", track.destPOI.latLng);
                if (track.stop_poi_list != null) {
                    Gson gson = new Gson();
                    params.put("STOP_POI_ARRAY", gson.toJson(track.stop_poi_list));
                }

                switch (listType) {
                    case BOOKMARK:
                        params.put("bookmark", "true");
                        break;
                    case RECENT:
                        params.put("recent", "true");
                        break;
                }

                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    @Override
    public void onBookmarkedSelected(Track track) {
        this.track = track;
        start_point.setText(track.startPOI.name);
        dest_point.setText(track.destPOI.name);
        if (!session.isSessionLoggedIn()) {
            db.updateLastUsedAtUserTrack(track);
            db.updateLastUsedAtTrack(track);
            db.updateBookmarkedTrack(track);
            redirectTrackActivity();
        } else {
            addOrUpdateUserTrackToServer(track, AppConfig.ListType.BOOKMARK);
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
                    if (start_poi.latLng.equals(dest_poi.latLng)) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        alertDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();   // 닫기
                            }
                        });
                        alertDialog.setMessage("시작 장소 위치와 도착장소 위치를 다르게 설정해주세요!");
                        alertDialog.show();
                    } else {
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                track = new Track();
                                track.startPOI = start_poi;
                                track.destPOI = dest_poi;

                                /**
                                 * 시작 장소와 도착장소가 다 설정되었으니
                                 * 로그인 하지 않은 경우라면 경로를 db에,
                                 * 로그인 한 경우라면 서버에 경로를 추가하자
                                 */
                                if (!session.isSessionLoggedIn()) {
                                    // 비 로그인 상태

                                    if (db.checkIfTrackExists(track)) {
                                        // 경로가 기존 경로 테이블에 있다면 이제 경로 정보를 업데이트 해봐야지

                                        db.updateLastUsedAtTrack(track);

                                        // 경로 업데이트.. 자 이제 비로그인 유저의 해당 장소 사용 기록을 갱신해볼까.. 안써봤던 경로라면 추가하자.
                                        if (db.checkIfUserTrackExists(track)) {
                                            db.updateLastUsedAtTrack(track);
                                            db.updateLastUsedAtUserTrack(track);
                                        } else {
                                            db.addLocalUserTrack(track);
                                        }
                                    } else {
                                        // 경로가 기존 경로 테이블에 없었으니 경로 테이블과 사용자가 경로 사용했던 기록도 같이 추가해두자. 몇번이나 말하지만 사용기록은 삭제할 수도 있으니 따로 보관하자.
                                        db.addTrack(track);
                                        db.addLocalUserTrack(track);
                                    }
                                    //                                if (trackPagerAdapter.trackListOfRecentUsedFragment != null) {
                                    //
                                    //                                    // 화면에 보이는 경로 리스트의 내용을 다시 최근 db의 내용으로 업데이트 해놓는다.
                                    //                                    trackPagerAdapter.trackListOfRecentUsedFragment.addOrUpdateTrack(track);
                                    //                                }
                                    // 이제 경로를 그려주는 액티비티 화면으로 넘어가자.
                                    redirectTrackActivity();
                                } else {
                                    /**
                                     * 로그인 한 경우라면 경로를 서버에 보내고 성공한뒤에 경로 액티비티로 넘어가자.
                                     * (그냥 보내고 넘어가도 되는지 모르겠다 상관없을듯도 한데 혹시라도 전송되기 전에 다른 액티비티로 넘어가서 취소가 되지는 않을까 걱정되서 이런다.)
                                     * 취소가 되는지도 모르겠다 흠흠..
                                     */
                                    addOrUpdateUserTrackToServer(track, AppConfig.ListType.RECENT);
                                }
                            }
                        });
                        thread.start();
                    }
                }
            }
        }
    }
}
