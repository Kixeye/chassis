package com.kixeye.chassis.chassis.eureka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;

@Configuration
@ComponentScan(basePackageClasses = EurekaConfiguration.class)
public class EurekaConfiguration {

    @Value("${chassis.eureka.disable}")
    private boolean disableEureka;

    @Value("${eureka.datacenter}")
    private String datacenter;

    /***
     * Initializes Eureka Client Library (aka DiscoveryManager)
     *
     * @return discovery manager bean
     */
    @Bean(destroyMethod = "shutdownComponent")
    public DiscoveryManager eurekaDiscoveryManager(MetadataCollector metadataCollector) {
        final DiscoveryManager bean = DiscoveryManager.getInstance();
        if (!disableEureka) {
            // set eureka.port via http.port if not already set
            int httpPort = ConfigurationManager.getConfigInstance().getInt("http.port",-1);
            int httpsPort = ConfigurationManager.getConfigInstance().getInt("https.port",-1);
            int eurekaPort = ConfigurationManager.getConfigInstance().getInt("eureka.port",-1);
            int eurekaSecurePort = ConfigurationManager.getConfigInstance().getInt("eureka.securePort",-1);
            boolean httpPortEnabled = ConfigurationManager.getConfigInstance().getBoolean("http.enabled", false);
            boolean httpsPortEnabled = ConfigurationManager.getConfigInstance().getBoolean("https.enabled", false);
            if (httpPort != -1 && eurekaPort == -1) {
                ConfigurationManager.getConfigInstance().setProperty("eureka.port", httpPort);
                ConfigurationManager.getConfigInstance().setProperty("eureka.port.enabled", httpPortEnabled);
            }
            if(httpsPort != -1 && eurekaSecurePort == -1){
                ConfigurationManager.getConfigInstance().setProperty("eureka.securePort", httpsPort);
                ConfigurationManager.getConfigInstance().setProperty("eureka.securePort.enabled", httpsPortEnabled);
            }

            // set eureka.name and eureka.vipAddress with @SpringApp name if not already set
            String appName = ConfigurationManager.getConfigInstance().getString("app.name",null);
            String eurekaName = ConfigurationManager.getConfigInstance().getString("eureka.name",null);
            String eurekaVip = ConfigurationManager.getConfigInstance().getString("eureka.vipAddress",null);
            String eurekaSecureVipAddress = ConfigurationManager.getConfigInstance().getString("eureka.secureVipAddress",null);
            if (appName != null && eurekaName == null) {
                ConfigurationManager.getConfigInstance().setProperty("eureka.name", appName);
            }
            if (appName != null && eurekaVip == null) {
                ConfigurationManager.getConfigInstance().setProperty("eureka.vipAddress", appName);
            }
            if (appName != null && eurekaSecureVipAddress == null) {
                ConfigurationManager.getConfigInstance().setProperty("eureka.secureVipAddress", appName);
            }

            // initialize DiscoveryManager if it hasn't already been done
            if (ApplicationInfoManager.getInstance().getInfo() == null) {
                EurekaInstanceConfig config;
                switch (datacenter.toLowerCase()) {
                    case "amazon":
                    case "cloud":
                        config = new KixeyeCloudInstanceConfig(metadataCollector);
                        break;
                    default:
                        config = new KixeyeMyDataCenterInstanceConfig(metadataCollector);
                        break;
                }
                bean.initComponent(config, new DefaultEurekaClientConfig());
            }
        }
        return bean;
    }
}
