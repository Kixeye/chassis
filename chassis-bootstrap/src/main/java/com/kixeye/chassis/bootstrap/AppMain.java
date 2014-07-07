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

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import com.kixeye.chassis.bootstrap.Process.Type;
import com.kixeye.chassis.bootstrap.aws.AwsInstanceContext;

/**
 * Main bootstrapping class for Spring applications using this bootstrap library.
 * Run this class with -h argument for usage information.
 *
 * @author dturner@kixeye.com
 */
public final class AppMain {
    public static ApplicationContainerFactory applicationContainerFactory = new ApplicationContainerFactory();
    public static ApplicationFactory applicationFactory = new ApplicationFactory(applicationContainerFactory);
    public static ProcessFactory processFactory = new ProcessFactory();
    public static Application application;
    private static Arguments arguments;
    private static boolean loggingConfigured = false;

    public static final Reflections reflections = new Reflections("");

    /**
     * Main entry method.
     *
     * @param args clientApplication args
     */
    public static void main(String[] args) throws Exception {
        initializeLogging();

        AwsInstanceContext awsInstanceContext = AwsInstanceContext.initialize();

        loadArguments(args, awsInstanceContext);

        if (AppMain.arguments == null) {
            return;
        }

        resetLoggingIfNecessary();

        AppMain.application = applicationFactory.getApplication(arguments, awsInstanceContext);
        try {
            processFactory.getProcess(AppMain.application, arguments).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.print(AppMain.application.getName() + " stopped.");
    }

    private static void initializeLogging() {
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
        logger.setLevel(Level.INFO);

        logger.info("Initialized logging at INFO level");

        loggingConfigured = true;
    }

    private static void resetLoggingIfNecessary() {
        if (!loggingConfigured) {
            return;
        }

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        if (logger.getLevel() == Level.valueOf(arguments.logLevel.name().toUpperCase())) {
            return;
        }

        logger.setLevel(Level.valueOf(arguments.logLevel.name().toUpperCase()));
    }

    private static void loadArguments(String[] args, AwsInstanceContext awsInstanceContext) {
        Arguments arguments = new Arguments();
        CmdLineParser parser = new CmdLineParser(arguments);
        parser.setUsageWidth(150);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println();
            parser.printUsage(System.err);
            return;
        }
        if (arguments.help) {
            parser.printUsage(System.err);
            return;
        }
        if (arguments.getProcessType() == Type.SERVER && StringUtils.isBlank(arguments.environment)) {
            if (awsInstanceContext == null) {
                System.err.println("Environment is required.");
                System.err.println();
                parser.printUsage(System.err);
                return;
            }
            arguments.environment = awsInstanceContext.getEnvironment();
        }
        if (arguments.getExhibitorHosts() == null && arguments.getZookeeperHost() == null) {
            // No exhibitor or zookeeper specified, try to get exhibitor from AWS instance context
            if (awsInstanceContext != null) {
                arguments.exhibitorHosts = new String[]{awsInstanceContext.getExhibitorHost()};
                arguments.exhibitorPort = awsInstanceContext.getExhibitorPort();
            }
        }

        // shove startup parameters into properties for easy viewing
        System.setProperty("debug.arguments.environment", (arguments.environment == null) ? "null" : arguments.environment);
        System.setProperty("debug.arguments.zookeeper", (arguments.zookeeper == null) ? "null" : arguments.zookeeper );
        System.setProperty("debug.arguments.exhibitorHosts", (arguments.exhibitorHosts == null) ? "null" : StringUtils.join(arguments.exhibitorHosts,","));
        System.setProperty("debug.arguments.exhibitorPort", "" + arguments.exhibitorPort);

        AppMain.arguments = arguments;
    }

    private static enum LogLevel {
        trace, debug, info, warn, error
    }

    public static class Arguments {
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

        @Option(required = false, name = "--extract-config", aliases = "-c", usage = "Whether to extract the known configs", depends = {"-e"})
        public boolean extractConfigs;

        @Option(required = false, name = "--force-extract-config", aliases = "-cf", usage = "Whether to extract the known configs", depends = {"-c", "-e"})
        public boolean forceExtractConfigs;

        @Option(required = false, name = "--extract-config-file", aliases = "-f", usage = "Extracts the known configs for this app to this file.", depends = {"-c", "-e"})
        public String extractConfigFile;

        @Option(hidden = true, required = false, name = "--processtype", aliases = "-p", usage = "The type of process to run in. Valid values are: SERVER, shell")
        public Type processType = Type.SERVER;

        @Option(required = false, handler = StringArrayOptionHandler.class, name = "--exhibitors", aliases = "-x", usage = "A space separated list of IP addresses or hostnames resolving to the Exhibitor servers managing the application's Zookeeper cluster.  When used, java-bootsrap will use the provided by the given Exhibitor cluster. The option can NOT be used in conjunction with -z.")
        String[] exhibitorHosts;

        @Option(required = false, name = "--exhibitors-port", aliases = "-xp", usage = "The port the Exhibitors servers are listening on. Defaults to Exhibitor's default (8080).")
        int exhibitorPort = 8080;

        public Type getProcessType() {
            if (extractConfigs) {
                return Type.EXTRACT_CONFIGS;
            }
            return processType;
        }

        public String getZookeeperHost() {
            return (zookeeper == null || StringUtils.equalsIgnoreCase(zookeeper, "disabled")) ? null : zookeeper;
        }

        public String[] getExhibitorHosts() {
            return (exhibitorHosts == null || StringUtils.equalsIgnoreCase(exhibitorHosts[0], "disabled")) ? null : exhibitorHosts;
        }
    }
}
