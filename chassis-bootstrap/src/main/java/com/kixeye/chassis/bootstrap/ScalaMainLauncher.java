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
        Method method = obj.getClass().getMethod(args[1], args.getClass());
        method.invoke(obj, new String[][]{Arrays.copyOfRange(args, 2, args.length)});
    }
}
