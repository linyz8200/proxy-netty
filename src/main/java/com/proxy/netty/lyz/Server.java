package com.proxy.netty.lyz;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;

/**
 * Created by LinLive on 2018/1/4.
 */
public class Server {

    private Map<Integer, Client> clientMap;

    public Server(Map<Integer, Client> clientMap){
        this.clientMap = clientMap;
        init();
    }

    private void init(){
        System.err.println("-----------代理服务端启动开始---------------");
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workGroup = new NioEventLoopGroup(4);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new IdleStateHandler(10, 0, 0));
                            p.addLast(new MsgPackDecode());
                            p.addLast(new MsgPackEncode());
                            p.addLast(new ServerHandle(Server.this.clientMap));
                        }
                    });

            //初始化代理服务端端口
            Channel ch = bootstrap.bind(12345).sync().channel();
            System.err.println("-----------代理服务端启动结束---------------");
            ch.closeFuture().sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
