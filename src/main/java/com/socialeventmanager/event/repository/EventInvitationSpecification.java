package com.socialeventmanager.event.repository;

import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.user.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class EventInvitationSpecification {

    public static Specification<EventInvitation> hasUser(User user) {
        return (root, query, cb) -> cb.equal(root.get("invitedUser"), user);
    }

    public static Specification<EventInvitation> hasStatus(InvitationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}