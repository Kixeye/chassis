package com.kixeye.chassis.bootstrap.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.ListenerDescription;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.util.EC2MetadataUtils;
import com.google.common.base.Preconditions;
import com.kixeye.chassis.bootstrap.BootstrapException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Instance context that the server is running in.
 *
 * @author dturner@kixeye.com
 */
public class AwsInstanceContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceContext.class);

    private String environment;
    private String exhibitorHost;
    private int exhibitorPort;
    private String userData;
    private String instanceId;
    private String availabilityZone;
    private String region;
    private String privateIp;
    private String publicIp;
    private String appName;
    private String version;

    private AmazonElasticLoadBalancing amazonElasticLoadBalancing;
    private AmazonEC2 amazonEC2;
    private Ec2MetadataClient ec2MetadataClient;

    private AwsInstanceContext(){
        amazonElasticLoadBalancing = new AmazonElasticLoadBalancingClient();
        amazonEC2 = new AmazonEC2Client();

        ec2MetadataClient = new Ec2MetadataClient() {
            @Override
            public String getAvailabilityZone() {
                return EC2MetadataUtils.getAvailabilityZone();
            }

            @Override
            public String getInstanceId() {
                return EC2MetadataUtils.getInstanceId();
            }

            @Override
            public String getUserData() {
                return EC2MetadataUtils.getUserData();
            }

            @Override
            public String getPrivateIpAddress() {
                return EC2MetadataUtils.getPrivateIpAddress();
            }

            @Override
            public String getPublicIpAddress() {
                for (EC2MetadataUtils.NetworkInterface net : EC2MetadataUtils.getNetworkInterfaces()) {
                    List<String> ips = net.getPublicIPv4s();
                    if (ips != null && ips.size() > 0) {
                        return ips.get(0);
                    }
                }
                return null;
            }
        };

        init();
    }

    public AwsInstanceContext(Ec2MetadataClient ec2MetadataClient, AmazonEC2 amazonEC2, AmazonElasticLoadBalancing amazonElasticLoadBalancing){
        this.ec2MetadataClient = ec2MetadataClient;
        this.amazonEC2 = amazonEC2;
        this.amazonElasticLoadBalancing = amazonElasticLoadBalancing;

        init();
    }

    private void init() {
        initInstanceMetadata();
        initUserData();
        initEnvironment();
        initExhibitor();
    }

    private void initInstanceMetadata() {
        LOGGER.info("Initializing instance meta-data...");

        this.instanceId = ec2MetadataClient.getInstanceId();

        if(this.instanceId == null){
            throw new BootstrapException("Unable to get instance-id from AwsUtils. Either the application is not running in AwsUtils, or AwsUtils is having issues.");
        }

        this.privateIp = ec2MetadataClient.getPrivateIpAddress();
        this.publicIp = ec2MetadataClient.getPublicIpAddress();
        this.availabilityZone = ec2MetadataClient.getAvailabilityZone();
        this.region = findRegion();
        initAmazonClients();
        this.appName = AwsUtils.getInstanceName(this.instanceId, amazonEC2);

        LOGGER.info("Initialed instance meta-data with: instance-id:{}, az:{}, region:{}, appName:{}", this.instanceId, this.availabilityZone, this.region, this.appName);
    }

    private void initAmazonClients() {
        amazonEC2.setRegion(com.amazonaws.regions.Region.getRegion(Regions.fromName(this.region)));
        amazonElasticLoadBalancing.setRegion(com.amazonaws.regions.Region.getRegion(Regions.fromName(this.region)));
    }

    private String findRegion() {
        for (Region region : amazonEC2.describeRegions().getRegions()) {
            if (this.availabilityZone.startsWith(region.getRegionName())) {
                return region.getRegionName();
            }
        }
        throw new BootstrapException("Unable to determine region");
    }

    public String getEnvironment() {
        return environment;
    }

    public String getExhibitorHost() {
        return exhibitorHost;
    }

    public String getUserData() {
        return userData;
    }

    private void initExhibitor() {
        LOGGER.info("Initializing exhibitor info...");

        List<LoadBalancerDescription> loadBalancers = AwsUtils.findLoadBalancers(amazonElasticLoadBalancing, new ZookeeperElbFilter(environment));

        if(loadBalancers.size() != 1){
            throw new BootstrapException("Found multiple Zookeeper ELBs for environment " + environment);
        }

        LoadBalancerDescription loadBalancer = loadBalancers.get(0);

        ListenerDescription exhibitorListenerDescription = getExhibitorListenerDescription(loadBalancer);

        this.exhibitorHost = loadBalancer.getDNSName();
        this.exhibitorPort = exhibitorListenerDescription.getListener().getLoadBalancerPort();

        LOGGER.info("Initialized exhibitor info with: exhibitorHost: {}, exhibitorPort: {}", exhibitorHost, exhibitorPort);
    }

    private ListenerDescription getExhibitorListenerDescription(LoadBalancerDescription loadBalancer) {
        for(ListenerDescription listenerDescription:loadBalancer.getListenerDescriptions()){
            if(listenerDescription.getListener().getProtocol().toLowerCase().equals("http")){
                return listenerDescription;
            }
        }

        throw new BootstrapException("Unable to find any listeners which supports http on ELB " + loadBalancer.getLoadBalancerName());
    }

    private void initUserData() {
        LOGGER.info("Initializing user-data...");

        this.userData = ec2MetadataClient.getUserData();

        LOGGER.info("Initialized user-data with: user-data:{}",userData);
    }

    private void initEnvironment() {
        LOGGER.info("Initializing environment...");

        this.environment = UserData.parse(userData).getEnvironment();

        LOGGER.info("Initialized environment as: environment:{}",environment);
    }

    public static AwsInstanceContext initialize() {
        try {
            return new AwsInstanceContext();
        } catch (Exception e) {
            LOGGER.warn("Caught exception initializing AWS Instance Context.",e);
            return null;
        }
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setVersion(String version){
        this.version = version;
    }

    public void shutdown() {
        try {
            amazonEC2.shutdown();
        } catch (Exception e) {
            LOGGER.warn("Failed to shutdown amazonEC2", e);
        }
        try {
            amazonElasticLoadBalancing.shutdown();
        } catch (Exception e) {
            LOGGER.warn("Failed to shutdown amazonElasticLoadBalancing", e);
        }
    }

    public String getRegion() {
        return region;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public String getVersion(){
        return version;
    }

    public String getAppName() {
        return appName;
    }

    public int getExhibitorPort() {
        return exhibitorPort;
    }

    public void tagInstance(String tagInstanceName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(appName), "App Name is required");
        Preconditions.checkArgument(StringUtils.isNotBlank(environment), "Environment is required");
        Preconditions.checkArgument(StringUtils.isNotBlank(version), "Version is required");

        Tag nameTag = new Tag().withKey("Name").withValue(tagInstanceName);
        Tag appTag = new Tag().withKey("Application").withValue(appName);
        Tag envTag = new Tag().withKey("Environment").withValue(environment);
        Tag versionTag = new Tag().withKey("Version").withValue(version);

        AwsUtils.tagInstance(instanceId, amazonEC2, nameTag, appTag, envTag, versionTag);
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }
}
