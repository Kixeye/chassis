package com.kixeye.chassis.bootstrap.aws;

import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.kixeye.chassis.bootstrap.aws.AwsUtils.ELBFilter;

/**
 * Filter which accepts ELB's with a specific naming conventions. It to be accepted, the
 * ELB name must have a minimum of 2 "-" separated tokens, and match:
 *
 * 1) $env-Zoo*
 * or
 * 2) exhibitor-$env-internal
 *
 * Additionally, if any accepted ELBs are found to have multiple listener descriptions, an exception is thrown.
 *
 * @author dturner@kixeye.com
 */
public class ZookeeperElbFilter implements ELBFilter {

    private String environment;

    public ZookeeperElbFilter(String environment) {
        this.environment = environment;
    }

    @Override
    public boolean accept(LoadBalancerDescription loadBalancer) {

        String[] pieces = loadBalancer.getLoadBalancerName().split("-");

        if (pieces.length < 2) {
            return false;
        }
        // match ENV-Zoo* (Cloud Formation naming scheme)
        if (pieces[0].equalsIgnoreCase(environment) && pieces[1].startsWith("Zoo")) {
            return true;
        }
        // match exhibitor-ENV-internal (Original naming scheme)
        if (pieces.length == 3 && pieces[0].equalsIgnoreCase("exhibitor") && pieces[1].equalsIgnoreCase(environment) && pieces[2].equalsIgnoreCase("internal")) {
            return true;
        }

        return false;

    }

}
