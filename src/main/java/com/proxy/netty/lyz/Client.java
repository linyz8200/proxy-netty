package com.proxy.netty.lyz;

import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by LinLive on 2018/1/4.
 */
public class Client {


    private NioEventLoopGroup workGroup = new NioEventLoopGroup(4);
    private Channel channel;
    private Bootstrap bootstrap;
    private String ip;
    private int port;
    /**
     * 线程锁集合
     */
    private Map<Integer, CountDownLatch> lockRequestMap = new HashMap<>();
    /**
     * 返回数据流集合
     */
    private Map<Integer, JSONObject> responseData = new HashMap<>();


    public Client(String ip, int port){
        this.ip = ip;
        this.port = port;
        init();
    }


    private void init(){
        System.err.println("-----------代理客户端启动开始-----------");
        try {
            bootstrap = new Bootstrap();
            bootstrap
                    .group(workGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new IdleStateHandler(0, 0, 5));
//                            p.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, -4, 0));
                            p.addLast(new MsgPackDecode());
                            p.addLast(new MsgPackEncode());
                            p.addLast(new ClientHandle(Client.this));
                        }
                    });
            doConnect();
            System.err.println("-----------代理客户端启动结束-----------");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 重连机制,每隔2s重新连接一次服务器
     */
    protected void doConnect() {
        if (channel != null && channel.isActive()) {
            return;
        }

        ChannelFuture future = bootstrap.connect(ip, port);

        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture futureListener) throws Exception {
                if (futureListener.isSuccess()) {
                    channel = futureListener.channel();
                    System.out.println("连接终端服务端成功");
                } else {
                    System.out.println("连接终端服务端失败，开始重试，间隔两秒");

                    futureListener.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            doConnect();
                        }
                    }, 2, TimeUnit.SECONDS);
                }
            }
        });
    }

    /**
     * 发送数据
     * @throws Exception
     */
    public void sendData(JSONObject j, CountDownLatch countDownLatch) throws Exception {
        if(channel != null && channel.isActive()){
            lockRequestMap.put((j.get("requestId").toString()).hashCode(), countDownLatch);
            System.out.println("客户端 开始转发数据" + j);
            channel.writeAndFlush(j.toJSONString());
            return;
        }
        System.out.println("客户端 channel 已经失效，不发生数据");
        countDownLatch.countDown();
        throw new RuntimeException("客户端 channel 已经失效，不发生数据");
    }

    public Map<Integer, CountDownLatch> getLockRequestMap() {
        return lockRequestMap;
    }

    public Map<Integer, JSONObject> getResponseData() {
        return responseData;
    }
}
