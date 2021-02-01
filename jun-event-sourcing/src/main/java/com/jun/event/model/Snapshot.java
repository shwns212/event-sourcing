package com.jun.event.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("snapshot")
public class Snapshot {
	@Id
	private UUID id;
	private UUID eventId;
	private String aggregateType;
	private UUID aggregateId;
	private Long version;
	private String payload;
	private LocalDateTime createdAt;
	
	public Snapshot() {}
	
	public Snapshot(UUID id, UUID eventId, String aggregateType, UUID aggregateId, Long version, String payload,
			LocalDateTime createdAt) {
		this.id = id;
		this.eventId = eventId;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.version = version;
		this.payload = payload;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getEventId() {
		return eventId;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public UUID getAggregateId() {
		return aggregateId;
	}

	public Long getVersion() {
		return version;
	}

	public String getPayload() {
		return payload;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
