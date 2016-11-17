/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
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
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.app.AppController;
import com.nagnek.bikenavi.guide.GuideContent;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;
import com.nagnek.bikenavi.util.NagneUtil;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.util.HttpConnect;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class TrackRealTImeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = TrackRealTImeActivity.class.getSimpleName();
    private final Handler mHandler = new Handler();
    ArrayList<TMapPoint> sourceAndDest;
    boolean animating; //애니메이션 진행중인지
    TMapPoint mSource;
    TMapPoint mDest;
    String start_poi_name;
    String dest_poi_name;
    private TrackRealTImeActivity.Animator animator = new TrackRealTImeActivity.Animator();
    private GoogleMap mGoogleMap;
    private SessionManager session; // 로그인했는지 확인용 변수
    private ArrayList<LatLng> pathStopPointList;    // 출발지 도착지를 포함한 경유지점(위도, 경도) 리스트
    private ArrayList<MarkerOptions> markerOptionsArrayList;    // 출발지 도착지 사이에 마커 리스트
    private List<Marker> descriptorMarkers = new ArrayList<Marker>(); //markers
    private List<Marker> markers = new ArrayList<Marker>(); //markers
    private SQLiteHandler db;   // sqlite
    private ProgressDialog pDialog; //진행 상황 확인용 다이얼로그

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_real_time);
        AppController.setCurrentActivity(this);
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(getApplicationContext());

        // Session manager
        session = new SessionManager(getApplicationContext());

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

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

        mGoogleMap.clear();

        try {
            ArrayList<TMapPOIItem> poiPositionOfStartItemArrayList = tmapData3.findAddressPOI(startPOIName);
            ArrayList<TMapPOIItem> poiPositionOfDestItemArrayList = tmapData3.findAddressPOI(destPOIName);
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

        performFindRoute(start_poi_name, dest_poi_name);
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

    @Override
    protected void onStop() {
        animator.stopAnimation();
        super.onStop();
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
