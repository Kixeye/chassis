package com.kixeye.chassis.bootstrap.utils;

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
