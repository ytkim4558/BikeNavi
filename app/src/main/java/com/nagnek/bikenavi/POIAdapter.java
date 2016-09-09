package com.nagnek.bikenavi;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by user on 2016-09-09.
 */
public class POIAdapter extends RecyclerView.Adapter<POIAdapter.ViewHolder> {
    private String[] mDataset;

    // 각 아이템에 대한 뷰의 레퍼런스를 제공한다.
    // 복잡한 데이터 아이템은 아이템마다 하나 이상의 뷰를 필요로 하고, 뷰홀더에서 데이터 아이템을 위한 모든 뷰에 대해 접근을 제공한다.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // 각 데이터 아이템은 이 클래스에서 시작한다.
        public TextView mTextView;
        public ViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }
    }

    // 적합한 생성자를 제공한다(데이터셋의 종류에 맞는)
    public POIAdapter(String[] myDataSet) {
        mDataset = myDataSet;
    }

    // 새로운 뷰를 만든다. (레이아웃 매니저에 의해 호출된다)
    @Override
    public POIAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 새로운 뷰를 만든다.
        TextView v = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poi, parent, false);

        // 뷰의 크기, 내부여백, 외부여백 과 레이아웃 인수들을 설정한다.
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // 뷰의 컨텐츠를 교체한다. (레이아웃 매니저에 의해 호출된다)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mTextView.setText(mDataset[position]);
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }


}
