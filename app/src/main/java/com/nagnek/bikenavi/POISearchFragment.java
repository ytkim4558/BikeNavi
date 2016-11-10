/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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

public class POISearchFragment extends Fragment implements OnMapReadyCallback {
    static final int SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT = 2; // 장소 검색 화면에서 장소 검색 request code
    static final LatLng SEOUL_STATION = new LatLng(37.555755, 126.970431);
    private static final String TAG = POISearchFragment.class.getSimpleName();
    DelayAutoCompleteTextView searchPoint = null;
    TextInputLayout textInputLayout = null;
    RelativeLayout detailPOILayout;
    TextView poiNameView, poiAddressView;
    ImageButton bookmarkImageButton;
    POI currentPOI;
    private GoogleMap mGoogleMap;
    private SQLiteHandler db;   // sqlite
    private SessionManager session; // 로그인했는지 확인용 변수
    private ProgressDialog pDialog; //진행 상황 확인용 다이얼로그

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(getContext().getApplicationContext());

        // Session manager
        session = new SessionManager(getContext().getApplicationContext());

        // Progress dialog
        pDialog = new ProgressDialog(getContext());
        pDialog.setCancelable(false);

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_poisearch, container, false);
        detailPOILayout = (RelativeLayout) rootView.findViewById(R.id.poi_detail_layout);

        poiNameView = (TextView) rootView.findViewById(R.id.text_poi_name);
        poiAddressView = (TextView) rootView.findViewById(R.id.text_poi_address);
        bookmarkImageButton = (ImageButton) rootView.findViewById(R.id.bookmark_button);
        bookmarkImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPOI != null) {
                    if (!session.isSessionLoggedIn()) {
                        // 로그인하지 않은 경우
                        if (!db.checkIFBookmarkedPOIExists(currentPOI)) {
                            db.addBookmarkedPOI(currentPOI);
                            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();   // 닫기
                                }
                            });
                            alert.setMessage("북마크 되었습니다.");
                            alert.show();
                            bookmarkImageButton.setSelected(true);
                        } else {
                            db.deleteBookmarkedPOIRow(currentPOI);
                            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();   // 닫기
                                }
                            });
                            alert.setMessage("북마크 해제 되었습니다.");
                            bookmarkImageButton.setSelected(false);
                            alert.show();
                        }
                    } else {
                        if (currentPOI.bookmarked) {
                            pDialog.setMessage("북마크 삭제중 ...");
                            showDialog();
                            deleteBookMarkUserPOIToServer(currentPOI);
                        } else {
                            pDialog.setMessage("북마크 추가중 ...");
                            showDialog();
                            addOrUpdateBookMarkUserPOIToServer(currentPOI);
                        }
                    }
                } else {
                    Log.d(TAG, "poi가 null입니다");
                }
            }
        });

        // Get the ViewPager and set it's recentPOIPagerAdapter so that it can display items
        long start = System.currentTimeMillis();
//        MapFragment mapFragment = (MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);
        ((SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map)).getMapAsync(this);
        long end = System.currentTimeMillis();
        Log.d(TAG, "구글맵 로딩 시간 : " + (end - start) / 1000.0);
        searchPoint = (DelayAutoCompleteTextView) rootView.findViewById(R.id.search_point);
        textInputLayout = (TextInputLayout) rootView.findViewById(R.id.ti_layout);

        searchPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Performing stop of activity that is not resumed: {com.nagnek.bikenavi/com.nagnek.bikenavi.MainActivity} 에러 방지
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getContext(), SearchActivity.class);
                        intent.putExtra(getResources().getString(R.string.name_purpose_search_point), "");
                        intent.putExtra(getResources().getString(R.string.current_point_text_for_transition), searchPoint.getText().toString());
                        // 화면전환 애니메이션을 생성한다. 트랜지션 이름은 양쪽 액티비티에 선언되어야한다.
                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity(),
                                searchPoint, searchPoint.getTransitionName());
                        getActivity().startActivityForResult(intent, SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT, options.toBundle());
                    }
                }, 300);
            }
        });

        return rootView;
    }

    private void deleteBookMarkUserPOIToServer(final POI poi) {
        // Tag used to cancel the request
        String tag_string_req = "req_delete_bookmark_poi";

        // poi정보 와 유저정보를 내 서버(회원가입쪽으로)로 HTTP POST를 이용해 보낸다
        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_POI_DELETE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "북마크 삭제의 Response: " + response);
                hideDialog();

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // 서버에 반영 성공했다. 딱히 뭐 할거 있나..?
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();   // 닫기
                            }
                        });
                        alert.setMessage("북마크 해제 되었습니다.");
                        currentPOI.bookmarked = false;
                        bookmarkImageButton.setSelected(false);
                        alert.show();

                        Log.d(TAG, "북마크 삭제가 성공했길 바랍니다 (__)");
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
                hideDialog();
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

    private void showDialog() {
        if (!pDialog.isShowing()) {
            pDialog.show();
        }
    }

    private void hideDialog() {
        if (pDialog.isShowing()) {
            pDialog.dismiss();
        }
    }

    private void addOrUpdateBookMarkUserPOIToServer(final POI poi) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_or_update_poi_to_table_bookmark_poi";

        // poi정보 와 유저정보를 내 서버(회원가입쪽으로)로 HTTP POST를 이용해 보낸다
        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_POI_REGISTER_OR_UPDATE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "북마크 추가 Response: " + response);
                hideDialog();

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("error");

                    // Check for error node in json
                    if (!error) {

                        // 서버에 반영 성공했다. 딱히 뭐 할거 있나..?
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();   // 닫기
                            }
                        });
                        alert.setMessage("북마크 되었습니다.");
                        alert.show();
                        bookmarkImageButton.setSelected(true);
                        currentPOI.bookmarked = true;
                        Log.d(TAG, "북마크 추가 성공했길 바랍니다 (__)");
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
                hideDialog();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "왓나?");
        Log.d(TAG, "requestCode = " + requestCode + " But, I want this requestCode: " + SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT);
        if (requestCode == SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT) { // 장소검색 요청한게 돌아온 경우
            Log.d(TAG, "SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT");
            if (resultCode == Activity.RESULT_OK) {// 장소 검색 결과 리턴
                currentPOI = (POI) data.getSerializableExtra(NagneUtil.getStringFromResources(getContext().getApplicationContext(), R.string.current_point_poi_for_transition));
                double wgs_84x = data.getDoubleExtra(NagneUtil.getStringFromResources(getActivity(), R.string.wgs_84_x), 0.0);
                double wgs_84y = data.getDoubleExtra(NagneUtil.getStringFromResources(getActivity(), R.string.wgs_84_y), 0.0);
                searchPoint.setText(currentPOI.name);
                moveCameraToPOIAndDisplay(wgs_84x, wgs_84y, currentPOI.name, currentPOI.address);
                View rootView = getView();

                // 장소 상세확인 내용창 표시
                if (rootView != null) {
                    detailPOILayout.setVisibility(View.VISIBLE);
                    poiNameView.setText(currentPOI.name);
                    poiAddressView.setText(currentPOI.address);

                    if (session.isSessionLoggedIn()) {
                        if (currentPOI.bookmarked) {
                            bookmarkImageButton.setSelected(true);
                        } else {
                            bookmarkImageButton.setSelected(false);
                        }
                    } else {
                        if (db.checkIFBookmarkedPOIExists(currentPOI)) {
                            bookmarkImageButton.setSelected(true);
                        }
                    }

                    Log.d(TAG, "상세창 높이 : " + detailPOILayout.getLayoutParams().height);
                }
            }
        }
    }

    void moveCameraToPOIAndDisplay(double wgs84_x, double wgs84_y, String poiName, String address) {
        Log.d("tag", "좌표위치 " + "Lat:" + wgs84_x + ", Long : " + wgs84_y);
        LatLng latLng = new LatLng(wgs84_x, wgs84_y);
        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(wgs84_x, wgs84_y));
        Log.d("tag", "좌표위치 가져옴" + "Lat:" + latLng.latitude + ", Long : " + latLng.longitude);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
        mGoogleMap.clear();

        // 카메라 좌표를 검색 지역으로 이동
        mGoogleMap.moveCamera(center);

        // animateCamera는 근거리에선 부드럽게 변경한다.
        if (mGoogleMap != null) {
            mGoogleMap.animateCamera(zoom);
            MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(wgs84_x, wgs84_y)).title(poiName).snippet(address);
            Marker marker = mGoogleMap.addMarker(markerOptions);
            marker.showInfoWindow();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

        // 카메라 좌표를 서울역 근처로 옮긴다.
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL_STATION) //위도 경도
        );

        // 구글지도에서의 zoom 레벨은 1~23 까지 가능하다
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
        googleMap.animateCamera(zoom); // moveCamera는 바로 변경하지만, animateCamera()는 근거리에서는 부드럽게 변경합니다.

        // marker 표시
        // marker의 위치, 타이틀, 짧은 설명 추가
        MarkerOptions marker = new MarkerOptions();
        marker.position(SEOUL_STATION).title("서울역").snippet("Seoul Station");
        googleMap.addMarker(marker).showInfoWindow(); // 마커 추가, 화면에 출력
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.setBuildingsEnabled(true);


        // 위치 권한을 매니페스트에서 설정했는지 확인.
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
        }

        // 내장 확대/축소 컨트롤을 제공
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        mGoogleMap = googleMap;
    }
}
