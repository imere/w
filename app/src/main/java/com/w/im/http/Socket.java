package com.w.im.http;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by shensky on 2018/1/15.
 * https://github.com/shenSKY/Android-TCP/blob/master/TCPDemo/app/src/main/java/tv/higlobal/tcpdemo/TaskCenter.java
 */

public class Socket {
    private static Socket instance;
    private static final String TAG = "Socket";
    //    Socket
    private java.net.Socket socket;
    //    IP地址
    public String ipAddress;
    //    端口号
    public int port;
    private Thread thread;
    //    Socket输出流
    private OutputStream outputStream;
    //    Socket输入流
    private InputStream inputStream;
    //    连接回调
    private OnServerConnectedCallbackBlock connectedCallback;
    //    断开连接回调(连接失败)
    private OnServerDisconnectedCallbackBlock disconnectedCallback;
    //    接收信息回调
    private OnReceiveCallbackBlock receivedCallback;

    //    构造函数私有化
    private Socket() {
        super();
    }

    //    提供一个全局的静态方法
    public static Socket sharedCenter() {
        if (instance == null) {
            synchronized (Socket.class) {
                if (instance == null) {
                    instance = new Socket();
                }
            }
        }
        return instance;
    }

    /**
     * 通过IP地址(域名)和端口进行连接
     *
     * @param ipAddress IP地址(域名)
     * @param port      端口
     */
    public void connect(final String ipAddress, final int port) {

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new java.net.Socket(ipAddress, port);
                    // socket.setSoTimeout ( 2 * 1000 ); // 设置超时时间
                    if (isConnected()) {
                        Socket.sharedCenter().ipAddress = ipAddress;
                        Socket.sharedCenter().port = port;
                        if (connectedCallback != null) {
                            connectedCallback.callback();
                        }
                        outputStream = socket.getOutputStream();
                        inputStream = socket.getInputStream();
                        receive();
                        Log.i(TAG, "连接成功");
                    } else {
                        Log.i(TAG, "连接失败");
                        if (disconnectedCallback != null) {
                            disconnectedCallback.callback();
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Log.e(TAG, "连接异常");
                    if (disconnectedCallback != null) {
                        disconnectedCallback.callback();
                    }
                }
            }
        });
        thread.start();
    }

    /**
     * 判断是否连接
     */
    public boolean isConnected() {
        return socket.isConnected();
    }

    /**
     * 连接
     */
    public void connect() {
        connect(ipAddress, port);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (socket == null) return;
        if (thread != null) {
            try {
                thread.interrupt();
            } catch (Exception ignored) {

            }
        }
        if (isConnected()) {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                socket.close();
                if (socket.isClosed()) {
                    if (disconnectedCallback != null) {
                        disconnectedCallback.callback();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 接收数据
     */
    public void receive() {
        while (isConnected()) {
            try {
                // 得到的是16进制数，需要进行解析
                byte[] bt = new byte[1024];
                // 获取接收到的字节和字节数
                int length = inputStream.read(bt);
                // 获取正确的字节
                byte[] bs = new byte[length];
                System.arraycopy(bt, 0, bs, 0, length);

                if (receivedCallback != null) {
                    receivedCallback.callback(new String(bs, StandardCharsets.UTF_8));
                }
                Log.i(TAG, "接收成功");
            } catch (IOException ex) {
                Log.i(TAG, "接收失败");
            }
        }
    }

    /**
     * 发送数据
     *
     * @param data 数据
     */
    public void send(final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket != null) {
                    try {
                        outputStream.write(data);
                        outputStream.flush();
                        Log.i(TAG, "发送成功");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i(TAG, "发送失败");
                    }
                } else {
                    connect();
                }
            }
        }).start();

    }

    /**
     * 回调声明
     */
    public interface OnServerConnectedCallbackBlock {
        void callback();
    }

    public interface OnServerDisconnectedCallbackBlock {
        void callback();
    }

    public interface OnReceiveCallbackBlock {
        void callback(String receivedMessage);
    }

    public void setConnectedCallback(OnServerConnectedCallbackBlock callback) {
        connectedCallback = callback;
    }

    public void setDisconnectedCallback(OnServerDisconnectedCallbackBlock callback) {
        disconnectedCallback = callback;
    }

    public void setReceivedCallback(OnReceiveCallbackBlock callback) {
        receivedCallback = callback;
    }

    /**
     * 移除回调
     */
    public void removeCallback() {
        connectedCallback = null;
        disconnectedCallback = null;
        receivedCallback = null;
    }
}