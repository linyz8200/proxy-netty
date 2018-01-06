package com.proxy.netty.lyz;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by LinLive on 2018/1/3.
 */
public class ProxyMain {

    public static void main(String arg[]){
        //启动直接初始化一个客户端，具体可拓展，根据服务化信息进行初始化
        Client client = new Client("127.0.0.1", 11111);
        Map<Integer, Client> clientHandleMap = new HashMap<>();
        clientHandleMap.put(("127.0.0.1:" + 11111).hashCode(), client);
        Server server = new Server(clientHandleMap);
    }
}
