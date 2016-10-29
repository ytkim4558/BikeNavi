/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nagnek.bikenavi.customview.DelayAutoCompleteTextView;
import com.nagnek.bikenavi.util.NagneUtil;

public class POISearchFragment extends Fragment implements OnMapReadyCallback {
    static final int SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT = 2; // 장소 검색 화면에서 장소 검색 request code
    static final LatLng SEOUL_STATION = new LatLng(37.555755, 126.970431);
    private static final String TAG = POISearchFragment.class.getSimpleName();
    DelayAutoCompleteTextView searchPoint = null;
    TextInputLayout textInputLayout = null;
    private GoogleMap mGoogleMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
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

        searchPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Performing stop of activity that is not resumed: {com.nagnek.bikenavi/com.nagnek.bikenavi.MainActivity} 에러 방지
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getContext(), SearchActivity.class);
                        intent.putExtra(getResources().getString(R.string.name_purpose_search_point), "");
                        intent.putExtra(getResources().getString(R.string.current_point_text_for_transition), searchPoint.getText().toString());
                        // 화면전환 애니메이션을 생성한다. 트랜지션 이름은 양쪽 액티비티에 선언되어야한다.
                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity(),
                                searchPoint, searchPoint.getTransitionName());
                        getActivity().startActivityForResult(intent, SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT, options.toBundle());
                    }
                }, 300);
            }
        });

        return rootView;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "왓나?");
        Log.d(TAG, "requestCode = " + requestCode + " But, I want this requestCode: " + SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT);
        if (requestCode == SEARCH_INTEREST_POINT_FROM_POI_SEARCH_FRAGMENT) { // 장소검색 요청한게 돌아온 경우
            Log.d(TAG, "SEARCH_INTEREST_POINT_TRACK_SETTING_FRAGMENT");
            if (resultCode == Activity.RESULT_OK) {// 장소 검색 결과 리턴
                String selectPointName = data.getStringExtra(NagneUtil.getStringFromResources(getActivity(), R.string.select_poi_name_for_transition));
                searchPoint.setText(selectPointName);
                String address = data.getStringExtra(NagneUtil.getStringFromResources(getActivity(), R.string.select_poi_address_for_transition));
                double wgs_84x = data.getDoubleExtra(NagneUtil.getStringFromResources(getActivity(), R.string.wgs_84_x), 0.0);
                double wgs_84y = data.getDoubleExtra(NagneUtil.getStringFromResources(getActivity(), R.string.wgs_84_y), 0.0);
                moveCameraToPOIAndDisplay(wgs_84x, wgs_84y, selectPointName, address);
            }
        }
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
        Log.d(TAG, "onMapReady");
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
}
