package com.kixeye.chassis.bootstrap;

import org.apache.curator.RetryPolicy;
import org.apache.curator.RetrySleeper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
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

    @Bean(destroyMethod="close")
    public TestingServer zookeeper() throws Exception {
        return new TestingServer(SocketUtils.findAvailableTcpPort());
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Bean(initMethod="start", destroyMethod="close")
    public CuratorFramework curator(TestingServer zookeeper) throws Exception{
        CuratorFramework curator = CuratorFrameworkFactory.newClient(
                zookeeper.getConnectString(),
                5000,
                5000,
                new RetryPolicy() {
					public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper) { return false; }
				});
        return curator;
    }

}
