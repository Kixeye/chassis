package com.kixeye.chassis.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the Application process as a SERVER (a blocking process)
 *
 * @author dturner@kixeye.com
 */
public class ServerProcess implements Process{
    private static Logger logger;

    private Application application;

    public ServerProcess(Application application){
        this.application = application;
        logger = LoggerFactory.getLogger(application.getName());
    }

    @Override
    public void run() {
        registerShutdownHook();
        application.start();
        while (application.isRunning()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new BootstrapException("Unexpected main thread interruption", e);
            }
        }
    }

    private void registerShutdownHook() {
        final Application thisApp = this.application;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                logger.debug("Shutdown detected. Shutting down " + thisApp.getName());
                thisApp.stop();
            }
        }));
    }
}
