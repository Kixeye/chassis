package com.kixeye.chassis.scala.transport

/*
 * #%L
 * Chassis Scala Transport Core
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

import org.springframework.context.annotation.{Import, ComponentScan, Bean, Configuration}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.kixeye.chassis.transport.TransportConfiguration

@Configuration
@Import(Array(classOf[TransportConfiguration]))
@ComponentScan(basePackageClasses = Array(classOf[ScalaTransportConfiguration]))
class ScalaTransportConfiguration {
  @Bean
  def jacksonScalaObjectMapper : ObjectMapper = {
    // Scala enabled Jackson ObjectMapper
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
  }
}
