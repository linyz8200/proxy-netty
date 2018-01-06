package com.proxy.netty.lyz;

import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by LinLive on 2018/1/3.
 */
public class ServerHandle extends CustomHandle{

    /**
     * 客户端集合
     */
    private Map<Integer, Client> clientMap;

    public ServerHandle(Map<Integer, Client> clientMap){
        this.clientMap = clientMap;
    }

    @Override
    protected void handleMercury(ChannelHandlerContext channelHandlerContext, JSONObject j, String goalAddress) {
        System.out.println("Server成功获取数据，开始选择client");
        Client c = clientMap.get(goalAddress.hashCode());
        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            c.sendData(j, countDownLatch);
            countDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("client转发数据发生异常");
        }
        JSONObject response = c.getResponseData().get(j.get(("requestId").toString()).hashCode());
        channelHandlerContext.writeAndFlush(response.toJSONString());
        System.out.println("server返回发送方数据:"+response.toJSONString());
    }


    @Override
    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        super.handleReaderIdle(ctx);
        System.err.println("---client " + ctx.channel().remoteAddress().toString() + " reader timeout, close it---");
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(" exception"+cause.toString());
    }
}
