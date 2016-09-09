package com.nagnek.bikenavi;

import android.Manifest;
import android.app.ActivityOptions;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapInfo;
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
    ArrayList<String> items;
    ArrayList<String> addressList, addressList2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addressList = new ArrayList<String>();
        addressList2 = new ArrayList<String>();
        setContentView(R.layout.activity_main);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.mapViewLayout);
        final TMapView tMapView = new TMapView(this);

        // 자전거 도로 표출
        tMapView.setBicycleInfo(true);
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
         AutoCompleteTextView start_point = (AutoCompleteTextView) findViewById(R.id.start_point);
        setupAutoCompleteTextView(start_point, addressList);
        AutoCompleteTextView dest_point = (AutoCompleteTextView) findViewById(R.id.dest_point);
        setupAutoCompleteTextView(dest_point, addressList2);

//        searchStartPointButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                EditText editText = (EditText) findViewById(R.id.editText);
//                Intent intent = new Intent(MainActivity.this, POISearchActivity.class);
//                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this, editText, "searchPOIBar");
//                startActivity(intent, options.toBundle());
//            }
//        });

        Button findrouteButton  =(Button) findViewById(R.id.findRouteButton);
        findrouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AutoCompleteTextView startPosition = (AutoCompleteTextView) findViewById(R.id.start_point);
                AutoCompleteTextView destinationPosition = (AutoCompleteTextView) findViewById(R.id.dest_point);
                String start = startPosition.getText().toString();
                String destination = destinationPosition.getText().toString();
                TMapData tmapData3 = new TMapData();
                try {
                    ArrayList<TMapPOIItem> poiItemArrayList = tmapData3.findAddressPOI(start);
                    source = poiItemArrayList.get(0).getPOIPoint();
                    poiItemArrayList = tmapData3.findAddressPOI(destination);
                    dest = poiItemArrayList.get(0).getPOIPoint();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }

                tmapData3.findPathData(source, dest, new TMapData.FindPathDataListenerCallback() {
                    @Override
                    public void onFindPathData(TMapPolyLine tMapPolyLine) {
                        tMapView.addTMapPath(tMapPolyLine);
                        //
                        TMapInfo info = tMapView.getDisplayTMapInfo(tMapPolyLine.getLinePoint());
                        tMapView.setCenterPoint(info.getTMapPoint().getLongitude(), info.getTMapPoint().getLatitude());
                        tMapView.setZoomLevel(info.getTMapZoomLevel());
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
            tmapData.findPathDataWithType(TMapData.TMapPathType.BICYCLE_PATH, source, dest, new TMapData.FindPathDataListenerCallback() {
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
    // final ArrayList 는 new ArrayList() 형태로 새로 ArrayList를 만드는게 안될 뿐 add 나 remove는 가능하다.
    private void setupAutoCompleteTextView(AutoCompleteTextView autoCompleteTextView, final ArrayList<String> addressList) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, addressList);
        autoCompleteTextView.setThreshold(1);
        autoCompleteTextView.setAdapter(adapter);

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                addressList.clear();
                getAddressInfo(s.toString(), addressList);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void getAddressInfo(String locationName, ArrayList addressList) {
        TMapData tmapData = new TMapData();
        try {
            ArrayList<TMapPOIItem> poiItemArrayList = tmapData.findAddressPOI(locationName);

            if(poiItemArrayList != null) {
                for (TMapPOIItem poiItem : poiItemArrayList) {
                    String str = poiItem.getPOIName().toString();
                    addressList.add(str);
                    Log.d("tag", str);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

    }
}
