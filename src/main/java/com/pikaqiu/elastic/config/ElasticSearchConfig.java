package com.pikaqiu.elastic.config;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-12-22 21:56
 **/
@Configuration
public class ElasticSearchConfig {

    @Bean
    public TransportClient transportClient()throws UnknownHostException {

        Settings settings = Settings.builder()
                .put("cluster.name", "my-application")
                .put("client.transport.sniff", true).build();
//        默认HTTP服务监听端口，课程里使用的是TCP的方式，默认是9300端口
        InetSocketTransportAddress inetSocketTransportAddress = new InetSocketTransportAddress(InetAddress.getByName("192.168.159.128"), 9300);

        return new PreBuiltTransportClient(settings).addTransportAddress(inetSocketTransportAddress);
    }
}
