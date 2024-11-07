package com.example.camareapplication;
/**
 * Copyright (c) 2023 Raysharp.cn. All rights reserved
 * <p>
 * CameraPreview
 *
 * @author longyanghe
 * @date 2023-08-25
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.example.camareapplication.h264encode.EncoderParams;
import com.example.camareapplication.h264encode.H264Encoder;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.databinding.ObservableBoolean;

/** A basic Camera preview class
 * @author Administrator*/
public enum CameraPreview {
    INSTANCE;
    private static final String TAG = CameraPreview.class.getSimpleName();
    private CameraManager cameraManager;
    private CameraDevice.StateCallback cam_stateCallback;
    private CaptureRequest request;
    private CameraCaptureSession.StateCallback cam_capture_session_stateCallback;
    public CameraCaptureSession cameraCaptureSession;
    public CameraDevice opened_camera;
    private ImageReader imageReader;
    Socket socket = null;
    private String gateway;
    byte[] h264 = new byte[1024 * 1024];
    BlockingQueue queue = new LinkedBlockingQueue<byte[]>();
    BlockingQueue<String> captureVideoPath = new LinkedBlockingQueue<String>();

    ObservableBoolean isStart = new ObservableBoolean();
    List a =new ArrayList<byte[]>();
    List tmp =new ArrayList<>();
    public boolean asServer = false;
    List<TaskTag> queueH264 = new ArrayList<TaskTag>();

    long time1 = System.currentTimeMillis();
    int count = 0;
    boolean isStartValue = false;
    private H264Encoder mH264Encoder;
    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            if (mH264Encoder.isEncodering()) {
                mH264Encoder.stopThread();
                Log.e(TAG, "count :"+count/120);
                initH264Encoder(UUID.randomUUID().toString());
            }
            long taskTime = (long) msg.obj;
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - taskTime;
            Log.d(TAG, "time2 completed in: " + elapsedTime + " ms");
            isCompleted2H264 = true;
            if (!asServer && !isSending) {
                sendDataBySocket();
            }
        }
    };
    ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    boolean isCompleted2H264 = false;


    public void init(Surface texture_surface){
        long startTime = System.currentTimeMillis();
        imageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.YUV_420_888, 1);//预览数据流最好用非JPEG
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onImageAvailable(ImageReader reader) {
                final Image image = reader.acquireNextImage();
                Log.i(TAG,"image format: " +image.getFormat());
                int n_image_size = image.getWidth()*image.getHeight()*3/2;
                final byte[] yuv420pbuf = new byte[n_image_size];
                System.arraycopy(ImageUtil.getBytesFromImageAsType(image, 2), 0, yuv420pbuf, 0, n_image_size);
                a.add(yuv420pbuf);
//                queue.add(yuv420pbuf);
                count++;
                if (count % 120 == 0) {
                    Log.d(TAG,"time1 is: " +(System.currentTimeMillis() - startTime));

                    a.forEach(element -> {
                        mH264Encoder.putYUVData((byte[]) element);
                    });
//                    isStartValue = !isStartValue;
//                    isStart.set(isStartValue);
//                    tmp.addAll(a);

//                    if (count < 122) {
                        if (!mH264Encoder.isEncodering()) {
                            mH264Encoder.StartEncoderThread();
                            a.clear();
                            count = 0;
                        }
//                    } else {
//                        mH264Encoder.setRuning(false);
//                    }

//                    queue.clear();
                    Log.e("h264-encode", "spend time2 =" + (System.currentTimeMillis() - time1));
                }
                Log.e("h264-encode", "a size =" + a.size());

                if (image != null) {
                    image.close();
                }
//                sendDataBySocket(yuv420pbuf);
            }
        }, null);
    }

    int tag = 0;
    public void sendDataBySocket(){
        String videoPath = captureVideoPath.poll();
        Log.e(TAG,"videoPath : " +videoPath);

        new AsyncTask<String,Void,String>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                isSending = true;
                //建立tcp服务
                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                if (socket == null) {
                                    socket = new Socket(gateway, 9090);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }finally {
                                Log.e(TAG,"videoPath start tag: " +tag);
                                SocketUtils.sendSocketOfClient("start".getBytes(), socket,tag);
                                tag++;
                            }
                        }
                    }.start();
            }

            @Override
            protected String doInBackground(String... voids) {
                InputStream is = null;
                byte[] h264Buf;
                try {
                    is = new DataInputStream(new FileInputStream(Environment.getExternalStorageDirectory()+"/yuv/"+videoPath+".h264"));
                    int len;
                    int size = 1024;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    h264Buf = new byte[size];
                    while ((len = is.read(h264Buf, 0, size)) != -1) {
                        bos.write(h264Buf, 0, len);
                    }
                    h264Buf = bos.toByteArray();

                    is.close();
                    bos.close();
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return SocketUtils.sendSocketOfClient(h264Buf, socket,tag);
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                FileUtils.deleteFile(Environment.getExternalStorageDirectory()+"/yuv/"+videoPath+".h264");
                if (result.equals("success")) {
                    new Thread(){
                        @Override
                        public void run() {
                            SocketUtils.sendSocketOfClient("success".getBytes(), socket,tag);
                        }
                    }.start();
                }
                isSending = false;
            }
        }.execute();
//        ThreadUtils.executeBySingle(new ThreadUtils.SimpleTask<String>() {
//            String videoPath = captureVideoPath.poll();
//
//            @Override
//            public String doInBackground() throws Throwable {// 获取捕获的照片数据
//                isSending = true;
//                if (socket == null) {
//                    //建立tcp服务
//                    try {
//                        socket = new Socket(gateway, 9090);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                InputStream is = new DataInputStream(new FileInputStream(videoPath));
//                int len;
//                int size = 1024;
//                byte[] h264Buf;
//                ByteArrayOutputStream bos = new ByteArrayOutputStream();
//                h264Buf = new byte[size];
//                while ((len = is.read(h264Buf, 0, size)) != -1) {
//                    bos.write(h264Buf, 0, len);
//                }
//                h264Buf = bos.toByteArray();
//
//                is.close();
//                bos.close();
////                tag++;
//                return SocketUtils.sendSocketOfClient(h264Buf, socket,tag);
////                return "success";
//            }
//
//            @Override
//            public void onSuccess(String result) {
//                if (result.equals("success")) {
//                    FileUtils.deleteFile(videoPath);
//                    new Thread(){
//                        @Override
//                        public void run() {
//                            SocketUtils.sendSocketOfClient("success".getBytes(), socket,tag);
//                        }
//                    }.start();
//                    isSending = false;
//                }
//            }
//        });
    }

    public void sendClosedCmd(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                SocketUtils.sendSocketOfClient("close".getBytes(), socket,tag);
            }
        }.start();
    }
    boolean isSending = false;
    public void sendDataBySocket(TaskTag task){
        ThreadUtils.executeBySingle(new ThreadUtils.SimpleTask<String>() {
            @Override
            public String doInBackground() throws Throwable {// 获取捕获的照片数据
                InputStream is = new DataInputStream(new FileInputStream(new File(task.getFilePath())));
                int len;
                int size = 1024;
                byte[] h264Buf;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                h264Buf = new byte[size];
                String sendResult = "";
                while ((len = is.read(h264Buf, 0, size)) != -1) {
                    bos.write(h264Buf, 0, len);
                }
                h264Buf = bos.toByteArray();
                if (socket == null) {
                    //建立tcp服务
                    try {
                        socket = new Socket(/*InetAddress.getLocalHost()*/gateway, 9090);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                sendResult = SocketUtils.sendSocketOfClient(h264Buf, socket,tag);
                isSending = true;
                return sendResult;
            }

            @Override
            public void onSuccess(String result) {
                isSending = false;
                queueH264.remove(task);
                if (queueH264.size() > 0 && "success".equals(result)){
//                    sendDataBySocket(queueH264.get(0));
                }
            }
        });
    }

    public void release(){
        imageReader.close();
    }

    public void openCamera(Context context, Surface texture_surface) {
        initH264Encoder(UUID.randomUUID().toString());
        init(texture_surface);
        // 1 创建相机管理器，调用系统相机
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        // 2 准备 相机状态回调对象为后面用
        cam_stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                // 2.1 保存已开启的相机对象
                opened_camera = camera;
                try {
                    // 2.2 构建请求对象（设置预览参数，和输出对象）
                    CaptureRequest.Builder requestBuilder = opened_camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW); // 设置参数：预览
                    requestBuilder.addTarget(texture_surface); // 设置参数：目标容器
                    requestBuilder.addTarget(imageReader.getSurface());
//                    // 自动对焦
                    requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                    // 打开闪光灯
//                    requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    request = requestBuilder.build();
                    //2.3 创建会话的回调函数，后面用
                    cam_capture_session_stateCallback = new CameraCaptureSession.StateCallback() {
                        @Override  //2.3.1  会话准备好了，在里面创建 预览或拍照请求
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            try {
                                // 2.3.2 预览请求
                                session.setRepeatingRequest(request, null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    };
                    // 2.3 创建会话
                    opened_camera.createCaptureSession(Arrays.asList(texture_surface,imageReader.getSurface()), cam_capture_session_stateCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {

            }
        };
        openCamera2(context);
    }
    private int mWidth = 640;
    private int mHeight = 480;
    private int framerate = 30;
    private int biterate = 8500 * 1000;
    private void initH264Encoder(String fileName) {
        //启动线程编码  注意宽高
//        if (null == mH264Encoder) {
            EncoderParams params = new EncoderParams();
            params.setVideoPath(Environment.getExternalStorageDirectory()+"/yuv/"+fileName+".h264");
            mH264Encoder = new H264Encoder(mWidth, mHeight, framerate, biterate, params);
            mH264Encoder.setHandler(handler,executor);
            captureVideoPath.add(fileName);

//        }
    }

    public void openCamera2(Context context){
        // 4 开启相机（传入：要开启的相机ID，和状态回调对象）
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                ActivityCompat.requestPermissions((Activity) context,new String[]{Manifest.permission.CAMERA},100);
                return;
            }
            cameraManager.openCamera(cameraManager.getCameraIdList()[0], cam_stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
}