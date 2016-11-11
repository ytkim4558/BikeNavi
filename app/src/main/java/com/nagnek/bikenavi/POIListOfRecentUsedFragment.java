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

public class POIListOfRecentUsedFragment extends Fragment implements POIListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = POIListOfRecentUsedFragment.class.getSimpleName();
    OnPoiSelectedListener mCallback;
    SQLiteHandler db;
    POIListOfRecentUsedAdapter adapter;
    RecyclerView rv;
    SQLiteHandler.UserType loginUserType;
    HashMap<String, String> user;
    ProgressBar progressBar;
    private List<POI> poiList;
    private SessionManager session; // 로그인했는지 확인용 변수
    private SwipeRefreshLayout mSwipeRefresh;
    //The request counter to send ?page=1, ?page=2  requests
    private int requestCount = 1;

    public POIListOfRecentUsedFragment() {
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

        // Session manager
        session = new SessionManager(getContext().getApplicationContext());

        // poiList 초기화
        poiList = new ArrayList<>();

        mSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);

        //Initializing ProgressBar
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        if (session.isSessionLoggedIn()) {
            loginUserType = session.getUserType();
            user = db.getLoginedUserDetails(loginUserType);
        }
        mSwipeRefresh.setOnRefreshListener(this);
        mSwipeRefresh.setColorSchemeResources(R.color.blue, R.color.red, R.color.yellow, R.color.black);

        // 새로고침시 돌아가는 애니메이션의 색상을 지정합니다. int color를 넣어준 순서대로 효과가 적용 됩니다.

        if (session.isSessionLoggedIn()) {
            try {
                getData();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        rv = (RecyclerView) rootView.findViewById(R.id.recent_recyclerView);
        rv.setHasFixedSize(true);

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

        if (session.isSessionLoggedIn()) {
            adapter = new POIListOfRecentUsedAdapter(getActivity(), poiList, this);
        } else {
            adapter = new POIListOfRecentUsedAdapter(getActivity(), db.getAllLocalUserPOI(), this);
        }
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

    // web api 로부터 데이터 가져오는 함수
    private void getData() throws JSONException {
        // Tag used to cancel the request
        String tag_string_req = "req_range_recent_poi";

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(getDataFromServer(requestCount), tag_string_req);
        requestCount++;
    }

    /**
     * 유저가 검색한 특정 POI 정보 삭제
     *
     * @param poi
     */
    private void deleteUSERPOI(final POI poi) {
        // Tag used to cancel the request
        String tag_string_req = "req_delete_user_poi";

        progressBar.setVisibility(View.VISIBLE);

        // id 와 이름을 내 서버(회원가입쪽으로)로 HTTP POST를 이용해 보낸다
        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_POI_DELETE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "USER POI DELETE Response: " + response);
                progressBar.setVisibility(View.GONE);

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // TODO: adapter 초기화할까?
                        Log.d(TAG, "유저 장소 정보 삭제됨");
                    } else {
                        // Error in Delete. Get the error message
                        String errorMsg = jsonObject.getString("error_msg");
                        showAlertDialogMessage(errorMsg);
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
                    showAlertDialogMessage(error.getMessage());
                } else if (error instanceof ServerError) {
                    Log.e(TAG, "서버 에러래" + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    showAlertDialogMessage(error.getMessage());
                } else {
                    Log.e(TAG, error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    showAlertDialogMessage(error.getMessage());
                }
                progressBar.setVisibility(View.GONE);
            }
        }) {
            @Override
            protected HashMap<String, String> getParams() {
                // Posting parameters to login url
                HashMap<String, String> params = new HashMap<String, String>();
                params = inputUserInfoToInputParams(params);
                params.put("POI_NAME", poi.name);
                params.put("POI_ADDRESS", poi.address);
                params.put("POI_LAT_LNG", poi.latLng);
                params.put("recent", "true");

                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    // 리스트를 서버에서 받아오기
    //Request to get json from server we are passing an integer here
    //This integer will used to specify the page number for the request ?page = requestcount
    //This method would return a JsonArrayRequest that will be added to the request queue
    private StringRequest getDataFromServer(final int requestCount) throws JSONException {
        if (requestCount == 1) {
            // 첫번째 페이지일때는 recyclerview를 gone시켜서 progressbar를 위로 띄운다.
            rv.setVisibility(View.GONE);
        }
        // progressbar 보여주기
        progressBar.setVisibility(View.VISIBLE);

        //JsonArrayRequest of volley
        final StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_POILIST_LOAD,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Calling method parsePOIList to parse the json response
                        try {
                            Log.d(TAG, "response : " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            boolean error = jsonObject.getBoolean("error");

                            if (!error) {
                                JSONArray poiList = jsonObject.getJSONArray("recent");
                                parsePOIList(poiList);
                            } else {
                                // Error in login. Get the error message
                                String errorMsg = jsonObject.getString("error_msg");
                                showAlertDialogMessage(errorMsg);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //Hiding the progressbar
                        mSwipeRefresh.setRefreshing(false);
                        progressBar.setVisibility(View.GONE);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mSwipeRefresh.setRefreshing(false);
                        progressBar.setVisibility(View.GONE);
                        //If an error occurs that means end of the list has reached
                        showAlertDialogMessage(error.getMessage());
                    }
                }) {
            @Override
            protected HashMap<String, String> getParams() {
                HashMap<String, String> mRequestParams = new HashMap<String, String>();
                mRequestParams.put("page", String.valueOf(requestCount));
                mRequestParams.put("recent", "true");
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

    // json Data 파싱
    private void parsePOIList(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            //장소 객체 생성
            POI poi = null;
            JSONObject jsonObject = null;
            try {
                // json 가져옴
                jsonObject = array.getJSONObject(i);
                poi = new POI();
                // poi 객체에 대한 데이터 추가
                poi.name = jsonObject.getString(SQLiteHandler.KEY_POI_NAME);
                poi.address = jsonObject.getString(SQLiteHandler.KEY_POI_ADDRESS);
                poi.latLng = jsonObject.getString(SQLiteHandler.KEY_POI_LAT_LNG);
                poi.created_at = jsonObject.getString(SQLiteHandler.KEY_CREATED_AT);
                poi.updated_at = jsonObject.getString(SQLiteHandler.KEY_UPDATED_AT);
                poi.last_used_at = jsonObject.getString(SQLiteHandler.KEY_LAST_USED_AT);
                poi.bookmarked = jsonObject.getBoolean("bookmarked");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // poi 객체를 리스트에 삽입
            if (poi != null) {
                poiList.add(poi);
            }
        }

        // Notifying the adapter that data has been added or changed
        adapter.notifyDataSetChanged();
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

    @Override
    public void latLngToDelete(POI poi) {
        if (session.isSessionLoggedIn()) {
            deleteUSERPOI(poi);
        } else {
            db.deleteUserPOIRow(poi);
        }
        Log.d(TAG, "latLng : " + poi.latLng);
    }

    @Override
    public void poiClickToSet(POI poi) {
        mCallback.onRecentPOISelected(poi);
    }

    @Override
    public void onRefresh() {
        requestCount = 1;
        mSwipeRefresh.setRefreshing(true);
        if (session.isSessionLoggedIn()) {
            try {
                poiList.clear();
                getData();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            mSwipeRefresh.setRefreshing(false);
            adapter.refresh();
        }
    }

    // 액티비티는 항상 이 인터페이스를 구현 해야한다
    public interface OnPoiSelectedListener {
        void onRecentPOISelected(POI poi);
    }
}
