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

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import java.util.ArrayList;
import java.util.List;

/**
 * For simplifying calls to AWS.
 *
 * @author dturner@kixeye.com
 */
public class AwsUtils {

    public static final String TAG_KEY_NAME = "Name";

    /**
     * Fetches and instance's name Tag or null if it does not have one
     * @param instanceId
     * @param amazonEC2
     * @return
     */
    public static String getInstanceName(String instanceId, AmazonEC2 amazonEC2){
        DescribeTagsResult result = amazonEC2.describeTags(new DescribeTagsRequest().withFilters(
                new Filter().withName("resource-id").withValues(instanceId),
                new Filter().withName("resource-type").withValues("instance"),
                new Filter().withName("key").withValues(TAG_KEY_NAME)));
        if(result.getTags().isEmpty()){
            return null;
        }
        String name = result.getTags().get(0).getValue();
        return name == null || name.trim().equals("") ? null : name;
    }

    /**
     * Fetches and filters a Region's ELBs
     * @param amazonElasticLoadBalancing
     * @param filter
     * @return
     */
    public static List<LoadBalancerDescription> findLoadBalancers(AmazonElasticLoadBalancing amazonElasticLoadBalancing, ELBFilter filter) {
        List<LoadBalancerDescription> loadBalancers = amazonElasticLoadBalancing.describeLoadBalancers().getLoadBalancerDescriptions();
        List<LoadBalancerDescription> result = new ArrayList<>(loadBalancers.size());
        for(LoadBalancerDescription loadBalancer:loadBalancers){
            if(filter.accept(loadBalancer)){
                result.add(loadBalancer);
            }
        }
        return result;
    }

    public interface
            ELBFilter{
        public boolean accept(LoadBalancerDescription loadBalancer);
    }
}
