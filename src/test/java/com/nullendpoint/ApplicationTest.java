package com.nullendpoint;


import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;


@RunWith(CamelSpringBootRunner.class)
@UseAdviceWith
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApplicationTest {

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    @LocalServerPort
    private int port;

    @BeforeClass
    public static void setupBeforeClass() {
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Before
    public void setup() throws Exception {
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("{{activemq.queue.uri}}").routeId("queueConsumer").autoStartup(true).to("mock:result");
            }
        });
    }


    @Test
    public void testValidSOAPRequest() throws Exception {



        Thread.sleep(5000);

        MockEndpoint mockResult = (MockEndpoint) camelContext.getEndpoint("mock:result");
        mockResult.expectedMessageCount(1);


        String requestContent
                = (String) template.requestBody("language:constant:resource:classpath:/requests/valid-request-1.xml", new Object());
        String expectedResponseContent
                = (String) template.requestBody("language:constant:resource:classpath:/requests/valid-response-1.xml", new Object());

        String expectedJSON
                = (String) template.requestBody("language:constant:resource:classpath:/json/expected.json", new Object());


        String actualResponse = (String) template.requestBody("direct:cxf", requestContent);

        Diff xmlDiff = XMLUnit.compareXML(expectedResponseContent, actualResponse);
        assert (xmlDiff.similar());

        mockResult.assertIsSatisfied();
        JSONAssert.assertEquals(expectedJSON, (String) mockResult.getExchanges().get(0).getIn().getBody(), false);

    }

}
