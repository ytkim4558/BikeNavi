package com.nagnek.bikenavi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.RelativeLayout;

import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;

public class MainActivity extends AppCompatActivity implements LocationListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

        RelativeLayout relativeLayout = new RelativeLayout(this);
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
        String locationProvider = locationManager.getBestProvider(new Criteria(), true);
        Location cur_locatoin = locationManager.getLastKnownLocation(locationProvider);
        tMapView.setCenterPoint(cur_locatoin.getLongitude(), cur_locatoin.getLatitude());
        TMapPoint source = new TMapPoint(cur_locatoin.getLatitude(), cur_locatoin.getLongitude());
        TMapPoint dest = new TMapPoint(cur_locatoin.getLatitude()+0.1, cur_locatoin.getLongitude()+0.1);
        TMapData tmapData = new TMapData();
        tmapData.findPathData(source, dest, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine tMapPolyLine) {
                tMapView.addTMapPath(tMapPolyLine);
            }
        });

        relativeLayout.addView(tMapView);
        setContentView(relativeLayout);
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
}
