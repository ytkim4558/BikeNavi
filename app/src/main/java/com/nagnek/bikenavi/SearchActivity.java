/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.nagnek.bikenavi.customview.DelayAutoCompleteTextView;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = SearchActivity.class.getSimpleName();
    DelayAutoCompleteTextView searchPoint = null;
    TextInputLayout textInputLayout = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        searchPoint = (DelayAutoCompleteTextView) findViewById(R.id.search_point);
        textInputLayout = (TextInputLayout) findViewById(R.id.ti_layout);
        ProgressBar progressBar1 = (ProgressBar) findViewById(R.id.pb_loading_indicator1);

        Intent intent = getIntent();
        if(intent != null) {
            String locationText = intent.getStringExtra(getStringFromResources(R.string.current_point_text_for_transition));
            searchPoint.setText(locationText);
            String search_purpose = intent.getStringExtra(getStringFromResources(R.string.name_purpose_search_point));
            if(search_purpose.equals("출발")) {
                textInputLayout.setHint(getStringFromResources(R.string.hint_start_point));
            } else if(search_purpose.equals("도착")) {
                textInputLayout.setHint(getStringFromResources(R.string.hint_destination));
            }
            setupTmapPOIToGoogleMapAutoCompleteTextView(searchPoint, progressBar1, search_purpose);
        }
    }

    // final ArrayList 는 new ArrayList() 형태로 새로 ArrayList를 만드는게 안될 뿐 add 나 remove는 가능하다.
    private void setupTmapPOIToGoogleMapAutoCompleteTextView(final DelayAutoCompleteTextView locationName, final ProgressBar progressBar, final String searchPurpose) {
        locationName.setThreshold(1);
        locationName.setAdapter(new TMapPOIAutoCompleteAdapter(this));
        locationName.setLoadingIndicator(progressBar);
        locationName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TMapPOIItem tMapPOIItem = (TMapPOIItem) parent.getItemAtPosition(position);
                locationName.setText(tMapPOIItem.getPOIName());

                TMapPoint tMapPoint = tMapPOIItem.getPOIPoint();

                // 위도를 반환
                double wgs84_x = tMapPoint.getLatitude();

                // 경도를 반환
                double wgs84_y = tMapPoint.getLongitude();

                Log.d("tag", "좌표위치 " + "Lat:" + wgs84_x + ", Long : " + wgs84_y);
                LatLng latLng = new LatLng(wgs84_x, wgs84_y);
                CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(wgs84_x, wgs84_y));
                Log.d("tag", "좌표위치 가져옴" + "Lat:" + latLng.latitude + ", Long : " + latLng.longitude);
                CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);

                String poiName = tMapPOIItem.getPOIName();
                String address = tMapPOIItem.getPOIAddress().replace("null", "");

                Intent intent = new Intent();
                intent.putExtra(getStringFromResources(R.string.current_point_text_for_transition), locationName.getText().toString());
                intent.putExtra(getStringFromResources(R.string.select_poi_address_for_transition), address);
                intent.putExtra(getStringFromResources(R.string.select_poi_name_for_transition), poiName);
                intent.putExtra(getStringFromResources(R.string.wgs_84_x), wgs84_x);
                intent.putExtra(getStringFromResources(R.string.wgs_84_y), wgs84_y);
                intent.putExtra(getStringFromResources(R.string.name_purpose_search_point), searchPurpose);
                setResult(RESULT_OK, intent);
                textInputLayout.setHint(null);
                finishAfterTransition();
            }
        });
    }

    @Override
    public void onBackPressed() {
        textInputLayout.setHint(null);
        super.onBackPressed();
    }

    private String getStringFromResources(final int id) {
        return getResources().getString(id);
    }
}
