/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.nagnek.bikenavi.app.AppController;

/**
 * Created by user on 2016-10-22.
 */

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private static final String TAG = MyAdapter.class.getSimpleName();
    private static final int TYPE_HEADER = 0;  // Declaring variable to understand which view is being worked on
    // If the view under inflation and population is header or Item
    private static final int TYPE_ITEM = 1;
    private static ClickListener mCallback;
    private static boolean loginState;
    private String mNavTitles[]; // String Array to store the passed titles Value from MainActivity.java
    private int mIcons[];       // Int Array to store the passed icons resource value from MainActivity.java
    private String name;        //String Resource for header View name
    private int profileID;        //int Resource for header view profileID picture
    private String email;       //String Resource for header view email


    MyAdapter(String titles[], int icons[], String name, String email, int profileID, ClickListener clickListener, boolean loginState) { // MyAdapter Constructor with titles and icons parameter
        // titles, icons, name, email, profileID pic are passed from the main activity as we
        mNavTitles = titles;                //have seen earlier
        mIcons = icons;
        this.name = name;
        this.email = email;
        this.profileID = profileID;                     //here we assign those passed values to the values we declared here
        mCallback = clickListener;
        MyAdapter.loginState = loginState;
        //in adapter
    }

    // Creating a ViewHolder which extends the RecyclerView View Holder
    // ViewHolder are used to to store the inflated views in order to recycle them

    public void swap(int icons[], String name, String email) {
        if (icons != null) {
            mIcons = icons;
        }

        this.name = name;
        this.email = email;
        notifyDataSetChanged();
    }

    // 회원가입 / 로그인, 로그아웃 상태 메시지
    public void changeLoginText(String text) {
        this.mNavTitles[3] = text;
    }

    public void changeLoginState(boolean loginState) {
        MyAdapter.loginState = loginState;
    }

    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder");
        if (viewType == TYPE_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false); //Inflating the layout
            ViewHolder vhItem = new ViewHolder(v, viewType); //Creating ViewHolder and passing the object of type view
            return vhItem; // Returning the created object
            //inflate your layout and pass it to view holder
        } else if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.header, parent, false); //Inflating the layout
            ViewHolder vhHeader = new ViewHolder(v, viewType); //Creating ViewHolder and passing the object of type view
            return vhHeader; //returning the object created
        }
        return null;
    }

    //Next we override a method which is called when the item in a row is needed to be displayed, here the int position
    // Tells us item at which position is being constructed to be displayed and the holder id of the holder object tell us
    // which view type is being created 1 for item row
    @Override
    public void onBindViewHolder(MyAdapter.ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder");
        if (holder.holderId == 1) {                              // as the list view is going to be called after the header view so we decrement the
            // position by 1 and pass it to the holder while setting the text and image
            if(position <= mNavTitles.length) {
                holder.textView.setText(mNavTitles[position - 1]); // Setting the text with the array of our Titles
            }
            if(position <= mIcons.length) {
                holder.imageView.setImageResource(mIcons[position - 1]);// Setting the image with array of our icons
            }
            holder.itemView.setTag(position);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) v.getTag();
                    Log.d(TAG, "position 클릭 : " + position);
                    mCallback.onNavItemClicked(position);
                }
            });
        } else {
            holder.profile.setImageResource(profileID);           // Similarly we set the resources for header view
            holder.name.setText(name);
            holder.email.setText(email);
            final LinearLayout backgroundLayout = holder.backGroundLayout;
            Glide.with(AppController.getGlobalApplicationContext())
                    .load(R.drawable.header_top)
                    .asBitmap()
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            Drawable drawable = new BitmapDrawable(resource);
                            backgroundLayout.setBackground(drawable);
                        }
                    });
        }
    }

    // Below first we override the method onCreateViewHolder which is called when the ViewHolder is
    // Created, In this method we inflate the item_row.xml layout if the viewType is Type_ITEM or else we inflate header.xml
    // if the viewType is TYPE_HEADER
    // and pass it to the view holder

    // This method returns the number of items present in the list
    @Override
    public int getItemCount() {
        return mNavTitles.length + 1; // the number of items in the list will be +1 the titles including the header view.
    }

    // With the following method we check what type of view is being passed
    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;

        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }


    // 액티비티는 항상 이 인터페이스를 구현해야 한다.
    public interface ClickListener {
        void onProfileImageClicked(ImageView profileImage);
        void onNavItemClicked(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View itemView;
        int holderId;
        TextView textView;
        ImageView imageView;
        ImageView profile;
        TextView name;
        TextView email;
        LinearLayout backGroundLayout; // 배경

        public ViewHolder(View itemView, int viewType) {                 // Creating ViewHolder Constructor with View and viewType As a parameter
            super(itemView);
            this.itemView = itemView;
            // Here we set the appropriate view in accordance with the the view type as passed when the holder object is created
            Log.d(TAG, "ViewHolder");
            if (viewType == TYPE_ITEM) {
                textView = (TextView) itemView.findViewById(R.id.rowText); // Creating TextView object with the id of textView from item_row.xml
                imageView = (ImageView) itemView.findViewById(R.id.rowIcon);// Creating ImageView object with the id of ImageView from item_row.xml
                holderId = 1;                                               // Setting holder id as 1 as the object being populated are of type item row
            } else {
                name = (TextView) itemView.findViewById(R.id.name);         // Creating Text View object from header.xml for name
                email = (TextView) itemView.findViewById(R.id.email);       // Creating Text View object from header.xml for email
                profile = (ImageView) itemView.findViewById(R.id.circleView);// Creating Image view object from header.xml for profile pic
                profile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCallback.onProfileImageClicked(profile);
                    }
                });
                holderId = 0;                                                // Setting holder id = 0 as the object being populated are of type header view
                backGroundLayout = (LinearLayout) itemView.findViewById(R.id.backGround);
            }
        }
    }
}