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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
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
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nagnek.bikenavi.guide.GuideContent;
import com.nagnek.bikenavi.ui.MarkerOverlay;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapKatec;
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
    TMapPoint mSource;
    TMapPoint mDest;
    DelayAutoCompleteTextView start_point, dest_point;
    ArrayList<TMapPoint> sourceAndDest;
    TMapView tMapView;
    TMapTapi tMapTapi;
    private GoogleMap map;
    static final LatLng SEOUL_STATION = new LatLng(37.555755, 126.970431);

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sourceAndDest = new ArrayList<TMapPoint>();

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

    void performFindRoute() {
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

                tmapData3.findPathDataWithType(TMapData.TMapPathType.BICYCLE_PATH, mSource, mDest, new TMapData.FindPathDataListenerCallback() {
                    @Override
                    public void onFindPathData(TMapPolyLine tMapPolyLine) {
                        //tMapView.removeAllMarkerItem();
                        InputMethodManager immhide = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        //tMapView.addTMapPath(tMapPolyLine);

                        //TMapInfo info = tMapView.getDisplayTMapInfo(tMapPolyLine.getLinePoint());
                        tMapPolyLine.setID("cyclePath");
                        //tMapView.setCenterPoint(info.getTMapPoint().getLongitude(), info.getTMapPoint().getLatitude());
                        //tMapView.setZoomLevel(info.getTMapZoomLevel());
                    }
                });

                tmapData3.findPathDataAllType(TMapData.TMapPathType.BICYCLE_PATH, mSource, mDest, new TMapData.FindPathDataAllListenerCallback() {
                    @Override
                    public void onFindPathDataAll(Document document) {
                        final NodeList list = document.getElementsByTagName("Placemark");
                        Log.d("count", "길이" + list.getLength());
                        int guide_length = 0;
                        GuideContent.ITEMS.clear();


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
                                    if (str != null) {
                                        String[] str2 = str.split(" ");

                                        for (int k = 0; k < str2.length; ++k) {
                                            try {
                                                String[] e1 = str2[k].split(",");
                                                TMapPoint point = new TMapPoint(Double.parseDouble(e1[1]), Double.parseDouble(e1[0]));
                                                // 경유지 마커 설정
                                                MarkerOverlay stop = new MarkerOverlay(MainActivity.this, tMapView, description);
                                                stop.setTMapPoint(point);

                                                Bitmap bitmap = getBitmapFromVectorDrawable(MainActivity.this, R.drawable.ic_place_colored_24dp);
                                                stop.setIcon(bitmap);
                                                stop.setID(i + "경유지" + k);

                                                //tMapView.addMarkerItem2(i + "경유지" + k, stop);

                                            } catch (Exception var13) {
                                                ;
                                            }
                                        }
//                                                    tMapView.setOnMarkerClickEvent(new TMapView.OnCalloutMarker2ClickCallback() {
//                                                        @Override
//                                                        public void onCalloutMarker2ClickEvent(String s, TMapMarkerItem2 tMapMarkerItem2) {
//
//                                                        }
//                                                    });
                                    }
                                }
                            } else {
                                Log.d("dd", "공백");
                            }

                        }

                        FragmentManager fragmentManager = getFragmentManager();
                        ItemFragment fragment = new ItemFragment().newInstance(GuideContent.ITEMS.size(), GuideContent.ITEMS);
                        Bundle mBundle = new Bundle();
                        fragment.setArguments(mBundle);
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.replace(R.id.fragment_container, fragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                        Log.d("count", "길이래" + list.getLength());
                        // fragment 제거
//                                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
//                                        if(fragment != null) {
//                                            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
//                                        }
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
                    Log.d("좌표위치", "Lat:" + wgs84_x + ", Long : " + wgs84_y);
                    CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(wgs84_x, wgs84_y));
                    CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);

                    // 카메라 좌표를 검색 지역으로 이동
                    map.moveCamera(center);

                    // animateCamera는 근거리에선 부드럽게 변경한다.
                    map.animateCamera(zoom);

                    map.addMarker(new MarkerOptions().position(new LatLng(wgs84_x, wgs84_y)).title("서울광장"));
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
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
        }

        // 내장 확대/축소 컨트롤을 제공
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        map = googleMap;
    }
}
