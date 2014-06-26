package com.kixeye.chassis.chassis.test.eureka;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.kixeye.chassis.chassis.ChassisConfiguration;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes={ChassisEurekaTestConfiguration.class,ChassisConfiguration.class})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@Ignore
public class ChassisEurekaRegistrationTest {
    @Test
    public void testServiceRegistration() throws InterruptedException {
    	// Registers "chasis-default-name" with a Eurkea server running on local host.
    	//   http://localhost:8184/v2/apps/chassis-default-name

    	// tell eureka the service is up which causes a registration
    	ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.UP);
    	
    	// get application registration from Eureka
    	DiscoveryClient client = DiscoveryManager.getInstance().getDiscoveryClient();
    	InstanceInfo instanceInfo = null;
    	for (int i = 0; (instanceInfo == null) && (i < 50); i++) {
    		Thread.sleep(5000);
    		try {
				instanceInfo =  client.getNextServerFromEureka("default-service", false);
			} catch (RuntimeException e) {
				// eat not found runtime exception
			}
    	}
    	Assert.assertNotNull(instanceInfo);
    	Assert.assertEquals(InstanceStatus.UP, instanceInfo.getStatus());
    	System.out.println("done");
    }
}
