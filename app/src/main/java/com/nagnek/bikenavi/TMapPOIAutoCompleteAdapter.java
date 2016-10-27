/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.nagnek.bikenavi.app.AppConfig;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapPOIItem;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by user on 2016-09-10.
 */
public class TMapPOIAutoCompleteAdapter extends BaseAdapter implements Filterable {
    private static final int MAX_RESULTS = 10;
    private static final String TAG = TMapPOIAutoCompleteAdapter.class.getSimpleName();
    private Context mContext;
    private List<TMapPOIItem> poiList = new ArrayList<TMapPOIItem>();

    public TMapPOIAutoCompleteAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return poiList.size();
    }

    @Override
    public TMapPOIItem getItem(int position) {
        return poiList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.simple_dropdown_item_2line, parent, false);
        }
        TextView poiNameView = (TextView) convertView.findViewById(R.id.text1);
        TextView poiAddressView = (TextView) convertView.findViewById(R.id.text2);
        if (poiNameView != null) {
            poiNameView.setText(getItem(position).getPOIName());
            Log.d("tag", "체크");
            Log.d("tag", getItem(position).getPOIName());
        } else {
            Log.d("tag", "메롱" + getItem(position).getPOIName());
        }
        if (poiAddressView != null) {
            poiAddressView.setText(getItem(position).getPOIAddress().replace("null", ""));
            Log.d("tag", "체크");
            Log.d("tag", getItem(position).getPOIAddress());
        } else {
            Log.d("tag", "메롱" + getItem(position).getPOIAddress().replace("null", ""));
        }
        return convertView;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            // constraint : autocompletetextview 에 친 명령어
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    AppConfig.initializeTMapTapi(mContext);
                    List<TMapPOIItem> tMapPOIItems = findAddressList(mContext, constraint.toString());

                    filterResults.values = tMapPOIItems;
                    if (tMapPOIItems != null) {
                        filterResults.count = tMapPOIItems.size();
                    } else {
                        filterResults.count = 0;
                    }
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    poiList = (List<TMapPOIItem>) results.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }

    private List<TMapPOIItem> findAddressList(Context context, String locationName) {
        List<TMapPOIItem> poiItemList = null;
        TMapData tmapData = new TMapData();
        try {
            poiItemList = tmapData.findAddressPOI(locationName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return poiItemList;
    }
}
