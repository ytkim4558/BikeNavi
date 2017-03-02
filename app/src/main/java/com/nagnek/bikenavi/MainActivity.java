/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.nagnek.bikenavi.activity.LoginActivity;
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.app.AppController;
import com.nagnek.bikenavi.customview.ClearableSqliteAutoCompleteTextView;
import com.nagnek.bikenavi.helper.IPManager;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MainActivity extends AppCompatActivity implements MyAdapter.ClickListener {

    // 첫번째로 네비게이션 드로어 리스트뷰에 타이틀과 아이콘을 선언한다.
    // 이 아이콘과 타이틀들은 배열에 담긴다

    static final int SEARCH_INTEREST_POINT_TRACK_SETTING_FRAGMENT = 1; // 경로 화면에서 장소 검색 request code
    static final int SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT = 2; // 장소 검색 화면에서 장소 검색 request code
    static final int SEARCH_INTEREST_POINT_FROM_REALTIME_TRACK_FRAGMENT = 3; // 리얼타임 경로 화면에서 장소 검색 request code
    private static final String TAG = MainActivity.class.getSimpleName();
    final String TITLES[] = {"길찾기", "장소찾기", "실시간 길찾기", "회원가입 / 로그인"};
    final int ICONS[] = {R.drawable.ic_directions_black_24dp, R.drawable.places_ic_search, R.drawable.real_time_track};
    // 비슷하게 헤더뷰에 이름과 이메일을 위한 String 리소스를 생성한다.
    // 그리고나서 proifle picture 리소스를 헤더뷰에 생성한다.
    final int PROFILE = R.drawable.ic_account_circle_white_24dp;
    RecyclerView mDrawerRecyclerView;
    MyAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    ActionBarDrawerToggle mDrawerToggle;
    String[] serverIPs = new String[]{};
    /**
     * AutoCompleteTextView 대신 ClearableSQliteAutoCompleteTextView로 변경했다.
     * 왜냐하면 view를 커스터마이징하기 위해 확장시키고, 필터를 비활성화 시키기 위해서다.
     * 또한 x를 눌러 지울 수도 있게 하기 위해서다.
     * xml view에도 똑같이 했고 이를 ClearableSqliteAutoCompleteTextView라고 했다.
     */
    ClearableSqliteAutoCompleteTextView serverIpAutoComplete;
    AlertDialog alertDialog;
    // 자동완성을 위한 어댑터
    ArrayAdapter<String> arrayAdapter;
    ActionBarDrawerToggle actionBarDrawerToggle;
    DrawerLayout drawerLayout;
    private SessionManager session; // 로그인했는지 확인용 변수
    private SQLiteHandler db;   // sqlite

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = AppCompatDrawableManager.get().getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static String docToString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    @Override
    public void onNavItemClicked(int position) {
        switch (position) {
            // 길찾기 아이콘 클릭했을 때
            case 1: {
                Fragment fragment = new TrackSettingFragment();
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();
            }
                break;
            // 장소찾기 아이콘 클릭했을 때
            case 2: {
                Fragment fragment = new POISearchFragment();
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();
            }
                break;
            // 실시간 경로찾기 아이콘 클릭했을 때
            case 3: {
                Fragment fragment = new TrackRealTimeSettingFragment();
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();
            }
            break;

            // 로그인을 클릭햇을 때
            case 4: {
                if (session.isLoggedIn() || session.isGoogleLoggedIn() || session.isFacebookIn() || session.isKakaoLoggedIn()) {
                    logoutUser();
                    mAdapter.changeLoginState(false);
                } else {
                    redirectLoginActivity();
                }
            }
        }
        // HIghlight the selected item, update the title, and close the drawer
        drawerLayout.closeDrawer(mDrawerRecyclerView);
    }

    private void redirectLoginActivity() { // Launching the login activity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     */
    private void logoutUser() {
        db.deleteUsers();
        if (session.isLoggedIn()) {
            session.setLogin(false);
        } else if (session.isGoogleLoggedIn()) {
            session.setGoogleLogin(false);
        } else if (session.isKakaoLoggedIn()) {
            Log.d(TAG, "카카오로갓");
            session.setKakaoLogin(false);
            UserManagement.requestLogout(new LogoutResponseCallback() {
                @Override
                public void onCompleteLogout() {
                    Log.d(TAG, "로갓 성공");
                }

                @Override
                public void onFailure(ErrorResult errorResult) {

                    super.onFailure(errorResult);
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) {

                    super.onSessionClosed(errorResult);
                }

                @Override
                public void onSuccess(Long result) {
                    super.onSuccess(result);
                }

                @Override
                public void onNotSignedUp() {
                    super.onNotSignedUp();
                }

                @Override
                public void onDidEnd() {
                    super.onDidEnd();
                }

                @Override
                public void onFailureForUiThread(ErrorResult errorResult) {
                    super.onFailureForUiThread(errorResult);
                }

            });
        } else if (session.isFacebookIn()) {
            session.setFacebookLogin(false);
            FacebookSdk.sdkInitialize(getApplicationContext());
            LoginManager.getInstance().logOut();
        }
        mAdapter.swap(null, "", null);
        refreshRecentUsedTrackListAndBookmarkedTrackList();
    }

    void refreshRecentUsedTrackListAndBookmarkedTrackList() {
        Fragment fragment = new TrackSettingFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppController.setCurrentActivity(this);
        Log.d(TAG, "메인액티비티 onCreate");
        setContentView(R.layout.activity_main);
        Fragment fragment = new TrackSettingFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

        long start = System.currentTimeMillis();
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(getApplicationContext());

        // Session manager
        session = new SessionManager(getApplicationContext());
        long end = System.currentTimeMillis();
        Log.d(TAG, "db, session쪽 로딩 시간 : " + (end - start) / 1000.0);
        start = System.currentTimeMillis();

        end = System.currentTimeMillis();
        Log.d(TAG, "setContentView 시간 : " + (end - start) / 1000.0);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        mDrawerRecyclerView = (RecyclerView) findViewById(R.id.left_drawer);
        mDrawerRecyclerView.setHasFixedSize(true);
        if (session.isLoggedIn() || session.isGoogleLoggedIn() || session.isFacebookIn() || session.isKakaoLoggedIn()) {
            TITLES[3] = "로그아웃";
        }
        mAdapter = new MyAdapter(TITLES, ICONS, null, null, PROFILE, this, session.isSessionLoggedIn());
        mDrawerRecyclerView.setAdapter(mAdapter);
        mLayoutManager = new LinearLayoutManager(this);
        mDrawerRecyclerView.setLayoutManager(mLayoutManager);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.openDrawer, R.string.closeDrawer) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // code here will execute once the drawer is opened( As I dont want anything happened whe drawer is
                // open I am not going to put anything here)
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                // Code here will execute once drawer is closed
            }
        };
        // Drawer Toggle Object Made
        drawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();


        if (toolbar != null) {
            toolbar.setLogo(R.drawable.bike);
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorAccent));
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayUseLogoEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
            }
            actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
            drawerLayout.addDrawerListener(actionBarDrawerToggle);
        }

        /**
         * ip 세팅화면
         */

        // SharedPreference에서 ip 가져오기
        IPManager ipManager = new IPManager(this);
        String savedIP = ipManager.loadServerIP();
        if (savedIP != null) {
            AppConfig.setServerIp(savedIP);
        }

        // ip 자동완성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 레이아웃 가져오기
        LayoutInflater inflater = this.getLayoutInflater();

        // 레이아웃 설정
        // parent view에 null을 넘기는 이유는 다이얼로그 레이아웃 안으로 가기 때문
        View view = inflater.inflate(R.layout.dialog_ip_setting, null);

        HashMap<String, String> serverIPMap = db.getIpDetails();
        Iterator<String> keys = serverIPMap.keySet().iterator();
        Log.d(TAG, "size : " + serverIPMap.size());

        // ip autocompletetextview is in activity_main.xml
        serverIpAutoComplete = (ClearableSqliteAutoCompleteTextView) view.findViewById(R.id.ip);

        // add the recentPOIListener so it will tries to suggest while the user types
        serverIpAutoComplete.addTextChangedListener(new CustomAutoCompleteTextChangedListener(this));

        // set adapter
        arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, serverIPs);
        serverIpAutoComplete.setAdapter(arrayAdapter);

        builder.setView(view)
                // 버튼 추가
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String serverIP = serverIpAutoComplete.getText().toString().trim();
                        db.addIp(serverIP);
                        AppConfig.setServerIp(serverIP);
                        IPManager ipManager = new IPManager(MainActivity.this);
                        ipManager.saveServerIP(serverIP);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        alertDialog = builder.create();
        Log.d(TAG, "세팅 버튼 누름");

        start = System.currentTimeMillis();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());
        end = System.currentTimeMillis();
        Log.d(TAG, "strict 로딩 시간 : " + (end - start) / 1000.0);
        start = System.currentTimeMillis();


        end = System.currentTimeMillis();
        Log.d(TAG, "구글맵 로딩 시간 : " + (end - start) / 1000.0);
        start = System.currentTimeMillis();


        end = System.currentTimeMillis();
        Log.d(TAG, "나머지 : " + (end - start) / 1000.0);

        displayNIckName();
    }

    void displayNIckName() {
        if (session.isLoggedIn()) {
            Log.d(TAG, "자체회원로긴");

            // Fetc1ng user details from sqlite
            HashMap<String, String> user = db.getUserNickname(SQLiteHandler.UserType.BIKENAVI);

            String email = user.get(SQLiteHandler.KEY_EMAIL);

            mAdapter.swap(null, null, email);
            mAdapter.changeLoginState(true);
        } else if (session.isGoogleLoggedIn()) {
            // Fetching user details from sqlite
            Log.d(TAG, "구글 자동로긴");
            HashMap<String, String> user = db.getUserNickname(SQLiteHandler.UserType.GOOGLE);

            String email = user.get(SQLiteHandler.KEY_GOOGLE_EMAIL);

            mAdapter.swap(null, null, email);
            mAdapter.changeLoginState(true);
        } else if (session.isFacebookIn()) {
            // Fetching user details from sqlite
            Log.d(TAG, "페북 자동로긴");
            HashMap<String, String> user = db.getUserNickname(SQLiteHandler.UserType.FACEBOOK);

            String name = user.get(SQLiteHandler.KEY_FACEBOOK_NAME);

            mAdapter.swap(null, name, null);
            mAdapter.changeLoginState(true);
        } else if (session.isKakaoLoggedIn()) {
            Log.d(TAG, "카카오로긴");
            // Fetching user details from sqlite
            HashMap<String, String> user = db.getUserNickname(SQLiteHandler.UserType.KAKAO);

            String email = user.get(SQLiteHandler.KEY_KAKAO_NICK_NAME);

            mAdapter.swap(null, null, email);
            mAdapter.changeLoginState(true);
        } else {
            mAdapter.swap(null, "", null);
            mAdapter.changeLoginState(false);
        }
    }

    // this function is used in CustomAutoCompleteTextChangedListener.java
    public String[] getItemsFromDb(String searchTerm) {

        // add items on the array dynamically
        List<String> products = db.read(searchTerm);
        int rowCount = products.size();

        String[] item = new String[rowCount];
        int x = 0;

        for (String record : products) {

            item[x] = record;
            x++;
        }

        return item;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // 메뉴 버튼이 처음 눌러졌을 때 실행되는 콜백메서드
        // 메뉴 버튼이 눌렸을 때 보여줄 menu에 대해서 정의
        getMenuInflater().inflate(R.menu.menu_main, menu);
        Log.d(TAG, "onCreateOptionsMenu - 최초 메뉴키를 눌렀을 때 호출됨");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "opPrepareOptionsMenu - 옵션 메뉴가 " +
                "화면에 보여질때마다 호출됨");
//
//        if (session.isLoggedIn() || session.isGoogleLoggedIn()) { // 로그인 한 상태확인
//            menu.getItem(1).setTitle("로그아웃");
//        } else {
//            menu.getItem(1).setTitle("로그인");
//        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occured.
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult : requestcode = " + requestCode);
        if (requestCode == SEARCH_INTEREST_POINT_TRACK_SETTING_FRAGMENT) { // 경로 검색용에서 장소검색 요청한게 돌아온 경우
            Log.d(TAG, "SEARCH_INTEREST_POINT_TRACK_SETTING_FRAGMENT");
            if (resultCode == RESULT_OK) {// 장소 검색 결과 리턴
                try {
                    TrackSettingFragment trackSettingFragment = (TrackSettingFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);

                    if (trackSettingFragment != null) {
                        trackSettingFragment.onActivityResult(requestCode, resultCode, data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendErrorReportToServer(e.toString());
                }
            }
        } else if (requestCode == SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT) {
            Log.d(TAG, "SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT");
            if (resultCode == RESULT_OK) {// 장소 검색 결과 리턴
                try {
                    POISearchFragment poiSearchFragment = (POISearchFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);

                    if (poiSearchFragment != null) {
                        poiSearchFragment.onActivityResult(requestCode, resultCode, data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendErrorReportToServer(e.toString());
                }
            }
        } else if (requestCode == SEARCH_INTEREST_POINT_FROM_REALTIME_TRACK_FRAGMENT) {
            // 실시간 네비게이션 화면용에서 장소 검색 결과 리턴
            if (resultCode == RESULT_OK) {// 장소 검색 결과 리턴
                try {
                    TrackRealTimeSettingFragment trackRealTimeSettingFragment = (TrackRealTimeSettingFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);

                    if (trackRealTimeSettingFragment != null) {
                        trackRealTimeSettingFragment.onActivityResult(requestCode, resultCode, data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendErrorReportToServer(e.toString());
                }
            }
        }
    }

    // 에러 전송
    private void sendErrorReportToServer(final String errorMessage) {

        // Tag used to cancel the request
        String tag_string_req = "send_error_report.";

        //JsonArrayRequest of volley
        final StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_USER_ERROR_REPORT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Calling method parsePOIList to parse the json response
                        try {
                            Log.d(TAG, "response : " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            boolean error = jsonObject.getBoolean("error");

                            if (!error) {
                                Log.d(TAG, "에러전송완료");
                            } else {
                                // Error in login. Get the error message
                                String errorMsg = jsonObject.getString("error_msg");
                                Log.d(TAG, errorMsg);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d(TAG, e.toString());
                        }
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //If an error occurs that means end of the list has reached
                        Log.d(TAG, error.toString());
                    }
                }) {
            @Override
            protected HashMap<String, String> getParams() {
                HashMap<String, String> mRequestParams = new HashMap<String, String>();
                if (session.isSessionLoggedIn()) {
                    mRequestParams = inputUserInfoToInputParams(mRequestParams);
                }
                mRequestParams.put("MESSAGE", errorMessage);

                return mRequestParams;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
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

    private String getStringFromResources(final int id) {
        return getResources().getString(id);
    }

    // 기기의 현재 gps좌표가 polyline의 10m 내외로 있는지 확인
    public boolean isLocationOnPath(LatLng currentGpsPoint, ArrayList<LatLng> pathStopPointList) {
        // Computes whether the given point lies on or near a polyline, within a specified tolerance in meters.
        // The polyline is composed of great circle segments if geodesic is true, and of Rhumb segments otherwise.
        // The polyline is not closed -- the closing segment between the first point and the last point is not included.
        // 마지막인자 : ~d : ~m(미터)
        return PolyUtil.isLocationOnPath(currentGpsPoint, pathStopPointList, true, 10d);
    }

    // 거리들 가져옴.
    public double getDistanceBetweenPoints(LatLng from, LatLng to) {
        return SphericalUtil.computeDistanceBetween(from, to);
    }

    @Override
    public void onProfileImageClicked(ImageView profileImageView) {

    }
}
