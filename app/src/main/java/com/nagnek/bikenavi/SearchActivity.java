/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.app.AppController;
import com.nagnek.bikenavi.customview.DelayAutoCompleteTextView;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.nagnek.bikenavi.util.NagneUtil.getStringFromResources;

public class SearchActivity extends AppCompatActivity implements POIRecentListFragment.OnPoiSelectedListener {
    private static final String TAG = SearchActivity.class.getSimpleName();
    DelayAutoCompleteTextView searchPoint = null;
    TextInputLayout textInputLayout = null;
    String search_purpose;
    private ProgressDialog progressDialog;
    private SessionManager session; // 로그인했는지 확인용 변수
    private SQLiteHandler db;   // sqlite

    @Override
    public void onRecentPOISelected(POI poi) {
        if (db != null) {
            db.updateLastUsedAtPOI(poi.latLng);
            searchPoint.setText(poi.name);
            Intent intent = new Intent();
            intent.putExtra(getStringFromResources(this.getApplicationContext(), R.string.current_point_text_for_transition), searchPoint.getText().toString());
            intent.putExtra(getStringFromResources(this.getApplicationContext(), R.string.select_poi_address_for_transition), poi.address);
            intent.putExtra(getStringFromResources(this.getApplicationContext(), R.string.select_poi_name_for_transition), poi.name);
            String[] splitStr = poi.latLng.split(",");
            Double wgs84_x = Double.parseDouble(splitStr[0]);
            Double wgs84_y = Double.parseDouble(splitStr[1]);
            intent.putExtra(getStringFromResources(this.getApplicationContext(), R.string.wgs_84_x), wgs84_x);
            intent.putExtra(getStringFromResources(this.getApplicationContext(), R.string.wgs_84_y), wgs84_y);
            intent.putExtra(getStringFromResources(this.getApplicationContext(), R.string.name_purpose_search_point), search_purpose);
            intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.current_point_poi_for_transition), poi);
            setResult(RESULT_OK, intent);
            //registerPOI(poiName, wgs84_x, wgs84_y);

            textInputLayout.setHint(null);

            // 메인 엑티비티로 돌아가기
            finishAfterTransition();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

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

        // ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);    // 백키로 캔슬 가능안하게끔 설정

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
                    } else {
                        db.addPOI(poi);
                    }
                }

                Intent intent = new Intent();
                intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.current_point_text_for_transition), locationName.getText().toString());
                intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.select_poi_address_for_transition), address);
                intent.putExtra(getStringFromResources(SearchActivity.this.getApplicationContext(), R.string.select_poi_name_for_transition), poiName);
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
        String tag_string_req = "req_add_or_update_poi_to_table_bookmark_track";

        // poi정보와 유저정보를 내 서버(회원가입쪽으로)로 HTTP POST를 이용해 보낸다
        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_POI_REGISTER_OR_UPDATE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "POI add or update Response: " + response);
                hideDialog();

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

    private void showDialog() {
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideDialog() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
