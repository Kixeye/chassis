KIXEYE Chassis
=========

KIXEYE Chassis is a collection of independent Java libraries aimed at providing application components needed for quickly building and deploying
production-ready services in your own datacenter or in the Cloud.  

Features provided by the Chassis include:

  - **REST and WebSocket server support** - Implement REST APIs and WebSocket message handlers by annotating handler methods.
                                          
  - **Configure your applications with dynamic, hierarchical configurations** - provide hierarchical configuration from multiple sources to your application.  Respond to your configuration updates dynamically within your application.

  - **Object serialization** - Serialize and Deserialize your data using the serde library.  Support for json, bson, xml, protobuff, yml. All formats are supported are REST and WebSockets.

  - **Swagger integration** - Self document your APIs with the integrated Swagger documentation framework.  REST APIs are automatically scanned and exposed in Swagger.

  - **Dynamic Logging configuration** - Configure and dynamically update your logging configurations for running applications

  - **Flume Integration** - Write events to flume using your standard Logger.

  - **Metrics collection and publication** - Collect and publish application metrics to multiple destinations

  - **Spring integration** - Configure your applications using the Spring Framework.


Getting Started
--------------
The easiest way to get started with KIXEYE Chassis is by exploring the [Java Service Template](https://github.com/Kixeye/chassis-java-service-template) or the [Scala Service Template](https://github.com/Kixeye/chassis-scala-service-template).

To run one of the template applications, fork it and then:

```
>mvn exec:java
```

Once the application is started, navigate to http://localhost:8080/swagger in your web browser.

For further details about how to use KIXEYE Chassis, take a look at the [wiki](https://github.com/Kixeye/chassis/wiki).