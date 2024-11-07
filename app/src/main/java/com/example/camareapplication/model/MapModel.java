package com.example.camareapplication.model;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.example.camareapplication.CameraPreview;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapModel extends BaseViewModel {

    public ObservableField<Activity> isOpen = new ObservableField<>();
    public ObservableField<Boolean> isStartPushVideoData = new ObservableField<>(false);
    public int REQUEST_TAKE_PHOTO = 1;
    public int REQUEST_VIDEO_CAPTURE = 2;
    private Activity context;
    public String currentPhotoPath;

    public MapModel(Activity mainActivity) {
        context = mainActivity;
    }
    public void receviceAndTransformVideoData(){
        isStartPushVideoData.set(!isStartPushVideoData.get());
        if (isStartPushVideoData.get()) {
            if (CameraPreview.INSTANCE.asServer){
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            createServer();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
    }


    public void transformVideoData(){

    }
    BufferedOutputStream outputStream = null;

    private void createfile(String path){
        File file = new File(path);
        if(file.exists()){
            file.delete();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    private void createServer() throws IOException {
        createfile(Environment.getExternalStorageDirectory()+"/yuv/A.h264");
        //建立tcp的服务端，并建立一个监听端口
        ServerSocket serversocket = new ServerSocket(9090);
        //接受客户端的连接，accept()接受客户端的连接方法也是一个阻塞型的方法，没有客户端与其连接时，会一直等待下去。
        Socket socket = serversocket.accept();
        //获取输入流对象，读取客户端发送的内容
        while (true) {
            InputStream inputstream=socket.getInputStream();
            byte[] buf=new byte[1024];
            int length=0;
            length=inputstream.read(buf);
            Log.e("test", "time is " +length);
            String result = new String(buf,0,length);

            if ("success".equals(result)) {
                Log.e("test","time result = "+result);
            }

            outputStream.write(buf);
//            System.out.println(new String(buf,0,length));
        }
//        //获取输出流对象,向客户端发消息
//        OutputStream outputstream= socket.getOutputStream();
//        outputstream.write("copied!".getBytes());
//        //关闭资源
//        socket.close();
//        serversocket.close();
    }

    public boolean checkPermissionOfCamera(){
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(context,new String[]{Manifest.permission.CAMERA},1);
            return false;
        }
        return true;
    }

    public void takePhoto(){
        if (checkPermissionOfCamera()) {
            dispatchTakePictureIntent();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(context,
                        "com.example.camareapplication.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                context.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void dispatchTakeRecoderIntent() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivityForResult(intent,REQUEST_VIDEO_CAPTURE);
        }
    }
    public void recodeVideo(){
        dispatchTakeRecoderIntent();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)//when implemant LifecycleObserver execute
    public void toastShow(){
        Log.e("onCreate","MapModel =");
    }

}
