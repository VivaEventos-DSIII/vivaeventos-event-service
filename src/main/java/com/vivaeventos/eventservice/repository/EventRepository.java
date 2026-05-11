package com.vivaeventos.eventservice.repository;

import com.vivaeventos.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

        List<Event> findByStatusAndAvailableTicketsGreaterThanOrderByEventDateAsc(
                Event.EventStatus status,
                Integer availableTickets
                );
    }