package com.example.camareapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaPlayer;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.widget.CompoundButton;

import com.blankj.utilcode.util.ToastUtils;
import com.example.camareapplication.databinding.ActivityMainBinding;
import com.example.camareapplication.model.MapModel;
import com.example.camareapplication.yuv2h264.H264DeCodePlay;
import com.rokejits.android.tool.utils.FileUtil;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mainBinding;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("h264-encode");
    }

    private LocationUtils instance;
    private SensorManager serviceManager;
    private Sensor sensor;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private Surface texture_surface;
    private H264DeCodePlay h264DeCodePlay = null;
    private MediaPlayer mediaPlayer = null;
    private boolean isCloseSend;

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mainBinding.getModel().currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        setPic();
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mainBinding.captrueImage.getWidth();
        int targetH = mainBinding.captrueImage.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
//        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = 5;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mainBinding.getModel().currentPhotoPath, bmOptions);
        mainBinding.captrueImage.setImageBitmap(bitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == mainBinding.getModel().REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
//            Bundle extras = data.getExtras();
//            Bitmap imageBitmap = (Bitmap) extras.get("data");
//            imageView.setImageBitmap(imageBitmap);
//            galleryAddPic();
            FileUtil.galleryAddPic(this, mainBinding.getModel().currentPhotoPath);
            setPic();
        } else if (requestCode == mainBinding.getModel().REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Log.e("test", "uri :" + uri.getPath() + ",videoview:" + uri.getEncodedPath());
            mainBinding.captureVideo.setVideoURI(uri);
            mainBinding.captureVideo.setVisibility(View.VISIBLE);
            mainBinding.captureVideo.start();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        MapModel mapModel = new MapModel(this);
        mainBinding.setModel(mapModel);
        getLifecycle().addObserver(mapModel);
        instance = LocationUtils.getInstance(this, locationListener);

        SensorUtils sensorUtils = SensorUtils.getSensorUtils(this);
        serviceManager = sensorUtils.getService();
        sensor = serviceManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        List<Sensor> dynamicSensorList = serviceManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor object :dynamicSensorList) {
            Log.e("test","object = "+object.getName()+","+object.getStringType());
        }
       startTimeDownCount(1000, new ThenAction() {
           @Override
           public String deal(int pos) {
               return null;
           }
        });

        WifiManager wifiManager =(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo connectionInfo = wifiManager.getDhcpInfo();
        CameraPreview.INSTANCE.setGateway(connectionInfo.toString().split("gateway")[1].split("netmask")[0].trim());
//        new Thread(){
//            @Override
//            public void run() {
//                createTxtFile();
//            }
//        }.start();
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ||ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},5);
            return;
        }
        mainBinding.surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                h264DeCodePlay = new H264DeCodePlay(holder.getSurface(), 640,480);
                h264DeCodePlay.setPlayStatusCallback(new H264DeCodePlay.PlayStatusCallback() {
                    @Override
                    public void isPlayPrepare(boolean isPlayPrepare) {
                        if (isPlayPrepare) {
                            Message message = new Message();
                            message.what = 2;
                            mHandler.sendMessageDelayed(message,50);
                        }
                    }

                    @Override
                    public void isPalying(boolean isPalying) {

                    }

                    @Override
                    public void isPlayed(boolean isPlayed) {
                        if (isPlayed && !isCloseSend) {
                            h264DeCodePlay.resetPlay(640,480);
                        } else if (isCloseSend) {
                            h264DeCodePlay.isPlaying = false;
                            h264DeCodePlay.closePlay(640,480);
                            holder.getSurface().release();
                            try {
                                serversocket.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        mainBinding.receviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isRecevie = true;
                    mainBinding.surface.setVisibility(View.VISIBLE);
                    new Thread(){
                        @Override
                        public void run() {
                            createServer();
                        }
                    }.start();
                    mainBinding.captrueSwitch.setClickable(false);
                } else {
                    isCloseSend = true;
                    mainBinding.surface.setVisibility(View.GONE);
                    isRecevie = false;
                }
            }
        });
        mainBinding.captrueSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mainBinding.textureview.setVisibility(View.VISIBLE);
                Log.e("captrueSwitch","captrueSwitch == 111");
                surfaceTextureListener = new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                        texture_surface = new Surface(mainBinding.textureview.getSurfaceTexture());
                        CameraPreview.INSTANCE.openCamera(MainActivity.this,texture_surface);
                        Log.e("captrueSwitch","captrueSwitch == 222");
                    }
                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    }
                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                        return false;
                    }
                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    }
                };
                mainBinding.textureview.setSurfaceTextureListener(surfaceTextureListener);
            } else {
                CameraPreview.INSTANCE.sendClosedCmd();
                mainBinding.textureview.setVisibility(View.GONE);
                surfaceTextureListener = null;
                texture_surface = null;
            }
        });
        //软解码转换
//        new Thread(){
//            @Override
//            public void run() {
//                File yuvFile = new File(Environment.getExternalStorageDirectory()+"/yuv/"+"test.yuv");
//                File h264File = new File(Environment.getExternalStorageDirectory()+"/yuv/"+"test00.h264");
//
//                FileInputStream fileInputStream = null;
//                try {
//                    fileInputStream = new FileInputStream(yuvFile);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//                FileOutputStream fos = null;
//                try {
//                    fos = new FileOutputStream(h264File);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//
//                FileOutputStream finalFos = fos;
//                byte[] bytes = new byte[1204];
//                while (true){
//                    try {
//                        if ((fileInputStream.read(bytes) == -1)) {
//                            finalFos.close();
//                            break;
//                        }
//                        YUVCompressor.compressYUV(640, 480, 30, bytes, new YUVCompressor.Output() {
//                            @Override
//                            public void write(byte[] data, int offset, int length) throws IOException {
//                                try {
//                                    finalFos.write(data);
//                                    finalFos.flush();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                } finally {
//                                }
//                            }
//                        },texture_surface);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();

    }

    private File createTxtFile(){
        File file = new File(txtPath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write("test".getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
    private File clearTxtFile(){
        File file = new File(txtPath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(new byte[0]);
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    ArrayList<TaskTag> tasktag = new ArrayList<TaskTag>();
    ThreadLocal<ArrayList<TaskTag>> threadLocalList = new ThreadLocal<ArrayList<TaskTag>>();

    private String createYUVFileName(int pos){
        return Environment.getExternalStorageDirectory()+"/yuv/"+"temp_"+pos+".yuv";
    }
    private String createH264FileName(int pos){
        return Environment.getExternalStorageDirectory()+"/yuv/"+"temp_"+pos+".h264";
    }
    long time1 = 0;
    long time2 = 0;

    ThenAction yuvFileSave = pos -> {
        time2 = System.currentTimeMillis();
        File file = new File(createYUVFileName(pos));
        Log.e("h264-encode", "file1 is " + file.length());
        FileOutputStream fos = null;
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fos = new FileOutputStream(file);
            int count = 0;
            while (count < 60) {
                try {
                    fos.write((byte[]) CameraPreview.INSTANCE.tmp.get(count));
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                count++;
            }
            CameraPreview.INSTANCE.tmp.clear();
            Log.e("h264-encode", "spend time2 ="+(System.currentTimeMillis() - time2));
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return "yuvFileSave";
    };

    ThenAction yuv2H264= pos -> {
        time1 = System.currentTimeMillis();

        File file = new File(createYUVFileName(pos));
        Log.e("h264-encode", "file1 is " + file.length());
        FileOutputStream fos = null;
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fos = new FileOutputStream(file);
            int count = 0;
            while (count < 60) {
                try {
                    fos.write((byte[]) CameraPreview.INSTANCE.tmp.get(count));
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                count++;
            }
            CameraPreview.INSTANCE.tmp.clear();
            Log.e("h264-encode", "spend time1 ="+(System.currentTimeMillis() - time1));
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        String filePath = "";
        int res = h264EncodeEncode(640, 480, createYUVFileName(pos), createH264FileName(pos), 0);
        Log.e("h264-encode", "res is " + res + ",spend time ="+(System.currentTimeMillis() - time1));
        File h264File = new File(createH264FileName(pos));
        Log.e("h264-encode", "file2 is " + h264File.length());
        Log.e("h264-encode", "thread is end " + Thread.currentThread().getId());
        tasktag.get(pos).setRunning(false);
        if (h264File.length() > 0) {
            filePath = h264File.getPath();
            tasktag.get(pos).setFilePath(filePath);
            threadLocalList.set(tasktag);
            return "yuv2H264";
        }
        threadLocalList.set(tasktag);
        return "";
    };

        private void FileDealWith(int pos,ThenAction thenAction){
            AsyncTask<Void, String, String> asyncTask = new AsyncTask<Void, String, String>() {

                @Override
                protected String doInBackground(Void... voids) {
                    try {
                        Log.e("h264-encode", "thread is start " + Thread.currentThread().getId());
//                        if ("yuvFileSave".equals(s)) {
                        return thenAction.deal(pos);
//                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                    }
                    return "";
                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
//                    if ("yuvFileSave".equals(s)) {
//                           FileDealWith(pos,yuv2H264);
//                        }else if ("yuv2H264".equals(s)){
                        tasktag.get(pos).setRunning(false);
                        threadLocalList.set(tasktag);

//                    }
                    }
            };
            asyncTask.execute();
    }
    private void yuvToH264(int pos,TaskTag tag) {
        AsyncTask<Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                tag.setRunning(true);
                String filePath = "";
                File file = new File(createYUVFileName(pos));
                Log.e("h264-encode", "file1 is " + file.length());
                FileOutputStream fos = null;
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                    try {
                        fos = new FileOutputStream(file);
                        int count = 0;
                        while (count < 60){
                            try {
                                fos.write((byte[]) CameraPreview.INSTANCE.tmp.get(count));
                                fos.flush();
                            } catch (IOException  e) {
                                e.printStackTrace();
                            }
                            count++;
                        }
                        CameraPreview.INSTANCE.tmp.clear();
                        Log.e("h264-encode", "remainingCapacity is " + CameraPreview.INSTANCE.queue.remainingCapacity());
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "";
                    } finally {
                        time1 = System.currentTimeMillis();
                        int res = h264EncodeEncode(640, 480, createYUVFileName(pos), createH264FileName(pos), 0);
                        Log.e("h264-encode", "res is " + res + ",spend time ="+(System.currentTimeMillis() - time1));
                        File h264File = new File(createH264FileName(pos));
                        Log.e("h264-encode", "file2 is " + h264File.length());
                        if (h264File.length() > 0) {
                            filePath = h264File.getPath();
                        }
                        return filePath;
                    }
            }

            @Override
            protected void onPostExecute(String filePath) {
                super.onPostExecute(filePath);
                tag.setRunning(false);
                if ("".equals(filePath)){
                    return;
                }
                tag.setFilePath(filePath);
                CameraPreview.INSTANCE.queueH264.add(tag);
                Collections.sort(CameraPreview.INSTANCE.queueH264, (o1, o2) -> (int) (o2.getStartTimeSec() - o1.getStartTimeSec()));
                CameraPreview.INSTANCE.sendDataBySocket(tag);
            }
        };
        asyncTask.execute();
    }

    private BufferedOutputStream createfile(String path){
        File file = new File(path);
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e){
            e.printStackTrace();
        }
        return outputStream;
    }
    String receviceVideoPath = Environment.getExternalStorageDirectory() + "/yuv/camera_";

//    String receviceVideoPath = Environment.getExternalStorageDirectory() + "/yuv/A.h264";
    String txtPath = Environment.getExternalStorageDirectory() + "/yuv/test.txt";

    BufferedOutputStream outputStream = null;
    boolean isRecevie = true;
    int tag = 0;
    ServerSocket serversocket = null;

    private void createServer() {
        //建立tcp的服务端，并建立一个监听端口
        try {
            serversocket = new ServerSocket(9090);
            //接受客户端的连接，accept()接受客户端的连接方法也是一个阻塞型的方法，没有客户端与其连接时，会一直等待下去。
            Socket socket = serversocket.accept();
            //获取输入流对象，读取客户端发送的内容
            boolean isStartSave = false;

            while (true) {
                if (!isRecevie) {
                    return;
                }
                BufferedOutputStream outputStream = null;
                InputStream inputstream = socket.getInputStream();
                byte[] buf = new byte[serversocket.getReceiveBufferSize()];
                int length = 0;
                String result = "";
                while ((length = inputstream.read(buf)) != -1) {
                    result = new String(buf,0,length);
                    try {
                        if (isStartSave && !isCloseSend) {
                            outputStream.write(buf,0,length);
                            Log.e("zqf-dev", "write");
                        }
                        if ("success".equals(result)) {
                            Log.e("zqf-dev", "decodePlay :success");
                            isStartSave = false;
                        } else if ("close".equals(result)) {
                            isCloseSend = true;
                            Log.e("zqf-dev", "decodePlay isCloseSend:success");
                        } else if ("start".equals(result)) {
                            isCloseSend = false;
                            String saveFilePath = receviceVideoPath + UUID.randomUUID().toString() + ".h264";
                            Log.e("zqf-dev", "decodePlay :saveFilePath="+saveFilePath);
                            if (h264DeCodePlay != null){
                                h264DeCodePlay.filePathQueue.add(saveFilePath);
                            }
                            outputStream = createfile(saveFilePath);
                            if (tag == 1) {
                                mHandler.sendEmptyMessage(1);
                            }
                            tag++;
                            isStartSave = true;
                        }
                    } catch (Exception exception) {
                        Log.e("exception", "result is " +exception.getMessage());
                        throw new RuntimeException(exception);
                    }
                }
//            System.out.println(new String(buf,0,length));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (h264DeCodePlay != null) {
                h264DeCodePlay.decodePlay();
            }
        }
    };
    Disposable mDisposable ;
    private void startTimeDownCount(int seconds,ThenAction action) {
         Observable.interval(100, TimeUnit.MILLISECONDS, Schedulers.io()).doOnSubscribe(new Consumer<Disposable>() {
             @Override
             public void accept(Disposable disposable) throws Throwable {
                 mDisposable = disposable;
             }
         }).subscribe(aLong -> {
//             if (CameraPreview.INSTANCE.queueH264.size() > 0 && !CameraPreview.INSTANCE.isSending) {
//                 Log.e("h264-encode"," CameraPreview.INSTANCE.queueH264.size = "+ CameraPreview.INSTANCE.queueH264.size());
//                 CameraPreview.INSTANCE.sendDataBySocket(CameraPreview.INSTANCE.queueH264.get(0));
//                 CameraPreview.INSTANCE.queueH264.remove(0);
//             }
//             action.deal(aLong);
//             if(aLong == seconds){
//                 mDisposable.dispose();
//             }
         });
    }

    private SensorEventListener sensorEventListener = new SensorEventListener(){
        @Override
        public void onSensorChanged(SensorEvent event) {
            float millibarsOfPressure = event.values[0];
            Log.e("test","millibarsOfPressure = "+millibarsOfPressure);

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    @Override
    protected void onResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            serviceManager.registerListener(sensorEventListener,sensor,SensorManager.SENSOR_DELAY_NORMAL);
        }
//        stringFromJNI();
        Log.e("test","SupportAvcCodec = "+SupportAvcCodec());
        super.onResume();
    }

    @Override
    protected void onPause() {
//        CameraPreview.INSTANCE.release();
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        CameraPreview.INSTANCE.release();
        CameraPreview.INSTANCE.init(texture_surface);
    }

    private LocationListener locationListener  = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

//            new Handler().post(()->{
                mainBinding.locationText.setText("我现在位置是"+instance.getAddress(location.getLatitude(),location.getLongitude())
                        +"，海拔是："+location.getAltitude()+",時間是："+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(location.getTime())));
//            });
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public static native int h264EncodeEncode(int w,int h,String srcpath,String despath,int type);
    public static native int encodeH264Pic(int w,int h,String srcpath,String despath);


    private boolean SupportAvcCodec(){
        if(Build.VERSION.SDK_INT>=18){
            for(int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--){
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);

                String[] types = codecInfo.getSupportedTypes();
                for (int i = 0; i < types.length; i++) {
                    if (types[i].equalsIgnoreCase("video/avc")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
