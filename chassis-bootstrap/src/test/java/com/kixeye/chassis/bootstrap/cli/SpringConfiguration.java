package com.kixeye.chassis.bootstrap.cli;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for integration tests
 *
 * @author dturner@kixeye.com
 */
@Configuration
public class SpringConfiguration {

    @Bean
    public TestingServer zookeeper() throws Exception {
        return new TestingServer(SocketUtils.findAvailableTcpPort());
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Bean
    public CuratorFramework curator() throws Exception{
        CuratorFramework curator = CuratorFrameworkFactory.newClient(
                zookeeper().getConnectString(),
                5000,
                5000,
                new RetryOneTime(1000));
        curator.start();
        return curator;
    }

}
