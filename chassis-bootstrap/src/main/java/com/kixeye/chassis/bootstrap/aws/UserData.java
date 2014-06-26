package com.kixeye.chassis.bootstrap.aws;

import com.kixeye.chassis.bootstrap.BootstrapException;
import org.apache.commons.lang.StringUtils;
import java.util.StringTokenizer;

/**
 * Represents the instance's userdata provided at launch time.
 *
 * @author dturner@kixeye.com
 */
public class UserData {

    public static final String ENVIRONMENT_TEXT = "export CLOUD_STACK=";

    public String environment;

    public UserData(String environment) {
        this.environment = environment;
    }

    public static UserData parse(String userData) {
        if (!StringUtils.isBlank(userData)) {
            StringTokenizer stringTokenizer = new StringTokenizer(userData, "\n");
            while (stringTokenizer.hasMoreTokens()) {
                String line = stringTokenizer.nextToken();
                int envStartIdx = line.indexOf(ENVIRONMENT_TEXT);
                if (envStartIdx >= 0) {
                    String env = line.substring(envStartIdx + ENVIRONMENT_TEXT.length());
                    return new UserData(StringUtils.trimToNull(env));
                }
            }
        }
        throw new BootstrapException("Found to environment data in user-data " + userData);
    }

    public String getEnvironment() {
        return environment;
    }

    public String toString(){
        return ENVIRONMENT_TEXT + environment;
    }
}
