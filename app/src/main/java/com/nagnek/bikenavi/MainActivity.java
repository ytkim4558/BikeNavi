/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatDrawableManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.nagnek.bikenavi.guide.GuideContent;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapMarkerItem;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapTapi;
import com.skp.Tmap.TMapView;
import com.skp.Tmap.util.HttpConnect;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    static final LatLng SEOUL_STATION = new LatLng(37.555755, 126.970431);
    TMapPoint mSource;
    TMapPoint mDest;
    DelayAutoCompleteTextView start_point, dest_point;
    ArrayList<TMapPoint> sourceAndDest;
    TMapView tMapView;
    TMapTapi tMapTapi;
    private GoogleMap mGoogleMap;
    private PolylineOptions polylineOptions; // polyline option
    private ArrayList<LatLng> pathStopPointList;    // 출발지 도착지를 포함한 경유지점(위도, 경도) 리스트
    private ArrayList<MarkerOptions> markerOptionsArrayList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sourceAndDest = new ArrayList<TMapPoint>();
        pathStopPointList = new ArrayList<LatLng>();
        markerOptionsArrayList = new ArrayList<MarkerOptions>();

        setContentView(R.layout.activity_main);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

        /**
         * 구글맵 생성
         */
        // 구글맵 초기 상태를 설정
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // 티맵 생성 -------------------------
//        tMapView = new TMapView(this);
//
//        // 자전거 도로 표출
//        tMapView.setBicycleInfo(true);
//        tMapView.setSKPMapApiKey("d2bc2636-c213-3bad-9058-7d46cf9f8039");

        // 화면중심을 단말의 현재위치로 이동시켜주는 트래킹모드로 설정한다.
        //tMapView.setTrackingMode(true);
        // 티맵 생성 끝 ---------------------


        //tmapApi 사용
        tMapTapi = new TMapTapi(this);
        tMapTapi.setSKPMapAuthentication("d2bc2636-c213-3bad-9058-7d46cf9f8039");

        start_point = (DelayAutoCompleteTextView) findViewById(R.id.start_point);
        ProgressBar progressBar1 = (ProgressBar) findViewById(R.id.pb_loading_indicator1);
        setupTmapPOIToGoogleMapAutoCompleteTextView(start_point, progressBar1, "출발");
        dest_point = (DelayAutoCompleteTextView) findViewById(R.id.dest_point);

        ProgressBar progressBar2 = (ProgressBar) findViewById(R.id.pb_loading_indicator2);
        setupTmapPOIToGoogleMapAutoCompleteTextView(dest_point, progressBar2, "도착");
    }


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

    void performFindRoute() {
        // 키보드 감추기
        InputMethodManager immhide = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

        // 시작위치. 도착지의 포커스 초기화.
        start_point.clearFocus();
        dest_point.clearFocus();
        String start = start_point.getText().toString();
        String destination = dest_point.getText().toString();
        TMapData tmapData3 = new TMapData();



        try {
            ArrayList<TMapPOIItem> poiPositionOfStartItemArrayList = tmapData3.findAddressPOI(start);
            ArrayList<TMapPOIItem> poiPositionOfDestItemArrayList = tmapData3.findAddressPOI(destination);
            if (poiPositionOfStartItemArrayList != null && poiPositionOfDestItemArrayList != null) {
                mSource = poiPositionOfStartItemArrayList.get(0).getPOIPoint();
                mDest = poiPositionOfDestItemArrayList.get(0).getPOIPoint();

                tmapData3.findPathDataAllType(TMapData.TMapPathType.BICYCLE_PATH, mSource, mDest, new TMapData.FindPathDataAllListenerCallback() {
                    @Override
                    public void onFindPathDataAll(Document document) {
                        final NodeList list = document.getElementsByTagName("Placemark");
                        Log.d("count", "길이" + list.getLength());
                        int guide_length = 0;
                        GuideContent.ITEMS.clear();
                        pathStopPointList.clear();

                        for (int i = 0; i < list.getLength(); ++i) {
                            Element item = (Element) list.item(i);
                            String description = HttpConnect.getContentFromNode(item, "description");

                            if (description != null) {
                                Log.d("description", description);
                                GuideContent.GuideItem guideItem = new GuideContent.GuideItem(String.valueOf(guide_length), description);
                                GuideContent.ITEMS.add(guideItem);
                                ++guide_length;

                                String index = HttpConnect.getContentFromNode(item, "tmap:pointIndex");
                                if (index != null) {
                                    String str = HttpConnect.getContentFromNode(item, "coordinates");
                                    Log.d("tag", "index");
                                    if (str != null) {
                                        String[] str2 = str.split(" ");
                                        Log.d("tag", "str");
                                        for (int k = 0; k < str2.length; ++k) {
                                            try {
                                                String[] e1 = str2[k].split(",");
                                                // 마커 및 path 포인트를 추가하기 위한 위도 경도 생성
                                                LatLng latLng = new LatLng(Double.parseDouble(e1[1]), Double.parseDouble(e1[0]));
                                                // 마커 생성
                                                MarkerOptions marker = new MarkerOptions().title("지점").snippet(description).position(latLng);
                                                // 마커리스트에 추가 (addmarker는 Main 스레드에서만 되므로 이 콜백함수에서 쓸수없다. 따라서 한번에 묶어서 핸들러로 호출한다.)
                                                markerOptionsArrayList.add(marker);
                                                pathStopPointList.add(latLng);
                                                Log.d("tag", "kk");
                                            } catch (Exception var13) {
                                                Log.d("tag", "에러 : " + var13.getMessage());
                                            }
                                        }
                                    }

                                }


                            } else {
                                Log.d("dd", "공백");
                            }

                        }
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(mGoogleMap != null) {
                                    // 경로 찾고나서 경로 지점들의 마커들 추가
                                    for(MarkerOptions markerOptions : markerOptionsArrayList) {
                                        mGoogleMap.addMarker(markerOptions);
                                    }
                                    // 경로 polyline 그리기
                                    addPolyLinetoGoogleMap(pathStopPointList);
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
                        fragmentTransaction.addToBackStack(null);
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

    private void addPolyLinetoGoogleMap(ArrayList<LatLng> list) {
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
                    if(mGoogleMap != null) {
                        mGoogleMap.animateCamera(zoom);
                        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(wgs84_x, wgs84_y)).title(tMapPOIItem.getPOIName()).snippet(tMapPOIItem.getPOIAddress().replace("null", "")));
                    }
                }


//                //TMapInfo info = tMapView.getDisplayTMapInfo(tMapPolyLine.getLinePoint());
//                if (tMapView.getMarkerItemFromID(markerTitle) != null) {
//                    tMapView.removeMarkerItem(markerTitle);
//                }
//
//                tMapView.addMarkerItem(markerTitle, tItem);
//                //ArrayList<TMapMarkerItem2> tMapMarkerItem2s = tMapView.getAllMarkerItem2();
//                tMapView.setCenterPoint(tMapPOIItem.getPOIPoint().getLongitude(), tMapPOIItem.getPOIPoint().getLatitude());

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

}
