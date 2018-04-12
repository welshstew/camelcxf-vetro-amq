package com.nullendpoint.enrich

import groovy.xml.XmlUtil
import groovy.util.XmlSlurper
import org.apache.camel.Exchange
import org.apache.camel.Processor

class XmlEnricher implements Processor{
    @Override
    void process(Exchange exchange) throws Exception {

        def xml = new XmlSlurper().parseText(exchange.in.body)
        xml.appendNode {
            Status(){
                SomeStatusCode('OK')
                SomeStatusDescription('SAULGOODMAN')
            }
        }
        def writer = new StringWriter()
        XmlUtil.serialize(xml, writer)
        exchange.in.body = writer.toString()
    }
}
