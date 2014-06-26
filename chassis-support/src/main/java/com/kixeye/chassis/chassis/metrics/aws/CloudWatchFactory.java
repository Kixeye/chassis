package com.kixeye.chassis.chassis.metrics.aws;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;

/**
 * For creating AmazonCloudWatchs
 *
 * @author dturner@kixeye.com
 */
public interface CloudWatchFactory {

    /**
     * get an AmazonCloudWatch
     * @return AmazonCloudWatch
     */
    public AmazonCloudWatch getCloudWatchClient();
}
