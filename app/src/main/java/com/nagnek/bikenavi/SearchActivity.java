/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ProgressBar;
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
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.app.AppController;
import com.nagnek.bikenavi.customview.DelayAutoCompleteTextView;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import static com.nagnek.bikenavi.util.NagneUtil.getStringFromResources;

public class SearchActivity extends AppCompatActivity implements POIListOfRecentUsedFragment.OnPoiSelectedListener, POIListOfBookmarkedFragment.OnPoiSelectedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = SearchActivity.class.getSimpleName();
    // Check Permissions Now
    private static final int REQUEST_LOCATION = 2;
    DelayAutoCompleteTextView searchPoint = null;
    TextInputLayout textInputLayout = null;
    String search_purpose;
    GoogleApiClient mGoogleApiClient = null;
    Location myLocation = null;
    SQLiteHandler.UserType loginUserType;
    HashMap<String, String> user;
    private SessionManager session; // 로그인했는지 확인용 변수
    private SQLiteHandler db;   // sqlite
    private boolean registerAndRedirectStart;

    @Override
    public void onBookmarkedPOISelected(POI poi) {
        addOrupdateBookmarkPOIToDBOrServer(poi);
    }

    // 주의 : 북마크된 리스트를 눌러도 북마크 테이블의 사용시각은 갱신이 안됨. 최근 유저 사용시각만 갱신됨. 북마크의 경우 생성 시각 기준으로 정렬하기 때문에 고려 안함.
    @Override
    public void onRecentPOISelected(POI poi) {
        addOrUpdateRecentPOIToDBOrServer(poi);
    }

    // 북마크된 POI 를 db나 서버에 추가하거나 업데이트한다.
    void addOrupdateBookmarkPOIToDBOrServer(POI poi) {
        if (!session.isSessionLoggedIn()) {
            if (db != null) {
                if (db.checkIfPOIExists(poi.latLng)) {
                    db.updateLastUsedAtPOI(poi.latLng);
                    if (db.checkIfUserPOIExists(poi)) {
                        db.updateLastUsedAtPOI(poi.latLng);
                        db.updateLastUsedAtUserPOI(poi);
                        if (db.checkIFBookmarkedPOIExists(poi)) {
                            db.updateLastUsedAtBookmarkedPOI(poi);
                        } else {
                            db.addBookmarkedPOI(poi);
                        }
                    } else {
                        db.addLocalUserPOI(poi);
                    }
                } else {
                    db.addPOI(poi);
                    db.addLocalUserPOI(poi);
                }
                redirectCalledActvity(poi);
            }
        } else {
            addOrUpdatePOIToServer(poi);
            updateBookmarkPOIToServer(poi);
        }
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
                redirectCalledActvity(poi);
            }
        } else {
            addOrUpdatePOIToServer(poi);
        }
    }

    // 호출한 액티비티로 돌려주기
    void redirectCalledActvity(POI poi) {
        searchPoint.setText(poi.name);
        Intent intent = new Intent();
        intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.current_point_text_for_transition), searchPoint.getText().toString());
        String[] splitStr = poi.latLng.split(",");
        Double wgs84_x = Double.parseDouble(splitStr[0]);
        Double wgs84_y = Double.parseDouble(splitStr[1]);
        intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.wgs_84_x), wgs84_x);
        intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.wgs_84_y), wgs84_y);
        intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.name_purpose_search_point), search_purpose);
        intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.current_point_poi_for_transition), poi);
        setResult(RESULT_OK, intent);
        //registerPOI(poiName, wgs84_x, wgs84_y);

        textInputLayout.setHint(null);

        // 메인 엑티비티로 돌아가기
        finishAfterTransition();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        AppController.setCurrentActivity(this);

        // Create an instance of GoogleApiClient
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Get the ViewPager and set it's POIPagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        POIPagerAdapter POIPagerAdapter = new POIPagerAdapter(getSupportFragmentManager(), SearchActivity.this);
        viewPager.setAdapter(POIPagerAdapter);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        // Iterate over all tabs and set the custom view
        for (int i = 0; i < tabLayout.getTabCount(); ++i) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setCustomView(POIPagerAdapter.getTabView(i));
            }
        }
        searchPoint = (DelayAutoCompleteTextView) findViewById(R.id.search_point);
        textInputLayout = (TextInputLayout) findViewById(R.id.ti_layout);
        ProgressBar progressBar1 = (ProgressBar) findViewById(R.id.pb_loading_indicator1);

        session = new SessionManager(getApplicationContext());
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(getApplicationContext());

        loginUserType = session.getUserType();
        user = db.getLoginedUserDetails(loginUserType);

        Button myLocationButton = (Button) findViewById(R.id.current_my_point);
        myLocationButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                chkGpsService();
                mGoogleApiClient.connect();
                if (myLocation == null) {
                    if (ActivityCompat.checkSelfPermission(SearchActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Check Permissions Now
                        ActivityCompat.requestPermissions(SearchActivity.this,
                                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_LOCATION);
                    } else {
                        // permission has been granted, continue as usual
                        myLocation =
                                LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    }
                }
                if (myLocation != null) {
                    try {
                        if (registerAndRedirectStart == false) {
                            registerAndRedirectStart = true;
                            TMapPOIItem tMapPOIItem = getTMapPOIItemUsingCurrentLocation(myLocation);
                            setPOIAndredirectCallingActivity(tMapPOIItem);
                        }
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    } catch (SAXException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            String locationText = intent.getStringExtra(getStringFromResources(this.getApplicationContext(), R.string.current_point_text_for_transition));
            searchPoint.setText(locationText);
            searchPoint.setSelection(searchPoint.length()); // 커서를 마지막 위치로 넣음
            search_purpose = intent.getStringExtra(getStringFromResources(this.getApplicationContext(), R.string.name_purpose_search_point));
            if (search_purpose.equals("출발")) {
                textInputLayout.setHint(getStringFromResources(this.getApplicationContext(), R.string.hint_start_point));
            } else if (search_purpose.equals("도착")) {
                textInputLayout.setHint(getStringFromResources(this.getApplicationContext(), R.string.hint_destination));
            } else {
                textInputLayout.setHint(getStringFromResources(this.getApplicationContext(), R.string.hint_search_point));
            }

            setupTmapPOIToGoogleMapAutoCompleteTextView(searchPoint, progressBar1, search_purpose);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    TMapPOIItem getTMapPOIItemUsingCurrentLocation(Location location) throws ParserConfigurationException, SAXException, IOException {
        AppConfig.initializeTMapTapi(SearchActivity.this);
        TMapData tmapData = new TMapData();
        String tmapaddress = tmapData.convertGpsToAddress(location.getLatitude(), location.getLongitude());
        ArrayList<TMapPOIItem> arTMapPOIITtem = tmapData.findAddressPOI(tmapaddress);
        if (arTMapPOIITtem.size() > 0) {
            TMapPOIItem tMapPOIItem = arTMapPOIITtem.get(0);
            return tMapPOIItem;
        }
        return null;
    }

    void setPOIAndredirectCallingActivity(TMapPOIItem tMapPOIItem) {
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
        addOrUpdateRecentPOIToDBOrServer(poi);
        redirectCalledActvity(poi);
    }

    // gps 요청에 대한 결과
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                myLocation =
                        LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            } else {
                // Permission was denied or request was cancelled
            }
        }
    }

    //GPS 설정 체크
    private boolean chkGpsService() {

        String gps = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        Log.d(gps, "aaaa");

        if (!(gps.matches(".*gps.*") && gps.matches(".*network.*"))) {

            // GPS OFF 일때 Dialog 표시
            AlertDialog.Builder gsDialog = new AlertDialog.Builder(this);
            gsDialog.setTitle("위치 서비스 설정");
            gsDialog.setMessage("무선 네트워크 사용, GPS 위성 사용을 모두 체크하셔야 정확한 위치 서비스가 가능합니다.\n위치 서비스 기능을 설정하시겠습니까?");
            gsDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // GPS설정 화면으로 이동
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivity(intent);
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



    // final ArrayList 는 new ArrayList() 형태로 새로 ArrayList를 만드는게 안될 뿐 add 나 remove는 가능하다.
    private void setupTmapPOIToGoogleMapAutoCompleteTextView(final DelayAutoCompleteTextView locationName, final ProgressBar progressBar, final String searchPurpose) {
        locationName.setThreshold(1);
        locationName.setAdapter(new TMapPOIAutoCompleteAdapter(this));
        locationName.setLoadingIndicator(progressBar);
        locationName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TMapPOIItem tMapPOIItem = (TMapPOIItem) parent.getItemAtPosition(position);
                locationName.setText(tMapPOIItem.getPOIName());

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

                if (session.isSessionLoggedIn()) {
                    /* 로그인 했는지 확인*/
                    addOrUpdatePOIToServer(poi);
                } else {
                    if (db.checkIfPOIExists(poi.latLng)) {
                        db.updateLastUsedAtPOI(poi.latLng);
                        if (db.checkIfUserPOIExists(poi)) {
                            db.updateLastUsedAtUserPOI(poi);
                        } else {
                            db.addLocalUserPOI(poi);
                        }
                    } else {
                        db.addPOI(poi);
                        db.addLocalUserPOI(poi);
                    }
                }

                Intent intent = new Intent();
                intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.current_point_text_for_transition), locationName.getText().toString());
                intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.wgs_84_x), wgs84_x);
                intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.wgs_84_y), wgs84_y);
                intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.name_purpose_search_point), searchPurpose);
                intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.current_point_poi_for_transition), poi);
                setResult(RESULT_OK, intent);
                //registerPOI(poiName, wgs84_x, wgs84_y);

                textInputLayout.setHint(null);

                // 메인 엑티비티로 돌아가기
                finishAfterTransition();
            }
        });
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
                        redirectCalledActvity(poi);
                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jsonObject.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError) {
                    Log.e(TAG, "POI 등록 또는 업데이트 에러 : 서버 응답시간이 초과되었습니다." + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            "POI 등록 또는 업데이트 에러 : 서버가 응답하지 않습니다." + error.getMessage(), Toast.LENGTH_LONG).show();
                } else if (error instanceof ServerError) {
                    Log.e(TAG, "서버 에러래" + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            "POI 등록 또는 업데이트 에러 : 서버 Error." + error.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
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

    // 유저정보와 POI 정보를 Server에 보내서 서버에 있는 mysql에 저장하기
    private void updateBookmarkPOIToServer(final POI poi) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_or_update_poi_to_table_bookmark_poi";

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
                        redirectCalledActvity(poi);
                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jsonObject.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError) {
                    Log.e(TAG, "POI 등록 또는 업데이트 에러 : 서버 응답시간이 초과되었습니다." + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            "POI 등록 또는 업데이트 에러 : 서버가 응답하지 않습니다." + error.getMessage(), Toast.LENGTH_LONG).show();
                } else if (error instanceof ServerError) {
                    Log.e(TAG, "서버 에러래" + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            "POI 등록 또는 업데이트 에러 : 서버 Error." + error.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            error.getMessage(), Toast.LENGTH_LONG).show();
                }
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
                params.put("bookmark", "true");

                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    @Override
    public void onBackPressed() {
        textInputLayout.setHint(null);
        super.onBackPressed();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Log.d(TAG, "onConnected");
        myLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (myLocation != null) {
            try {
                if (registerAndRedirectStart == false) {
                    registerAndRedirectStart = true;
                    TMapPOIItem tMapPOIItem = getTMapPOIItemUsingCurrentLocation(myLocation);
                    setPOIAndredirectCallingActivity(tMapPOIItem);
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
