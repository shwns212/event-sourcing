package com.jun.event.repository;

import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;

import com.jun.event.db.QueryExecutor;
import com.jun.event.model.Snapshot;

import reactor.core.publisher.Mono;

public class SnapshotRepo {
	
	private QueryExecutor queryExecutor;
	public SnapshotRepo(DatabaseClient dbClient) {
		this.queryExecutor = new QueryExecutor(dbClient);
	}
	
	public Mono<Snapshot> findRecentlySnapshot(String aggregateType, UUID aggregateId) {
		String sql = "select * from snapshot where aggregate_type = :aggregateType and aggregate_id = :aggregateId order by version desc limit 1";
		return queryExecutor.findOne(sql, Snapshot.class, aggregateType, aggregateId);
	};
	
	public Mono<Snapshot> save(Snapshot snapshot) {
		return queryExecutor.save(snapshot);
	}
}
