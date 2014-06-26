package com.kixeye.chassis.bootstrap;

import java.lang.annotation.Annotation;

import com.kixeye.chassis.bootstrap.utils.ReflectionUtils;
import com.kixeye.chassis.bootstrap.utils.ReflectionUtils.AnnotationResult;

/**
 * Application default no-op implementation.
 *
 * @author dturner@kixeye.com
 */
public class DefaultApplication implements Application {
    private ApplicationDefinition definition;
    private AppMain.Arguments arguments;
    private boolean running = false;

    public DefaultApplication(AppMain.Arguments arguments) {
        this.arguments = arguments;
        AnnotationResult<Annotation> appResult;
        if (arguments.appClass != null) {
            appResult = manuallyLoadMetadata(arguments.appClass);
        } else {
            appResult = ReflectionUtils.findApp();
        }
        if(appResult == null){
            throw new BootstrapException("Found no @App classes to load.");
        }
        definition = ApplicationDefinition.create(appResult.getAnnotation(), appResult.getType());
    }

    private AnnotationResult<Annotation> manuallyLoadMetadata(String appClass) {
        Class<?> annotatedClass;
        try {
            annotatedClass = AppMain.class.getClassLoader().loadClass(appClass);
        } catch (ClassNotFoundException e) {
            throw new BootstrapException("Failed to load setAnnotatedClassName " + appClass
                    , e);
        }
        return ReflectionUtils.findAppInClass(annotatedClass);
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public String getName() {
        return definition.getAppName();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public ApplicationDefinition getDefinition() {
        return definition;
    }

    public AppMain.Arguments getArguments() {
        return arguments;
    }

    @Override
    public ApplicationContainer getApplicationContainer() {
        return new ApplicationContainer() {
            @Override
            public void onStart() {
            }

            @Override
            public void onStop() {

            }
        };
    }
}
