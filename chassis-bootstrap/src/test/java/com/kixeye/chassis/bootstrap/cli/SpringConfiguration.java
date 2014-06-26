package com.kixeye.chassis.bootstrap.cli;

/*
 * #%L
 * Chassis Bootstrap
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
