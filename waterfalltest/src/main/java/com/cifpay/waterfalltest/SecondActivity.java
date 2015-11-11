package com.cifpay.waterfalltest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.cifpay.waterfalltest.utils.ImageLoader;
import com.cifpay.waterfalltest.utils.Images;
import com.cifpay.waterfalltest.view.WaterScrollView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class SecondActivity extends Activity {

    private List<Integer> list = new ArrayList<Integer>();
    private WaterScrollView myScrollView;
    private String[] imageUrls;
    private ImageLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        Random random = new Random();
        for (int i = 0; i < 500; i++) {
            list.add(random.nextInt(200) + 200);
        }

        imageUrls = Images.imageUrls;
        loader = new ImageLoader(SecondActivity.this);

        myScrollView = (WaterScrollView) findViewById(R.id.myScrollView);
        myScrollView.setAdapter(new MyAdapter());
        myScrollView.setOnItemClickListener(new WaterScrollView.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Toast.makeText(SecondActivity.this, String.valueOf(position), Toast.LENGTH_SHORT).show();
            }
        });

    }


    class MyAdapter implements WaterScrollView.WaterAdapter {

        @Override
        public int getPagerCount() {
            return imageUrls.length;
        }

        @Override
        public View getView(int columnWidth, int position) {
            View view = View.inflate(SecondActivity.this, R.layout.item, null);
           ImageView imageView = (ImageView) view.findViewById(R.id.iv);
            loader.display(imageView,imageUrls[position],ImageLoader.RELATIVE_WIDTH,columnWidth-150);
//            imageView.setImageUrl(imageUrls[position],imageLoader);
            return view;
        }

        @Override
        public void loadNextPager() {
            System.out.println("加载下一页");
//            list = getList();
            myScrollView.notifyDataChanage();
        }
    }

    private List<Integer> getList() {
        List<Integer> data = new ArrayList<Integer>();
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            data.add(random.nextInt(200) + 200);
        }
        return data;
    }
}
