/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

public class POIListOfBookmarkedFragment extends Fragment implements POIListener {
    private static final String TAG = POIListOfBookmarkedFragment.class.getSimpleName();
    OnPoiSelectedListener mCallback;
    SQLiteHandler db;
    POIListOfBookmarkedAdapter adapter;
    RecyclerView rv;
    ProgressBar progressBar;
    private List<POI> poiList;
    private SessionManager session; // 로그인했는지 확인용 변수

    //The request counter to send ?page=1, ?page=2  requests
    private int requestCount = 1;

    public POIListOfBookmarkedFragment() {
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
        View rootView = inflater.inflate(R.layout.fragment_bookmarked, container, false);

        db = SQLiteHandler.getInstance(getActivity().getApplicationContext());

        // Session manager
        session = new SessionManager(getContext().getApplicationContext());

        // poiList 초기화
        poiList = new ArrayList<>();

        //Initializing ProgressBar
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        if (session.isSessionLoggedIn()) {
            try {
                getData();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        rv = (RecyclerView) rootView.findViewById(R.id.bookmarked_recyclerView);
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
            //Displaying Progressbar
            progressBar.setVisibility(View.VISIBLE);
            adapter = new POIListOfBookmarkedAdapter(getActivity(), poiList, this);
        } else {
            progressBar.setVisibility(View.GONE);
            adapter = new POIListOfBookmarkedAdapter(getActivity(), db.getAllLocalUserBookmarkPOI(), this);
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
     * 유저가 즐겨찾기한 특정 POI 정보 삭제
     *
     * @param poi
     */
    private void deleteBOOKMARKUSERPOI(final POI poi) {
        // Tag used to cancel the request
        String tag_string_req = "req_delete_bookmark_user_poi";

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
                    boolean delete = jsonObject.getBoolean("delete");

                    // Check for error node in json
                    if (delete) {
                        // TODO: adapter 초기화할까?
                    } else {
                        // Error in login. Get the error message
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
                params.put("POI_NAME", poi.name);
                params.put("POI_ADDRESS", poi.address);
                params.put("POI_LAT_LNG", poi.latLng);
                params.put("bookmark", "true");

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

        // Tag used to cancel the request
        String tag_string_req = "req_range_bookmark_poi";

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
                                JSONArray poiList = jsonObject.getJSONArray("bookmark");
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
                        progressBar.setVisibility(View.GONE);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
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

    private HashMap<String, String> inputUserInfoToInputParams(HashMap<String, String> params) {
        SQLiteHandler.UserType loginUserType = session.getUserType();
        HashMap<String, String> user = db.getLoginedUserDetails(loginUserType);

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
            deleteBOOKMARKUSERPOI(poi);
        } else {
            db.deleteBookmarkedPOIRow(poi);
        }
        Log.d(TAG, "latLng : " + poi.latLng);
    }

    @Override
    public void poiClickToSet(POI poi) {
        mCallback.onBookmarkedPOISelected(poi);
    }

    // 액티비티는 항상 이 인터페이스를 구현 해야한다
    public interface OnPoiSelectedListener {
        void onBookmarkedPOISelected(POI poi);
    }
}