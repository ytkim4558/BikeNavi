/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by user on 2016-10-29.
 */

public class TrackListOfBookmarkedFragment extends Fragment implements TrackListListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = TrackListOfBookmarkedFragment.class.getSimpleName();
    public List<Track> trackList;
    //The request counter to send ?page=1, ?page=2  requests
    // 리스트뷰의 페이지 요청
    public int requestCount = 1;
    TrackListOfBookmarkedFragment.OnTrackSelectedListener mCallback;
    SQLiteHandler db;
    TrackListOfBookmarkedAdapter adapter;
    RecyclerView rv;
    ProgressBar progressBar;
    SQLiteHandler.UserType loginUserType;
    HashMap<String, String> user;
    private SessionManager session; // 로그인했는지 확인용 변수
    private SwipeRefreshLayout mSwipeRefresh;

    public TrackListOfBookmarkedFragment() {
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
        View rootView = inflater.inflate(R.layout.fragment_bookmarked, container, false);
        Log.d(TAG, "inflater.inflate");
        db = SQLiteHandler.getInstance(getContext().getApplicationContext());
        Log.d(TAG, "SQLiteHandler.getInstance");

        // Session manager
        session = new SessionManager(getContext().getApplicationContext());

        if (session.isSessionLoggedIn()) {
            loginUserType = session.getUserType();
            user = db.getLoginedUserDetails(loginUserType);
        }

        // trackList 초기화
        trackList = new ArrayList<>();

        //Initializing ProgressBar
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        mSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);

        mSwipeRefresh.setOnRefreshListener(this);

        rv = (RecyclerView) rootView.findViewById(R.id.bookmark_recyclerView);
        rv.setHasFixedSize(true);

        if (session.isSessionLoggedIn()) {
            try {
                getData();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "rootView.findViewById(R.id.recenet_search_recyclerView");

        // 로그과 비로그인 유저 구별
        if (session.isSessionLoggedIn()) {
            // Displaying Progressbar
            progressBar.setVisibility(View.VISIBLE);
            adapter = new TrackListOfBookmarkedAdapter(getContext().getApplicationContext(), trackList, this);
        } else {
            adapter = new TrackListOfBookmarkedAdapter(getContext().getApplicationContext(), db.getAllBookmarkedTrack(), this);
        }

        rv.setAdapter(adapter);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    // check for scroll down
                    if (isLastItemDisplaying(recyclerView)) {
                        // Calling the method getdata again
                        try {
                            if (session.isSessionLoggedIn()) {
                                getData();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, e.toString());
                        }
                    }
                }
            }
        });


        LinearLayoutManager llm = new LinearLayoutManager(getContext().getApplicationContext());
        rv.setLayoutManager(llm);

        try {
            mCallback = (TrackListOfBookmarkedFragment.OnTrackSelectedListener) getParentFragment();
            if (mCallback == null) {
                Log.d(TAG, "mCallback은 null이야 ㅠ");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString() + "must implement OnTrackSelectedListener");
        }

        return rootView;
    }

    //This method would check that the recyclerview scroll has reached the bottom or not
    private boolean isLastItemDisplaying(RecyclerView recyclerView) {
        if (recyclerView.getAdapter().getItemCount() != 0) {
            int lastVisibleItemPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1)
                return true;
        }
        return false;
    }

    // web api 로부터 데이터 가져오는 함수
    public void getData() throws JSONException {
        // Tag used to cancel the request
        String tag_string_req = "req_range_bookmark_track";

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(getDataFromServer(requestCount), tag_string_req);
        requestCount++;
    }

    /**
     * 유저가 검색한 특정 track 정보 삭제
     *
     * @param track : 선택한 경로
     */
    private void deleteBOOKMARKUSERTRACK(final Track track) {
        // Tag used to cancel the request
        String tag_string_req = "req_delete_bookmark_user_track";

        progressBar.setVisibility(View.VISIBLE);

        // id 와 이름을 내 서버(회원가입쪽으로)로 HTTP POST를 이용해 보낸다
        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_USER_TRACK_DELETE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "USER TRACK DELETE Response: " + response);
                progressBar.setVisibility(View.GONE);

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // TODO: adapter 초기화할까?
                        Log.d(TAG, "트랙 정보 삭제됨");
                    } else {
                        // Error in Delete. Get the error message
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
                progressBar.setVisibility(View.GONE);
            }
        }) {
            @Override
            protected HashMap<String, String> getParams() {
                // Posting parameters to login url
                HashMap<String, String> params = new HashMap<String, String>();
                params = inputUserInfoToInputParams(params);
                params.put("START_POI_LAT_LNG", track.startPOI.latLng);
                params.put("DEST_POI_LAT_LNG", track.destPOI.latLng);
                if (track.stop_poi_list != null) {
                    Gson gson = new Gson();
                    params.put("STOP_POI_ARRAY", gson.toJson(track.stop_poi_list));
                }
                params.put("bookmark", "true");

                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private HashMap<String, String> inputUserInfoToInputParams(HashMap<String, String> params) {
        switch (loginUserType) {
            case BIKENAVI:
                String email = user.get(SQLiteHandler.KEY_EMAIL);
                params.put("email", email);
                Log.d(TAG, "bikenavi타입 유저네" + email);
                break;
            case GOOGLE:
                String googleemail = user.get(SQLiteHandler.KEY_GOOGLE_EMAIL);
                params.put("googleemail", googleemail);
                Log.d(TAG, "구글 유저네" + googleemail);
                break;
            case KAKAO:
                String kakaoId = user.get(SQLiteHandler.KEY_KAKAO_ID);
                params.put("kakaoid", kakaoId);
                Log.d(TAG, "카카오 유저네" + kakaoId);
                break;
            case FACEBOOK:
                String facebookId = user.get(SQLiteHandler.KEY_FACEBOOK_ID);
                params.put("facebookid", facebookId);
                Log.d(TAG, "페북 유저네" + facebookId);
                break;
        }

        return params;

    }

    // 리스트를 서버에서 받아오기
    //Request to get json from server we are passing an integer here
    //This integer will used to specify the page number for the request ?page = requestcount
    //This method would return a JsonArrayRequest that will be added to the request queue
    private StringRequest getDataFromServer(final int requestCount) throws JSONException {

        // progressbar 보여주기
        progressBar.setVisibility(View.VISIBLE);

        //JsonArrayRequest of volley
        final StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_TRACK_LIST_LOAD,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Calling method parsePOIList to parse the json response
                        try {
                            Log.d(TAG, "response : " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            boolean error = jsonObject.getBoolean("error");

                            if (!error) {
                                String recentTrackList = jsonObject.getString("track");
                                parseTrackList(new JSONArray(recentTrackList));
                            } else {
                                // Error in login. Get the error message
                                String errorMsg = jsonObject.getString("error_msg");
                                showAlertDialogMessage(errorMsg);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //Hiding the progressbar
                        progressBar.setVisibility(View.GONE);
                        mSwipeRefresh.setRefreshing(false);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        mSwipeRefresh.setRefreshing(false);
                        //If an error occurs that means end of the list has reached
                        showAlertDialogMessage(error.getMessage());
                    }
                }) {
            @Override
            protected HashMap<String, String> getParams() {
                HashMap<String, String> mRequestParams = new HashMap<String, String>();
                mRequestParams.put("page", String.valueOf(requestCount));
                mRequestParams.put("bookmark", "true");
                mRequestParams = inputUserInfoToInputParams(mRequestParams);

                return mRequestParams;
            }
        };

        //Returning the request
        return strReq;
    }

    void showAlertDialogMessage(String message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();   // 닫기
            }
        });
        alert.setMessage(message);
        alert.show();
    }

    // json Data 파싱
    private void parseTrackList(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            //장소 객체 생성
            Track track = null;
            JSONObject jsonObject = null;
            try {
                // json 가져옴
                jsonObject = array.getJSONObject(i);
                track = new Track();
                // poi 객체에 대한 데이터 추가
                JSONObject start_poiJson = jsonObject.getJSONObject("start_poi");
                track.startPOI = savePOIUsingJsonObject(start_poiJson);
                JSONObject dest_poiJSon = jsonObject.getJSONObject("dest_poi");
                track.destPOI = savePOIUsingJsonObject(dest_poiJSon);
                //track.stop_poi_list = jsonObject.getString(SQLiteHandler.KEY_JSON_STOP_POI_ID_ARRAY);
                track.created_at = jsonObject.getString(SQLiteHandler.KEY_CREATED_AT);
                track.updated_at = jsonObject.getString(SQLiteHandler.KEY_UPDATED_AT);
                track.last_used_at = jsonObject.getString(SQLiteHandler.KEY_LAST_USED_AT);
                track.bookmarked = jsonObject.getBoolean("bookmarked");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // poi 객체를 리스트에 삽입
            if (track != null) {
                trackList.add(track);
            }
        }

        // Notifying the adapter that data has been added or changed
        adapter.notifyDataSetChanged();
        Log.d(TAG, "notifyDataSetChanged");
    }

    private POI savePOIUsingJsonObject(JSONObject jsonObject) throws JSONException {
        POI poi = new POI();
        poi.name = jsonObject.getString(SQLiteHandler.KEY_POI_NAME);
        poi.address = jsonObject.getString(SQLiteHandler.KEY_POI_ADDRESS);
        poi.latLng = jsonObject.getString(SQLiteHandler.KEY_POI_LAT_LNG);
        poi.created_at = jsonObject.getString(SQLiteHandler.KEY_CREATED_AT);
        poi.updated_at = jsonObject.getString(SQLiteHandler.KEY_UPDATED_AT);
        poi.last_used_at = jsonObject.getString(SQLiteHandler.KEY_LAST_USED_AT);
        return poi;
    }

    @Override
    public void trackClickToDelete(Track track) {
        Gson gson = new Gson();
        Log.d(TAG, "delete track : " + gson.toJson(track));
        if (session.isSessionLoggedIn()) {
            deleteBOOKMARKUSERTRACK(track);
        } else {
            db.deleteBookmarkedTrackRow(track);
        }
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

    @Override
    public void onRefresh() {
        requestCount = 1;
        progressBar.setVisibility(View.VISIBLE);
        if (session.isSessionLoggedIn()) {
            try {
                trackList.clear();
                getData();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            mSwipeRefresh.setRefreshing(false);
        }
    }

    // 부모 프래그먼트는 항상 이 인터페이스를 구현 해야한다
    public interface OnTrackSelectedListener {
        void onBookmarkedSelected(Track track);
    }
}
