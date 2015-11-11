package com.cifpay.waterfalltest.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 作者： XuDiWei
 * <p/>
 * 日期：2015/6/15  11:10.
 * <p/>
 * 文件描述:   此框架是用于图片三缓级存的应用,包括.从网络获取图片.本地SD卡获取图片，对图片进行压缩.缓存.旋转角度校正等
 */
public class ImageLoader {

    /**
     * 图片在内存中的缓存容器
     */
    private static LruCache<String, Bitmap> bitmapLruCache;

    private Context mContext;

    /**
     * 缓存目录
     */
    private File mCacheDir = null;

    /**
     * 线程池
     */
    private ExecutorService mPool;

    /**
     * 当前设备的宽
     */
    private final int screenWidth;
    /**
     * 当前设备的高
     */
    private final int screenHeight;

    /**
     * 打印调试
     */
    private boolean debug = false;

    /**
     * 默认的图片id
     */
    private int defaultImage;

    /**
     * 相对宽度,相对宽度计算出缩放比例
     */
    public static int RELATIVE_WIDTH = 2;
    /**
     * 相对高度,相对高度计算出缩放比例
     */
    public static int RELATIVE_HEIGHT = 3;

    /**
     * 相对哪个
     */
    private int relative;

    /**
     * 指定图片的宽度
     */
    private int specifySize;

    /**
     * 指定图片的高度
     */
    private int specifyHeight;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Object[] obj = (Object[]) msg.obj;
            ImageView iv = (ImageView) obj[0];
            String url = (String) obj[1];

            display(iv, url);
        }
    };

    public ImageLoader(Context context) {
        this.mContext = context;

        //内存缓存目录
        if (null == bitmapLruCache) {
            //缓存的大小是当前应用运行环境的8分之1
            int maxSize = (int) (Runtime.getRuntime().maxMemory() / 8);
            bitmapLruCache = new LruCache<String, Bitmap>(maxSize) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getRowBytes() * value.getHeight();
                }
            };
        }

        //本地缓存目录
        mCacheDir = getCacheDir();

        //线程池
        if (null == mPool) {
            mPool = Executors.newFixedThreadPool(3);
        }

        //获取当前设备的屏幕的宽高
        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        screenWidth = manager.getDefaultDisplay().getWidth();
        screenHeight = manager.getDefaultDisplay().getHeight();

    }

    /**
     * 显示图片。传入指定的宽或是高的大小来计算出图片的缩放比例
     * @param view  要显示的控件
     * @param url   图片的url
     * @param relative  相对什么进行计算出图片的缩放比例，可以是RELATIVE_WIDTH;RELATIVE_HEIGHT;
     * @param specifySize  指定图片的宽或是高的大小
     */
    public void display(ImageView view, String url, int relative, int specifySize){
        this.relative = relative;
        this.specifySize = specifySize;
        this.display(view,url);
    }

    /**
     * 显示图片，把url的图片显示到ImageView里
     *
     * @param view 要显示的控件
     * @param url  图片的url
     */
    public void display(ImageView view, String url) {

        //每次都先设置一个默认的图片
        if (defaultImage != 0) {
            view.setImageResource(defaultImage);
        }

        //从内存中获取
        Bitmap bitmapToCache = bitmapLruCache.get(url);
        if (null != bitmapToCache) {
            view.setImageBitmap(bitmapToCache);
            debugLog("从内存中获取");
            return;
        }

        //从本地中获取
        Bitmap bitmapToLocal = getBitmapFromLocal(url);
        if (null != bitmapToLocal) {
            view.setImageBitmap(bitmapToLocal);
            debugLog("从本地中获取");
            return;
        }

        if (url.startsWith("http://")) {
            //从网络中获取
            getBitmapFromNet(view, url);
            debugLog("从网络中获取");

        } else {
            //从SD卡中获取
            getBitmapFromSDCard(view, url);
            debugLog("从SD卡中获取");
        }

    }

    /**
     * 从SD卡中获取一张图片
     * @param View
     * @param url
     */
    private void getBitmapFromSDCard(ImageView View, String url) {
        mPool.execute(new RequestBitmapFromSDCard(View, url));
    }

    /**
     * 从SD卡获取图片。虽然可以放在主线程上操作。但是为了流畅度还是放在子线程中
     */
    private class RequestBitmapFromSDCard implements Runnable {
        private String url;
        private ImageView view;

        RequestBitmapFromSDCard(ImageView view, String url) {
            this.url = url;
            this.view = view;

        }

        @Override
        public void run() {
            try {
                FileInputStream fis = new FileInputStream(new File(url));
                Bitmap bitmap = compressImage(fis);

                //存到内存
                bitmapLruCache.put(url, bitmap);
                //存到本地
                setBitmapToLocal(url, bitmap);

                Message message = mHandler.obtainMessage();
                message.obj =  new Object[]{view, url};
                mHandler.sendMessage(message);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 从网络获取图片
     *
     * @param view
     * @param url
     */
    private void getBitmapFromNet(ImageView view, String url) {
        debugLog(url);
        mPool.execute(new RequestBitmapFromNetTask(view, url));

    }

    /**
     * 从网络获取图片的线程
     */
    private class RequestBitmapFromNetTask implements Runnable {

        private String url;
        private ImageView iv;

        RequestBitmapFromNetTask(ImageView iv, String url) {
            this.url = url;
            this.iv = iv;
        }

        @Override
        public void run() {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setReadTimeout(5000);
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    InputStream is = conn.getInputStream();

                    Bitmap bitmap = compressImage(is);
                    //存到缓存(内存)
                    bitmapLruCache.put(url, bitmap);
                    //存到本地
                    setBitmapToLocal(url, bitmap);
                    //发送消息到主线程显示处理的图片.
                    Message message = mHandler.obtainMessage();
                    message.obj = new Object[]{iv, url};
                    mHandler.sendMessage(message);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据原图片的宽高和当前设备的屏幕宽高计算出当前图片缩放的比例
     *
     * @param imageWidth  图片的宽
     * @param imageHeight 图片的高
     * @return
     */
    private int getSampleSize(int imageWidth, int imageHeight) {
        debugLog("原图的高:" + imageWidth);
        debugLog("原图的宽:" + imageHeight);
        int scale = 0;

        //如果有指定图片大小的话就进行图片大小的进行缩放比例计算
        if(specifySize != 0){
            if(relative == RELATIVE_WIDTH){
                scale = imageWidth / specifySize;
            }else {
                scale = imageHeight / specifySize;
            }
            debugLog("缩放大小："+scale);
            return scale;
        }

        //以下是根据屏幕的大小进行计算图片的缩放比例
        int scaleSizeToWidth = imageWidth / screenWidth;
        int scaleSizeToHeight = imageHeight / screenHeight;

        if (scaleSizeToWidth > scaleSizeToHeight && scaleSizeToWidth > 1) {
            scale = scaleSizeToWidth;
        } else if (scaleSizeToHeight > scaleSizeToWidth && scaleSizeToHeight > 1) {
            scale = scaleSizeToHeight;
        } else if (scaleSizeToHeight == scaleSizeToWidth) {
            scale = scaleSizeToHeight;//scale = scaleSizeToWidth
        }
        debugLog("需要缩放的大小:" + scale);
        return scale;
    }

    /**
     * 根据一个流对大图片进行压缩和旋转角度的校正
     *
     * @param is
     * @return
     */
    private Bitmap compressImage(InputStream is) {
        FileOutputStream fos = null;
        File rawFile = null;
        try {
            String name = System.currentTimeMillis() + ".jpg";
            if (!mCacheDir.exists()) {
                System.out.println("::" + mCacheDir.mkdirs());
            }
            rawFile = new File(mCacheDir, name);
            fos = new FileOutputStream(rawFile);
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = is.read(buf)) > -1) {
                fos.write(buf, 0, len);
                fos.flush();
            }
            //获取图片的旋转角度
            float degree = getImageDegree(rawFile.getAbsolutePath());
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(rawFile.getAbsolutePath(), options);
            int sampleSize = getSampleSize(options.outWidth, options.outHeight);

            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeFile(rawFile.getAbsolutePath(), options);

            Matrix matrix = new Matrix();
            matrix.setRotate(degree);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);


        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            //删除原图片文件
            if(null != rawFile && rawFile.exists()){
                rawFile.delete();
            }

            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 获取图片的旋转角度
     *
     * @return 返回图片的旋转角度
     */
    public float getImageDegree(String path) {
        float degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    degree = 0;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 根据url从本地中获取一个bitmap对象
     *
     * @param url 图片的url
     * @return 返回url所对应的Bitmap格式
     */
    private Bitmap getBitmapFromLocal(String url) {
        try {
            String fileName = encode(url);
            File file = new File(mCacheDir, fileName + ".jpg");
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                bitmapLruCache.put(url, bitmap);
                return bitmap;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据图片的url为文件名,把图片保存到本地
     *
     * @param bitmap
     * @param url
     */
    private void setBitmapToLocal(String url, Bitmap bitmap) {
        FileOutputStream fos = null;
        try {
            String fileName = encode(url);
            File file = new File(mCacheDir, fileName + ".jpg");
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取一个缓存的目录
     *
     * @return
     */
    private File getCacheDir() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdDir = Environment.getExternalStorageDirectory();
            File cacheDir = new File(sdDir, "Android/data/" + mContext.getPackageName() + "/cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            return cacheDir;
        } else {
            return mContext.getCacheDir();
        }
    }

    /**
     * 获取缓存目录大小
     *
     * @return
     */
    public long getCacheDirSize() {
        long size = 0;
        if (null != mCacheDir) {
            File[] files = mCacheDir.listFiles();
            for (int i = 0; i < files.length; i++) {
                size = size + files[i].length();
            }
        }
        return size;
    }

    /**
     * 打印调试
     *
     * @param message
     */
    private void debugLog(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    /**
     * 设置默认的图片
     *
     * @param defaultImageId
     */
    public void setDefaultImage(int defaultImageId) {
        this.defaultImage = defaultImageId;
    }

    /**
     * MD5加密
     * @param string
     * @return
     * @throws Exception
     */
    public static String encode(String string) throws Exception {
        byte[] hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

}
