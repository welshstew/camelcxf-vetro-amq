/*
 * Copyright 2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package com.nullendpoint;

import com.nullendpoint.enrich.XmlEnricher;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.model.dataformat.XmlJsonDataFormat;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
// load regular Spring XML file from the classpath that contains the Camel XML DSL
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application extends RouteBuilder {
    /**
     * A main method to start this application.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void configure() throws Exception {

        XmlJsonDataFormat xmlJsonFormat = new XmlJsonDataFormat();

        from("{{cxf.endpoint.uri}}").routeId("vetro")
                .log("start VETRO")
                .convertBodyTo(String.class)
                .to("xslt:classpath:/xsl/removeSOAP.xsl")
                .doTry()
                    .to("validator:/wsdl/SimpleSchema.xsd") //validate
                    .process(new XmlEnricher()) //enrich
                    .marshal(xmlJsonFormat) //transform
                    .to("seda:sendtoactivemq") //route
                    .to("language:constant:resource:classpath:/static/successSOAP.xml") //operate (return to client caller)
                .endDoTry()
                .doCatch(ValidationException.class)
                    .log(LoggingLevel.ERROR, "invalid message")
                    .to("language:constant:resource:classpath:/static/failSOAP.xml")
                .doFinally()
                    .log("end VETRO")
                .end();

        from("seda:sendtoactivemq").setExchangePattern(ExchangePattern.InOnly).routeId("queueProducer")
                .log("Sending to ActiveMQ: ${body}")
                .convertBodyTo(String.class)
                .to("{{activemq.queue.uri}}");
    }
}