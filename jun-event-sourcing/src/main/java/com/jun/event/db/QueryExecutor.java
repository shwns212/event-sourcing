package com.jun.event.db;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.r2dbc.core.Parameter;

import com.jun.event.exception.FailedEventSaveException;
import com.jun.event.util.ExtStringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class QueryExecutor {
	
	private DatabaseClient dbClient;
	
	public QueryExecutor(DatabaseClient dbClient) {
		this.dbClient = dbClient;
	}

	public <T> Mono<T> save(T entity) {
		String tableName = entity.getClass().getDeclaredAnnotation(Table.class).value(); // 테이블명
		List<String> columnList = new ArrayList<>(); // insert into table (...) 에 들어갈 컬럼명 리스트
		List<String> paramList = new ArrayList<>(); // values (...) 에 들어갈 바인드 파라미터명 리스트
		List<Object> valueList = new ArrayList<>(); // 실제 파라미터 값 리스트
		
		Field[] fields = entity.getClass().getDeclaredFields();
		
		// 컬럼명, 파라미터명, 값 채우기
		for(Field field : fields) {
			try {
				field.setAccessible(true);
				columnList.add(ExtStringUtils.camelCaseToUnderScore(field.getName()));
				paramList.add(":"+field.getName());
				valueList.add(field.get(entity));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new FailedEventSaveException(e);
			}
		}
		
		// 쿼리 생성
		String sql =  new StringBuilder()
		.append("insert into ").append(tableName).append(" (")
		.append(String.join(",", columnList))
		.append(") values (")
		.append(String.join(",",paramList))
		.append(")").toString();
		
		// 쿼리 실행
		GenericExecuteSpec genericExecuteSpec = dbClient.sql(sql.toString());
		int i = 0;
		for(Object obj : valueList) {
			String parameterName = paramList.get(i).substring(1); // :(콜론)을 제거한 파라미터명
			if(obj == null) {
				genericExecuteSpec = genericExecuteSpec.bindNull(parameterName, Object.class);
			}else {
				genericExecuteSpec = genericExecuteSpec.bind(parameterName, obj);
			}
			i++;
		}
		
		// 등록된 엔티티 반환
		return (Mono<T>) genericExecuteSpec.fetch()
				.rowsUpdated()
				.thenReturn(entity);
	}
	
	/**
	 * 한개의 결과 반환
	 * @param <T>
	 * @param sql
	 * @param clazz
	 * @param params
	 * @return
	 */
	public <T> Mono<T> findOne(String sql, Class<T> clazz, Object... params) {
		return (Mono<T>) execute(sql,clazz, genericExecuteSpec -> {
			return genericExecuteSpec.fetch()
			.one()
			.map(x -> {
				return mapToObject(x, clazz);
			});
		}, params);
		
	}
	
	/**
	 * 다수의 결과 반환
	 * @param <T>
	 * @param sql
	 * @param clazz
	 * @param params
	 * @return
	 */
	public <T> Flux<T> findAll(String sql, Class<T> clazz, Object... params) {
		return (Flux<T>) execute(sql,clazz, genericExecuteSpec -> {
			return genericExecuteSpec.fetch()
			.all()
			.map(x -> {
				return mapToObject(x, clazz);
			});
		}, params);
		
	}
	
	/**
	 * 쿼리 실행부
	 * @param <T>
	 * @param sql
	 * @param clazz
	 * @param function
	 * @param params
	 * @return
	 */
	private <T> Publisher<T> execute(String sql, Class<T> clazz, Function<GenericExecuteSpec, Publisher<T>> function, Object... params) {
		int paramIndex = 0;
		List<String> paramList = new ArrayList<>();
		
		// where param = '{name:test}' 일 경우 :test를 쿼리 파라미터로 인식하므로 정규식으로 제거 -> param = '' 으로 치환한다.
		String replaceSql = sql.replaceAll("('([^ ]+)')", "''");
		
		while(true) {
			paramIndex = replaceSql.indexOf(":",paramIndex+1); // 예) :parameter 
			
			if(paramIndex == -1) break; // 파라미터가 더이상 존재하지 않으면 break;
			
			int spaceIndex = replaceSql.indexOf(" ", paramIndex+1); // : 부터 공백까지 단어
			
			paramList.add(replaceSql.substring(paramIndex+1, spaceIndex)); // 파라미터 리스트에 추가
		}
		
		GenericExecuteSpec genericExecuteSpec = dbClient.sql(sql);
		
		// 파라미터 세팅
		for(int i=0; i<params.length; i++) {
			genericExecuteSpec = genericExecuteSpec.bind(paramList.get(i), params[i]);
		}
		
		// execute
		return function.apply(genericExecuteSpec);
	}
	
	/**
	 * Map -> Object변환
	 * @param <T>
	 * @param clazz
	 * @param map
	 * @return
	 */
	private <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
		try {
			T entity = clazz.newInstance();
			Field[] fields = entity.getClass().getDeclaredFields();
			for(Field field : fields) {
				field.setAccessible(true);
				field.set(entity, map.getOrDefault(ExtStringUtils.camelCaseToUnderScore(field.getName()), null));
			}
			return entity;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException();
		}
	}
	
	
}
