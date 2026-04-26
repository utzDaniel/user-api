package br.com.user.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ActiveMQConfigTest {

    private static final String BROKER_URL = "tcp://localhost:61616";

    private ActiveMQConfig activeMQConfig;

    @BeforeEach
    void setUp() {
        activeMQConfig = new ActiveMQConfig();
        ReflectionTestUtils.setField(activeMQConfig, "brokerUrl", BROKER_URL);
    }

    @Test
    void activeMQConnectionFactoryShouldBeConfiguredWithBrokerUrl() {
        ActiveMQConnectionFactory factory = activeMQConfig.activeMQConnectionFactory();
        assertNotNull(factory);
        assertEquals(BROKER_URL, factory.getBrokerURL());
    }

    @Test
    void jacksonJmsMessageConverterShouldBeConfigured() {
        JacksonJsonMessageConverter converter = activeMQConfig.jacksonJmsMessageConverter();
        assertNotNull(converter);
    }

    @Test
    void jmsTemplateShouldBeCreatedWithConnectionFactory() {
        JmsTemplate jmsTemplate = activeMQConfig.jmsTemplate();
        assertNotNull(jmsTemplate);
        assertNotNull(jmsTemplate.getMessageConverter());
    }

    @Test
    void jmsListenerContainerFactoryShouldBeConfigured() {
        DefaultJmsListenerContainerFactory factory = activeMQConfig.jmsListenerContainerFactory();
        assertNotNull(factory);
    }
}
