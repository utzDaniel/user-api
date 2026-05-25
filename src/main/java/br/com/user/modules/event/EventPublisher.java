package br.com.user.modules.event;

import br.com.user.config.TimestampUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventPublisher {

    private static final String QUEUE = "events";

    private final JmsTemplate jmsTemplate;

    public EventPublisher(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void publish(EventType type, String userId, Object payload) {
        Map<String, Object> event = Map.of(
                "type", type.name(),
                "timestamp", TimestampUtils.now(),
                "userId", userId,
                "payload", payload
        );
        jmsTemplate.convertAndSend(QUEUE, event);
    }
}

