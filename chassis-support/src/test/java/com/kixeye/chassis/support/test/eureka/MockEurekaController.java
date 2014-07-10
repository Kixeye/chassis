package com.kixeye.chassis.support.test.eureka;

/*
 * #%L
 * Chassis Support
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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eureka/v2/")
public class MockEurekaController {

    @RequestMapping(value="/apps",method= RequestMethod.GET,produces = "application/json; charset=utf-8")
    public String mockApps() {
        final String response = "{\"applications\":{\"versions__delta\":1,\"apps__hashcode\":\"UP_4_\",\"application\":[{\"name\":\"EUREKA\",\"instance\":[{\"hostName\":\"ec2-54-200-137-11.us-west-2.compute.amazonaws.com\",\"app\":\"EUREKA\",\"ipAddr\":\"172.31.37.244\",\"status\":\"UP\",\"overriddenstatus\":\"UNKNOWN\",\"port\":{\"@enabled\":\"true\",\"$\":\"8184\"},\"securePort\":{\"@enabled\":\"false\",\"$\":\"443\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.AmazonInfo\",\"name\":\"Amazon\",\"metadata\":{\"availability-zone\":\"us-west-2b\",\"public-ipv4\":\"54.200.137.11\",\"instance-id\":\"i-dd15a3d4\",\"public-hostname\":\"ec2-54-200-137-11.us-west-2.compute.amazonaws.com\",\"local-ipv4\":\"172.31.37.244\",\"ami-id\":\"ami-3484e404\",\"instance-type\":\"m1.small\"}},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":%1$s,\"lastRenewalTimestamp\":%1$s,\"evictionTimestamp\":0,\"serviceUpTimestamp\":%1$s},\"metadata\":{\"@class\":\"java.util.Collections$EmptyMap\"},\"homePageUrl\":\"http:\\/\\/ec2-54-200-137-11.us-west-2.compute.amazonaws.com:8184\\/\",\"statusPageUrl\":\"http:\\/\\/ec2-54-200-137-11.us-west-2.compute.amazonaws.com:8184\\/Status\",\"healthCheckUrl\":\"http:\\/\\/ec2-54-200-137-11.us-west-2.compute.amazonaws.com:8184\\/healthcheck\",\"vipAddress\":\"ec2-54-200-137-11.us-west-2.compute.amazonaws.com:8184\",\"isCoordinatingDiscoveryServer\":true,\"lastUpdatedTimestamp\":%1$s,\"lastDirtyTimestamp\":%1$s,\"actionType\":\"ADDED\"},{\"hostName\":\"ec2-54-213-38-47.us-west-2.compute.amazonaws.com\",\"app\":\"EUREKA\",\"ipAddr\":\"172.31.30.68\",\"status\":\"UP\",\"overriddenstatus\":\"UNKNOWN\",\"port\":{\"@enabled\":\"true\",\"$\":\"8184\"},\"securePort\":{\"@enabled\":\"false\",\"$\":\"443\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.AmazonInfo\",\"name\":\"Amazon\",\"metadata\":{\"availability-zone\":\"us-west-2a\",\"public-ipv4\":\"54.213.38.47\",\"instance-id\":\"i-960c3b9f\",\"public-hostname\":\"ec2-54-213-38-47.us-west-2.compute.amazonaws.com\",\"local-ipv4\":\"172.31.30.68\",\"ami-id\":\"ami-3484e404\",\"instance-type\":\"m1.small\"}},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":%1$s,\"lastRenewalTimestamp\":%1$s,\"evictionTimestamp\":0,\"serviceUpTimestamp\":%1$s},\"metadata\":{\"@class\":\"java.util.Collections$EmptyMap\"},\"homePageUrl\":\"http:\\/\\/ec2-54-213-38-47.us-west-2.compute.amazonaws.com:8184\\/\",\"statusPageUrl\":\"http:\\/\\/ec2-54-213-38-47.us-west-2.compute.amazonaws.com:8184\\/Status\",\"healthCheckUrl\":\"http:\\/\\/ec2-54-213-38-47.us-west-2.compute.amazonaws.com:8184\\/healthcheck\",\"vipAddress\":\"ec2-54-213-38-47.us-west-2.compute.amazonaws.com:8184\",\"isCoordinatingDiscoveryServer\":false,\"lastUpdatedTimestamp\":%1$s,\"lastDirtyTimestamp\":%1$s,\"actionType\":\"ADDED\"}]},{\"name\":\"KVPSERVICE\",\"instance\":[{\"hostName\":\"ec2-54-201-147-243.us-west-2.compute.amazonaws.com\",\"app\":\"KVPSERVICE\",\"ipAddr\":\"172.31.12.59\",\"status\":\"UP\",\"overriddenstatus\":\"UNKNOWN\",\"port\":{\"@enabled\":\"true\",\"$\":\"8180\"},\"securePort\":{\"@enabled\":\"false\",\"$\":\"443\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.AmazonInfo\",\"name\":\"Amazon\",\"metadata\":{\"availability-zone\":\"us-west-2c\",\"public-ipv4\":\"54.201.147.243\",\"instance-id\":\"i-9202889a\",\"public-hostname\":\"ec2-54-201-147-243.us-west-2.compute.amazonaws.com\",\"local-ipv4\":\"172.31.12.59\",\"ami-id\":\"ami-3484e404\",\"instance-type\":\"m1.small\"}},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":%1$s,\"lastRenewalTimestamp\":%1$s,\"evictionTimestamp\":0,\"serviceUpTimestamp\":%1$s},\"metadata\":{\"@class\":\"java.util.Collections$EmptyMap\"},\"homePageUrl\":\"http:\\/\\/ec2-54-201-147-243.us-west-2.compute.amazonaws.com:8180\\/\",\"statusPageUrl\":\"http:\\/\\/ec2-54-201-147-243.us-west-2.compute.amazonaws.com:8180\\/Status\",\"healthCheckUrl\":\"http:\\/\\/ec2-54-201-147-243.us-west-2.compute.amazonaws.com:8180\\/healthcheck\",\"vipAddress\":\"kvpservice\",\"isCoordinatingDiscoveryServer\":false,\"lastUpdatedTimestamp\":%1$s,\"lastDirtyTimestamp\":%1$s,\"actionType\":\"ADDED\"},{\"hostName\":\"ec2-54-201-3-173.us-west-2.compute.amazonaws.com\",\"app\":\"KVPSERVICE\",\"ipAddr\":\"172.31.18.26\",\"status\":\"UP\",\"overriddenstatus\":\"UNKNOWN\",\"port\":{\"@enabled\":\"true\",\"$\":\"8180\"},\"securePort\":{\"@enabled\":\"false\",\"$\":\"443\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.AmazonInfo\",\"name\":\"Amazon\",\"metadata\":{\"availability-zone\":\"us-west-2a\",\"public-ipv4\":\"54.201.3.173\",\"instance-id\":\"i-2647792f\",\"public-hostname\":\"ec2-54-201-3-173.us-west-2.compute.amazonaws.com\",\"local-ipv4\":\"172.31.18.26\",\"ami-id\":\"ami-3484e404\",\"instance-type\":\"m1.small\"}},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":%1$s,\"lastRenewalTimestamp\":%1$s,\"evictionTimestamp\":0,\"serviceUpTimestamp\":%1$s},\"metadata\":{\"@class\":\"java.util.Collections$EmptyMap\"},\"homePageUrl\":\"http:\\/\\/ec2-54-201-3-173.us-west-2.compute.amazonaws.com:8180\\/\",\"statusPageUrl\":\"http:\\/\\/ec2-54-201-3-173.us-west-2.compute.amazonaws.com:8180\\/Status\",\"healthCheckUrl\":\"http:\\/\\/ec2-54-201-3-173.us-west-2.compute.amazonaws.com:8180\\/healthcheck\",\"vipAddress\":\"kvpservice\",\"isCoordinatingDiscoveryServer\":false,\"lastUpdatedTimestamp\":%1$s,\"lastDirtyTimestamp\":%1$s,\"actionType\":\"ADDED\"}]}]}}";
        return String.format(response,"" + System.currentTimeMillis());
    }

    @RequestMapping(value="/apps/delta",method= RequestMethod.GET,produces = "application/json; charset=utf-8")
    public String mockAppsDelta() {
        final String response = "{\"applications\":{\"versions__delta\":5951,\"apps__hashcode\":\"UP_5_\",\"application\":{\"name\":\"DEFAULT-SERVICE\",\"instance\":{\"hostName\":\"cbarry\",\"app\":\"DEFAULT-SERVICE\",\"ipAddr\":\"127.0.0.1\",\"status\":\"UP\",\"overriddenstatus\":\"UNKNOWN\",\"port\":{\"@enabled\":\"true\",\"$\":\"80\"},\"securePort\":{\"@enabled\":\"false\",\"$\":\"443\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\",\"name\":\"MyOwn\"},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":%1$s,\"lastRenewalTimestamp\":%1$s,\"evictionTimestamp\":0,\"serviceUpTimestamp\":%1$s},\"metadata\":{\"@class\":\"java.util.Collections$EmptyMap\"},\"homePageUrl\":\"http:\\/\\/cbarry:80\\/\",\"statusPageUrl\":\"http:\\/\\/cbarry:80\\/Status\",\"healthCheckUrl\":\"http:\\/\\/cbarry:80\\/healthcheck\",\"vipAddress\":\"default-service\",\"isCoordinatingDiscoveryServer\":false,\"lastUpdatedTimestamp\":%1$s,\"lastDirtyTimestamp\":%1$s,\"actionType\":\"ADDED\"}}}}";
        return String.format(response, "" + System.currentTimeMillis());
    }

    @RequestMapping(value="/apps/DEFAULT-SERVICE",method= RequestMethod.POST,produces = "application/json; charset=utf-8")
    public ResponseEntity<String> mockRegisterDefaultService() {
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value="/eureka/v2/apps/DEFAULT-SERVICE/{server}",method = RequestMethod.PUT,produces = "application/json; charset=utf-8")
    public ResponseEntity<String> mockUpdateDefaultService(@RequestParam String server) {
        return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value="/eureka/v2/apps/DEFAULT-SERVICE/{server}",method = RequestMethod.DELETE,produces = "application/json; charset=utf-8")
    public ResponseEntity<String> mockDeleteDefaultService(@RequestParam String server) {
        return new ResponseEntity<String>(HttpStatus.OK);
    }
}
