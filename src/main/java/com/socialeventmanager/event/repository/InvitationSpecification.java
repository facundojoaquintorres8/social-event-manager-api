package com.socialeventmanager.event.repository;

import com.socialeventmanager.event.entity.EventInvitation;
import com.socialeventmanager.event.enums.InvitationStatus;
import com.socialeventmanager.user.entity.User;

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;

public class InvitationSpecification {

    public static Specification<EventInvitation> hasUser(User user) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("event", JoinType.LEFT);
                root.fetch("invitedBy", JoinType.LEFT);
            }
            return cb.equal(root.get("invitedUser"), user);
        };
    }

    public static Specification<EventInvitation> hasStatus(InvitationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<EventInvitation> hasNotStatus(InvitationStatus status) {
        return (root, query, cb) -> cb.notEqual(root.get("status"), status);
    }
}