package com.kixeye.chassis.bootstrap.aws;

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
