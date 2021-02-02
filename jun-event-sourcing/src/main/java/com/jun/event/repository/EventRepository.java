package com.jun.event.repository;

import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;

import com.jun.event.db.QueryExecutor;
import com.jun.event.model.Event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class EventRepository {
	
	private QueryExecutor queryExecutor;
	public EventRepository(DatabaseClient dbClient) {
		this.queryExecutor = new QueryExecutor(dbClient);
	}

	public Mono<Event> findRecentlyEvent(String aggregateType, UUID aggregateId){
		String sql = "select * from event where aggregate_type = :aggregateType and aggregate_id = :aggregateId order by version desc limit 1";
		return queryExecutor.findOne(sql, Event.class, aggregateType, aggregateId);
	};
	
	public Flux<Event> findAfterSnapshotEvents(String aggregateType, UUID aggregateId, Long version){
		String sql = "select * from event where aggregate_type = :aggregateType and aggregate_id = :aggregateId and version > :version order by version";
		return queryExecutor.findAll(sql, Event.class, aggregateType, aggregateId, version);
	};
	
	public Mono<Event> save(Event event) {
		return queryExecutor.save(event);
	}
}
