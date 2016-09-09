package com.nagnek.bikenavi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class POISearchActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    String[] myDataSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poisearch);
        mRecyclerView = (RecyclerView) findViewById(R.id.poi_list_recycler_view);

        if(mRecyclerView != null) {
            // 리사이클러뷰의 레이아웃 사이즈가 바뀌지 않을 때 이 설정을 사용해 퍼포먼스를 향상시킨다.
            mRecyclerView.setHasFixedSize(true);

            // 리니어 레이아웃 매니저를 사용한다.
            mLayoutManager = new LinearLayoutManager(this);
            mRecyclerView.setLayoutManager(mLayoutManager);

            // 어댑터를 설정한다.
            mAdapter = new POIAdapter(myDataSet);
            mRecyclerView.setAdapter(mAdapter);
        }
    }
}
