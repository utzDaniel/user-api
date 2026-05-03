package br.com.user.modules.family;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class FamilyEventPublisher {

    private static final String QUEUE = "events";

    private final JmsTemplate jmsTemplate;

    public FamilyEventPublisher(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void publishFamilyCreated(Long familyId, String userId) {
        publish("FAMILY_CREATED", familyId, userId);
    }

    public void publishInvitationRequested(Long invitationId, String userId) {
        publish("FAMILY_INVITATION_REQUESTED", invitationId, userId);
    }

    public void publishInvitationApprovedByTitular(Long invitationId, String userId) {
        publish("FAMILY_INVITATION_APPROVED_BY_TITULAR", invitationId, userId);
    }

    public void publishInvitationRejectedByTitular(Long invitationId, String userId) {
        publish("FAMILY_INVITATION_REJECTED_BY_TITULAR", invitationId, userId);
    }

    public void publishInvitationAccepted(Long invitationId, String userId) {
        publish("FAMILY_INVITATION_ACCEPTED", invitationId, userId);
    }

    public void publishInvitationRejected(Long invitationId, String userId) {
        publish("FAMILY_INVITATION_REJECTED", invitationId, userId);
    }

    public void publishMemberRemoved(Long memberId, String userId) {
        publish("FAMILY_MEMBER_REMOVED", memberId, userId);
    }

    public void publishMemberUpdated(Long memberId, String userId) {
        publish("FAMILY_MEMBER_UPDATED", memberId, userId);
    }

    private void publish(String type, Long id, String userId) {
        Map<String, Object> event = Map.of(
                "type", type,
                "id", id,
                "timestamp", Instant.now().toString(),
                "userId", userId,
                "payload", Map.of()
        );
        jmsTemplate.convertAndSend(QUEUE, event);
    }
}
