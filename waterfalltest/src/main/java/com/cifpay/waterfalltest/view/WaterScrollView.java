package com.cifpay.waterfalltest.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cifpay.waterfalltest.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 作者： XuDiWei
 * <p/>
 * 日期：2015/8/18  15:37.
 * <p/>
 * 文件描述:    瀑布流
 */
public class WaterScrollView extends ScrollView implements View.OnClickListener {

    /**
     * 第一列容器
     */
    private LinearLayout linearLayout1;

    /**
     * 第二列的容器
     */
    private LinearLayout linearLayout2;

    /**
     * 是否第一次加载
     */
    private boolean isFristLoad = true;

    /**
     * 列的宽度
     */
    private int columnWidth;

    /**
     * 列1的高度
     */
    private int column1Height;

    /**
     * 列2的高度
     */
    private int column2Height;

    /**
     * 自增长索引
     */
    private int autoIncrementPosition = 0;
    /**
     * 条目点击事件
     */
    private OnItemClickListener onItemClickListener;

    /**
     * 适配器
     */
    private WaterAdapter adapter;
    private Rect rect;

    public WaterScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initView();
    }

    /**
     * 模拟数据
     */
    private void initView() {
        rect = new Rect();
        this.getHitRect(rect);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (isFristLoad) {
            LinearLayout llContainer = (LinearLayout) getChildAt(0);
            linearLayout1 = (LinearLayout) llContainer.findViewById(R.id.ll1);
            linearLayout2 = (LinearLayout) llContainer.findViewById(R.id.ll2);
            columnWidth = linearLayout1.getWidth();
            isFristLoad = false;

        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        loadMore();
        super.onWindowFocusChanged(hasWindowFocus);
    }

    public void loadMore() {
        if (null == adapter) {
            return;
        }
        /**
         * 获取当前页数量的数量
         */
        int count = adapter.getPagerCount();
        View view = null;
        for (int i = 0; i < count; i++) {
            if (column1Height <= column2Height) {
                view = adapter.getView(columnWidth, i);
                view.setTag(autoIncrementPosition++);
                linearLayout1.addView(view);
                view.measure(0, 0);
                column1Height += view.getMeasuredHeight();
//                System.out.println(column1Height);
            } else {
                view = adapter.getView(columnWidth, i);
                view.setTag(autoIncrementPosition++);
                linearLayout2.addView(view);
                view.measure(0, 0);
                column2Height += view.getMeasuredHeight();
//                System.out.println(column2Height);
            }
            view.setOnClickListener(this);
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {


        //判断是否滑到了底部
        //获取ScrollView的子控件的高度(ScrollView只有一个直接控件)
        //获取当前ScrollView的的高度
        //获取当前ScrollY的位置
        //ScrollView的子控件高度 ＝ 当前ScrollView的高度 + 当前ScrollY的位置；若成立则滑到底了。
        if (getChildAt(0).getHeight() == getHeight() + getScrollY()) {
            System.out.println("滚到底了");
            if (null != adapter) {
                adapter.loadNextPager();
            }
        }
        super.onScrollChanged(l, t, oldl, oldt);
    }

    /**
     * 数据发生改变
     */
    public void notifyDataChanage() {
        loadMore();
    }


    /**
     * 设置条目点击事件
     *
     * @param listener
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    /**
     * 设置适配器
     *
     * @param adapter
     */
    public void setAdapter(WaterAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onClick(View v) {
        if (null != onItemClickListener) {
            int tag = (int) v.getTag();
            onItemClickListener.onItemClick(tag);
        }
    }

    /**
     * 条目点击事件
     */
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    /**
     * 适配器
     */
    public interface WaterAdapter {
        /**
         * 获取当前分页条目的数量
         *
         * @return
         */
        int getPagerCount();

        /**
         * 获取当前请求的条目
         *
         * @param columnWidth 当前列的宽度
         * @param position    当前view在当前分页中的索引
         * @return 返回一个要添加到容器的view
         */
        View getView(int columnWidth, int position);

        /**
         * 加载下一下页
         */
        void loadNextPager();

    }

}
