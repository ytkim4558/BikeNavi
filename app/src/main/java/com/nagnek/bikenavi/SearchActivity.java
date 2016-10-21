/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
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

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = SearchActivity.class.getSimpleName();
    DelayAutoCompleteTextView searchPoint = null;
    TextInputLayout textInputLayout = null;
    private ProgressDialog progressDialog;
    private SessionManager session; // 로그인했는지 확인용 변수
    private SQLiteHandler db;   // sqlite
    enum UserType{
        GOOGLE,
        KAKAO,
        FACEBOOK,
        BIKENAVI,
        NONLOGIN
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        PagerAdatper pagerAdapter = new PagerAdatper(getSupportFragmentManager(), SearchActivity.this);
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        // Iterate over all tabs and set the custom view
        for (int i = 0; i < tabLayout.getTabCount(); ++i) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            tab.setCustomView(pagerAdapter.getTabView(i));
        }
        searchPoint = (DelayAutoCompleteTextView) findViewById(R.id.search_point);
        textInputLayout = (TextInputLayout) findViewById(R.id.ti_layout);
        ProgressBar progressBar1 = (ProgressBar) findViewById(R.id.pb_loading_indicator1);

        session = new SessionManager(getApplicationContext());
        // SqLite database handler 초기화
        db = new SQLiteHandler(getApplicationContext());

        // ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);    // 백키로 캔슬 가능안하게끔 설정

        Intent intent = getIntent();
        if(intent != null) {
            String locationText = intent.getStringExtra(getStringFromResources(R.string.current_point_text_for_transition));
            searchPoint.setText(locationText);
            searchPoint.setSelection(searchPoint.length()); // 커서를 마지막 위치로 넣음
            String search_purpose = intent.getStringExtra(getStringFromResources(R.string.name_purpose_search_point));
            if(search_purpose.equals("출발")) {
                textInputLayout.setHint(getStringFromResources(R.string.hint_start_point));
            } else if(search_purpose.equals("도착")) {
                textInputLayout.setHint(getStringFromResources(R.string.hint_destination));
            }

            /**
             * 유저 로그인 타입(비로그인 포함)에 따라 다른 함수 호출
             */
            String primaryKey = null;   // 각 로그인 타입별 유저를 구별할 수 있는 필드. (임의로 primary Key로 잡음. 실제로 mysql 상에서의 primarykey는 아님.)
            if (session.isLoggedIn()) {
                Log.d(TAG, "자체회원로긴");

                // Fetching user details from sqlite
                HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.BIKENAVI);

                primaryKey = user.get(SQLiteHandler.KEY_EMAIL);
            } else if (session.isGoogleLoggedIn()) {
                // Fetching user details from sqlite
                Log.d(TAG, "구글 자동로긴");
                HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.GOOGLE);

                primaryKey = user.get(SQLiteHandler.KEY_GOOGLE_EMAIL);
            } else if (session.isFacebookIn()) {
                // Fetching user details from sqlite
                Log.d(TAG, "페북 자동로긴");
                HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.FACEBOOK);

                primaryKey = user.get(SQLiteHandler.KEY_FACEBOOK_ID);
            } else if (session.isKakaoLoggedIn()) {
                Log.d(TAG, "카카오로긴");
                // Fetching user details from sqlite
                HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.KAKAO);

                primaryKey = user.get(SQLiteHandler.KEY_KAKAO_ID);
            } else {
                Log.d(TAG, "비로그인 상태");
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
                LatLng latLng = new LatLng(wgs84_x, wgs84_y);
                CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(wgs84_x, wgs84_y));
                Log.d("tag", "좌표위치 가져옴" + "Lat:" + latLng.latitude + ", Long : " + latLng.longitude);
                CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);

                String poiName = tMapPOIItem.getPOIName();
                String address = tMapPOIItem.getPOIAddress().replace("null", "");

                Intent intent = new Intent();
                intent.putExtra(getStringFromResources(R.string.current_point_text_for_transition), locationName.getText().toString());
                intent.putExtra(getStringFromResources(R.string.select_poi_address_for_transition), address);
                intent.putExtra(getStringFromResources(R.string.select_poi_name_for_transition), poiName);
                intent.putExtra(getStringFromResources(R.string.wgs_84_x), wgs84_x);
                intent.putExtra(getStringFromResources(R.string.wgs_84_y), wgs84_y);
                intent.putExtra(getStringFromResources(R.string.name_purpose_search_point), searchPurpose);
                setResult(RESULT_OK, intent);
                registerPOI(poiName, wgs84_x, wgs84_y);
                textInputLayout.setHint(null);
            }
        });
    }

    private void registerPOI(final String poiName, final double wgs84_x, final double wgs84_y) {    // 장소이름, 위도, 경도
        // Tag used to cancel the request
        String tag_string_req = "req_poi_register";

        progressDialog.setMessage("등록중 ...");
        showDialog();

        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                AppConfig.URL_REGISTER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("error");
                    if (!error) {
                        // User successfully stored in MySQL
                        // Now store the user in sqlite

                        Toast.makeText(getApplicationContext(), "성공적으로 장소가 등록되었습니다.!", Toast.LENGTH_LONG).show();

                        // 메인 엑티비티로 돌아가기
                        finishAfterTransition();
                    } else {
                        // Error occured in registration. Get the error message
                        String errorMsg = jsonObject.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("POI_LAT_LNG", wgs84_x + "," + wgs84_y);
                params.put("POI_NAME", poiName);

                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(stringRequest, tag_string_req);
    }

    @Override
    public void onBackPressed() {
        textInputLayout.setHint(null);
        super.onBackPressed();
    }

    private String getStringFromResources(final int id) {
        return getResources().getString(id);
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

    class PagerAdatper extends FragmentPagerAdapter {
        String tabTitles[] = new String[] { "최근 검색", "즐겨 찾기"};
        Context context;

        public PagerAdatper(FragmentManager fm, Context context) {
            super(fm);
            this.context = context;
        }

        @Override
        public int getCount() {
            return tabTitles.length;
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return new RecentFragment();
                case 1:
                    return new RecentFragment();
            }

            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // item 위치에 따른 title 생성
            return tabTitles[position];
        }

        public View getTabView(int position) {
            View tab = LayoutInflater.from(SearchActivity.this).inflate(R.layout.custom_tab, null);
            TextView tv = (TextView) tab.findViewById(R.id.custom_text);
            tv.setText(tabTitles[position]);
            return tab ;
        }
    }
}
