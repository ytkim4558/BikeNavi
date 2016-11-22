/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.Activity;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
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
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class TrackRealTImeActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    private static final String TAG = TrackRealTImeActivity.class.getSimpleName();
    private static final int REQUEST_CHECK_SETTINGS = 5;
    private final Handler mHandler = new Handler();
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
    private Marker trackingCycleMarker; // 내위치를 표시해주는 마커
    private TrackRealTImeActivity.Animator animator = new TrackRealTImeActivity.Animator();
    private GoogleMap mGoogleMap;
    private SessionManager session; // 로그인했는지 확인용 변수
    private ArrayList<LatLng> pathStopPointList;    // 출발지 도착지를 포함한 경유지점(위도, 경도) 리스트
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
    private Location mCurrentLocation;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Polyline polyLine;
    private PolylineOptions rectOptions;

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

        sourceAndDest = new ArrayList<TMapPoint>();
        pathStopPointList = new ArrayList<LatLng>();
        markerOptionsArrayList = new ArrayList<MarkerOptions>();

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
        directionMap = new SparseArray<String>();
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
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000); // 수피트 간격에 대한 정확한 위치 업데이트를 반환한다.
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);   // ACCESS_FINE_LOCATION 권한과 관계 있다.
        mLocationRequest.setSmallestDisplacement(10);   //10m 변경될때마다 알림
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
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                mGoogleMap.clear();
                                if (mGoogleMap != null && document != null) {
                                    //마커리스트 초기화
                                    markers.clear();

                                    pathStopPointList.clear();

                                    final NodeList lineList = document.getElementsByTagName("LineString");

                                    for (int i = 0; i < lineList.getLength(); ++i) {
                                        Element item = (Element) lineList.item(i);
                                        String str = HttpConnect.getContentFromNode(item, "coordinates");
                                        if (str != null) {
                                            String[] str2 = str.split(" ");

                                            for (int k = 0; k < str2.length; ++k) {
                                                try {
                                                    String[] e1 = str2[k].split(",");
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
                                    addPolyLineUsingGoogleMap(pathStopPointList);

                                    final NodeList list = document.getElementsByTagName("Placemark");
//                        Log.d("count", "길이" + list.getLength());
                                    int guide_length = 0;
                                    GuideContent.ITEMS.clear();
                                    //마커추가예정 리스트 초기화
                                    markerOptionsArrayList.clear();

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
                                                // 방향전환 추가
                                                String direction = HttpConnect.getContentFromNode(item, "tmap:turnType");
                                                if (direction != null) {
                                                    directionList.add(Integer.valueOf(direction));
                                                } else {
                                                    Log.d(TAG, "distance가 없네? pointIndex : " + String.valueOf(pointIndex));
                                                    directionList.add(null);
                                                }
                                                String distance = HttpConnect.getContentFromNode(item, "tmap:distance");
                                                if (distance != null) {
                                                    distanceList.add(Integer.valueOf(distance));
                                                } else {
                                                    distanceList.add(null);
                                                }

                                                // 좌표 정보 추가
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
                                                // 방향전환 추가
                                                String direction = HttpConnect.getContentFromNode(item, "tmap:turnType");
                                                if (direction != null) {
                                                    directionList.add(Integer.valueOf(direction));
                                                } else {
                                                    directionList.add(11); // 직진
                                                }
                                                String distance = HttpConnect.getContentFromNode(item, "tmap:distance");
                                                if (distance != null) {
                                                    distanceList.add(Integer.valueOf(distance));
                                                } else {
                                                    distanceList.add(null);
                                                }

                                                // 좌표 정보 추가
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

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
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

    private void addPolyLineUsingGoogleMap(ArrayList<LatLng> list) {
        Polyline polyline = mGoogleMap.addPolyline(new PolylineOptions().geodesic(true).color(Color.RED).width(5).addAll(list));
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
            updatePolyLine(latLng);
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
    }

    @Override
    public void onLocationChanged(Location location) {
        // it happens
        if (location == null) {
            return;
        }
        // all location should have an accuracy
        if (!location.hasAccuracy()) {
            return;
        }
        // if its not accurate enough don't use it
        // this value is in meters
        if (location.getAccuracy() > 200) {
            return;
        }

        mCurrentLocation = location;
        updateUI();
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
    }
}
