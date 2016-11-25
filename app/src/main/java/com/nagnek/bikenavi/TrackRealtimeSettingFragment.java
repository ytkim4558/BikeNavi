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
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
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
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by user on 2016-11-16.
 */

public class TrackRealTimeSettingFragment extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    static final int SEARCH_INTEREST_POINT_FROM_REALTIME_TRACK_FRAGMENT = 3; // 리얼타임 경로 화면에서 장소 검색 request code
    private static final String TAG = POISearchFragment.class.getSimpleName();
    DelayAutoCompleteTextView destPoint = null;
    TextInputLayout textInputLayout = null;
    TextView poiNameView, poiAddressView;
    POI currentPOI;
    GoogleApiClient mGoogleApiClient = null;
    Location mLastLocation;
    SQLiteHandler.UserType loginUserType;
    HashMap<String, String> user;
    private GoogleMap mGoogleMap;
    private SQLiteHandler db;   // sqlite
    private SessionManager session; // 로그인했는지 확인용 변수
    private ProgressDialog pDialog; //진행 상황 확인용 다이얼로그

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create an instance of GoogleApiClient
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(getContext().getApplicationContext());

        // Session manager
        session = new SessionManager(getContext().getApplicationContext());

        if (session.isSessionLoggedIn()) {
            loginUserType = session.getUserType();
            user = db.getLoginedUserDetails(loginUserType);
            getLastUsedPOI();
        } else {
            currentPOI = db.getLastUserPOI();
        }
        Log.d(TAG, "onCreate");
        chkGpsService();
    }

    //GPS 설정 체크
    private boolean chkGpsService() {

        String gps = android.provider.Settings.Secure.getString(getContext().getContentResolver(), android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        Log.d(gps, "aaaa");

        if (!(gps.matches(".*gps.*") && gps.matches(".*network.*"))) {

            // GPS OFF 일때 Dialog 표시
            AlertDialog.Builder gsDialog = new AlertDialog.Builder(getContext());
            gsDialog.setTitle("위치 서비스 설정");
            gsDialog.setMessage("무선 네트워크 사용, GPS 위성 사용을 모두 체크하셔야 정확한 위치 서비스가 가능합니다.\n위치 서비스 기능을 설정하시겠습니까?");
            gsDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // GPS설정 화면으로 이동
                    if (isAdded()) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        startActivity(intent);
                    }
                }
            })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    }).create().show();
            return false;

        } else {
            return true;
        }
    }

    /**
     * Function to store user in MySQL database will post params(tag, email, password) to register url
     */
    private void getLastUsedPOI() {
        // Tag used to cancel the request
        String tag_string_req = "req_last_user_poi";

        //StringRequest of volley
        final StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_GET_LAST_UESD_POI,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Calling method parsePOIList to parse the json response
                        try {
                            Log.d(TAG, "response : " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            boolean error = jsonObject.getBoolean("error");

                            if (!error) {
                                // poi 객체에 대한 데이터 추가
                                if (currentPOI == null) {
                                    currentPOI = new POI();
                                }
                                currentPOI.name = jsonObject.getString(SQLiteHandler.KEY_POI_NAME);
                                currentPOI.address = jsonObject.getString(SQLiteHandler.KEY_POI_ADDRESS);
                                currentPOI.latLng = jsonObject.getString(SQLiteHandler.KEY_POI_LAT_LNG);
                                currentPOI.created_at = jsonObject.getString(SQLiteHandler.KEY_CREATED_AT);
                                currentPOI.updated_at = jsonObject.getString(SQLiteHandler.KEY_UPDATED_AT);
                                currentPOI.last_used_at = jsonObject.getString(SQLiteHandler.KEY_LAST_USED_AT);
                                currentPOI.bookmarked = jsonObject.getBoolean("bookmarked");
                                String[] splitStr = currentPOI.latLng.split(",");
                                Double wgs84_x = Double.parseDouble(splitStr[0]);
                                Double wgs84_y = Double.parseDouble(splitStr[1]);
                                if (mGoogleMap != null) {
                                    moveCameraToPOI(wgs84_x, wgs84_y);
                                }
                            } else {
                                // Error in login. Get the error message
                                String errorMsg = jsonObject.getString("error_msg");
                                showAlertDialogMessage(errorMsg);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //Hiding the progressbar
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //If an error occurs that means end of the list has reached
                        showAlertDialogMessage(error.getMessage());
                    }
                }) {
            @Override
            protected HashMap<String, String> getParams() {
                HashMap<String, String> mRequestParams = new HashMap<String, String>();
                mRequestParams.put("bookmark", "true");
                mRequestParams = inputUserInfoToInputParams(mRequestParams);

                return mRequestParams;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
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

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        // Progress dialog
        pDialog = new ProgressDialog(getContext());
        pDialog.setCancelable(false);

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_trackrealtime, container, false);

        poiNameView = (TextView) rootView.findViewById(R.id.text_poi_name);
        poiAddressView = (TextView) rootView.findViewById(R.id.text_poi_address);

        // Get the ViewPager and set it's recentPOIPagerAdapter so that it can display items
        long start = System.currentTimeMillis();
        ((SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map)).getMapAsync(this);
        long end = System.currentTimeMillis();
        Log.d(TAG, "구글맵 로딩 시간 : " + (end - start) / 1000.0);
        destPoint = (DelayAutoCompleteTextView) rootView.findViewById(R.id.search_point);
        textInputLayout = (TextInputLayout) rootView.findViewById(R.id.ti_layout);

        destPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Performing stop of activity that is not resumed: {com.nagnek.bikenavi/com.nagnek.bikenavi.MainActivity} 에러 방지
                redirectSearchPOIActivity();
            }
        });

        return rootView;
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "왓나?");
        if (requestCode == SEARCH_INTEREST_POINT_FROM_REALTIME_TRACK_FRAGMENT) { // 장소검색 요청한게 돌아온 경우
            Log.d(TAG, "SEARCH_INTEREST_POINT_FROM_REALTIME_TRACK_FRAGMENT");
            if (resultCode == Activity.RESULT_OK) {// 장소 검색 결과 리턴
                currentPOI = (POI) data.getSerializableExtra(NagneUtil.getStringFromResources(getContext().getApplicationContext(), R.string.current_point_poi_for_transition));
                double wgs_84x = data.getDoubleExtra(NagneUtil.getStringFromResources(getActivity(), R.string.wgs_84_x), 0.0);
                double wgs_84y = data.getDoubleExtra(NagneUtil.getStringFromResources(getActivity(), R.string.wgs_84_y), 0.0);
                destPoint.setText(currentPOI.name);
                animateCameraToPOIAndDisplay(wgs_84x, wgs_84y, currentPOI.name, currentPOI.address);
            }
        }
    }

    void animateCameraToPOIAndDisplay(double wgs84_x, double wgs84_y, String poiName, String address) {
        LatLng latLng = new LatLng(wgs84_x, wgs84_y);
        CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        Log.d("tag", "좌표위치 가져옴" + "Lat:" + latLng.latitude + ", Long : " + latLng.longitude);
        mGoogleMap.clear();

        // 카메라 좌표를 검색 지역으로 이동
        mGoogleMap.animateCamera(center);

        // animateCamera는 근거리에선 부드럽게 변경한다.
        if (mGoogleMap != null) {
            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(poiName).snippet(address);
            Marker marker = mGoogleMap.addMarker(markerOptions);
            marker.showInfoWindow();
            if (mLastLocation != null) {
               redirectTrackRealTImeActivity();
            } else {
                chkGpsService();
            }
        }
    }

    // 도착지 장소를 설정하기 위해 장소 검색 액티비티로 이동한다.
    void redirectSearchPOIActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getContext(), SearchActivity.class);
                intent.putExtra(getResources().getString(R.string.name_purpose_search_point), "");
                intent.putExtra(getResources().getString(R.string.current_point_text_for_transition), destPoint.getText().toString());
                // 화면전환 애니메이션을 생성한다. 트랜지션 이름은 양쪽 액티비티에 선언되어야한다.
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity(),
                        destPoint, destPoint.getTransitionName());
                getActivity().startActivityForResult(intent, SEARCH_INTEREST_POINT_FROM_REALTIME_TRACK_FRAGMENT, options.toBundle());
            }
        }, 300);
    }

    void redirectTrackRealTImeActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mLastLocation != null) {
                    TMapPOIItem tMapPOIItem;
                    try {
                        tMapPOIItem = getTMapPOIItemUsingCurrentLocation(mLastLocation);
                        Intent intent = new Intent(getContext(), TrackRealTImeActivity.class);
                        POI startPOI = getPOI(tMapPOIItem, false);
                        intent.putExtra(getResources().getString(R.string.start_point_text_for_transition), startPOI.name);
                        intent.putExtra(getResources().getString(R.string.dest_point_text_for_transition), destPoint.getText().toString());
                        // 화면전환 애니메이션을 생성한다. 트랜지션 이름은 양쪽 액티비티에 선언되어야한다.
                        getActivity().startActivityForResult(intent, SEARCH_INTEREST_POINT_FROM_REALTIME_TRACK_FRAGMENT);
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    } catch (SAXException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "mLastLocation이 null이래");
                }
            }
        }, 300);
    }

    POI getPOI(TMapPOIItem tMapPOIItem, boolean updateToServer) {
        TMapPoint tMapPoint = tMapPOIItem.getPOIPoint();

        // 위도를 반환
        double wgs84_x = tMapPoint.getLatitude();

        // 경도를 반환
        double wgs84_y = tMapPoint.getLongitude();

        Log.d("tag", "좌표위치 " + "Lat:" + wgs84_x + ", Long : " + wgs84_y);

        String poiName = tMapPOIItem.getPOIName();
        String address = tMapPOIItem.getPOIAddress().replace("null", "");

        POI poi = new POI();
        poi.name = poiName;
        poi.address = address;
        poi.latLng = "" + wgs84_x + "," + wgs84_y;
        if (updateToServer) {
            addOrUpdateRecentPOIToDBOrServer(poi);
        }
       return poi;
    }

    void addOrUpdateRecentPOIToDBOrServer(POI poi) {
        if (!session.isSessionLoggedIn()) {
            if (db != null) {
                if (db.checkIfPOIExists(poi.latLng)) {
                    db.updateLastUsedAtPOI(poi.latLng);
                    if (db.checkIfUserPOIExists(poi)) {
                        db.updateLastUsedAtPOI(poi.latLng);
                        db.updateLastUsedAtUserPOI(poi);
                    } else {
                        db.addLocalUserPOI(poi);
                    }
                } else {
                    db.addPOI(poi);
                    db.addLocalUserPOI(poi);
                }
            }
        } else {
            addOrUpdatePOIToServer(poi);
        }
    }

    // 유저정보와 POI 정보를 Server에 보내서 서버에 있는 mysql에 저장하기
    private void addOrUpdatePOIToServer(final POI poi) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_or_update_poi_to_table_recent_poi";

        // poi정보와 유저정보를 내 서버(회원가입쪽으로)로 HTTP POST를 이용해 보낸다
        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_POI_REGISTER_OR_UPDATE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "POI add or update Response: " + response);

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // Now store or update the poi in SQLite
                        JSONObject poiObject = jsonObject.getJSONObject("poi");
                        Log.d(TAG, "poi : " + poiObject.toString());
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
                    Log.e(TAG, "POI 등록 또는 업데이트 에러 : 서버 응답시간이 초과되었습니다." + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getContext().getApplicationContext(),
                            "POI 등록 또는 업데이트 에러 : 서버가 응답하지 않습니다." + error.getMessage(), Toast.LENGTH_LONG).show();
                } else if (error instanceof ServerError) {
                    Log.e(TAG, "서버 에러래" + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getContext().getApplicationContext(),
                            "POI 등록 또는 업데이트 에러 : 서버 Error." + error.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getContext().getApplicationContext(),
                            error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                // 로그인 한 경우

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
                // poi를 final로 선언한 이유가 이곳에서 error 떠서 인데 나중에
                // poi정보를 서버측에서 바꾼다면.. 뭐 상관없나 값이 바뀌는거야.. 객체가 삭제되는게 아니니까 =_=a;
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

    TMapPOIItem getTMapPOIItemUsingCurrentLocation(Location location) throws ParserConfigurationException, SAXException, IOException {
        AppConfig.initializeTMapTapi(getContext());
        TMapData tmapData = new TMapData();
        String tmapaddress = tmapData.convertGpsToAddress(location.getLatitude(), location.getLongitude());
        ArrayList<TMapPOIItem> arTMapPOIITtem = tmapData.findAddressPOI(tmapaddress);
        if (arTMapPOIITtem.size() > 0) {
            return arTMapPOIITtem.get(0);
        }
        return null;
    }

    void moveCameraToPOI(double wgs84_x, double wgs84_y) {
        LatLng latLng = new LatLng(wgs84_x, wgs84_y);
        CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        Log.d("tag", "좌표위치 가져옴" + "Lat:" + latLng.latitude + ", Long : " + latLng.longitude);
        mGoogleMap.clear();

        // 카메라 좌표를 검색 지역으로 이동
        mGoogleMap.moveCamera(center);

        if(!destPoint.getText().toString().equals("")) {
            // 도착지를 설정했으면 경로 검색 화면으로 넘어간다.
            redirectTrackRealTImeActivity();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.setBuildingsEnabled(true);

        // 위치 권한을 매니페스트에서 설정했는지 확인.
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
            chkGpsService();
        }

        // 내장 확대/축소 컨트롤을 제공
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        mGoogleMap = googleMap;

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            moveCameraToPOI(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            Log.d(TAG, "마지막 위치 이동");
        }

        if (currentPOI != null && currentPOI.latLng != null) {
            String[] splitStr = currentPOI.latLng.split(",");
            Double wgs84_x = Double.parseDouble(splitStr[0]);
            Double wgs84_y = Double.parseDouble(splitStr[1]);
            moveCameraToPOI(wgs84_x, wgs84_y);
            Log.d(TAG, currentPOI.name);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
