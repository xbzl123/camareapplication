package com.example.camareapplication.yuv2h264;

/**
 * Copyright (c) 2023 Raysharp.cn. All rights reserved
 * <p>
 * H264DeCodePlay
 *
 * @author longyanghe
 * @date 2023-09-05
 */
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Surface;

import com.blankj.utilcode.util.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class H264DeCodePlay {

    private static final String TAG = "zqf-dev";
    //视频路径
    //使用android MediaCodec解码
    private volatile MediaCodec mediaCodec;
    private Surface mSurface;
    public ArrayBlockingQueue<String> filePathQueue = new ArrayBlockingQueue<>(50);

    PlayStatusCallback playStatusCallback;
    public H264DeCodePlay(Surface surface, int w, int h) {
        if (mSurface == null) {
            this.mSurface = surface;
        }
        Log.e(TAG, "00001");
        initMediaCodec(w,h);
    }

    private boolean initMediaCodec(int w,int h) {
        try {
            Log.e(TAG, "0000");
            //创建解码器 H264的Type为  AAC
            mediaCodec = MediaCodec.createDecoderByType("video/avc");
            //创建配置
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", w, h);
            //设置解码预期的帧速率【以帧/秒为单位的视频格式的帧速率的键】
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            //配置绑定mediaFormat和surface
            mediaCodec.configure(mediaFormat, mSurface, null, 0);
            Log.e(TAG, "1111");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            //创建解码失败
            Log.e(TAG, "创建解码失败");
        }
        return false;
    }

    public boolean isPlaying = false;

    public void resetPlay(int w,int h){
        Log.e(TAG, "mediaCodec reset");
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        if (initMediaCodec(w,h)) {
            isPlaying = false;
            playStatusCallback.isPlayPrepare(true);
        }
    }
    public void closePlay(int w,int h){
        Log.e(TAG, "mediaCodec close");
//        mediaCodec.stop();
//        mediaCodec.release();
        mediaCodec = null;
        isPlaying = false;
    }

    /**
     * 解码播放
     */
    public void decodePlay() {
        if (!isPlaying) {
            mediaCodec.start();
            new Thread(new MyRun()).start();
            isPlaying = true;
        }
    }

    private class MyRun implements Runnable {

        @Override
        public void run() {
            try {
                playStatusCallback.isPalying(true);
                //1、IO流方式读取h264文件【太大的视频分批加载】
                byte[] bytes = null;
                String pollFilePath = filePathQueue.poll();
                if (!FileUtils.isFileExists(pollFilePath)) {
                    return;
                }
                bytes = getBytes(pollFilePath);
                Log.e(TAG, "bytes size " + bytes.length+",pollFilePath: "+pollFilePath);
                //2、拿到 mediaCodec 所有队列buffer[]
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                //开始位置
                int startIndex = 0;
                //h264总字节数
                int totalSize = bytes.length;
                //3、解析
                while (true) {
                    //寻找索引
                    int nextFrameStart = findByFrame(bytes, startIndex + 1, totalSize);
                    Log.e(TAG, "startIndex="+startIndex+",totalSize="+totalSize+",nextFrameStart="+nextFrameStart);
                    //判断是否符合
                    if (totalSize == 0 || startIndex >= totalSize || nextFrameStart == -1 || !isPlaying) {
                        FileUtils.deleteFile(pollFilePath);
                        break;
                    }
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    // 查询10000毫秒后，如果dSP芯片的buffer全部被占用，返回-1；存在则大于0
                    int inIndex = mediaCodec.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        //根据返回的index拿到可以用的buffer
                        ByteBuffer byteBuffer = inputBuffers[inIndex];
                        //清空缓存
                        byteBuffer.clear();
                        //开始为buffer填充数据
                        byteBuffer.put(bytes, startIndex, nextFrameStart - startIndex);
                        //填充数据后通知mediacodec查询inIndex索引的这个buffer,
                        mediaCodec.queueInputBuffer(inIndex, 0, nextFrameStart - startIndex, 0, 0);
                        //为下一帧做准备，下一帧首就是前一帧的尾。
                        startIndex = nextFrameStart;
                    } else {
                        //等待查询空的buffer
                        continue;
                    }
                    //mediaCodec 查询 "mediaCodec的输出方队列"得到索引
                    int outIndex = mediaCodec.dequeueOutputBuffer(info, 10000);
                    Log.e(TAG, "outIndex " + outIndex);
                    if (outIndex >= 0) {
                        try {
                            //暂时以休眠线程方式放慢播放速度
                            Thread.sleep(33);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //如果surface绑定了，则直接输入到surface渲染并释放
                        mediaCodec.releaseOutputBuffer(outIndex, true);
                        Log.e(TAG, "3333");

                    } else {
                        Log.e(TAG, "没有解码成功");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                playStatusCallback.isPlayed(true);
            }
        }
    }


    //读取一帧数据
    private int findByFrame(byte[] bytes, int start, int totalSize) {
        for (int i = start; i < totalSize - 4; i++) {
            //对output.h264文件分析 可通过分隔符 0x00000001 读取真正的数据
            if (bytes[i] == 0x00 && bytes[i + 1] == 0x00 && bytes[i + 2] == 0x00 && bytes[i + 3] == 0x01) {
                return i;
            }
        }
        return -1;
    }

    private byte[] getBytes(String videoPath) throws IOException {
        InputStream is = new DataInputStream(new FileInputStream(videoPath));
        int len;
        int size = 1024;
        byte[] buf;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        buf = new byte[size];
        while ((len = is.read(buf, 0, size)) != -1) {
            bos.write(buf, 0, len);
        }
        buf = bos.toByteArray();
        return buf;
    }

    public interface PlayStatusCallback{
        void isPlayPrepare(boolean isPlayPrepare);
        void isPalying(boolean isPalying);
        void isPlayed(boolean isPlayed);
    }

    public void setPlayStatusCallback(PlayStatusCallback playStatusCallback) {
        this.playStatusCallback = playStatusCallback;
    }

    AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                playStatusCallback.isPalying(true);
                //1、IO流方式读取h264文件【太大的视频分批加载】
                byte[] bytes = null;
                String pollFilePath = filePathQueue.poll();
                bytes = getBytes(pollFilePath);
                Log.e(TAG, "bytes size " + bytes.length+",pollFilePath: "+pollFilePath);
                //2、拿到 mediaCodec 所有队列buffer[]
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                //开始位置
                int startIndex = 0;
                //h264总字节数
                int totalSize = bytes.length;
                //3、解析
                while (true) {
                    //寻找索引
                    int nextFrameStart = findByFrame(bytes, startIndex + 1, totalSize);
                    Log.e(TAG, "startIndex="+startIndex+",totalSize="+totalSize+",nextFrameStart="+nextFrameStart);
                    //判断是否符合
                    if (totalSize == 0 || startIndex >= totalSize || nextFrameStart == -1) {
                        return new Boolean(true);
                    }
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    // 查询10000毫秒后，如果dSP芯片的buffer全部被占用，返回-1；存在则大于0
                    int inIndex = mediaCodec.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        //根据返回的index拿到可以用的buffer
                        ByteBuffer byteBuffer = inputBuffers[inIndex];
                        //清空缓存
                        byteBuffer.clear();
                        //开始为buffer填充数据
                        byteBuffer.put(bytes, startIndex, nextFrameStart - startIndex);
                        //填充数据后通知mediacodec查询inIndex索引的这个buffer,
                        mediaCodec.queueInputBuffer(inIndex, 0, nextFrameStart - startIndex, 0, 0);
                        //为下一帧做准备，下一帧首就是前一帧的尾。
                        startIndex = nextFrameStart;
                    } else {
                        //等待查询空的buffer
                        continue;
                    }
                    //mediaCodec 查询 "mediaCodec的输出方队列"得到索引
                    int outIndex = mediaCodec.dequeueOutputBuffer(info, 10000);
                    Log.e(TAG, "outIndex " + outIndex);
                    if (outIndex >= 0) {
                        try {
                            //暂时以休眠线程方式放慢播放速度
                            Thread.sleep(33);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //如果surface绑定了，则直接输入到surface渲染并释放
                        mediaCodec.releaseOutputBuffer(outIndex, true);
                        Log.e(TAG, "3333");

                    } else {
                        Log.e(TAG, "没有解码成功");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                playStatusCallback.isPlayed(true);
            }
        }
    };

}