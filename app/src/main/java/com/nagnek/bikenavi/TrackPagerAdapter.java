/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Created by user on 2016-10-27.
 */

public class TrackPagerAdapter extends FragmentPagerAdapter {
    public TrackListOfRecentUsedFragment recentTrackFragment;
    public TrackListOfBookmarkedFragment trackListOfBookmarkedFragment;
    String tabTitles[] = new String[]{"최근 검색", "즐겨 찾기"};
    Context context;

    public TrackPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public int getCount() {
        return tabTitles.length;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                recentTrackFragment = new TrackListOfRecentUsedFragment();
                return recentTrackFragment;
            case 1:
                trackListOfBookmarkedFragment = new TrackListOfBookmarkedFragment();
                return trackListOfBookmarkedFragment;
        }

        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // item 위치에 따른 title 생성
        return tabTitles[position];
    }

    public View getTabView(int position) {
        View tab = LayoutInflater.from(context).inflate(R.layout.custom_tab, null);
        TextView tv = (TextView) tab.findViewById(R.id.custom_text);
        tv.setText(tabTitles[position]);
        return tab;
    }
}
