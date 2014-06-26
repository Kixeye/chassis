package com.kixeye.chassis.bootstrap.aws;

/**
 * Metadata client
 *
 * @author dturner@kixeye.com
 */
public interface Ec2MetadataClient {

    String getAvailabilityZone();

    String getInstanceId();

    String getUserData();

    String getPrivateIpAddress();

    String getPublicIpAddress();
}
