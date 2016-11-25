/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2016-10-21.
 * 최근 장소 어댑터
 */

public class POIListOfBookmarkedAdapter extends RecyclerView.Adapter<POIListOfBookmarkedAdapter.BookmarkedPOIListViewHolder> {

    LayoutInflater inflater;
    private Context context;
    private List<POI> bookmarkedPOIList = new ArrayList<>();
    private POIListener poiListener;
    private SQLiteHandler db;   // sqlite
    private SessionManager session; // 로그인했는지 확인용 변수

    // Provide a suitable constructor (depends on the kind of dataset)
    public POIListOfBookmarkedAdapter(Context context, List<POI> poiList, POIListener poiListener) {

        this.context = context;
        this.bookmarkedPOIList = poiList;
        this.poiListener = poiListener;
        inflater = LayoutInflater.from(context);
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(context.getApplicationContext());
        session = new SessionManager(context.getApplicationContext());
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BookmarkedPOIListViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item_bookmarked_poi, parent, false);
        // set the view's size, margins, paddings and layout parameters
        BookmarkedPOIListViewHolder vh = new BookmarkedPOIListViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(BookmarkedPOIListViewHolder holder, final int position) {
        POI currentPOI = bookmarkedPOIList.get(position);
        holder.iv_delete.setTag(position);
        holder.poi_name.setText(currentPOI.name);
        holder.poi_address.setText(currentPOI.address);
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int select_position = (Integer) v.getTag();
                poiListener.poiClickToSet(bookmarkedPOIList.get(select_position));
            }
        });

        holder.iv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int delete_position = (Integer) v.getTag();
                poiListener.latLngToDelete(bookmarkedPOIList.get(delete_position));
                bookmarkedPOIList.remove(delete_position);
                notifyItemRemoved(delete_position);
                //this line below gives you the animation and also updates the
                //list items after the deleted item
                notifyItemRangeChanged(delete_position, getItemCount());
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookmarkedPOIList.size();
    }

    void refresh() {
        this.bookmarkedPOIList = db.getAllLocalUserBookmarkPOI();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    class BookmarkedPOIListViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_delete;
        CardView card_view;
        TextView poi_name; //poiName
        TextView poi_address; //poiAddress

        BookmarkedPOIListViewHolder(View v) {
            super(v);

            card_view = (CardView) v.findViewById(R.id.card_view);
            poi_name = (TextView) v.findViewById(R.id.text_poi_name);
            poi_address = (TextView) v.findViewById(R.id.text_poi_address);
            iv_delete = (ImageView) v.findViewById(R.id.iv_delete);
        }
    }
}
