package com.drrcp.reporting.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurationSupport;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@EnableWs
@Configuration
public class WsConfig extends WsConfigurationSupport {

    private static final String NAMESPACE = "http://drrcp.com/reporting";

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext context) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(context);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean
    public XsdSchema reliefReportSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/relief-report.xsd"));
    }

    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("com.drrcp.reporting.generated");
        return marshaller;
    }

    @Bean(name = "relief-report")
    public DefaultWsdl11Definition reliefReportWsdl(XsdSchema reliefReportSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("ReliefReportPort");
        definition.setLocationUri("/ws");
        definition.setTargetNamespace(NAMESPACE);
        definition.setRequestSuffix("Request");
        definition.setResponseSuffix("Response");
        definition.setSchema(reliefReportSchema);
        return definition;
    }
}