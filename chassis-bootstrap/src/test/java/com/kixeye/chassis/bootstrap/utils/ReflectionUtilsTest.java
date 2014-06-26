package com.kixeye.chassis.bootstrap.utils;

import com.kixeye.chassis.bootstrap.BootstrapException;
import com.kixeye.chassis.bootstrap.annotation.BasicApp;
import com.kixeye.chassis.bootstrap.annotation.OnStart;
import com.kixeye.chassis.bootstrap.annotation.OnStop;
import com.kixeye.chassis.bootstrap.annotation.SpringApp;
import com.kixeye.chassis.bootstrap.utils.ReflectionUtils.AnnotationResult;
import com.kixeye.chassis.bootstrap.utils.TestApps.BasicTestApp;
import org.junit.Assert;
import org.junit.Test;
import org.reflections.Reflections;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Unit tests for ReflectionUtils
 *
 * @author dturner@kixeye.com
 */
public class ReflectionUtilsTest {

    private static final Reflections REFLECTIONS = new Reflections(ReflectionUtilsTest.class.getPackage().getName());

    @Test
    public void testFindApp(){
        AnnotationResult<Annotation> result = ReflectionUtils.findApp(REFLECTIONS, BasicApp.class);
        Assert.assertEquals(BasicTestApp.class, result.getType());
        Assert.assertEquals(BasicApp.class, result.getAnnotation().annotationType());
    }

    @Test
    public void testFindApp_notFound(){
        AnnotationResult<Annotation> result = ReflectionUtils.findApp(REFLECTIONS, SpringApp.class);
        Assert.assertNull(result);
    }

    @Test(expected = BootstrapException.class)
    public void testFindApp_multipleFound(){
        ReflectionUtils.findApp(REFLECTIONS, TestApps.TestAppAnnotation1.class);
    }

    @Test
    public void testFindAppInClass(){
        AnnotationResult<Annotation> result = ReflectionUtils.findAppInClass(TestApps.BasicTestApp.class);
        Assert.assertEquals(BasicTestApp.class, result.getType());
        Assert.assertEquals(BasicApp.class, result.getAnnotation().annotationType());
    }

    @Test
    public void testFindAppInClass_notFound(){
        AnnotationResult<Annotation> result = ReflectionUtils.findAppInClass(TestApps.class);
        Assert.assertNull(result);
    }

    @Test
    public void testFindAppInClass_notFound_noAnnotationsOnClass(){
        AnnotationResult<Annotation> result = ReflectionUtils.findAppInClass(ReflectionUtilsTest.class);
        Assert.assertNull(result);
    }

    @Test(expected = BootstrapException.class)
    public void testFindAppInClass_multipleFound(){
        ReflectionUtils.findAppInClass(TestApps.TestAppWithMultipleAnnotations.class);
    }

    @Test
    public void testFindMethodAnnotatedWith() throws NoSuchMethodException {
        Method method = ReflectionUtils.findMethodAnnotatedWith(TestApps.BasicTestApp.class, OnStart.class);
        Assert.assertEquals(TestApps.BasicTestApp.class.getMethod("onStart"), method);
    }

    @Test
    public void testFindMethodAnnotatedWith_notFound() throws NoSuchMethodException {
        Method method = ReflectionUtils.findMethodAnnotatedWith(TestApps.BasicTestApp.class, OnStop.class);
        Assert.assertNull(method);
    }

    @Test(expected = BootstrapException.class)
    public void testFindMethodAnnotatedWith_multipleFound() throws NoSuchMethodException {
        ReflectionUtils.findMethodAnnotatedWith(TestApps.TestApp1.class, OnStart.class);
    }

}
