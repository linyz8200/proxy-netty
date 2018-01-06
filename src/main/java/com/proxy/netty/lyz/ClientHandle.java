package com.proxy.netty.lyz;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.*;


/**
 * Created by LinLive on 2018/1/3.
 */
public class ClientHandle extends CustomHandle {


    private Client client;



    public ClientHandle(Client client){
        this.client = client;
    }


    @Override
    protected void handleMercury(ChannelHandlerContext channelHandlerContext, JSONObject j, String goalAddress) {
        System.out.println("client 接收返回数据" + j.toString());
        int key = j.get("requestId").toString().hashCode();
        //返回数据放回单例数据集
        client.getResponseData().put(key, j);
        //解锁主线程
        client.getLockRequestMap().get(key).countDown();
        client.getLockRequestMap().remove(key);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        client.doConnect();
    }

    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        super.handleAllIdle(ctx);
        sendPingMsg(ctx);
    }


}
