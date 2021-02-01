package com.jun.event.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.jun.event.model.Event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface EventRepository extends ReactiveCrudRepository<Event, Long> {
	
	@Query("select * from event where aggregate_type = :aggregateType and aggregate_id = :aggregateId order by version desc limit 1")
	Mono<Event> findRecentlyEvent(String aggregateType, String aggregateId);
	
	@Query("select * from event where aggregate_type = :aggregateType and aggregate_id = :aggregateId and version > :version order by version")
	Flux<Event> findAfterSnapshotEvents(String aggregateType, String aggregateId, Long version);
}
