KIXEYE Bootstrap
=========

The chassis-bootstrap library provides application bootstrapping functionality to start and configure your self-contained applications.

Let's start with a simple example:
````java
package com.kixeye.template;

import com.kixeye.chassis.bootstrap.annotation.App;
import com.kixeye.chassis.bootstrap.annotation.Destroy;
import com.kixeye.chassis.bootstrap.annotation.Init;
import org.apache.commons.configuration.Configuration;

@App(name = "MySampleApp")
public class SampleApp {

    @Init
    public static void initialize(Configuration configuration){
        //perform application initialization...
    }

    @Destroy
    public static void destroy(){
        //perform application teardown...
    }
    
}
````
In this simple example, we define our application "MySampleApp". When the application is started,  the _initialize_ method will be invoked, providing you with an instance of org.apache.commons.configuration.Configuration.  Since we've not given Bootstrap any details about how the application is configured, the given Configuration instance contains only System properties.  When the application is stopped, the _destroy_ method will be invoked, allowing you to perform any application teardown needed.

To run your SampleApp, compile and then:

````
java -Dapp.version=1.0 -cp ${myclasspath} com.kixeye.chassis.bootstrap.AppMain --environment local
````

The chassis-bootstrap jar is intended to be included as an application dependency in your application's classpath. It contains a Main class (_com.kixeye.chassis.bootstrap.AppMain_) that should be configured as the application's main entry point via your application jar's [Manifest](http://docs.oracle.com/javase/tutorial/deployment/jar/appman.html). The _-Dapp.version_ System Property can be ommitted if your application's Manifest contains the *Implementation-Version* for the version of your application.  Let's say for example, you packaged your application into a self contain executable jar file:

````
java -jar sampleapp.jar --environment local
````

Examples on how you can package your application can be found in the [Chassis Java Service Template](https://github.com/Kixeye/chassis-java-service-template) example application.

##Configuration
####Local Filesystem
So, Let's say your application gets its configuration from a properties file on the local file system.

````java
#sampleapp.properties
mypropkey=mypropvalue

@App(name = "MySampleApp",
     propertiesResourceLocation = "file://${user.dir}/conf/sampleapp.properties")
public class SampleApp {

    @Init
    public static void initialize(Configuration configuration){
        //perform application initialization...
        
        //returns "mypropvalue"
        String value = configuration.getString("mypropkey")
    }

    @Destroy
    public static void destroy(){
        //perform application teardown...
    }
    
}
````
When started, chassis-bootstrap will load the properties from _sampleapp.properties_, resolving the property _${user.dir}_ for you. The properties contained within _sampleapp.properties_ will be included in the given Configuration instance.

Additionally, a environment specific properties files can be defined in the same directory as the main config file. Properties in the environment config will override the properties with the same name from the base config. From above, an environment properties file _${user.dir}/conf/sampleapp-local.properties_ could be defined to provide environment specific properties for the _local_ environment

The returned Configuration instance will resolve properties in order:

* system properties
* environment file properties (if any)
* base application file properties

````java
#sampleapp.properties
mypropkey=mypropvalue

#sampleapp-local.properties
mypropkey=foo

#sampleapp-dev.properties
mypropkey=bar

@App(name = "MySampleApp",
     propertiesResourceLocation = "file://${user.dir}/conf/sampleapp.properties")
public class SampleApp {

    @Init
    public static void initialize(Configuration configuration){
        //perform application initialization...
        
        //returns "mypropvalue" if no environment specific files exist
        //returns "foo" if running in the "local" environment
        //returns "bar" if running in the "dev" environment
        String value = configuration.getString("mypropkey")
    }

    @Destroy
    public static void destroy(){
        //perform application teardown...
    }
    
}
````

The _propertiesResourceLocation_ does not actually have to resolve to the local filesystem. Properties can be resolved from the local filesytem, classpath, or url.

* file://${user.dir}/conf/sampleapp.properties
* classpath:org/sampleorg/sampleapp/sampleapp.properties
* url:http://sampleorg.org/sampleapp/config/sampleapp.properties

####Zookeeper
Chassis Bootstrap supports [Zookeeper](http://zookeeper.apache.org/) as a source of configuration.  In this case, Bootstrap will pull in configuration properties from Zookeeper nodes under the root path:

/{environment}/{application name}/{application version}/config

For example, 
````
/dev/MySampleApp/1.0/config
        /mypropkey1=mypropvalue1
        /mypropkey2=mypropvalue2
        ...

````
In Zookeeper, the property nodes are named with the property name (ie _mypropkey1_) and the node's value is the properties value (ie _mypropvalue2_).

To run your application with Zookeeper, start it with the _--zookeeper_ argument
````java
java -jar sampleapp.jar --environment dev -z {zookeeper host}:{zookeeper port}
````
When using Zookeeper for configuration, evaluation order is as follows:

* system properties
* zookeeper configuration
* environment file properties (if any)
* base application file properties (if any)

This makes it convenient to store your application's default properties with your application (in the classpath or filesystem), and override them where needed in Zookeeper.

If running Zookeeper in a cluster, Chassis Bootstrap integrates with Netflix's [Exhibitor](https://github.com/Netflix/exhibitor/wiki) cluster management application.  [Exhibitor](https://github.com/Netflix/exhibitor/wiki) can be queried (over REST) for a listing of all the Zookeeper instances participating in the cluster. Bootstrap will use the result from the [Exhibitor](https://github.com/Netflix/exhibitor/wiki) query to determine which Zookeeper to connect to.

````java
java -jar sampleapp.jar --environment dev --exhibitors {host1},{host2},... --exhibitors-port 80 (defaults to 8080)
````
