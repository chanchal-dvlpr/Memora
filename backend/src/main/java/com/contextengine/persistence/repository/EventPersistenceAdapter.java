package com.contextengine.persistence.repository;

import com.contextengine.application.event.UniversalEventFrame;
import com.contextengine.application.port.EventJournal;
import com.contextengine.persistence.entity.EventEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adapter implementing the EventJournal port using JPA and Spring Data repository layers.
 * Performs serialization and deserialization of the event frame payloads using Jackson.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class EventPersistenceAdapter implements EventJournal {

    private final SpringDataEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the EventPersistenceAdapter.
     *
     * @param eventRepository Spring Data JPA Repository interface
     * @param objectMapper Jackson Mapper for JSON transformations
     */
    public EventPersistenceAdapter(SpringDataEventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "SpringDataEventRepository must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper must not be null");
    }

    @Override
    @Transactional
    public synchronized void persist(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");

        String projectIdStr = envelope.projectId() != null ? envelope.projectId().toString() : null;
        Long nextSeq = eventRepository.findMaxSequenceNum(projectIdStr).orElse(0L) + 1;

        EventEntity entity = new EventEntity();
        entity.setId(envelope.eventId().toString());
        entity.setProjectId(projectIdStr);
        entity.setName(envelope.topic());
        entity.setSequenceNum(nextSeq);
        entity.setOccurredAt(envelope.timestamp());
        entity.setCorrelationId(envelope.correlationId().toString());
        entity.setCausationId(envelope.causationId().toString());
        entity.setVersion(envelope.version());

        try {
            String payloadJson = objectMapper.writeValueAsString(envelope.payload());
            entity.setPayload(payloadJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload to JSON", e);
        }

        eventRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UniversalEventFrame> retrieveEvents(UUID projectId, Long startSequenceNum) {
        String projectIdStr = projectId != null ? projectId.toString() : null;
        Long startSeq = startSequenceNum != null ? startSequenceNum : 1L;

        List<EventEntity> entities = eventRepository.findEvents(projectIdStr, startSeq);
        List<UniversalEventFrame> frames = new ArrayList<>();

        for (EventEntity entity : entities) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payloadMap = objectMapper.readValue(entity.getPayload(), Map.class);
                frames.add(new UniversalEventFrame(
                    UUID.fromString(entity.getId()),
                    entity.getName(),
                    entity.getOccurredAt(),
                    UUID.fromString(entity.getCorrelationId()),
                    UUID.fromString(entity.getCausationId()),
                    entity.getProjectId() != null ? UUID.fromString(entity.getProjectId()) : null,
                    entity.getVersion(),
                    payloadMap
                ));
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize event payload from JSON", e);
            }
        }
        return frames;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getLatestSequenceNum(UUID projectId) {
        String projectIdStr = projectId != null ? projectId.toString() : null;
        return eventRepository.findMaxSequenceNum(projectIdStr).orElse(0L);
    }
}
