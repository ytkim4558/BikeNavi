/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
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
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.app.AppController;
import com.nagnek.bikenavi.guide.GuideContent;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;
import com.nagnek.bikenavi.util.NagneUtil;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.util.HttpConnect;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class TrackRealTImeActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key"; // 마지막 사용한 시각
    protected final static String BEFORE_UPDATED_TIME_STRING_KEY = "before-updated-time-string-key"; // 이전에 사용한 시각
    protected final static String MORE_BEFORE_UPDATED_TIME_STRING_KEY = "more-before-updated-time-string-key"; // 이전에 사용한 시각
    protected final static int TRACK_IN_TOLERANCE = 50; // 추적 오차 완전히 벗어났다고 판단되는 거리
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LOCATION_BEFORE_KEY = "location-before_key";
    protected final static String LOCATION_MORE_BEFORE_KEY = "location-more_before_key";
    private static final String TAG = TrackRealTImeActivity.class.getSimpleName();
    private static final int REQUEST_CHECK_SETTINGS = 5;
    private static final int REQUEST_RESOLVE_ERROR = 6;
    private static int over_location_count = 0; // 경로에서 벗어난 횟수
    private final Handler mHandler = new Handler();
    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime, mBeforeUpdateTime, mMoreBeforeUpdateTime; // 이전에 저장한시각과 그 보다 이전에 저장한 시각
    ArrayList<TMapPoint> sourceAndDest;
    boolean animating; //애니메이션 진행중인지
    TMapPoint mSource;
    TMapPoint mDest;
    String start_poi_name;
    String dest_poi_name;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient = null;
    ImageView turnGuideFirstImage, turnGuideSecondImage, turnGuideThirdImage; // 첫번째 턴(좌회전 우회전등, 현재 시점), 두번째 턴, 세번째턴
    TextView remainingFutureFirstText, remainingFutureSecondText, remainingFutureThirdText; // 첫번째 남은거리, 두번째 남은거리, 세번째 남은거리
    LinearLayout firstFutureGuideLayout, secondFutureGuideLayout, thirdFutureGuideLayout; // 위의 턴과 거리를 표시하는 레이아웃의 첫번째, 두번째, 세번째
    SparseArray<String> directionMap; // tmap 방향 정보 반환
    boolean isShowCheckingChangeRouteDialog; // 경로 변경을 선택할지 결정하는 변수
    private Marker trackingCycleMarker; // 내위치를 표시해주는 마커
    private TrackRealTImeActivity.Animator animator = new TrackRealTImeActivity.Animator();
    private GoogleMap mGoogleMap;
    private SessionManager session; // 로그인했는지 확인용 변수
    private ArrayList<LatLng> pathStopPointList;    // 출발지 도착지를 포함한 경유지점(위도, 경도) 리스트
    private ArrayList<CustomPoint> descriptorPointList;    // 설명지를 가지고 있는 지점 (위도, 경도) 리스트
    private ArrayList<MarkerOptions> markerOptionsArrayList;    // 출발지 도착지 사이에 마커 리스트
    private List<Marker> descriptorMarkers = new ArrayList<Marker>(); //markers
    private List<Integer> directionList = new ArrayList<Integer>(); // tmap 방향 전환
    private List<Integer> distanceList = new ArrayList<Integer>(); // tmap 거리
    private List<Marker> markers = new ArrayList<Marker>(); //markers
    private SQLiteHandler db;   // sqlite
    // Keys for storing activity state in the Bundle.
    private ProgressDialog pDialog; // 진행 상황 확인용 다이얼로그
    private TextView guideTextVIew; // 가이드
    private ImageView guideImageView; // 가이드
    private Location mCurrentLocation, mBeforeLocation, mMoreBeforeLocation;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Polyline polyLine;  // 현재 그리는 경로
    private ArrayList<Polyline> guideSegmentPolyLines; // 안내별 분할 경로
    private boolean resolvingError;
    private boolean enableSnapOnRoad;
    private int currentTotalRidingDistance = 0;   // 주행한 총 거리
    private Integer lastSegmentIndex; // 마지막으로 추적된 gps의 위치 인덱스
    private double currentSegmentDistance = 0;   //위의 마지막 인덱스에서 계산된 거리
    private ArrayList<String> realTimedescriptionArrayList; // tmap 결과로 나오는 description의 모음
    private List<Integer> realTimedirectionList;    // 실시간 때 필요한 방향 전환 리스트;
    private List<Double> realTimedistanceList; // tmap 실시간 거리
    private Polyline realtimeAllPolylines;  // tmap 전체 경로

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_real_time);
        AppController.setCurrentActivity(this);
        initialDirectionMap();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);


        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices
        // API.
        buildGoogleApiClient();

        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(getApplicationContext());

        // Session manager
        session = new SessionManager(getApplicationContext());

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        /**
         * guide 레이아웃 아이디 초기화
         */
        guideImageView = (ImageView) findViewById(R.id.bigTurnGuide);
        turnGuideFirstImage = (ImageView) findViewById(R.id.firstTurnGuide);
        turnGuideSecondImage = (ImageView) findViewById(R.id.secondTurnGuide);
        turnGuideThirdImage = (ImageView) findViewById(R.id.thirdTurnGuide);
        remainingFutureFirstText = (TextView) findViewById(R.id.firstText);
        remainingFutureSecondText = (TextView) findViewById(R.id.secondText);
        remainingFutureThirdText = (TextView) findViewById(R.id.thirdText);
        firstFutureGuideLayout = (LinearLayout) findViewById(R.id.firstFutureGuideLayout);
        secondFutureGuideLayout = (LinearLayout) findViewById(R.id.secondFutureGuideLayout);
        thirdFutureGuideLayout = (LinearLayout) findViewById(R.id.thirdFutureGuideLayout);

        guideTextVIew = (TextView) findViewById(R.id.guide);

        Intent receivedIntent = getIntent();
        start_poi_name = receivedIntent.getStringExtra(NagneUtil.getStringFromResources(this.getApplicationContext(), R.string.start_point_text_for_transition));
        dest_poi_name = receivedIntent.getStringExtra(NagneUtil.getStringFromResources(this.getApplicationContext(), R.string.dest_point_text_for_transition));

        TextView route = (TextView) findViewById(R.id.track_log);

        route.setText(start_poi_name + "=>" + dest_poi_name);

        /**
         * 구글맵 생성
         */
        // 구글맵 초기 상태를 설정
        long start = System.currentTimeMillis();
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        long end = System.currentTimeMillis();
        Log.d(TAG, "구글맵 로딩 시간 : " + (end - start) / 1000.0);

        sourceAndDest = new ArrayList<>();
        pathStopPointList = new ArrayList<>();
        markerOptionsArrayList = new ArrayList<>();
        descriptorPointList = new ArrayList<>();

        // 현재 위치 가져오기 https://developer.android.com/training/location/change-location-settings.html?hl=ko
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        // 수정 : https://github.com/googlesamples/android-play-location/blob/master/LocationSettings/app/src/main/java/com/google/android/gms/location/sample/locationsettings/MainActivity.java
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "All location settings are satisfied.");
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to" +
                                "upgrade location settings ");
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // 위치 권한 변경하는 다이얼로그를 띄운다고 함.
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    TrackRealTImeActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                                "not created.");
                        break;
                }
            }
        });
    }

    void initialDirectionMap() {
        directionMap = new SparseArray<>();
        directionMap.put(0, "안내 없음");
        directionMap.put(11, "직진");
        directionMap.put(12, "좌회전");
        directionMap.put(13, "우회전");
        directionMap.put(14, "U-turn");
        directionMap.put(15, "P-turn");
        directionMap.put(16, "8시방향 좌회전");
        directionMap.put(17, "10시방향 좌회전");
        directionMap.put(18, "2시방향 우회전");
        directionMap.put(19, "4시방향 우회전");
        directionMap.put(117, "우측");
        directionMap.put(118, "좌측");
        directionMap.put(119, "지하차도로 진입");
        directionMap.put(120, "고가도로로 진입");
        directionMap.put(121, "터널로 진입");
        directionMap.put(122, "교량으로 진입");
        directionMap.put(210, "지하보도로 진입");
        directionMap.put(211, "계단으로 진입");
        directionMap.put(212, "경사로로 진입");
        directionMap.put(213, "계단+경사로로 진입");
        directionMap.put(214, "토끼굴로 진입");
        directionMap.put(123, "지하차도 옆길");
        directionMap.put(124, "고가차도 옆길");
        directionMap.put(131, "로타리 1시방향");
        directionMap.put(132, "로타리 2시방향");
        directionMap.put(133, "로타리 3시방향");
        directionMap.put(134, "로타리 4시방향");
        directionMap.put(135, "로타리 5시방향");
        directionMap.put(136, "로타리 6시방향");
        directionMap.put(137, "로타리 7시방향");
        directionMap.put(138, "로타리 8시방향");
        directionMap.put(139, "로타리 9시방향");
        directionMap.put(140, "로타리 10시방향");
        directionMap.put(141, "로타리 11시방향");
        directionMap.put(142, "로타리 12시방향");
        directionMap.put(200, "출발지");
        directionMap.put(201, "목적지");
        directionMap.put(184, "경유지");
        directionMap.put(185, "첫번째 경유지");
        directionMap.put(186, "두번째 경유지");
        directionMap.put(187, "세번째 경유지");
        directionMap.put(188, "네번째 경유지");
        directionMap.put(189, "다섯번째 경유지");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_BEFORE_KEY)) {
                mBeforeLocation = savedInstanceState.getParcelable(LOCATION_BEFORE_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_MORE_BEFORE_KEY)) {
                mMoreBeforeLocation = savedInstanceState.getParcelable(LOCATION_MORE_BEFORE_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }

            if (savedInstanceState.keySet().contains(BEFORE_UPDATED_TIME_STRING_KEY)) {
                mBeforeUpdateTime = savedInstanceState.getString(BEFORE_UPDATED_TIME_STRING_KEY);
            }

            if (savedInstanceState.keySet().contains(MORE_BEFORE_UPDATED_TIME_STRING_KEY)) {
                mMoreBeforeUpdateTime = savedInstanceState.getString(MORE_BEFORE_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    // 참고 : https://developer.android.com/training/location/change-location-settings.html?hl=ko
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2000); // 수피트 간격에 대한 정확한 위치 업데이트를 반환한다.
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);   // ACCESS_FINE_LOCATION 권한과 관계 있다.
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

    void performFindRoute(String startPOIName, String destPOIName) {
        // 이전에 애니메이션 시작했으면 중지 후 초기화
        if (animating) {
            animator.stopAnimation();
            clearMarkers();
        }

        AppConfig.initializeTMapTapi(TrackRealTImeActivity.this);

        TMapData tmapData3 = new TMapData();

        try {
            ArrayList<TMapPOIItem> poiPositionOfStartItemArrayList = tmapData3.findAddressPOI(startPOIName);
            ArrayList<TMapPOIItem> poiPositionOfDestItemArrayList = tmapData3.findAddressPOI(destPOIName);
            if (poiPositionOfStartItemArrayList != null && poiPositionOfDestItemArrayList != null) {
                mSource = poiPositionOfStartItemArrayList.get(0).getPOIPoint();
                mDest = poiPositionOfDestItemArrayList.get(0).getPOIPoint();

                tmapData3.findPathDataAllType(TMapData.TMapPathType.BICYCLE_PATH, mSource, mDest, new TMapData.FindPathDataAllListenerCallback() {
                    @Override
                    public void onFindPathDataAll(final Document document) {
                        //String s = documenttoString(document);
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                mGoogleMap.clear();
                                if (mGoogleMap != null && document != null) {
                                    //마커리스트 초기화
                                    markers.clear();
                                    descriptorMarkers.clear();
                                    pathStopPointList.clear();
                                    descriptorPointList.clear();

                                    /**
                                     * 전체 경로 그리기
                                     */
                                    final NodeList lineList = document.getElementsByTagName("LineString");

                                    for (int i = 0; i < lineList.getLength(); ++i) {
                                        Element item = (Element) lineList.item(i);
                                        String str = HttpConnect.getContentFromNode(item, "coordinates");
                                        if (str != null) {
                                            String[] coordinateList = str.split(" ");

                                            for (String coordinate : coordinateList) {
                                                try {
                                                    String[] e1 = coordinate.split(",");
                                                    LatLng latLng = new LatLng(Double.parseDouble(e1[1]), Double.parseDouble(e1[0]));
                                                    pathStopPointList.add(latLng);
                                                    Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(latLng).visible(false));
                                                    markers.add(marker);
                                                } catch (Exception var13) {
                                                    Log.d(TAG, var13.getMessage());
                                                }
                                            }
                                        }
                                    }

                                    // 경로 polyline 그리기
                                    realtimeAllPolylines = addPolyLineUsingGoogleMap(pathStopPointList);

                                    /**
                                     * tmap 점별, 라인 그리기
                                     */
                                    final NodeList list = document.getElementsByTagName("Placemark");
//                        Log.d("count", "길이" + list.getLength());
                                    int guide_length = 0;
                                    GuideContent.ITEMS.clear();
                                    //마커추가예정 리스트 초기화
                                    markerOptionsArrayList.clear();

                                    /**
                                     * 애니메이션에 필요한 반복
                                     */

                                    for (int i = 0; i < list.getLength(); ++i) {
                                        Element item = (Element) list.item(i);
                                        String description = HttpConnect.getContentFromNode(item, "description");

                                        if (description != null) {
//                                Log.d("description", description);
                                            GuideContent.GuideItem guideItem = new GuideContent.GuideItem(String.valueOf(guide_length), description);
                                            GuideContent.ITEMS.add(guideItem);
                                            ++guide_length;

                                            /**
                                             * tmap 점별 그리기
                                             */

                                            String pointIndex = HttpConnect.getContentFromNode(item, "tmap:pointIndex");
                                            if (pointIndex != null) {

                                                /**
                                                 * 거리 추가
                                                 */
                                                String distance = HttpConnect.getContentFromNode(item, "tmap:distance");
                                                if (distance != null) {
                                                    distanceList.add(Integer.valueOf(distance));
                                                } else {
                                                    distanceList.add(null);
                                                }

                                                // 방향전환 추가
                                                String direction = HttpConnect.getContentFromNode(item, "tmap:turnType");
                                                if (direction != null) {
                                                    directionList.add(Integer.valueOf(direction));
                                                } else {
                                                    Log.d(TAG, "distance가 없네? pointIndex : " + String.valueOf(pointIndex));
                                                    directionList.add(null);
                                                }

                                                // 좌표 정보 추가
                                                String str = HttpConnect.getContentFromNode(item, "coordinates");
                                                if (str != null) {
                                                    String[] coordinateList = str.split(" ");
                                                    for (String coordinates : coordinateList) {
                                                        try {
                                                            String[] e1 = coordinates.split(",");
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

                                            /**
                                             * tmap 라인 그리기
                                             */
                                            String lineIndex = HttpConnect.getContentFromNode(item, "tmap:lineIndex");
                                            if (lineIndex != null) {
                                                /**
                                                 * 방향전환 추가
                                                 */
                                                String direction = HttpConnect.getContentFromNode(item, "tmap:turnType");
                                                if (direction != null) {
                                                    directionList.add(Integer.valueOf(direction));
                                                } else {
                                                    directionList.add(11); // 직진
                                                }

                                                /**
                                                 * 거리 추가
                                                 */
                                                String distance = HttpConnect.getContentFromNode(item, "tmap:distance");
                                                if (distance != null) {
                                                    distanceList.add(Integer.valueOf(distance));
                                                } else {
                                                    distanceList.add(null);
                                                }

                                                /**
                                                 * 좌표 정보 추가
                                                 */
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

                                    /**
                                     * 실시간 경로에 필요한 내용을 위한 반복
                                      */
                                    guideSegmentPolyLines = new ArrayList<>();
                                    realTimedescriptionArrayList = new ArrayList<>();
                                    realTimedirectionList = new ArrayList<>();
                                    realTimedistanceList = new ArrayList<>();

                                    for (int i = 0; i < list.getLength(); ++i) {
                                        Element item = (Element) list.item(i);
                                        /**
                                         * tmap 점별 그리기
                                         */

                                        String pointIndex = HttpConnect.getContentFromNode(item, "tmap:pointIndex");
                                        if (pointIndex != null) {
                                            /**
                                             * 방향전환 추가
                                             */
                                            String direction = HttpConnect.getContentFromNode(item, "tmap:turnType");
                                            if (direction != null) {
                                                realTimedirectionList.add(Integer.valueOf(direction));
                                            } else {
                                                realTimedirectionList.add(11); // 직진
                                            }

                                            // 좌표 정보 추가
                                            String str = HttpConnect.getContentFromNode(item, "coordinates");
                                            if (str != null) {
                                                String[] coordinateList = str.split(" ");
                                                for (String coordinates : coordinateList) {
                                                    try {
                                                        String[] e1 = coordinates.split(",");
                                                        // 마커 및 path 포인트를 추가하기 위한 위도 경도 생성
                                                        LatLng latLng = new LatLng(Double.parseDouble(e1[1]), Double.parseDouble(e1[0]));

                                                        CustomPoint customPoint = new CustomPoint();
                                                        customPoint.latLng = latLng;
                                                        customPoint.pointType = PointType.point;
                                                        descriptorPointList.add(customPoint);
                                                    } catch (Exception var13) {
                                                        Log.d("tag", "에러 : " + var13.getMessage());
                                                    }
                                                }
                                            }
                                        }

                                        /**
                                         * tmap 라인 그리기
                                         */
                                        String lineIndex = HttpConnect.getContentFromNode(item, "tmap:lineIndex");
                                        if (lineIndex != null) {
                                            /**
                                             * 설명 추가
                                             */
                                            String description = HttpConnect.getContentFromNode(item, "description");
                                            realTimedescriptionArrayList.add(description);

                                            /**
                                             * 거리 추가
                                             */
                                            String distance = HttpConnect.getContentFromNode(item, "tmap:distance");
                                            if (distance != null) {
                                                realTimedistanceList.add(Double.valueOf(distance));
                                            } else {
                                                realTimedistanceList.add(null);
                                            }

                                            String str = HttpConnect.getContentFromNode(item, "coordinates");

                                            // customPointList 생성용
                                            if (str != null) {
                                                String[] coordinateList = str.split(" ");
                                                Polyline guideSegmentPolyLine = newPolyLine();
                                                for (String coordinates : coordinateList) {
                                                    try {
                                                        String[] e1 = coordinates.split(",");
                                                        // 마커 및 path 포인트를 추가하기 위한 위도 경도 생성
                                                        LatLng latLng = new LatLng(Double.parseDouble(e1[1]), Double.parseDouble(e1[0]));
                                                        CustomPoint customPoint = new CustomPoint();
                                                        customPoint.latLng = latLng;
                                                        customPoint.pointType = PointType.line;
                                                        descriptorPointList.add(customPoint);

                                                        // segment polyline에 좌표 업데이트
                                                        updatePolyLine(guideSegmentPolyLine, latLng);
                                                    } catch (Exception var13) {
                                                        Log.d("tag", "에러 : " + var13.getMessage());
                                                    }
                                                }
                                                // 세그먼트별 polyline추가
                                                guideSegmentPolyLines.add(guideSegmentPolyLine);
                                            }
                                        }

                                        // passLineStyle인 경우
                                        if(pointIndex == null && lineIndex == null) {
                                            String lineString = HttpConnect.getContentFromNode(item, "LineString");
                                            if(lineString != null) {
                                                /**
                                                 * 설명 추가
                                                 */
                                                String description = HttpConnect.getContentFromNode(item, "description");
                                                realTimedescriptionArrayList.add(description);

                                                /**
                                                 * 거리 추가
                                                 */
                                                String distance = HttpConnect.getContentFromNode(item, "tmap:distance");
                                                if (distance != null) {
                                                    realTimedistanceList.add(Double.valueOf(distance));
                                                } else {
                                                    realTimedistanceList.add(null);
                                                }

                                                String str = HttpConnect.getContentFromNode(item, "coordinates");
                                                if (str != null) {
                                                    String[] coordinateList = str.split(" ");

                                                    Polyline guideSegmentPolyLine = newPolyLine();
                                                    for (String coordinate : coordinateList) {
                                                        try {
                                                            String[] e1 = coordinate.split(",");
                                                            LatLng latLng = new LatLng(Double.parseDouble(e1[1]), Double.parseDouble(e1[0]));
                                                            CustomPoint customPoint = new CustomPoint();
                                                            customPoint.latLng = latLng;
                                                            customPoint.pointType = PointType.line;
                                                            descriptorPointList.add(customPoint);
                                                            // segment polyline에 좌표 업데이트
                                                            updatePolyLine(guideSegmentPolyLine, latLng);
                                                        } catch (Exception var13) {
                                                            Log.d(TAG, var13.getMessage());
                                                        }
                                                    }
                                                    // 세그먼트별 polyline추가
                                                    guideSegmentPolyLines.add(guideSegmentPolyLine);
                                                }
                                            }
                                        }
                                    }

                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                    // 경로 찾고나서 경로 지점들의 마커들 추가, 모든 마커들을 표시할 수 있는 줌레벨 계산
                                    for (MarkerOptions markerOptions : markerOptionsArrayList) {
                                        Marker marker = mGoogleMap.addMarker(markerOptions);
                                        descriptorMarkers.add(marker);
                                        builder.include(markerOptions.getPosition());
                                    }

                                    LatLngBounds bounds = builder.build();

                                    // TODO : 디버깅시 descriporPointList와 stopPointList 갯수가 같은 지 확인
                                    // 가이드별 polyline 생성
                                    //setGuidePolylines(descriptorPointList);

                                    int padding = 0; // offset from edges of the map in pixels
                                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                    mGoogleMap.moveCamera(cu);

                                    FragmentManager fragmentManager = getFragmentManager();
                                    ItemFragment fragment = new ItemFragment().newInstance(GuideContent.ITEMS.size(), GuideContent.ITEMS);
                                    Bundle mBundle = new Bundle();
                                    fragment.setArguments(mBundle);
                                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                                    fragmentTransaction.replace(R.id.fragment_container, fragment);
//                        fragmentTransaction.addToBackStack(null);
                                    fragmentTransaction.commit();
                                    Log.d("count", "길이래" + list.getLength());
                                } else {
                                    Log.d("tag", "아직 맵이 준비안됬어 또는 document가 없어");
                                }

                                if (markers.size() < 2) {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(TrackRealTImeActivity.this);
                                    alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            redirectMainActivity();
                                        }
                                    });
                                    alert.setMessage("경로를 찾을 수 없습니다. 재 설정해주세요");
                                    alert.show();
                                    return;
                                }

                                // 현재 위치 마커 띄우기
                                trackingCycleMarker = mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(mSource.getLatitude(), mSource.getLongitude()))
                                        .title("접니다")
                                        .snippet("자전거에요")
                                        .anchor(0.5f, 0.5f)
                                        .flat(true));
                                trackingCycleMarker.setIcon(getBitmapDescriptor(R.drawable.directionarrow));
                                polyLine = initializePolyLine();

                                LatLng markerPos = markers.get(0).getPosition();
                                LatLng secondPos = markers.get(1).getPosition();
                                float bearing = bearingBetweenLatLngs(markerPos, secondPos);

                                CameraPosition cameraPosition =
                                        new CameraPosition.Builder()
                                                .target(markerPos)
                                                .bearing(bearing)
                                                .zoom(mGoogleMap.getCameraPosition().zoom >= 16 ? mGoogleMap.getCameraPosition().zoom : 16)
                                                .build();
                                mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                updateUI();
                            }
                        });
                    }
                });
            }

        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }

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
    }

    public static String documenttoString(Document doc) {
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

    // 전환점설명 XXXXXXXX라인설명XXXXXXXXXXx전환점설명 XXXXX라인설명//
    // ABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBCDDDDDDDDDDDDD
    // A(좌회전), B(신반포로 23길 직진), C(우회전), D(신반포로 25길 직진)
    // A와 B는 같은 위치에 있을수도 있음.
//    ® 현재 설명이 전환점인 경우
//        ◊ 현재 좌표가 설명좌표 + 2인경우
//           } 새로운 라인을 생성한다.
//           } 새로운 라인에 점 추가
//           } 새로운 설명으로 넘어간다.
//        ◊ 그 외
//           }  해당 라인에 점을 계속 추가한다.
//    ® 현재 설명이 라인인 경우
//        ◊ 다음 설명 좌표(x)가 현재 위치의 +1 인 경우
//           } 새로운 라인을 생성한다.
//           } 새로운 라인에 점 추가
//           } 새로운 설명으로 넘어간다.
//        ◊ 그 외
//           } 해당 라인에 점을 계속 추가한다.
// 가이드 별 폴리라인들 생성
    // 각 폴리라인을 설명 마커 별로 구분하고 , 설명 마커는 라인지점(예 : 신반포로)와 전환점(예 : 좌회전 우회전) 을 구분하여 설정한다.
    public void setGuidePolylines(ArrayList<CustomPoint> stopPoints) {

        PointType beforePointType = null;
        if (stopPoints != null && stopPoints.size() > 0) {
            guideSegmentPolyLines = new ArrayList<>();
            // 각 경유지점에서 설명이 있는 지점에 도달할 경우 분할해서 하나의 경로를 생성한다.
//            CustomPoint nextGuidePoint = j + 1 < guidePointList.size() ? guidePointList.get(j+1) : null;
            int guidePointI = 0; // guidePoint와 같은 좌표를 가진 i 위치
            // polyline 초기화
            Polyline polyline = newPolyLine();
            polyline.setVisible(true);
            for (int i = 0; i < stopPoints.size(); ++i) {
                CustomPoint stopPoint = stopPoints.get(i);
                LatLng stopLatLng = stopPoint.latLng;

                switch (stopPoint.pointType) {
                    // 다음 설명이 지점 마커의 설명인 경우
                    case point:
                        if (beforePointType == null || beforePointType == PointType.line) {
                            beforePointType = PointType.point;
                            // 새로운 설명 라인 추가한다.
                            // 새로 라인 생성
                            polyline = newPolyLine();
                            guideSegmentPolyLines.add(polyline);
                        }
                        break;
                    // 라인 마커인 경우
                    case line:
                        if (beforePointType == null || beforePointType == PointType.point) {
                            beforePointType = PointType.line;
                            // 새로운 설명 라인 추가한다.
                            // 새로 라인 생성
                            polyline = newPolyLine();
                            guideSegmentPolyLines.add(polyline);
                        }
                        break;
                }
                // polyline에 점 추가
                updatePolyLine(polyline, stopLatLng);
            }
        }
    }

    void redirectMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
//        googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
//
//        // 카메라 좌표를 서울역 근처로 옮긴다.
//        googleMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL_STATION) //위도 경도
//        );
//
//        // 구글지도에서의 zoom 레벨은 1~23 까지 가능하다
//        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
//        googleMap.animateCamera(zoom); // moveCamera는 바로 변경하지만, animateCamera()는 근거리에서는 부드럽게 변경합니다.
//
//        // marker 표시
//        // marker의 위치, 타이틀, 짧은 설명 추가
//        MarkerOptions marker = new MarkerOptions();
//        marker.position(SEOUL_STATION).title("서울역").snippet("Seoul Station");
//        googleMap.addMarker(marker).showInfoWindow(); // 마커 추가, 화면에 출력
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                performFindRoute(start_poi_name, dest_poi_name);
            }
        }).start();
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

    private Polyline addPolyLineUsingGoogleMap(ArrayList<LatLng> list) {
        Polyline polyline = mGoogleMap.addPolyline(new PolylineOptions().geodesic(true).color(Color.RED).width(5).addAll(list));
        return polyline;
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
        for (Marker marker : this.descriptorMarkers) {
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
    }

    private BitmapDescriptor getBitmapDescriptor(int id) {
        Drawable vectorDrawable = TrackRealTImeActivity.this.getDrawable(id);
        int h = vectorDrawable.getIntrinsicHeight();
        int w = vectorDrawable.getIntrinsicWidth();
        vectorDrawable.setBounds(0, 0, w, h);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bm);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putParcelable(LOCATION_BEFORE_KEY, mBeforeLocation);
        savedInstanceState.putParcelable(LOCATION_MORE_BEFORE_KEY, mMoreBeforeLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        savedInstanceState.putString(BEFORE_UPDATED_TIME_STRING_KEY, mBeforeUpdateTime);
        savedInstanceState.putString(MORE_BEFORE_UPDATED_TIME_STRING_KEY, mMoreBeforeUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStop() {
        animator.stopAnimation();
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // 현재 사용한 시각 갱신
        // 이전 사용한 시각 갱신
        if (mLastUpdateTime != null) {
            if (mBeforeUpdateTime != null) {
                mMoreBeforeUpdateTime = mBeforeUpdateTime;
            }
            mBeforeUpdateTime = mLastUpdateTime;
        }

        // 이전과 그보다 더이전 위치 갱신
        if (mCurrentLocation != null) {
            if (mBeforeLocation != null) {
                mMoreBeforeLocation = mBeforeLocation;
            }
            mBeforeLocation = mCurrentLocation;
        }
        if (mCurrentLocation == null) {
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
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            updateUI();
        }
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
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
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void updateUI() {
        if (mCurrentLocation != null && trackingCycleMarker != null) {
            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            trackingCycleMarker.setPosition(latLng);
            if (polyLine != null) {
                updatePolyLine(latLng);
            }
            if (mGoogleMap != null) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            }
            Log.d(TAG, "업데이트 UI");
        }
    }

    private Polyline initializePolyLine() {
        //polyLinePoints = new ArrayList<LatLng>();
        if (polyLine != null) {
            polyLine.remove();
        }
        PolylineOptions rectOptions = new PolylineOptions().geodesic(true).color(Color.CYAN);
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

    /**
     * Add the marker to the polyline.
     */
    private void updatePolyLine(Polyline polyLine, LatLng latLng) {
        List<LatLng> points = polyLine.getPoints();
        points.add(latLng);
        polyLine.setPoints(points);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "connection suspended");
        Toast.makeText(this, "connection suspended",
                Toast.LENGTH_SHORT).show();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
        Toast.makeText(this, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
        if (resolvingError) {
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                resolvingError = true;
                connectionResult.startResolutionForResult(TrackRealTImeActivity.this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                mGoogleApiClient.connect();
            }
        } else {
            // deprecated 해결
            // http://stackoverflow.com/questions/31016722/googleplayservicesutil-vs-googleapiavailability
            GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
            int result = googleAPI.isGooglePlayServicesAvailable(this);
            if (result != ConnectionResult.SUCCESS) {
                if (googleAPI.isUserResolvableError(result)) {
                    Dialog errorDialog = googleAPI.getErrorDialog(TrackRealTImeActivity.this, result, REQUEST_RESOLVE_ERROR);
                    errorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            resolvingError = false;
                        }
                    });
                    errorDialog.show();
                    resolvingError = true;
                }
            }
        }
    }

    private void showToastMessage(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        // it happens
        if (location == null) {
            Log.d(TAG, "location이 없습니다");
            showToastMessage("location이 없습니다");
            return;
        }
        // all location should have an accuracy
        if (!location.hasAccuracy()) {
            Log.d(TAG, "정확도가 없습니다");
            showToastMessage("정확도가 없습니다.");
            return;
        }
        if (polyLine != null) {
            // 현재 사용한 시각 갱신
            // 이전 사용한 시각 갱신
            if (mLastUpdateTime != null) {
                if (mBeforeUpdateTime != null) {
                    mMoreBeforeUpdateTime = mBeforeUpdateTime;
                }
                mBeforeUpdateTime = mLastUpdateTime;
            }
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

            // 이전과 그보다 더이전 위치 갱신
            if (mCurrentLocation != null) {
                if (mBeforeLocation != null) {
                    mMoreBeforeLocation = mBeforeLocation;
                }
                mBeforeLocation = mCurrentLocation;
            }

            mCurrentLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            // 길 위에 있는지 확인
            if (isLocationOnPath(latLng, realtimeAllPolylines)) {
                showToastMessage("길 위에 있습니다");
                location = snapOnRoad(location, realtimeAllPolylines);
                mCurrentLocation = location;
                latLng = new LatLng(location.getLatitude(), location.getLongitude());
                double currentTotalRidingDistance = 0; // 현재 총 주행 거리

                // lastSegment가 있었던 경우라면 해당 폴리라인 인덱스부터 현재 위치가 있는 폴리라인을 찾는다.
                if(lastSegmentIndex != null) {
                    // 이전의 lastSegmentIndex의 guideSegmentPolyline에 현재 위치가 있는지 확인
                    if(isLocationOnPath(latLng, guideSegmentPolyLines.get(lastSegmentIndex))) {
                        updateSegmentDistanceAndLineInfoOfTextView(lastSegmentIndex, latLng);
                    } else if(lastSegmentIndex + 1 < guideSegmentPolyLines.size() && isLocationOnPath(latLng, guideSegmentPolyLines.get(lastSegmentIndex + 1))) {
                        updateSegmentDistanceAndLineInfoOfTextView(lastSegmentIndex + 1, latLng);
                    } else {
                        for (int i = 0; i < guideSegmentPolyLines.size(); ++i) {
                            Integer currentDistance = distanceList.get(i);
                            if (currentDistance != null) {
                                currentTotalRidingDistance += currentDistance;
                            }
                            if (isLocationOnPath(latLng, guideSegmentPolyLines.get(i))) {
                                // 텍스트뷰에 있는 정보 업데이트
                                updateSegmentDistanceAndLineInfoOfTextView(i, latLng);
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < guideSegmentPolyLines.size(); ++i) {
                        Double currentDistance = realTimedistanceList.get(i);
                        if (currentDistance != null) {
                            currentTotalRidingDistance += currentDistance;
                        }
                        if (isLocationOnPath(latLng, guideSegmentPolyLines.get(i))) {
                            showToastMessage(i+"번째에 있다");
                            // 텍스트뷰에 있는 정보 업데이트
                            updateSegmentDistanceAndLineInfoOfTextView(i, latLng);
                        }
                    }
                }
            } else {
                Toast.makeText(this, "벗어났습니다", Toast.LENGTH_SHORT).show();
                ++over_location_count;
                if (over_location_count >= 3 && over_location_count % 3 == 0 && !isShowCheckingChangeRouteDialog) {
                    // 3번 연속으로 범위가 벗어난 경우 잠깐 튄것이 아니라고 판단한다.

                    // 위치 벗어남을 알림. 다시 길을 찾을지를 문의하는 창을 띄움.
                    checkRefindRouteToDestinationFromCurrent();
                }
            }
        }
        updateUI();
    }

    // 지도위에 표시되는 거리 업데이트 및 텍스트 업데이트
    void updateSegmentDistanceAndLineInfoOfTextView(int i, LatLng latLng) {
        final int distanceIndex = i;

        if (lastSegmentIndex == null) {
            lastSegmentIndex = i;
        } else if (lastSegmentIndex != i) {
            lastSegmentIndex = i;
            currentSegmentDistance = 0;
        }

        double betweenCurrentAndBeforeLocation;
        // 이전의 위치를 포함해서 현재의 latlng 지점을 추가해서 거리를 갱신한다.
        if(mBeforeLocation != null) {
            betweenCurrentAndBeforeLocation = getDistance(mBeforeLocation, latLng);
        } else {
            betweenCurrentAndBeforeLocation = 0;
        }
        updateSegmentDistance(betweenCurrentAndBeforeLocation);
        updateTotalDistance(betweenCurrentAndBeforeLocation);

        if(realTimedistanceList.get(i) != null) {
            currentSegmentDistance += realTimedistanceList.get(i);
        }
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // distance의 경우 출발점은 라인인덱스와 같다.
                String remaingFutureFirstText = distanceIndex < realTimedistanceList.size() ? String.valueOf(realTimedistanceList.get(distanceIndex)) : null;
                guideTextVIew.setText(remaingFutureFirstText);
                Integer direction = realTimedirectionList.get(distanceIndex);
                setDirectionImage(direction, guideImageView);
                setDirectionImage(direction, turnGuideFirstImage);
                remainingFutureFirstText.setText(remaingFutureFirstText);
                if(distanceIndex + 1 < realTimedistanceList.size()) {
                    remainingFutureSecondText.setText( String.valueOf(realTimedistanceList.get(distanceIndex + 1)));
                    direction = realTimedirectionList.get(distanceIndex + 1);
                    setDirectionImage(direction, turnGuideSecondImage);
                    if(distanceIndex + 2 < realTimedistanceList.size()) {
                        remainingFutureThirdText.setText( String.valueOf(realTimedistanceList.get(distanceIndex + 2)));
                        direction = realTimedirectionList.get(distanceIndex + 2);
                        setDirectionImage(direction, turnGuideThirdImage);
                    } else {
                        thirdFutureGuideLayout.setVisibility(View.GONE);
                    }
                } else {
                    secondFutureGuideLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    void updateSegmentDistance(double distance) {
        currentSegmentDistance += distance;
    }

    void updateTotalDistance(double distance) {
        currentTotalRidingDistance += distance;
    }

    double getDistance(Location beforelocation, LatLng latLng) {
        LatLng beforeLatLng = getLatLngFromLocation(beforelocation);
        return getDistanceBetweenPoints(beforeLatLng, latLng);
    }

    LatLng getLatLngFromLocation(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    // 거리들 가져옴.
    public double getDistanceBetweenPoints(LatLng from, LatLng to) {
        return SphericalUtil.computeDistanceBetween(from, to);
    }


    // 경로를 재설정할지 정하는 다이얼로그 띄움.
    private void checkRefindRouteToDestinationFromCurrent() {
        isShowCheckingChangeRouteDialog = true;
        AlertDialog.Builder gsDialog = new AlertDialog.Builder(this);
        gsDialog.setTitle("위치 벗어남");
        gsDialog.setMessage("경로를 다시 설정하시겠습니까?");
        gsDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                TMapPOIItem tMapPOIItem = null;
                isShowCheckingChangeRouteDialog = false;
                lastSegmentIndex = null;
                currentTotalRidingDistance = 0;
                currentSegmentDistance = 0;
                try {
                    // 위치 변경 추적 중단
                    if (mGoogleApiClient.isConnected()) {
                        stopLocationUpdates();
                    }
                    over_location_count = 0;
                    tMapPOIItem = getTMapPOIItemUsingCurrentLocation(mCurrentLocation);
                    performFindRoute(tMapPOIItem.getPOIName(), dest_poi_name);
                } catch (ParserConfigurationException | SAXException | IOException e) {
                    e.printStackTrace();
                }
            }
        })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        isShowCheckingChangeRouteDialog = false;
                    }
                }).create().show();
    }

    TMapPOIItem getTMapPOIItemUsingCurrentLocation(Location location) throws ParserConfigurationException, SAXException, IOException {
        AppConfig.initializeTMapTapi(this);
        TMapData tmapData = new TMapData();
        String tmapaddress = tmapData.convertGpsToAddress(location.getLatitude(), location.getLongitude());
        ArrayList<TMapPOIItem> arTMapPOIITtem = tmapData.findAddressPOI(tmapaddress);
        if (arTMapPOIITtem.size() > 0) {
            return arTMapPOIITtem.get(0);
        }
        return null;
    }

    void setDirectionImage(Integer direction, ImageView guideImageView) {
        if (direction != null) {
            Log.d(TAG, "direction : " + directionMap.get(direction));
            switch (direction) {
                case -1: // 아무것도 없는 경우
                    guideImageView.setImageDrawable(ContextCompat.getDrawable(TrackRealTImeActivity.this, R.drawable.ic_arrow_upward_black_24dp));
                    break;
                case 11: //직진
                    guideImageView.setImageDrawable(ContextCompat.getDrawable(TrackRealTImeActivity.this, R.drawable.ic_arrow_upward_black_24dp));
                    break;
                case 12: //좌회전
                    guideImageView.setImageDrawable(ContextCompat.getDrawable(TrackRealTImeActivity.this, R.drawable.ic_left_arrow_black_24dp));
                    break;
                case 13: //우회전
                    guideImageView.setImageDrawable(ContextCompat.getDrawable(TrackRealTImeActivity.this, R.drawable.ic_arrow_right_black_24dp));
                    break;
                default:
                    guideImageView.setImageDrawable(ContextCompat.getDrawable(TrackRealTImeActivity.this, R.drawable.ic_arrow_upward_black_24dp));
                    break;
            }
        }
    }

    private LatLng findNearestPoint(final LatLng p, final LatLng start, final LatLng end) {
        if (start.equals(end)) {
            return start;
        }

        final double s0lat = Math.toRadians(p.latitude);
        final double s0lng = Math.toRadians(p.longitude);
        final double s1lat = Math.toRadians(start.latitude);
        final double s1lng = Math.toRadians(start.longitude);
        final double s2lat = Math.toRadians(end.latitude);
        final double s2lng = Math.toRadians(end.longitude);

        double s2s1lat = s2lat - s1lat;
        double s2s1lng = s2lng - s1lng;
        final double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
                / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);
        if (u <= 0) {
            return start;
        }
        if (u >= 1) {
            return end;
        }

        return new LatLng(start.latitude + (u * (end.latitude - start.latitude)),
                start.longitude + (u * (end.longitude - start.longitude)));
    }

    boolean isLocationOnPath(LatLng latLng, Polyline polyline) {
        // 현재 경로에 있는지 확인
        return PolyUtil.isLocationOnPath(latLng, polyline.getPoints(), true, TRACK_IN_TOLERANCE);
    }

    // location기반으로 마커 위치를 도로(polyline)에 매칭시키게끔 변경한다.
    Location snapOnRoad(Location currentLocation, Polyline polyline) {
        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        LatLng nearestPoint = findNearestPoint(latLng, polyline.getPoints().get(0), polyline.getPoints().get(polyline.getPoints().size() - 1));
        currentLocation.setLatitude(nearestPoint.latitude);
        currentLocation.setLongitude(nearestPoint.longitude);
        return currentLocation;
    }

    // 마지막 위치 기반으로 애니메이션 시작.. 도중에 현재 위치를 찾으면 해당 도로에 snap한다.
    void cycleAnimateStart(Location lastLocation, Polyline polyline, int speed) {

    }

    // 새로운 폴리라인 생성
    private Polyline newPolyLine() {
        PolylineOptions rectOptions = new PolylineOptions().geodesic(true).color(Color.BLUE);
        Polyline polyline = mGoogleMap.addPolyline(rectOptions);
        polyline.setVisible(false);
        return polyline;
    }

    public class Animator implements Runnable {

        private static final int ANIMATE_SPEEED = 1500;
        private static final int ANIMATE_SPEEED_TURN = 1000;
        private static final int BEARING_OFFSET = -90;

        private final Interpolator interpolator = new LinearInterpolator();

        int movingCurrentMarkerIndex = 0;
        int descriptorMarkerIndex = 0;

        float zoom = 15.5f;
        boolean upward = true;

        long start = SystemClock.uptimeMillis();

        LatLng endLatLng = null;
        LatLng beginLatLng = null;

        boolean showPolyline = false;

        private Marker trackingMarker;
        private Polyline polyLine;

        public void reset() {
            resetMarkers();
            start = SystemClock.uptimeMillis();
            movingCurrentMarkerIndex = 0;
            descriptorMarkerIndex = 1;
            endLatLng = getEndLatLng();
            beginLatLng = getBeginLatLng();
        }

        public void stop() {
            if (trackingMarker != null) {
                trackingMarker.remove();
            }
            if (animator != null) {
                mHandler.removeCallbacks(animator);
            }
        }

        public void initialize(boolean showPolyLine) {
            reset();
            this.showPolyline = showPolyLine;

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
            Drawable vectorDrawable = TrackRealTImeActivity.this.getDrawable(id);
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
            if (trackingMarker != null) {
                trackingMarker.remove();
            }
            trackingMarker = mGoogleMap.addMarker(new MarkerOptions().position(markerPos)
                    .title("접니다")
                    .snippet("자전거에요")
                    .anchor(0.5f, 0.5f)
                    .rotation(bearing + BEARING_OFFSET)
                    .flat(true));
            trackingMarker.setIcon(getBitmapDescriptor(R.drawable.directionarrow));

            CameraPosition cameraPosition =
                    new CameraPosition.Builder()
                            .target(markerPos)
                            .bearing(bearing)
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

            if (movingCurrentMarkerIndex == 0) {
                highLightMarker(0, 0);
            }

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

                    // 회전 정보(좌회전 우회전)를 읽어서 가이드 뷰에 표시
                    Integer direction = directionList.get(descriptorMarkerIndex);
                    Integer distance = distanceList.get(descriptorMarkerIndex);
                    setDirectionImage(direction, guideImageView);
                    setDirectionImage(direction, turnGuideFirstImage);
                    displayRemainingDistance(distance, remainingFutureFirstText);
                    // 사이즈가 2면 인덱스는 0과 1만 있으므로 최소한 하나 더 커야한다.
                    if (directionList.size() > descriptorMarkerIndex + 1) {
                        secondFutureGuideLayout.setVisibility(View.VISIBLE);
                        direction = directionList.get(descriptorMarkerIndex + 1);
                        distance = distanceList.get(descriptorMarkerIndex + 1);
                        setDirectionImage(direction, turnGuideSecondImage);
                        displayRemainingDistance(distance, remainingFutureSecondText);
                    } else {
                        secondFutureGuideLayout.setVisibility(View.GONE);
                    }

                    if (directionList.size() > descriptorMarkerIndex + 2) {
                        thirdFutureGuideLayout.setVisibility(View.VISIBLE);
                        direction = directionList.get(descriptorMarkerIndex + 2);
                        distance = distanceList.get(descriptorMarkerIndex + 2);
                        setDirectionImage(direction, turnGuideThirdImage);
                        displayRemainingDistance(distance, remainingFutureThirdText);
                    } else {
                        thirdFutureGuideLayout.setVisibility(View.GONE);
                    }

                    guideTextVIew.setText(descriptorMarkers.get(descriptorMarkerIndex).getSnippet());
                    if (highLighted) {
                        ++descriptorMarkerIndex;
                    }

                    CameraPosition cameraPosition =
                            new CameraPosition.Builder()
                                    .target(end) // changed this...
                                    .bearing(bearingL)
                                    .zoom(mGoogleMap.getCameraPosition().zoom)
                                    .build();

                    mGoogleMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(cameraPosition),
                            ANIMATE_SPEEED_TURN,
                            null
                    );

                    trackingMarker.setRotation(bearingL);

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

        void displayRemainingDistance(Integer distance, TextView distanceTextView) {
            if (distance != null) {
                // 남은 거리 출력
                distanceTextView.setText(getString(R.string.remaining_distance, distance));
            }
        }
    }
}
