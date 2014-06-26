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
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for UserData
 *
 * @author dturner@kixeye.com
 */
public class UserDataTest {

    @Test(expected = BootstrapException.class)
    public void nullUserData(){
        UserData.parse(null);
    }

    @Test(expected = BootstrapException.class)
    public void emptyUserData(){
        UserData.parse(" ");
    }

    @Test(expected = BootstrapException.class)
    public void noEnvironment(){
        UserData.parse("FOO=BAR");
    }

    @Test
    public void singleLine(){
        Assert.assertEquals("myenvironment", UserData.parse(UserData.ENVIRONMENT_TEXT+"myenvironment").getEnvironment());
    }

    @Test
    public void multiLine(){
        String userData = UserData.ENVIRONMENT_TEXT+"myenvironment";
        userData += "\n";
        userData += "export KEY=VAL";

        Assert.assertEquals("myenvironment", UserData.parse(userData).getEnvironment());
    }

}
