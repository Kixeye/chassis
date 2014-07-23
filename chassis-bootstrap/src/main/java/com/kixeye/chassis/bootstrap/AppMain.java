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

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.collect.Iterators;
import com.kixeye.chassis.bootstrap.annotation.SpringApp;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.EnumerablePropertySource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Main bootstrapping class for Spring applications using this bootstrap library.
 * Run this class with -h argument for usage information.
 *
 * @author dturner@kixeye.com
 */
public final class AppMain {
    public static Application application;

    /**
     * Main entry method.
     *
     * @param args the application arguments
     */
    public static void main(String[] args) throws Exception {

        System.out.println("Starting application with arguments " + Arrays.toString(args));

        Arguments arguments = loadArguments(args);

        if (arguments == null) {
            return;
        }

        initializeLogging(arguments);

        application = new Application(arguments);
        application.start();

        registerShutdownHook();

        while (application.isRunning()) {
            //keep the main thread alive
            Thread.sleep(500);
        }
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                application.stop();
            }
        }));
    }

    private static void initializeLogging(Arguments arguments) {
        //bootstrap depends on logback for logging console output. if the clientApplication does not want logback, they
        //need to exclude logback from their project dependencies and initialize their own logging
        try {
            AppMain.class.getClassLoader().loadClass("ch.qos.logback.classic.LoggerContext");
        } catch (ClassNotFoundException e) {
            System.err.println("No Logback found in classpath. Skipping logging initialization. This is expected if you are configuring the logging system yourself. If you are not configuring logging yourself and would like to use logging provided by java-bootstrap, ensure that the Logback dependency jar exists in your classpath.");
            return;
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        BasicConfigurator.configure(context);
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        Level level = Level.valueOf(arguments.logLevel.name().toUpperCase());
        logger.setLevel(level);

        logger.info("Initialized logging at {} level", level);
    }

    private static Arguments loadArguments(String[] args) {
        Arguments arguments = new Arguments();
        CmdLineParser parser = new CmdLineParser(arguments);
        parser.setUsageWidth(150);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println();
            parser.printUsage(System.err);
            return null;
        }
        if (arguments.help) {
            parser.printUsage(System.err);
            return null;
        }

        return arguments;
    }

    private static enum LogLevel {
        trace, debug, info, warn, error
    }

    public static class Arguments {
        private Map<String, Object> map;

        @Option(required = false, name = "--environment", aliases = "-e", usage = "The environment to run in. If using Zookeeper (-z), a Zookerkeeper configuration should exist for the given value. This argument is required if not running the admin shell.")
        public String environment;

        @Option(required = false, name = "--zookeeper", aliases = "-z", usage = "The connection string ({host}:{port}) for the Zookeeper configuration SERVER. If omitted, the client application will not use Zookeeper for its configuration, but will instead use defaults. This option can NOT be used in conjunction with -x.")
        public String zookeeper;

        @Option(required = false, name = "--loglevel", aliases = "-l", usage = "Specifies the initial log level that will be used until a logging configuration is picked up configured. Defaults to info.")
        public LogLevel logLevel = LogLevel.info;

        @Option(hidden = true, required = false, name = "--appclass", aliases = "-a", usage = "Explicitly defines the class annotated with @App. If used, scanning for the @App class will be omitted. This is for testing purposes only.")
        public String appClass;

        @Option(hidden = true, required = false, name = "--skipModuleScanning", aliases = "-s", usage = "Skip dependency library scanning")
        public boolean skipModuleScanning = false;

        @Option(required = false, name = "--help", aliases = "-h", usage = "Display usage information")
        public boolean help;

        @Option(required = false, handler = StringArrayOptionHandler.class, name = "--exhibitors", aliases = "-x", usage = "A space separated list of IP addresses or hostnames resolving to the Exhibitor servers managing the application's Zookeeper cluster.  When used, java-bootsrap will use the provided by the given Exhibitor cluster. The option can NOT be used in conjunction with -z.")
        public String[] exhibitorHosts;

        @Option(required = false, name = "--exhibitors-port", aliases = "-xp", usage = "The port the Exhibitors servers are listening on. Defaults to Exhibitor's default (8080).")
        public int exhibitorPort = 8080;

        public String getZookeeperHost() {
            return (zookeeper == null || StringUtils.equalsIgnoreCase(zookeeper, "disabled")) ? null : zookeeper;
        }

        public String[] getExhibitorHosts() {
            return (exhibitorHosts == null || StringUtils.equalsIgnoreCase(exhibitorHosts[0], "disabled")) ? null : exhibitorHosts;
        }

        public Map<String, Object> asPropertyMap() {
            if (map == null) {
                initMap();
            }
            return map;
        }

        private void initMap() {
            map = new HashMap<>();
            map.put("appEnvironment", environment);
            map.put("zookeeperConnectionString", getZookeeperHost());
            map.put("appClass", appClass);
            map.put("exhibitorPort", exhibitorPort);
            map.put("exhibitorHosts", getExhibitorHosts());
            map.put("scanModuleConfigurations", !skipModuleScanning);
        }
    }

}
