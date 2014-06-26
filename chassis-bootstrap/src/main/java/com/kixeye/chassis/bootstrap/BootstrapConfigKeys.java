package com.kixeye.chassis.bootstrap;

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
