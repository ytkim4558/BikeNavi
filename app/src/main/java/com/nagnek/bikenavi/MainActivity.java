package com.nagnek.bikenavi;

import android.Manifest;
import android.app.Activity;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.nagnek.bikenavi.guide.GuideContent;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapInfo;
import com.skp.Tmap.TMapMarkerItem;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;
import com.skp.Tmap.util.HttpConnect;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity implements LocationListener, ItemFragment.OnListFragmentInteractionListener{
    TMapPoint mSource;
    TMapPoint mDest;
    TMapData mTmapData;
    ArrayList<String> mAddressList;
    ArrayAdapter<String> mAdapter;
    SimpleCursorAdapter mSimpleCursorAdapter;
    DelayAutoCompleteTextView start_point, dest_point;
    ArrayList<TMapPoint> sourceAndDest;
    TMapView tMapView;
    boolean bShowGuidance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sourceAndDest = new ArrayList<TMapPoint>();

        setContentView(R.layout.activity_main);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.mapViewLayout);
        tMapView  = new TMapView(this);

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
       start_point = (DelayAutoCompleteTextView) findViewById(R.id.start_point);
        ProgressBar progressBar1 = (ProgressBar) findViewById(R.id.pb_loading_indicator1);
        setupTmapPOIAutoCompleteTextView(start_point, progressBar1, "출발");
        dest_point = (DelayAutoCompleteTextView) findViewById(R.id.dest_point);
        ProgressBar progressBar2 = (ProgressBar) findViewById(R.id.pb_loading_indicator2);
        setupTmapPOIAutoCompleteTextView(dest_point, progressBar2, "도착");

        Button findrouteButton  =(Button) findViewById(R.id.findRouteButton);
        findrouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(start_point != null && dest_point != null) {
                    start_point.clearFocus();
                    dest_point.clearFocus();
                    String start = start_point.getText().toString();
                    String destination = dest_point.getText().toString();
                    TMapData tmapData3 = new TMapData();
                    try {
                        ArrayList<TMapPOIItem> poiItemArrayList = tmapData3.findAddressPOI(start);
                        mSource = poiItemArrayList.get(0).getPOIPoint();
                        poiItemArrayList = tmapData3.findAddressPOI(destination);
                        mDest = poiItemArrayList.get(0).getPOIPoint();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    } catch (SAXException e) {
                        e.printStackTrace();
                    }

                    tmapData3.findPathDataWithType(TMapData.TMapPathType.BICYCLE_PATH, mSource, mDest, new TMapData.FindPathDataListenerCallback() {
                        @Override
                        public void onFindPathData(TMapPolyLine tMapPolyLine) {
                            tMapView.removeAllMarkerItem();
                            InputMethodManager immhide = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                            immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                            tMapView.addTMapPath(tMapPolyLine);
                            //
                            TMapInfo info = tMapView.getDisplayTMapInfo(tMapPolyLine.getLinePoint());
                            tMapView.setCenterPoint(info.getTMapPoint().getLongitude(), info.getTMapPoint().getLatitude());
                            tMapView.setZoomLevel(info.getTMapZoomLevel());
                        }
                    });

                    tmapData3.findPathDataAllType(TMapData.TMapPathType.BICYCLE_PATH, mSource, mDest, new TMapData.FindPathDataAllListenerCallback() {
                        @Override
                        public void onFindPathDataAll(Document document) {
                            final NodeList list = document.getElementsByTagName("Placemark");
                            Log.d("count", "길이"+list.getLength());
                            int guide_length = 0;
                            GuideContent.ITEMS.clear();


                            for(int i = 0; i < list.getLength(); ++i) {
                                Element item = (Element)list.item(i);
                                String description = HttpConnect.getContentFromNode(item, "description");

                                if(description != null) {
                                    Log.d("description", description);
                                    GuideContent.GuideItem guideItem = new GuideContent.GuideItem(String.valueOf(guide_length), description);
                                    GuideContent.ITEMS.add(guideItem);
                                    ++guide_length;
                                } else {
                                    Log.d("dd", "공백");
                                }
                                String str = HttpConnect.getContentFromNode(item, "coordinates");
                                if(str != null) {
                                    String[] str2 = str.split(" ");

                                    for(int k = 0; k < str2.length; ++k) {
                                        try {
                                            String[] e1 = str2[k].split(",");
                                            TMapPoint point = new TMapPoint(Double.parseDouble(e1[1]), Double.parseDouble(e1[0]));
                                            //polyline.addLinePoint(point);
                                        } catch (Exception var13) {
                                            ;
                                        }
                                    }
                                }
                            }
                            Button guide_button  = (Button) findViewById(R.id.guide_button);
                            guide_button.setVisibility(View.VISIBLE);
                            guide_button.setOnClickListener(new Button.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    bShowGuidance = !bShowGuidance;
                                    if(bShowGuidance) {
                                        FragmentManager fragmentManager = getSupportFragmentManager();
                                        ItemFragment fragment = new ItemFragment().newInstance(GuideContent.ITEMS.size(), GuideContent.ITEMS);
                                        Bundle mBundle = new Bundle();
                                        fragment.setArguments(mBundle);
                                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                                        fragmentTransaction.replace(R.id.fragment_container, fragment);
                                        fragmentTransaction.addToBackStack(null);
                                        fragmentTransaction.commit();
                                        Log.d("count", "길이래" + list.getLength());
                                    } else {
                                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                                        if(fragment != null) {
                                            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                                        }
                                    }
                                }
                            });

                        }
                    });
                }
            }
        });

        String locationProvider = locationManager.getBestProvider(new Criteria(), true);

        Location cur_locatoin = locationManager.getLastKnownLocation(locationProvider);
        if(cur_locatoin != null) {
            tMapView.setCenterPoint(cur_locatoin.getLongitude(), cur_locatoin.getLatitude());
            mSource = new TMapPoint(cur_locatoin.getLatitude(), cur_locatoin.getLongitude());
            mDest = new TMapPoint(cur_locatoin.getLatitude() + 0.1, cur_locatoin.getLongitude() + 0.1);
            mTmapData = new TMapData();
            mTmapData.findPathDataWithType(TMapData.TMapPathType.BICYCLE_PATH, mSource, mDest, new TMapData.FindPathDataListenerCallback() {
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
    private void setupTmapPOIAutoCompleteTextView(final DelayAutoCompleteTextView locationName, final ProgressBar progressBar, final String markerTitle) {
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
                //TMapInfo info = tMapView.getDisplayTMapInfo(tMapPolyLine.getLinePoint());
                if(tMapView.getMarkerItemFromID(markerTitle) != null) {
                    tMapView.removeMarkerItem(markerTitle);
                }

                tMapView.addMarkerItem(markerTitle, tItem);
                //ArrayList<TMapMarkerItem2> tMapMarkerItem2s = tMapView.getAllMarkerItem2();
                tMapView.setCenterPoint(tMapPOIItem.getPOIPoint().getLongitude(), tMapPOIItem.getPOIPoint().getLatitude());

            }
        });
    }

    @Override
    public void onListFragmentInteraction(GuideContent.GuideItem item) {

    }
}
