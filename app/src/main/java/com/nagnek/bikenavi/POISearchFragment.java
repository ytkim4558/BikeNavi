/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nagnek.bikenavi.customview.DelayAutoCompleteTextView;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;

public class POISearchFragment extends Fragment implements OnMapReadyCallback {
    static final LatLng SEOUL_STATION = new LatLng(37.555755, 126.970431);
    private static final String TAG = POISearchFragment.class.getSimpleName();
    DelayAutoCompleteTextView searchPoint = null;
    TextInputLayout textInputLayout = null;
    private GoogleMap mGoogleMap;
    private ProgressDialog progressDialog;
    private SessionManager session; // 로그인했는지 확인용 변수
    private SQLiteHandler db;   // sqlite

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_poisearch, container, false);

        // Get the ViewPager and set it's recentPOIPagerAdapter so that it can display items
        long start = System.currentTimeMillis();
//        MapFragment mapFragment = (MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);
        ((SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map)).getMapAsync(this);
        long end = System.currentTimeMillis();
        Log.d(TAG, "구글맵 로딩 시간 : " + (end - start) / 1000.0);
        searchPoint = (DelayAutoCompleteTextView) rootView.findViewById(R.id.search_point);
        textInputLayout = (TextInputLayout) rootView.findViewById(R.id.ti_layout);

        ProgressBar progressBar1 = (ProgressBar) rootView.findViewById(R.id.pb_loading_indicator1);

        session = new SessionManager(getContext().getApplicationContext());
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(getContext().getApplicationContext());

        // ProgressDialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);    // 백키로 캔슬 가능안하게끔 설정

        setupTmapPOIToGoogleMapAutoCompleteTextView(searchPoint, progressBar1);
        return rootView;
    }

    void moveCameraToPOIAndDisplay(double wgs84_x, double wgs84_y, String poiName, String address) {
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
            MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(wgs84_x, wgs84_y)).title(poiName).snippet(address);
            Marker marker = mGoogleMap.addMarker(markerOptions);
            marker.showInfoWindow();
        }
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
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
        }

        // 내장 확대/축소 컨트롤을 제공
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        mGoogleMap = googleMap;
    }

    // final ArrayList 는 new ArrayList() 형태로 새로 ArrayList를 만드는게 안될 뿐 add 나 remove는 가능하다.
    private void setupTmapPOIToGoogleMapAutoCompleteTextView(final DelayAutoCompleteTextView locationName, final ProgressBar progressBar) {
        locationName.setThreshold(1);
        locationName.setAdapter(new TMapPOIAutoCompleteAdapter(getContext()));
        locationName.setLoadingIndicator(progressBar);
        locationName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 키보드 감추기
                InputMethodManager immhide = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

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
                if (db.checkIfPOIExists(poi.latLng)) {
                    db.updateLastUsedAtPOI(poi.latLng);
                } else {
                    db.addPOI(poi);
                }

                mGoogleMap.clear();

                moveCameraToPOIAndDisplay(wgs84_x, wgs84_y, poiName, address);
            }
        });
    }
}
