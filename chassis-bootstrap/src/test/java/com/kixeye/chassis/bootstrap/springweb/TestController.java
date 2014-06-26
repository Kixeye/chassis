package com.kixeye.chassis.bootstrap.springweb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Exposes an http endpoint used in a unit test.
 *
 * @author dturner@kixeye.com
 */
@Controller
public class TestController {

    @Value(SpringWebAppTest.KEY_PLACEHOLDER)
    private String property;

    @RequestMapping(value = "/getZookeeperProperty", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String ping() {
        return property;
    }
}
