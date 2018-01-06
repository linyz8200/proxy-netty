package com.proxy.netty.lyz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义handle
 * Created by LinLive on 2018/1/3.
 */
public abstract class CustomHandle extends ChannelInboundHandlerAdapter {

    /**
     * 路由服务器集合，这里直接初始化方便测试，后期可拓展为zookeeper注册服务初始化和定时更新
     */
    public static Map<Integer, String> mercuryToServer = new HashMap();

    /**
     * type可根据服务化前缀进行设定，这里为了测试方便使用，直接写死
     * 拓展方案思路：代理服务端路径也可以通过数据封装，添加标示符，使用轮坐式策略实现负载均衡，复杂一点还可以判断ip权值等进行综合分发
     */
    static {
        mercuryToServer.put(1, "127.0.0.1:11111");
        mercuryToServer.put(2, "127.0.0.1:11112");
    }

    protected abstract void handleMercury(ChannelHandlerContext channelHandlerContext, JSONObject j, String goalAddress);


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String s = (String)msg;
        JSONObject j = JSON.parseObject(s);
        Integer type = j.getInteger("type");
        if(type == 0){
            //心跳检测
            sendPongMsg(ctx);
            return;
        }else if(type == 999){
            System.out.println("心跳检测连接成功");
        }else{
            //路由服务器
            String goalAddress = mercuryToServer.get(type);
            handleMercury(ctx, j, goalAddress);
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // IdleStateHandler 所产生的 IdleStateEvent 的处理逻辑.
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case READER_IDLE:
                    handleReaderIdle(ctx);
                    break;
                case WRITER_IDLE:
                    handleWriterIdle(ctx);
                    break;
                case ALL_IDLE:
                    handleAllIdle(ctx);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.err.println("---" + ctx.channel().remoteAddress() + " is inactive---");
    }

    protected void sendPingMsg(ChannelHandlerContext context) {
        JSONObject j = new JSONObject();
        j.put("type", 0);
        context.channel().writeAndFlush(j.toJSONString());
        System.out.println("心跳检测开始： " + context.channel().remoteAddress());
    }


    private void sendPongMsg(ChannelHandlerContext context) {
        JSONObject j = new JSONObject();
        j.put("type", 999);
        context.channel().writeAndFlush(j.toJSONString());
        System.out.println("心跳检测返回： " + context.channel().remoteAddress());
    }

    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        System.err.println("---READER_IDLE---");
    }

    protected void handleWriterIdle(ChannelHandlerContext ctx) {
        System.err.println("---WRITER_IDLE---");
    }

    protected void handleAllIdle(ChannelHandlerContext ctx) {
        System.err.println("---ALL_IDLE---");
    }

}
