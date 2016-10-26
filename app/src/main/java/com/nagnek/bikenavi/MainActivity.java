/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
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

import com.facebook.login.LoginManager;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.nagnek.bikenavi.activity.LoginActivity;
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.customview.ClearableSqliteAutoCompleteTextView;
import com.nagnek.bikenavi.customview.DelayAutoCompleteTextView;
import com.nagnek.bikenavi.helper.IPManager;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;
import com.nagnek.bikenavi.util.NagneUtil;

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
    // 이 아아콘과 타이틀들은 배열에 담긴다

    final String TITLES[] = {"길찾기", "장소찾기"};
    final int ICONS[] = {R.drawable.ic_directions_black_24dp, R.drawable.places_ic_search};

    // 비슷하게 헤더뷰에 이름과 이메일을 위한 String 리소스를 생성한다.
    // 그리고나서 proifle picture 리소스를 헤더뷰에 생성한다.
    final int PROFILE = R.drawable.ic_account_circle_white_24dp;

    RecyclerView mRecyclerView;
    MyAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    ActionBarDrawerToggle mDrawerToggle;

    private static final String TAG = MainActivity.class.getSimpleName();
    DelayAutoCompleteTextView start_point, dest_point;
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
    private SessionManager session; // 로그인했는지 확인용 변수
    private SQLiteHandler db;   // sqlite
    static final int SEARCH_INTEREST_POINT = 1; // 장소 검색 request code
    ActionBarDrawerToggle actionBarDrawerToggle;
    DrawerLayout drawerLayout;

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
    public void onLoginStateButtonClicked(Button loginStateButton) {
        if (session.isLoggedIn() || session.isGoogleLoggedIn() || session.isFacebookIn() || session.isKakaoLoggedIn()) {
            logoutUser();
            mAdapter.changeLoginState(false);
        } else {
            redirectLoginActivity();
        }
    }

    @Override
    public void onNavItemClicked(int position) {
        Intent intent = null;
        switch (position) {
            // 첫번째 아이콘
            case 1:
                break;
            //두번째 아이콘
            case 2:
                intent = new Intent(MainActivity.this, POISearchActivity.class);
                break;
        }
        if(intent != null) {
            startActivity(intent);
        }
    }

    private void redirectLoginActivity() { // Launching the login activity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
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
            LoginManager.getInstance().logOut();
        }
        mAdapter.swap(null, "로그인 해주세요", null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long start = System.currentTimeMillis();
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(getApplicationContext());

        // Session manager
        session = new SessionManager(getApplicationContext());
        long end = System.currentTimeMillis();
        Log.d(TAG, "db, session쪽 로딩 시간 : " + (end - start) / 1000.0);
        start = System.currentTimeMillis();

        setContentView(R.layout.activity_main);
        end = System.currentTimeMillis();
        Log.d(TAG, "setContentView 시간 : " + (end - start) / 1000.0);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.left_drawer);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new MyAdapter(TITLES, ICONS, null, null, PROFILE, this, session.isSessionLoggedIn());
        mRecyclerView.setAdapter(mAdapter);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
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
            toolbar.setLogo(R.drawable.ic_directions_bike_red_24dp);
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

        // add the listener so it will tries to suggest while the user types
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

        final TextInputLayout ti_start = (TextInputLayout) findViewById(R.id.ti_start_point);
        final TextInputLayout ti_dest = (TextInputLayout) findViewById(R.id.ti_dest_point);
        /**
         * 출발지나 도착지 입력창을 클릭하면 검색 액티비티로 넘어간다.
         */
        start_point = (DelayAutoCompleteTextView) findViewById(R.id.start_point);
        start_point.setKeyListener(null);
        start_point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Performing stop of activity that is not resumed: {com.nagnek.bikenavi/com.nagnek.bikenavi.MainActivity} 에러 방지
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                        intent.putExtra(getResources().getString(R.string.name_purpose_search_point), "출발");
                        intent.putExtra(getResources().getString(R.string.current_point_text_for_transition), start_point.getText().toString());
                        // 화면전환 애니메이션을 생성한다. 트랜지션 이름은 양쪽 액티비티에 선언되어야한다.
                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this,
                                start_point, start_point.getTransitionName());
                        startActivityForResult(intent, SEARCH_INTEREST_POINT, options.toBundle());
                    }
                }, 300);
            }
        });

        dest_point = (DelayAutoCompleteTextView) findViewById(R.id.dest_point);
        dest_point.setKeyListener(null);
        dest_point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                        intent.putExtra(getResources().getString(R.string.name_purpose_search_point), "도착");
                        intent.putExtra(getResources().getString(R.string.current_point_text_for_transition), dest_point.getText().toString());
                        // 화면전환 애니메이션을 생성한다. 트랜지션 이름은 양쪽 액티비티에 선언되어야한다.
                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this,
                                dest_point, dest_point.getTransitionName());
                        startActivityForResult(intent, SEARCH_INTEREST_POINT, options.toBundle());
                    }
                }, 300);
            }
        });
        end = System.currentTimeMillis();
        Log.d(TAG, "나머지 : " + (end - start) / 1000.0);

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
    protected void onStart() {
        super.onStart();

        if (session.isLoggedIn()) {
            Log.d(TAG, "자체회원로긴");

            // Fetching user details from sqlite
            HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.BIKENAVI);

            String email = user.get(SQLiteHandler.KEY_EMAIL);

            mAdapter.swap(null, null, email);
            mAdapter.changeLoginState(true);
        } else if (session.isGoogleLoggedIn()) {
            // Fetching user details from sqlite
            Log.d(TAG, "구글 자동로긴");
            HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.GOOGLE);

            String email = user.get(SQLiteHandler.KEY_GOOGLE_EMAIL);

            mAdapter.swap(null, null, email);
            mAdapter.changeLoginState(true);
        } else if (session.isFacebookIn()) {
            // Fetching user details from sqlite
            Log.d(TAG, "페북 자동로긴");
            HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.FACEBOOK);

            String name = user.get(SQLiteHandler.KEY_FACEBOOK_NAME);

            mAdapter.swap(null, name, null);
            mAdapter.changeLoginState(true);
        } else if (session.isKakaoLoggedIn()) {
            Log.d(TAG, "카카오로긴");
            // Fetching user details from sqlite
            HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.KAKAO);

            String email = user.get(SQLiteHandler.KEY_KAKAO_NICK_NAME);

            mAdapter.swap(null, null, email);
            mAdapter.changeLoginState(true);
        } else {
            mAdapter.swap(null, "로그인 해주세요", null);
            mAdapter.changeLoginState(false);
        }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // 메뉴의 항목을 선택(클릭)했을 때 호출되는 콜백메서드
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.d(TAG, "onOptionsItemSelected - 메뉴항목을 클릭했을 때 호출됨.");

        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                IPManager ipManager = new IPManager(this);
                String savedIP = ipManager.loadServerIP();
                if (savedIP != null) {
                    AppConfig.setServerIp(savedIP);
                    serverIpAutoComplete.setText(savedIP);
                } else {
                    serverIpAutoComplete.setText(AppConfig.HOSTING_IP);
                }
                alertDialog.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occured.

    }

    // 출발지 도착지 설정할때마다 불러옴.
    // 둘다 설정한 경우는 경로를 탐색하고 한곳만 설정한 경우는 해당 좌표를 표시한다.
    void reactionSearchResult() {
        // start 지점과 도착지점 모두 설정되었으면 경로를 찾는다.
        if (!start_point.getText().toString().equals("") && !dest_point.getText().toString().equals("")) {
            redirectTrackActivity();
        }
    }

    void redirectTrackActivity() {
        Intent intent = new Intent(MainActivity.this, TrackActivity.class);
        intent.putExtra(NagneUtil.getStringFromResources(this.getApplicationContext(), R.string.start_point_text_for_transition), start_point.getText().toString());
        intent.putExtra(NagneUtil.getStringFromResources(this.getApplicationContext(), R.string.dest_point_text_for_transition), dest_point.getText().toString());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult ");
        if (requestCode == SEARCH_INTEREST_POINT) { // 장소검색 요청한게 돌아온 경우
            Log.d(TAG, "SEARCH_INTEREST_POINT");
            if (resultCode == RESULT_OK) {// 장소 검색 결과 리턴
                String purposePoint = data.getStringExtra(getStringFromResources(R.string.name_purpose_search_point));
                Log.d(TAG, "장소입력한 곳은? " + purposePoint);
                String selectPoint = data.getStringExtra(getStringFromResources(R.string.select_poi_name_for_transition));
                String address = data.getStringExtra(getStringFromResources(R.string.select_poi_address_for_transition));
                if (purposePoint.equals("출발")) {
                    start_point.setText(selectPoint);
                } else if (purposePoint.equals("도착")) {
                    dest_point.setText(selectPoint);
                } else {
                    Log.d(TAG, "purposePoint에 값이 없나? 아님 이상한가?");
                }
                reactionSearchResult();
            }
        }
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
