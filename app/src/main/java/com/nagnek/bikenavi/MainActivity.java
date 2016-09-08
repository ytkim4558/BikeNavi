package com.nagnek.bikenavi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.google.android.gms.common.api.GoogleApiClient;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity implements LocationListener {
    TMapPoint source;
    TMapPoint dest;
    TMapData tmapData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.mapViewLayout);
        final TMapView tMapView = new TMapView(this);
        tMapView.setSKPMapApiKey("d2bc2636-c213-3bad-9058-7d46cf9f8039");

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 10, this);
        Button googleMapButton = (Button) findViewById(R.id.button);
        googleMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });
        Button searchStartPointButton = (Button) findViewById(R.id.search_start_point_button);
        searchStartPointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TMapData tmapData2 = new TMapData();
                TMapData tmapData3 = new TMapData();
                EditText editText = (EditText) findViewById(R.id.editText);
                String start = editText.getText().toString();

                try {
                    ArrayList<TMapPOIItem> poiItemArrayList = tmapData2.findAddressPOI(start);
                    for(TMapPOIItem poiITem:poiItemArrayList) {
                        String str = poiITem.getPOIName().toString();
                        Log.d("tag",str);
                    }
                    TMapPoint tp = poiItemArrayList.get(0).getPOIPoint();
                    source = tp;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });

        Button searchDestPointButton = (Button) findViewById(R.id.search_destination_point_button);
        searchDestPointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TMapData tmapData2 = new TMapData();
                TMapData tmapData3 = new TMapData();
                EditText editText2 = (EditText) findViewById(R.id.editText2);
                String start = editText2.getText().toString();

                try {
                    ArrayList<TMapPOIItem> poiItemArrayList = tmapData2.findAddressPOI(start);
                    for(TMapPOIItem poiITem:poiItemArrayList) {
                        String str = poiITem.getPOIName().toString();
                        Log.d("tag",str);
                    }
                    TMapPoint tp = poiItemArrayList.get(0).getPOIPoint();
                    dest = tp;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });

        Button findrouteButton  =(Button) findViewById(R.id.findRouteButton);
        findrouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TMapData tmapData3 = new TMapData();
                tmapData3.findPathData(source, dest, new TMapData.FindPathDataListenerCallback() {
                    @Override
                    public void onFindPathData(TMapPolyLine tMapPolyLine) {
                        tMapView.addTMapPath(tMapPolyLine);
                    }
                });
            }
        });

        String locationProvider = locationManager.getBestProvider(new Criteria(), true);

        Location cur_locatoin = locationManager.getLastKnownLocation(locationProvider);
        if(cur_locatoin != null) {
            tMapView.setCenterPoint(cur_locatoin.getLongitude(), cur_locatoin.getLatitude());
            source = new TMapPoint(cur_locatoin.getLatitude(), cur_locatoin.getLongitude());
            dest = new TMapPoint(cur_locatoin.getLatitude() + 0.1, cur_locatoin.getLongitude() + 0.1);
            tmapData = new TMapData();
            tmapData.findPathData(source, dest, new TMapData.FindPathDataListenerCallback() {
                @Override
                public void onFindPathData(TMapPolyLine tMapPolyLine) {
                    tMapView.addTMapPath(tMapPolyLine);
                }
            });
        }

        relativeLayout.addView(tMapView);
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // Google Play서비스 접근 승인 요청
//    public GoogleApiClient.Builder setGoogleServiceBuilder() {
//        // Google Api Client 생성
//        GoogleApiClient.Builder
//    }
}
