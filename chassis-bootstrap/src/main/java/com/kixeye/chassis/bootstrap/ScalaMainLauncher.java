package com.kixeye.chassis.bootstrap;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A very simple Main class intended for launching "main" methods on Scala classes.
 * This class only works on Scala classes (it instantiates a new instance of the class) and not
 * scala objects.  The target Scala class is expected to have a method called "main" with a single Array[String]
 * parameter
 *
 * @author dturner@kixeye.com
 */
public class ScalaMainLauncher {

    public static void main(String [] args) throws Exception {
        Object obj = Class.forName(args[0]).newInstance();
        Method method = obj.getClass().getMethod("main", args.getClass());
        method.invoke(obj, new String[][]{Arrays.copyOfRange(args, 1, args.length)});
    }
}
