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

import com.kixeye.chassis.bootstrap.spring.AbstractSpringApplicationContainer;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.util.SystemPropertyUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Some simple test utilities
 *
 * @author dturner@kixeye.com
 */
public class TestUtils {
//    private static Logger logger = LoggerFactory.getLogger(TestUtils.class);

    public static Thread stopAppAfterLaunch() {
        Thread watcher = new Thread(new Runnable() {
            @Override
            public void run() {
                while (AppMain.application == null || !AppMain.application.isRunning()) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                AppMain.application.stop();
            }
        });
        watcher.start();
        return watcher;
    }

    public static OutputStream createFile(String path) {
        try {
            Path p = Paths.get(SystemPropertyUtils.resolvePlaceholders(path));
            if (Files.exists(p)) {
                Files.delete(p);
            }
            File file = new File(path);
            if (!file.getParentFile().exists()) {
                if(!file.getParentFile().mkdirs()){
                    throw new RuntimeException("Unable to create parent file(s) " + file.getParent());
                }
            }
            return Files.newOutputStream(p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void writePropertiesToFile(String path, Set<Path> filesCreated, SimpleEntry<String, String>... entries) {
        Properties properties = new Properties();
        for (SimpleEntry<String, String> entry : entries) {
            properties.put(entry.getKey(), entry.getValue());
        }
        Path p = Paths.get(SystemPropertyUtils.resolvePlaceholders(path));
        try (OutputStream os = createFile(p.toString())) {
            properties.store(os, "test properties");
            filesCreated.add(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(Set<Path> paths) {
        for (Path path : paths) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void deleteAll(CuratorFramework curatorFramework) {
        deletePath("/", curatorFramework);
    }

    /**
     * @param path
     * @param curatorFramework
     */
    public static void deletePath(String path, CuratorFramework curatorFramework) {
        try {
            List<String> children = curatorFramework.getChildren().forPath(path);
            if ("/zookeeper".equals(path)) {
                //can't delete zookeeper's own admin stuff
                return;
            }
            for (String child : children) {
                deletePath(path + (path.endsWith("/") ? "" : "/") + child, curatorFramework);
            }
            if ("/".equals(path)) {
                //can't delete the root
                return;
            }
            curatorFramework.delete().forPath(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void resetArchaius() throws Exception {
        //super hacky and brittle, but archaius provides no way to reset its configuration
        FieldUtils.writeStaticField(ConfigurationManager.class, "customConfigurationInstalled", false, true);
        FieldUtils.writeStaticField(DynamicPropertyFactory.class, "initializedWithDefaultConfig", false, true);
        FieldUtils.writeStaticField(ConfigurationManager.class, "instance", null, true);
        FieldUtils.writeStaticField(DynamicPropertyFactory.class, "config", null, true);
    }

    @SuppressWarnings("unchecked")
    public static void addAppProperties(String name, String environment, String version, CuratorFramework curator, SimpleEntry<String, String>... props) throws Exception {
        if (!environment.startsWith("/")) {
            environment = "/" + environment;
        }
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        if (!version.startsWith("/")) {
            version = "/" + version;
        }
        if (curator.checkExists().forPath(environment) == null) {
            curator.create().forPath(environment);
        }
        if (curator.checkExists().forPath(environment + name) == null) {
            curator.create().forPath(environment + name);
        }
        if (curator.checkExists().forPath(environment + name + version) == null) {
            curator.create().forPath(environment + name + version);
        }
        if (curator.checkExists().forPath(environment + name + version + "/config") == null) {
            curator.create().forPath(environment + name + version + "/config");
        }
        for (SimpleEntry<String, String> entry : props) {
            curator.create().forPath(environment + name + version + "/config/" + entry.getKey(), entry.getValue().getBytes());
        }
    }

    @SuppressWarnings("unchecked")
	public static <T> T getCustomAppInstance() {
        return (T) ((CustomApplicationContainer) AppMain.application.getApplicationContainer()).getCustomApplicationInstance();
    }

    public static AbstractApplicationContext getSpringContextFromApp() {
        return ((AbstractSpringApplicationContainer<?>) AppMain.application.getApplicationContainer()).getApplicationContext();
    }

    public static void blockUntilAppStarts() throws InterruptedException {
        while (AppMain.application == null || !AppMain.application.isRunning()) {
            Thread.sleep(200);
        }
    }

    public static Thread runAppAsServer(final String[] args) {
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AppMain.main(args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        serverThread.start();
        return serverThread;
    }
}
