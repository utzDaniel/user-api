package br.com.user.modules.profile;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class ProfileEventPublisher {

    private static final String QUEUE = "events";

    private final JmsTemplate jmsTemplate;

    public ProfileEventPublisher(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void publishProfileUpdated(Long profileId, String username) {
        publish("PROFILE_UPDATED", profileId, username, Map.of());
    }

    public void publishProfilePasswordChanged(Long profileId, String username) {
        publish("PROFILE_PASSWORD_CHANGED", profileId, username, Map.of());
    }

    private void publish(String type, Long id, String userId, Map<String, Object> payload) {
        Map<String, Object> event = Map.of(
                "type", type,
                "id", id,
                "timestamp", Instant.now().toString(),
                "userId", userId,
                "payload", payload
        );
        jmsTemplate.convertAndSend(QUEUE, event);
    }
}
