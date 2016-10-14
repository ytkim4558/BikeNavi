/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.nagnek.bikenavi.activity.LoginActivity;
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.customview.ClearableSqliteAutoCompleteTextView;
import com.nagnek.bikenavi.customview.DelayAutoCompleteTextView;
import com.nagnek.bikenavi.guide.GuideContent;
import com.nagnek.bikenavi.helper.IPManager;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapMarkerItem;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.util.HttpConnect;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    static final LatLng SEOUL_STATION = new LatLng(37.555755, 126.970431);
    private static final String TAG = MainActivity.class.getSimpleName();
    private final Handler mHandler = new Handler();
    TMapPoint mSource;
    TMapPoint mDest;
    DelayAutoCompleteTextView start_point, dest_point;
    ArrayList<TMapPoint> sourceAndDest;
    boolean animating; //애니메이션 진행중인지
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
    private GoogleMap mGoogleMap;
    private ArrayList<LatLng> pathStopPointList;    // 출발지 도착지를 포함한 경유지점(위도, 경도) 리스트
    private ArrayList<MarkerOptions> markerOptionsArrayList;    // 출발지 도착지 사이에 마커 리스트
    private List<Marker> descriptorMarkers = new ArrayList<Marker>(); //markers
    private List<Marker> markers = new ArrayList<Marker>(); //markers
    private SessionManager session; // 로그인했는지 확인용 변수
    private SQLiteHandler db;   // sqlite
    private Animator animator = new Animator();

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // SqLite database handler 초기화
        db = new SQLiteHandler(getApplicationContext());

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

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            toolbar.setNavigationIcon(R.drawable.ic_directions_bike_red_24dp);
        }

        sourceAndDest = new ArrayList<TMapPoint>();
        pathStopPointList = new ArrayList<LatLng>();
        markerOptionsArrayList = new ArrayList<MarkerOptions>();

        long start = System.currentTimeMillis();

        // Session manager
        session = new SessionManager(getApplicationContext());

        long end = System.currentTimeMillis();
        Log.d(TAG, "db쪽 로딩 시간 : " + (end - start) / 1000.0);
        start = System.currentTimeMillis();

        end = System.currentTimeMillis();
        Log.d(TAG, "setContentView 시간 : " + (end - start) / 1000.0);
        start = System.currentTimeMillis();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());
        end = System.currentTimeMillis();
        Log.d(TAG, "strict 로딩 시간 : " + (end - start) / 1000.0);
        start = System.currentTimeMillis();

        /**
         * 구글맵 생성
         */
        // 구글맵 초기 상태를 설정
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        end = System.currentTimeMillis();
        Log.d(TAG, "구글맵 로딩 시간 : " + (end - start) / 1000.0);
        start = System.currentTimeMillis();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                start_point = (DelayAutoCompleteTextView) findViewById(R.id.start_point);
                ProgressBar progressBar1 = (ProgressBar) findViewById(R.id.pb_loading_indicator1);
                setupTmapPOIToGoogleMapAutoCompleteTextView(start_point, progressBar1, "출발");
                dest_point = (DelayAutoCompleteTextView) findViewById(R.id.dest_point);

                ProgressBar progressBar2 = (ProgressBar) findViewById(R.id.pb_loading_indicator2);
                setupTmapPOIToGoogleMapAutoCompleteTextView(dest_point, progressBar2, "도착");
            }
        });

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 이전에 애니메이션 시작했으면 중지 후 초기화
                if (animating) {
                    animator.stopAnimation();
                }
                animator.startAnimation(true);

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

        TextView textView = (TextView) findViewById(R.id.name);
        if (session.isLoggedIn()) {
            Log.d(TAG, "자체회원로긴");

            // Fetching user details from sqlite
            HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.BIKENAVI);

            String email = user.get(SQLiteHandler.KEY_EMAIL);

            textView.setText(email);
            textView.setVisibility(View.VISIBLE);
        } else if (session.isGoogleLoggedIn()) {
            // Fetching user details from sqlite
            Log.d(TAG, "구글 자동로긴");
            HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.GOOGLE);

            String email = user.get(SQLiteHandler.KEY_GOOGLE_EMAIL);

            textView.setText(email);
            textView.setVisibility(View.VISIBLE);
        } else if (session.isFacebookIn()) {
            // Fetching user details from sqlite
            Log.d(TAG, "페북 자동로긴");
            HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.FACEBOOK);

            String name = user.get(SQLiteHandler.KEY_FACEBOOK_NAME);

            textView.setText(name);
            textView.setVisibility(View.VISIBLE);
        } else if (session.isKakaoLoggedIn()) {
            Log.d(TAG, "카카오로긴");
            // Fetching user details from sqlite
            HashMap<String, String> user = db.getUserDetails(SQLiteHandler.UserType.KAKAO);

            String email = user.get(SQLiteHandler.KEY_KAKAO_NICK_NAME);

            textView.setText(email);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
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

        if (session.isLoggedIn() || session.isGoogleLoggedIn()) { // 로그인 한 상태확인
            menu.getItem(1).setTitle("로그아웃");
        } else {
            menu.getItem(1).setTitle("로그인");
        }

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
                    serverIpAutoComplete.setText("192.168.1.189");
                }
                alertDialog.show();
                return true;

            case R.id.menu_login:
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void performFindRoute() {
        // 키보드 감추기
        InputMethodManager immhide = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

        // 이전에 애니메이션 시작했으면 중지 후 초기화
        if (animating) {
            animator.stopAnimation();
            clearMarkers();
        }
        markers.clear();
        descriptorMarkers.clear();
        markerOptionsArrayList.clear();

        // 시작위치. 도착지의 포커스 초기화.
        start_point.clearFocus();
        dest_point.clearFocus();
        String start = start_point.getText().toString();
        String destination = dest_point.getText().toString();
        TMapData tmapData3 = new TMapData();

        mGoogleMap.clear();

        try {
            ArrayList<TMapPOIItem> poiPositionOfStartItemArrayList = tmapData3.findAddressPOI(start);
            ArrayList<TMapPOIItem> poiPositionOfDestItemArrayList = tmapData3.findAddressPOI(destination);
            if (poiPositionOfStartItemArrayList != null && poiPositionOfDestItemArrayList != null) {
                mSource = poiPositionOfStartItemArrayList.get(0).getPOIPoint();
                mDest = poiPositionOfDestItemArrayList.get(0).getPOIPoint();
                tmapData3.findPathDataWithType(TMapData.TMapPathType.BICYCLE_PATH, mSource, mDest, new TMapData.FindPathDataListenerCallback() {
                    @Override
                    public void onFindPathData(TMapPolyLine tMapPolyLine) {
                        pathStopPointList.clear();
                        final ArrayList<TMapPoint> pointArrayList = tMapPolyLine.getLinePoint();
                        for (TMapPoint point : pointArrayList) {
                            LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
                            Log.d("tag", "위도 : " + latLng.latitude + ", 경도 : " + latLng.longitude);
                            pathStopPointList.add(latLng);
                        }

                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                if (mGoogleMap != null) {
                                    // 경로 polyline 그리기
                                    addPolyLineUsingGoogleMap(pathStopPointList);
                                    for (LatLng latLng : pathStopPointList) {
                                        Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(latLng).visible(false));
                                        markers.add(marker);
                                    }
                                } else {
                                    Log.d("tag", "아직 맵이 준비안됬어");
                                }
                            }
                        });
                    }
                });

                tmapData3.findPathDataAllType(TMapData.TMapPathType.BICYCLE_PATH, mSource, mDest, new TMapData.FindPathDataAllListenerCallback() {
                    @Override
                    public void onFindPathDataAll(Document document) {
                        final NodeList list = document.getElementsByTagName("Placemark");
//                        Log.d("count", "길이" + list.getLength());
                        int guide_length = 0;
                        GuideContent.ITEMS.clear();
                        //마커추가예정 리스트 초기화
                        markerOptionsArrayList.clear();
                        //마커리스트 초기화
                        markers.clear();

                        for (int i = 0; i < list.getLength(); ++i) {
                            Element item = (Element) list.item(i);
                            String description = HttpConnect.getContentFromNode(item, "description");

                            if (description != null) {
//                                Log.d("description", description);
                                GuideContent.GuideItem guideItem = new GuideContent.GuideItem(String.valueOf(guide_length), description);
                                GuideContent.ITEMS.add(guideItem);
                                ++guide_length;

                                String pointIndex = HttpConnect.getContentFromNode(item, "tmap:pointIndex");
                                if (pointIndex != null) {
                                    String str = HttpConnect.getContentFromNode(item, "coordinates");
                                    if (str != null) {
                                        String[] str2 = str.split(" ");
                                        for (int k = 0; k < str2.length; ++k) {
                                            try {
                                                String[] e1 = str2[k].split(",");
                                                // 마커 및 path 포인트를 추가하기 위한 위도 경도 생성
                                                LatLng latLng = new LatLng(Double.parseDouble(e1[1]), Double.parseDouble(e1[0]));
                                                // 마커 생성
                                                MarkerOptions marker = new MarkerOptions().title("지점").snippet(description).position(latLng);
                                                // 마커리스트에 추가 (addmarker는 Main 스레드에서만 되므로 이 콜백함수에서 쓸수없다. 따라서 한번에 묶어서 핸들러로 호출한다.)
                                                markerOptionsArrayList.add(marker);
                                            } catch (Exception var13) {
                                                Log.d("tag", "에러 : " + var13.getMessage());
                                            }
                                        }
                                    }

                                }

                                String lineIndex = HttpConnect.getContentFromNode(item, "tmap:lineIndex");
                                if (lineIndex != null) {
                                    String str = HttpConnect.getContentFromNode(item, "coordinates");
                                    if (str != null) {
                                        String[] str2 = str.split(" ");
                                        try {
                                            String[] e1 = str2[str2.length / 2].split(",");
                                            // 마커 및 path 포인트를 추가하기 위한 위도 경도 생성
                                            LatLng latLng = new LatLng(Double.parseDouble(e1[1]), Double.parseDouble(e1[0]));
                                            // 마커 생성
                                            MarkerOptions marker = new MarkerOptions().title("지점").snippet(description).position(latLng);
                                            // 마커리스트에 추가 (addmarker는 Main 스레드에서만 되므로 이 콜백함수에서 쓸수없다. 따라서 한번에 묶어서 핸들러로 호출한다.)
                                            markerOptionsArrayList.add(marker);
                                        } catch (Exception var13) {
                                            Log.d("tag", "에러 : " + var13.getMessage());
                                        }
                                    }

                                }


                            } else {
//                                Log.d("dd", "공백");
                            }

                        }
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mGoogleMap != null) {
                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                    // 경로 찾고나서 경로 지점들의 마커들 추가, 모든 마커들을 표시할 수 있는 줌레벨 계산
                                    for (MarkerOptions markerOptions : markerOptionsArrayList) {
                                        Marker marker = mGoogleMap.addMarker(markerOptions);
                                        descriptorMarkers.add(marker);
                                        builder.include(markerOptions.getPosition());
                                    }
                                    LatLngBounds bounds = builder.build();

                                    int padding = 0; // offset from edges of the map in pixels
                                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                    mGoogleMap.moveCamera(cu);
                                } else {
                                    Log.d("tag", "아직 맵이 준비안됬어");
                                }
                            }
                        });
                        FragmentManager fragmentManager = getFragmentManager();
                        ItemFragment fragment = new ItemFragment().newInstance(GuideContent.ITEMS.size(), GuideContent.ITEMS);
                        Bundle mBundle = new Bundle();
                        fragment.setArguments(mBundle);
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.replace(R.id.fragment_container, fragment);
//                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                        Log.d("count", "길이래" + list.getLength());
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clears all markers from the map.
     */
    public void clearMarkers() {
        mGoogleMap.clear();
        markers.clear();
        descriptorMarkers.clear();
        markerOptionsArrayList.clear();
    }

    private void addPolyLineUsingGoogleMap(ArrayList<LatLng> list) {
        Polyline polyline = mGoogleMap.addPolyline(new PolylineOptions().geodesic(true).color(Color.RED).width(5).addAll(list));
    }

    // final ArrayList 는 new ArrayList() 형태로 새로 ArrayList를 만드는게 안될 뿐 add 나 remove는 가능하다.
    private void setupTmapPOIToGoogleMapAutoCompleteTextView(final DelayAutoCompleteTextView locationName, final ProgressBar progressBar, final String markerTitle) {
        locationName.setThreshold(1);
        locationName.setAdapter(new TMapPOIAutoCompleteAdapter(this));
        locationName.setLoadingIndicator(progressBar);
        locationName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                TMapPOIItem tMapPOIItem = (TMapPOIItem) parent.getItemAtPosition(position);
                locationName.setText(tMapPOIItem.getPOIName());
                TMapMarkerItem tItem = new TMapMarkerItem();
                tItem.setTMapPoint(tMapPOIItem.getPOIPoint());
                tItem.setName(markerTitle);

                TMapPoint tMapPoint = tMapPOIItem.getPOIPoint();

                // 위도를 반환
                double wgs84_x = tMapPoint.getLatitude();

                // 경도를 반환
                double wgs84_y = tMapPoint.getLongitude();

                // start 지점과 도착지점 모두 설정되었으면 경로를 찾는다.
                if (start_point.getText().toString().equals("") != true && dest_point.getText().toString().equals("") != true) {
                    performFindRoute();
                } else {
                    Log.d("tag", "좌표위치 " + "Lat:" + wgs84_x + ", Long : " + wgs84_y);
                    LatLng latLng = new LatLng(wgs84_x, wgs84_y);
                    CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(wgs84_x, wgs84_y));
                    Log.d("tag", "좌표위치 가져옴" + "Lat:" + latLng.latitude + ", Long : " + latLng.longitude);
                    CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);

                    // 카메라 좌표를 검색 지역으로 이동
                    mGoogleMap.moveCamera(center);

                    // animateCamera는 근거리에선 부드럽게 변경한다.
                    if (mGoogleMap != null) {
                        mGoogleMap.animateCamera(zoom);
                        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(wgs84_x, wgs84_y)).title(tMapPOIItem.getPOIName()).snippet(tMapPOIItem.getPOIAddress().replace("null", "")));
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
        }

        // 내장 확대/축소 컨트롤을 제공
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        mGoogleMap = googleMap;
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

    /**
     * Highlight the marker by index.
     */
    // moveMarkerIndex : 실제 애니메이션하기 위해 움직이는데 필요한 마커, descrioptorMarkerIndex : 풍선 띄우는 마커들 인덱스
    private boolean highLightMarker(int moveMarkerIndex, int descriptorMarkerIndex) {
        LatLng descriptorLatLng = descriptorMarkers.get(descriptorMarkerIndex).getPosition();
        LatLng usedForMovingMarkers = markers.get(moveMarkerIndex).getPosition();
        Log.d("tag", "markerIndex :" + moveMarkerIndex + ", descIndex : " + descriptorMarkerIndex);
        if (descriptorLatLng.latitude == usedForMovingMarkers.latitude && descriptorLatLng.longitude == usedForMovingMarkers.longitude) {
            highLightMarker(descriptorMarkers.get(descriptorMarkerIndex));
            return true;
        }
        return false;
    }

    /**
     * Highlight the marker by marker.
     */
    private void highLightMarker(Marker marker) {
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        marker.showInfoWindow();
    }

    // 카메라 베어링(각도 조정)에 필요한 Location을 latLng를 이용해 반환
    private Location convertLatLngToLocation(LatLng latLng) {
        Location location = new Location("someLoc");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        return location;
    }

    // 카메라 각도 조정
    private float bearingBetweenLatLngs(LatLng beginLatLng, LatLng endLatLng) {
        Location beginLocation = convertLatLngToLocation(beginLatLng);
        Location endLocation = convertLatLngToLocation(endLatLng);
        return beginLocation.bearingTo(endLocation);
    }

    private void resetMarkers() {
        Log.d("tag", "초기화");
        for (Marker marker : this.markers) {
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            Log.d("tag", "색깔변함");
        }
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     */
    private void logoutUser() {
        session.setLogin(false);
        db.deleteUsers();
    }

    public class Animator implements Runnable {

        private static final int ANIMATE_SPEEED = 1500;
        private static final int ANIMATE_SPEEED_TURN = 1000;
        private static final int BEARING_OFFSET = -90;

        private final Interpolator interpolator = new LinearInterpolator();

        int movingCurrentMarkerIndex = 0;
        int descriptorMarkerIndex = 0;

        float tilt = 90;
        float zoom = 15.5f;
        boolean upward = true;

        long start = SystemClock.uptimeMillis();

        LatLng endLatLng = null;
        LatLng beginLatLng = null;

        boolean showPolyline = false;

        private Marker trackingMarker;
        private Polyline polyLine;
        private PolylineOptions rectOptions;

        public void reset() {
            resetMarkers();
            start = SystemClock.uptimeMillis();
            movingCurrentMarkerIndex = 0;
            descriptorMarkerIndex = 1;
            endLatLng = getEndLatLng();
            beginLatLng = getBeginLatLng();
        }

        public void stop() {
            trackingMarker.remove();
            mHandler.removeCallbacks(animator);

        }

        public void initialize(boolean showPolyLine) {
            reset();
            this.showPolyline = showPolyLine;

            highLightMarker(0, 0);
            Log.d("tag", "descriptorIndex :" + descriptorMarkerIndex);

            if (showPolyLine) {
                polyLine = initializePolyLine();
            }

            // We first need to put the camera in the correct position for the first run (we need 2 markers for this).....
            LatLng markerPos = markers.get(0).getPosition();
            LatLng secondPos = markers.get(1).getPosition();

            setupCameraPositionForMovement(markerPos, secondPos);

        }

        private BitmapDescriptor getBitmapDescriptor(int id) {
            Drawable vectorDrawable = MainActivity.this.getDrawable(id);
            int h = vectorDrawable.getIntrinsicHeight();
            int w = vectorDrawable.getIntrinsicWidth();
            vectorDrawable.setBounds(0, 0, w, h);
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            vectorDrawable.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bm);
        }

        private void setupCameraPositionForMovement(LatLng markerPos,
                                                    LatLng secondPos) {

            float bearing = bearingBetweenLatLngs(markerPos, secondPos);

            trackingMarker = mGoogleMap.addMarker(new MarkerOptions().position(markerPos)
                    .title("접니다")
                    .snippet("자전거에요"));
            trackingMarker.setIcon(getBitmapDescriptor(R.drawable.ic_directions_bike_red_24dp));

            CameraPosition cameraPosition =
                    new CameraPosition.Builder()
                            .target(markerPos)
                            .bearing(bearing + BEARING_OFFSET)
                            .tilt(90)
                            .zoom(mGoogleMap.getCameraPosition().zoom >= 16 ? mGoogleMap.getCameraPosition().zoom : 16)
                            .build();

            mGoogleMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(cameraPosition),
                    ANIMATE_SPEEED_TURN,
                    new GoogleMap.CancelableCallback() {

                        @Override
                        public void onFinish() {
                            System.out.println("finished camera");
                            animator.reset();
                            Handler handler = new Handler();
                            handler.post(animator);
                            animating = true;
                        }

                        @Override
                        public void onCancel() {
                            System.out.println("cancelling camera");
                            animating = false;
                        }
                    }
            );
        }

        private Polyline initializePolyLine() {
            //polyLinePoints = new ArrayList<LatLng>();
            if (polyLine != null) {
                polyLine.remove();
            }
            rectOptions = new PolylineOptions().geodesic(true).color(Color.CYAN);
            rectOptions.add(markers.get(0).getPosition());
            return mGoogleMap.addPolyline(rectOptions);
        }

        /**
         * Add the marker to the polyline.
         */
        private void updatePolyLine(LatLng latLng) {
            List<LatLng> points = polyLine.getPoints();
            points.add(latLng);
            polyLine.setPoints(points);
        }


        public void stopAnimation() {
            animator.stop();
        }

        public void startAnimation(boolean showPolyLine) {
            if (markers.size() > 2) {
                animator.initialize(showPolyLine);
            }
        }


        @Override
        public void run() {

            long elapsed = SystemClock.uptimeMillis() - start;
            double t = interpolator.getInterpolation((float) elapsed / ANIMATE_SPEEED);

//			LatLng endLatLng = getEndLatLng();
//			LatLng beginLatLng = getBeginLatLng();

            double lat = t * endLatLng.latitude + (1 - t) * beginLatLng.latitude;
            double lng = t * endLatLng.longitude + (1 - t) * beginLatLng.longitude;
            LatLng newPosition = new LatLng(lat, lng);

            trackingMarker.setPosition(newPosition);

            if (showPolyline) {
                updatePolyLine(newPosition);
            }

            // It's not possible to move the marker + center it through a cameraposition update while another camerapostioning was already happening.
            //navigateToPoint(newPosition,tilt,bearing,currentZoom,false);
            //navigateToPoint(newPosition,false);

            if (t < 1) {
                mHandler.postDelayed(this, 16);
            } else {

                System.out.println("Move to next marker.... current = " + movingCurrentMarkerIndex + " and size = " + markers.size());
                // imagine 5 elements -  0|1|2|3|4 currentindex must be smaller than 4
                if (movingCurrentMarkerIndex < markers.size() - 2) {

                    movingCurrentMarkerIndex++;

                    endLatLng = getEndLatLng();
                    beginLatLng = getBeginLatLng();


                    start = SystemClock.uptimeMillis();

                    LatLng begin = getBeginLatLng();
                    LatLng end = getEndLatLng();

                    float bearingL = bearingBetweenLatLngs(begin, end);

                    boolean highLighted = highLightMarker(movingCurrentMarkerIndex, descriptorMarkerIndex);
                    if (highLighted) {
                        ++descriptorMarkerIndex;
                    }

                    CameraPosition cameraPosition =
                            new CameraPosition.Builder()
                                    .target(end) // changed this...
                                    .bearing(bearingL + BEARING_OFFSET)
                                    .tilt(tilt)
                                    .zoom(mGoogleMap.getCameraPosition().zoom)
                                    .build();


                    mGoogleMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(cameraPosition),
                            ANIMATE_SPEEED_TURN,
                            null
                    );

                    start = SystemClock.uptimeMillis();
                    mHandler.postDelayed(animator, 16);

                } else {
                    movingCurrentMarkerIndex++;
                    highLightMarker(movingCurrentMarkerIndex, descriptorMarkerIndex);
                    stopAnimation();
                }

            }
        }


        private LatLng getEndLatLng() {
            return markers.get(movingCurrentMarkerIndex + 1).getPosition();
        }

        private LatLng getBeginLatLng() {
            return markers.get(movingCurrentMarkerIndex).getPosition();
        }

        private void adjustCameraPosition() {
            //System.out.println("tilt = " + tilt);
            //System.out.println("upward = " + upward);
            //System.out.println("zoom = " + zoom);
            if (upward) {

                if (tilt < 90) {
                    tilt++;
                    zoom -= 0.01f;
                } else {
                    upward = false;
                }

            } else {
                if (tilt > 0) {
                    tilt--;
                    zoom += 0.01f;
                } else {
                    upward = true;
                }
            }
        }
    }
}
