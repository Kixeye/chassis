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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.ListenerDescription;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.google.common.base.Joiner;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for AwsInstanceContext
 *
 * @author dturner@kixeye.com
 */
public class AwsInstanceContextTest {


    private static com.amazonaws.regions.Region eqRegion(final com.amazonaws.regions.Region region) {
        EasyMock.reportMatcher(new IArgumentMatcher() {
            @Override
            public boolean matches(Object o) {
                com.amazonaws.regions.Region givenRegion = (com.amazonaws.regions.Region) o;
                return givenRegion.getName().equals(region.getName());
            }

            @Override
            public void appendTo(StringBuffer stringBuffer) {
                stringBuffer.append("eqReqion for ").append(region.getName());
            }
        });
        return region;
    }

    public static CreateTagsRequest eqCreateTagsRequest(final CreateTagsRequest request) {
        EasyMock.reportMatcher(new IArgumentMatcher() {
            @Override
            public boolean matches(Object o) {
                CreateTagsRequest rq = (CreateTagsRequest) o;

                if(!(rq.getResources().size() == request.getResources().size())){
                    return false;
                }
                if(!(rq.getTags().size() == request.getTags().size())){
                    return false;
                }
                boolean equal = rq.getResources().equals(request.getResources());
                if(!equal){
                    return false;
                }
                for(Tag tag:rq.getTags()){
                    equal = false;
                    for(Tag rtag:request.getTags()){
                        if(EqualsBuilder.reflectionEquals(tag, rtag)){
                            equal = true;
                            continue;
                        }
                    }
                    if(!equal){
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void appendTo(StringBuffer stringBuffer) {
                stringBuffer.append("eqCreateTagsRequest for ").append(ReflectionToStringBuilder.toString(request));
            }
        });
        return request;
    }

    @Test
    public void initializeOutsideofAws() {
        Assert.assertNull(AwsInstanceContext.initialize());
    }

    @Test
    public void testValidContext() {
        final String region = Regions.US_WEST_2.getName();
        final String instanceId = RandomStringUtils.random(20, "abcdefghi");
        final String availabilityZone = region + RandomStringUtils.random(20, "abcdefghi");
        final String environment = RandomStringUtils.random(20, "abcdefghi");
        final String userData = new UserData(environment).toString();
        final String elbName = environment + "-Zookeeper";
        final String exhibitorUrl = "http://" + elbName + "-" + region + ".aws.amazon.com";
        final int exhibitorPort = 80;
        final String version  = "1.0.0";

        Ec2MetadataClient ec2MetadataClient = EasyMock.createMock(Ec2MetadataClient.class);

        EasyMock.expect(ec2MetadataClient.getAvailabilityZone()).andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
                return availabilityZone;
            }
        });

        EasyMock.expect(ec2MetadataClient.getInstanceId()).andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
                return instanceId;
            }
        });

        EasyMock.expect(ec2MetadataClient.getUserData()).andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
                return userData;
            }
        });

        EasyMock.expect(ec2MetadataClient.getPrivateIpAddress()).andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
                return "127.0.0.1";
            }
        });

        EasyMock.expect(ec2MetadataClient.getPublicIpAddress()).andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
                return "127.0.0.1";
            }
        });

        AmazonEC2 amazonEC2 = EasyMock.createMock(AmazonEC2.class);

        EasyMock.expect(amazonEC2.describeRegions()).andAnswer(new IAnswer<DescribeRegionsResult>() {
            @Override
            public DescribeRegionsResult answer() throws Throwable {
                DescribeRegionsResult result = new DescribeRegionsResult();
                List<Region> regions = new ArrayList<>();
                for (Regions region : Regions.values()) {
                    Region r = new Region();
                    r.setRegionName(region.getName());
                    regions.add(r);
                }
                result.setRegions(regions);
                return result;
            }
        });

        EasyMock.expect(amazonEC2.describeTags(EasyMock.anyObject(DescribeTagsRequest.class))).andAnswer(new IAnswer<DescribeTagsResult>() {
            @Override
            public DescribeTagsResult answer() throws Throwable {
                return new DescribeTagsResult();
            }
        });

        com.amazonaws.regions.Region r = com.amazonaws.regions.Region.getRegion(Regions.fromName(region));
        amazonEC2.setRegion(eqRegion(r));
        EasyMock.expectLastCall();

        AmazonElasticLoadBalancing amazonElasticLoadBalancing = EasyMock.createMock(AmazonElasticLoadBalancing.class);

        EasyMock.expect(amazonElasticLoadBalancing.describeLoadBalancers()).andAnswer(new IAnswer<DescribeLoadBalancersResult>() {
            @Override
            public DescribeLoadBalancersResult answer() throws Throwable {
                DescribeLoadBalancersResult result = new DescribeLoadBalancersResult();
                List<LoadBalancerDescription> loadBalancers = new ArrayList<>();
                LoadBalancerDescription lb1 = new LoadBalancerDescription();
                lb1.setDNSName(exhibitorUrl);
                lb1.setLoadBalancerName(elbName);
                List<ListenerDescription> listenerDescriptions = new ArrayList<>();

                ListenerDescription httpListenerDescription = new ListenerDescription();
                Listener httpListener = new Listener();
                httpListener.setProtocol("HTTP");
                httpListener.setLoadBalancerPort(exhibitorPort);
                httpListener.setInstancePort(8080);
                httpListenerDescription.setListener(httpListener);

                ListenerDescription httpsListenerDescription = new ListenerDescription();
                Listener httpsListener = new Listener();
                httpsListener.setProtocol("HTTPS");
                httpsListener.setLoadBalancerPort(443);
                httpsListener.setInstancePort(8080);
                httpsListenerDescription.setListener(httpListener);

                listenerDescriptions.add(httpListenerDescription);
                listenerDescriptions.add(httpsListenerDescription);

                lb1.setListenerDescriptions(listenerDescriptions);
                loadBalancers.add(lb1);
                result.setLoadBalancerDescriptions(loadBalancers);

                return result;
            }
        });

        amazonElasticLoadBalancing.setRegion(eqRegion(r));
        EasyMock.expectLastCall();

        EasyMock.replay(ec2MetadataClient, amazonEC2, amazonElasticLoadBalancing);

        AwsInstanceContext context = new AwsInstanceContext(ec2MetadataClient, amazonEC2, amazonElasticLoadBalancing);

        Assert.assertEquals(region, context.getRegion());
        Assert.assertEquals(instanceId, context.getInstanceId());
        Assert.assertEquals(availabilityZone, context.getAvailabilityZone());
        Assert.assertEquals(environment, context.getEnvironment());
        Assert.assertEquals(exhibitorUrl, context.getExhibitorHost());
        Assert.assertEquals(exhibitorPort, context.getExhibitorPort());
        Assert.assertNull(context.getAppName());

        EasyMock.verify(ec2MetadataClient, amazonEC2, amazonElasticLoadBalancing);

        EasyMock.reset(amazonEC2, amazonElasticLoadBalancing);

        amazonEC2.shutdown();
        EasyMock.expectLastCall();

        amazonElasticLoadBalancing.shutdown();
        EasyMock.expectLastCall();

        String name = RandomStringUtils.random(20, "abcdefghi");

        CreateTagsRequest request = new CreateTagsRequest();

        String tagName = Joiner.on("-").join(environment, name, version);

        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag().withKey("Name").withValue(tagName));
        tags.add(new Tag().withKey("Environment").withValue(environment));
        tags.add(new Tag().withKey("Version").withValue(version));
        tags.add(new Tag().withKey("Application").withValue(name));
        request.setTags(tags);
        ArrayList<String> resources = new ArrayList<>();
        resources.add(instanceId);
        request.setResources(resources);

        amazonEC2.createTags(eqCreateTagsRequest(request));
        EasyMock.expectLastCall();

        EasyMock.replay(amazonEC2, amazonElasticLoadBalancing);

        context.setAppName(name);
        context.setVersion(version);
        context.tagInstance(tagName);

        Assert.assertEquals(name, context.getAppName());

        context.shutdown();

        EasyMock.verify(ec2MetadataClient, amazonEC2, amazonElasticLoadBalancing);
    }
}
