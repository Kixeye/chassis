package com.kixeye.chassis.bootstrap.utils;

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

import com.kixeye.chassis.bootstrap.annotation.App;
import com.kixeye.chassis.bootstrap.annotation.BasicApp;
import com.kixeye.chassis.bootstrap.annotation.OnStart;
import com.kixeye.chassis.bootstrap.utils.TestApps.AnnotationForClass;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author dturner@kixeye.com
 */
@AnnotationForClass
public class TestApps {

    @BasicApp(name="BasicTestApp")
    public static class BasicTestApp {
        @OnStart
        public void onStart(){

        }
    }

    @TestAppAnnotation1
    public static class TestApp1{
        @OnStart
        public void onStart1(){
        }

        @OnStart
        public void onStart2(){
        }
    }

    @TestAppAnnotation1
    public static class TestApp2{
    }

    @TestAppAnnotation1
    @TestAppAnnotation2
    public static class TestAppWithMultipleAnnotations{
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @App
    public static @interface TestAppAnnotation1 {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @App
    public static @interface TestAppAnnotation2 {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface AnnotationForClass {
    }
}
