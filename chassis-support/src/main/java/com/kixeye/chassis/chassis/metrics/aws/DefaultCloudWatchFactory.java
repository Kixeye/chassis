package com.kixeye.chassis.chassis.metrics.aws;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.google.common.base.Preconditions;

/**
 * Construct a CloudWatchClient with credentials obtained by attempting the following (in order)
 *
 * 1) using given accessKeyId or secretKey (if provided)
 * 2) getting accessKeyId and secretKey from environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
 * 3) getting accessKeyId and secretKey from system properties aws.accessKeyId and aws.secretKey
 * 4) obtaining and caching IAM instance profile credentials.
 *
 * @author dturner@kixeye.com
 */
@Component
public class DefaultCloudWatchFactory implements CloudWatchFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudWatchFactory.class);

    private String accessKeyId;
    private String secretKey;
    private Region region;

    @Autowired
    public DefaultCloudWatchFactory(
            @Value(MetricsCloudWatchReporter.AWS_ACCESS_ID_PROPERTY_NAME) String accessKeyId,
            @Value(MetricsCloudWatchReporter.AWS_SECRET_KEY_PROPERTY_NAME) String secretKey,
            @Value(MetricsCloudWatchReporter.METRICS_AWS_REGION_PROPERTY_NAME) String region){

        Preconditions.checkArgument(StringUtils.isNotBlank(region),"Region is required");

        this.accessKeyId = accessKeyId;
        this.secretKey = secretKey;
        this.region = getCloudWatchRegion(region);
    }

    @Override
    public AmazonCloudWatchClient getCloudWatchClient() {
        AmazonCloudWatchClient client;
        if (StringUtils.isBlank(accessKeyId) || StringUtils.isBlank(secretKey)) {
            LOGGER.debug("Constructing AmazonCloudWatchClient using DefaultAWSCredentialsProviderChain for region {}.", region);
            client = new AmazonCloudWatchClient(new DefaultAWSCredentialsProviderChain());
        } else {
            LOGGER.debug("Constructing AmazonCloudWatchClient from given credentials for region {}.", region);
            client = new AmazonCloudWatchClient(new BasicAWSCredentials(accessKeyId, secretKey));
        }
        client.setRegion(region);
        return client;
    }

    private Region getCloudWatchRegion(String region) {
        if ("default".equals(region)) {
            return Region.getRegion(Regions.DEFAULT_REGION);
        }
        return Region.getRegion(Regions.fromName(region));
    }
}
