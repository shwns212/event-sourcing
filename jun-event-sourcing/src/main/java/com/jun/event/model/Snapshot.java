package com.jun.event.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("snapshot")
public class Snapshot {
	@Id
	private Long id;
	private Long eventId;
	private String aggregateType;
	private String aggregateId;
	private Long version;
	private String payload;
	private LocalDateTime createdAt;
	
	public Snapshot() {}
	
	public Snapshot(Long eventId, String aggregateType, String aggregateId, Long version, String payload,
			LocalDateTime createdAt) {
		this.eventId = eventId;
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.version = version;
		this.payload = payload;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getEventId() {
		return eventId;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public String getAggregateId() {
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
