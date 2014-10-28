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
        throw new BootstrapException("Found no environment data in user-data " + userData);
    }

    public String getEnvironment() {
        return environment;
    }

    public String toString(){
        return ENVIRONMENT_TEXT + environment;
    }
}
