package com.socialeventmanager.event.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socialeventmanager.event.entity.Contribution;
import com.socialeventmanager.event.entity.Event;

public interface ContributionRepository extends JpaRepository<Contribution, UUID> {

    List<Contribution> findAllByEventOrderByCompletedAscCreatedAtAsc(Event event);
}