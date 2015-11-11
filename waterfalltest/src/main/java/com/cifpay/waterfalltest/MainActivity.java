package com.cifpay.waterfalltest;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.cifpay.waterfalltest.utils.ImageLoader;
import com.cifpay.waterfalltest.utils.Images;
import com.huewu.pla.lib.MultiColumnListView;


public class MainActivity extends ActionBarActivity {

    private MultiColumnListView listView;

    private int count = 50;
    private ListView listView1;
    private ImageLoader loader;
    private String[] imageUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loader = new ImageLoader(this);
        imageUrls = Images.imageUrls;


        listView1 = (ListView) findViewById(R.id.listview);
        listView1.setAdapter(new MyAdapter());
        listView1.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });

    }



    class MyAdapter extends BaseAdapter implements View.OnSystemUiVisibilityChangeListener {

        @Override
        public int getCount() {
            return imageUrls.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if(null == convertView){
                convertView = View.inflate(MainActivity.this,R.layout.listview_item,null);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.imageview);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }

            loader.display(holder.imageView, imageUrls[position], ImageLoader.RELATIVE_WIDTH, 300);
//            holder.imageView.setImageUrl(imageUrls[position],imageLoader);
            convertView.setOnSystemUiVisibilityChangeListener(this);
            return convertView;
        }

        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            System.out.println("不可见："+visibility);
        }
    }

    class ViewHolder{
        ImageView imageView;
    }
}
