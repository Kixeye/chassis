package com.kixeye.chassis.bootstrap;

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

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration keys that Bootstrap configures.
 *
 * @author dturner@kixeye.com
 */
public enum BootstrapConfigKeys {

    ZOOKEEPER_MAX_RETRIES("bootstrap.zookeeper.max_retries"),
    ZOOKEEPER_INITIAL_SLEEP_MILLIS("bootstrap.zookeeper.initial_sleep_millis"),
    ZOOKEEPER_RETRIES_MAX_MILLIS("bootstrap.zookeeper.retries_max_millis"),
    ZOOKEEPER_SESSION_TIMEOUT_MILLIS("bootstrap.zookeeper.session_timeout_millis"),
    ZOOKEEPER_CONNECTION_TIMEOUT_MILLIS("bootstrap.zookeeper.connection_timeout_millis"),
    EXHIBITOR_URI_PATH("bootstrap.exhibitor.uri"),
    EXHIBITOR_POLL_INTERVAL("bootstrap.exhibitor.poll_interval_millis"),
    EXHIBITOR_MAX_RETRIES("bootstrap.exhibitor.max_retries"),
    EXHIBITOR_INITIAL_SLEEP_MILLIS("bootstrap.exhibitor.initial_sleep_millis"),
    EXHIBITOR_RETRIES_MAX_MILLIS("bootstrap.exhibitor.retries_max_millis"),
    EXHIBITOR_USE_HTTPS("bootstrap.exhibitor.use_https"),
    APP_VERSION_KEY("app.version"),
    APP_NAME_KEY("app.name"),
    APP_ENVIRONMENT_KEY("app.environment"),
    AWS_METADATA_TIMEOUTSECONDS("aws.metadata.timeoutseconds"),
    AWS_INSTANCE_ID("aws.instance.id"),
    AWS_INSTANCE_REGION("aws.instance.region"),
    AWS_INSTANCE_AVAILABILITY_ZONE("aws.instance.az"),
    AWS_INSTANCE_PRIVATE_IP("aws.instance.private_ip"),
    AWS_INSTANCE_PUBLIC_IP("aws.instance.public_ip"),
    AWS_INSTANCE_NAME("aws.instance.name"),
    ZOOKEEPER_CONFIG_BASE_PATH("bootstrap.zookeeper.config_base_path"),
    PUBLISH_DEFAULTS_TO_ZOOKEEPER_KEY("bootstrap.zookeeper.publish_defaults");

    private static Map<String, BootstrapConfigKeys> keysByPropertyName = new HashMap<>();

    static{
        for(BootstrapConfigKeys key: BootstrapConfigKeys.values()){
            keysByPropertyName.put(key.getPropertyName(), key);
        }
    }

    private String propertyName;

    private BootstrapConfigKeys(String propertyName){
        this.propertyName = propertyName;

    }

    public String getPropertyName(){
        return propertyName;
    }

    public static BootstrapConfigKeys fromPropertyName(String propertyName){
        return keysByPropertyName.get(propertyName);
    }
}
