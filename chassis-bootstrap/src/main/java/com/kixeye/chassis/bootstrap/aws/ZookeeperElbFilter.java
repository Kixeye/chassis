package com.kixeye.chassis.bootstrap.aws;

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
