package com.example.camareapplication;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Copyright (c) 2023 Raysharp.cn. All rights reserved
 * <p>
 * SocketUtils
 *
 * @author longyanghe
 * @date 2023-08-28
 */
public class SocketUtils {
    public static String sendSocketOfClient(byte[] data,Socket socket,int tag) {
//        if (socket == null || null == data) {
//            return "fail";
//        }
        try {
            /*InetAddress.getLocalHost()*/

//            DatagramSocket mSocket = new DatagramSocket();
//            DatagramPacket pack = new DatagramPacket(data, data.length, InetAddress.getByName("192.168.53.20"),9090);
//            mSocket.send(pack);
//            mSocket.close();

            //局域网内通信需要设置超时时间
            socket.setSoTimeout(5);
            //获取到socket的输出流对象
//            socket.setSendBufferSize(data.length);
//            socket.setReceiveBufferSize(data.length);
            OutputStream outputstream = null;
            outputstream = socket.getOutputStream();
            //利用输出流把数据写出即可
            outputstream.write(data);
            //获取到一个输入流对象，读取服务端回送的数据
//        if(!socket.getKeepAlive()){
//             socket=new Socket(/*InetAddress.getLocalHost()*/"127.0.0.1",9090);
//        }
//            InputStream inputStream = socket.getInputStream();
//            byte[] buf = new byte[1024];
//        int len;
//        while ((len = inputStream.read(buf))!= -1){
//            System.out.println(new String(buf,0,len));
//        }
//            int length = inputStream.read(buf);
//            System.out.println(new String(buf, 0, length));
            Log.e("1234","data="+new String(data, 0, data.length));
            //关闭资源
//            socket.close();
        } catch (Exception e) {
            String msg = e.getMessage();
            Log.e("1234Exception","data="+msg);

            return "fail";
        }
        return "success"+tag;
    }
}
