package com.jun.event.service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jun.event.annotation.EventHandler;
import com.jun.event.annotation.Identifier;
import com.jun.event.exception.FailedEventSaveException;
import com.jun.event.exception.NotExistEventParameterException;
import com.jun.event.exception.NotExistEventTypeException;
import com.jun.event.exception.NotExistIdentifierException;
import com.jun.event.exception.NotOneParameterException;
import com.jun.event.model.Event;
import com.jun.event.model.Snapshot;
import com.jun.event.repository.EventRepository;
import com.jun.event.repository.SnapshotRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class EventService {
	

	private  ObjectMapper objectMapper = new ObjectMapper();
	
	private EventRepository eventRepository;
	
	private SnapshotRepository snapshotRepository;
	
//	@Value("${event.snapshot.distance.count:10}")
	private Integer snapshotCount = 30;
	
	private static final String DATA_NAME = "data"; 
	
	private static final String VERSION_NAME = "version";
	
	private DatabaseClient dbClient;
	
	public EventService(DatabaseClient dbClient) {
		this.dbClient = dbClient;
		this.eventRepository = new EventRepository(dbClient);
		this.snapshotRepository = new SnapshotRepository(dbClient);
	}
	
	public EventService(DatabaseClient dbClient, Integer snapshotCount) {
		this(dbClient);
		this.snapshotCount = snapshotCount;
	}

	
	
	/**
	 * �ĺ��� �ʵ带 ã�Ƽ� ��ȯ�Ѵ�.
	 * @param <T>
	 * @param event
	 * @return
	 */
	private <T> Field findIdentifierField(Class<T> clazz) {
		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields) {
			Annotation[] annotations = field.getDeclaredAnnotations();
			for(Annotation annotation : annotations) {
				if(annotation.annotationType().equals(Identifier.class)) {
					Field idField = field;
					idField.setAccessible(true);
					return idField;
				}
			}
		}
		throw new NotExistIdentifierException("Not exist "+Identifier.class.getName()+" annotation in "+clazz.getName()+". this class must have @Identifier annotation.");
	}
//	
//	
//	public void transaction() {
//		ReactiveTransactionManager t = new R2dbcTransactionManager(dbClient.getConnectionFactory());
//		TransactionalOperator op = TransactionalOperator.create(t);
//		op.execute(t -> {
//			
//		});
//	}
	
	/**
	 * �̺�Ʈ�� �����Ѵ�.
	 * @param <T>
	 * @param aggregateId
	 * @param aggregateTypeClass
	 * @param event
	 * @return
	 */
	public <T> Mono<Event> saveEvent(T event, Class<?> aggregateTypeClass) {
		TransactionalOperator transactionOperator = TransactionalOperator.create(new R2dbcTransactionManager(dbClient.getConnectionFactory()));
		try {
			// �̺�Ʈ ��ü�� ����ִ� �ĺ��� �ʵ�
			final Field idField = findIdentifierField(event.getClass());
			
			// aggregateType�� aggregateId�� ���� �ֱ� �̺�Ʈ�� ��ȸ�Ѵ�.
			// ù �ֱ׸���Ʈ ��Ͻÿ��� aggregateId�� null �̹Ƿ� �� �̺�Ʈ�� �����Ѵ�.
			UUID aggregateId = (UUID) idField.get(event);
			Mono<Event> recentlyEvent = aggregateId == null 
					? Mono.just(new Event()) 
					: eventRepository.findRecentlyEvent(aggregateTypeClass.getSimpleName(), aggregateId);
				Mono<Event> result = recentlyEvent
					.switchIfEmpty(Mono.just(new Event()))
					.flatMap(x ->{
						try {
							// �ĺ��ڰ� null�̸� UUID�� ���� ä���. (ó�� �̺�Ʈ�� ����Ҷ��� �ĺ��ڰ� ����.)
							UUID newId = idField.get(event) == null ? UUID.randomUUID() : (UUID) idField.get(event); 
							idField.set(event, newId);
							// ��ȸ�� �������� ������ +1�� �Ѵ�.
							Long version = Optional.ofNullable(x.getVersion()).orElseGet(() -> 0L) + 1;
							// �̺�Ʈ ���� ����ȭ
							String payload = objectMapper.writeValueAsString(event);
							// ������ȭ�� �������� �˻� �Ұ����� ��� catch�� ����
							objectMapper.readValue(payload, Map.class);
							// ���� �����ϰ�
							Event newEvent = new Event(UUID.randomUUID(), aggregateTypeClass.getSimpleName(), newId
									, event.getClass().getSimpleName(), version, payload, LocalDateTime.now());
							// ����� �̺�Ʈ ��ȯ
							return eventRepository.save(newEvent) // ����
									.doOnSuccess(evt -> saveSnapshot(newEvent, aggregateTypeClass).subscribe()) // �������� Ȯ���ϰ� ����
									.as(transactionOperator::transactional); // Ʈ����� ó��
						} catch (Exception e) {
							// JsonProcessingException�� ���� ���� �������� �̺�Ʈ ���� ó���� �Ǹ� �� �ǹǷ� ���� �Ұ��� ���� ó�� 
							throw new FailedEventSaveException(e);
						}
					});
				return result;
		} catch (IllegalAccessException e) {
			throw new FailedEventSaveException(e);
		}
	}
	
	/**
	 * �������� �����Ѵ�.
	 * @param <T>
	 * @param event
	 * @param aggregateTypeClass
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	public <T> Mono<Snapshot> saveSnapshot(Event event, Class<T> aggregateTypeClass) {
		return findAggregateAndVersion(aggregateTypeClass, event.getAggregateId())
		.flatMap(x -> {
			T data = (T) x.get(DATA_NAME);
			Long snapshotVersion = Optional.ofNullable((Long) x.get(VERSION_NAME)).orElseGet(() -> 0L);
			// �������� �̺�Ʈ�� ���� ���̸� ���� Ư�� ��ġ �̻� ���̳��� �������� �����Ѵ�.
			if(Math.subtractExact(event.getVersion(), snapshotVersion) >= snapshotCount) {
				try {
					// �ֽ� ������ �ֱ׸���Ʈ�� ����ȭ �Ѵ�.
					String payload = objectMapper.writeValueAsString(data);
					// ������ ��ü�� �����Ѵ�.
					Snapshot newSnapshot = new Snapshot(UUID.randomUUID(), event.getId(), aggregateTypeClass.getSimpleName(), event.getAggregateId()
							,event.getVersion(), payload, LocalDateTime.now());
					// ������ ����
					return snapshotRepository.save(newSnapshot)
							.doOnSuccess(v -> {
								System.out.println("snap??");
								System.out.println("snap??");
								System.out.println("snap??");
								System.out.println("snap??");
							});
				} catch (JsonProcessingException e) {
					throw new FailedEventSaveException(e);
				}
			}
			return Mono.empty();
		});
	}
	
	/**
	 * �ֽ� ������ �ֱ׸���Ʈ�� ��ȯ�Ѵ�.
	 * @param <T>
	 * @param aggregateTypeClass
	 * @param aggregateId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> Mono<T> findAggregate(Class<T> aggregateTypeClass, UUID aggregateId) {
		// ���� �ֱ��� �������� ��ȸ�Ѵ�.
		return (Mono<T>) findAggregateAndVersion(aggregateTypeClass, aggregateId)
				.map(x -> x.get(DATA_NAME));
	}
	
	/**
	 * �ֽ� ������ �ֱ׸���Ʈ�� ���������� �Բ� ��ȯ�Ѵ�.
	 * @param <T>
	 * @param aggregateTypeClass
	 * @param aggregateId
	 * @return
	 */
	public <T> Mono<Map<String, Object>> findAggregateAndVersion(Class<T> aggregateTypeClass, UUID aggregateId) {
		// ���� �ֱ��� �������� ��ȸ�Ѵ�.
		return snapshotRepository.findRecentlySnapshot(aggregateTypeClass.getSimpleName(), aggregateId)
				.switchIfEmpty(Mono.just(new Snapshot()))
				.flatMap(snapshot -> {
					// ������ ������ ��� �̺�Ʈ���� ��ȸ�Ѵ�.
					Flux<Event> eventListFlux = eventRepository.findAfterSnapshotEvents(aggregateTypeClass.getSimpleName()
							, aggregateId, Optional.ofNullable(snapshot.getVersion()).orElseGet(() -> 0L));
					
					try {
						T aggregate = ObjectUtils.isEmpty(snapshot.getPayload()) ? aggregateTypeClass.newInstance()
								: objectMapper.readValue(snapshot.getPayload(), aggregateTypeClass);
						
						// �ֱ׸���Ʈ �ĺ��� ������ �������ش�.
						findIdentifierField(aggregateTypeClass);
						Field aggregateIdentifier = findIdentifierField(aggregateTypeClass);
						aggregateIdentifier.setAccessible(true);
						aggregateIdentifier.set(aggregate, aggregateId);

						// �̺�Ʈ���� ���� �����鼭 �ֱ׸���Ʈ�� �ֽŻ��·� �����.
						return eventListFlux.collectList()
								.map(eventList -> {
									// �ֽ� ������ �ֱ׸���Ʈ
									Map<String, Object> resultMap = new HashMap<>();
									resultMap.put(DATA_NAME, eventReplay(eventList, aggregate));
									resultMap.put(VERSION_NAME, snapshot.getVersion());
									return resultMap;
								});
					}catch(Exception e) {
						throw new RuntimeException(e);
					}
					
				});
	}
	
	/**
	 * Ư�� �ֱ׸���Ʈ�� �ֽ� ���¸� ���� ��ȯ�Ѵ�.
	 * @param <T>
	 * @param eventList
	 * @param aggregate
	 * @return
	 */
	public <T> T eventReplay(List<Event> eventList, T aggregate) {
		// �̺�Ʈ�� ���鼭 �ֽŻ��� ä���
		for(Event event : eventList) {
			try {
				boolean isExistEventType = false; // �̺�Ʈ Ÿ���� �̺�Ʈ ������ �̺�Ʈ �ڵ鷯 �� �� �����ϴ��� üũ (�߰��� �̺�Ʈ Ÿ�Ը��� �����Ͽ��� ���)
				Method[] methods = aggregate.getClass().getDeclaredMethods();
				for(Method method : methods) {
					method.setAccessible(true);
					EventHandler annotation = method.getDeclaredAnnotation(EventHandler.class);
					if(annotation != null) {
						if(method.getParameterCount() > 1) throw new NotOneParameterException();
						if(method.getParameterCount() == 0) throw new NotExistEventParameterException();
						Class<?> eventClass = annotation.eventClass();
						if(event.getEventType().equals(eventClass.getSimpleName())) {
							isExistEventType = true;
							method.invoke(aggregate, objectMapper.readValue(event.getPayload(), eventClass));
						}
					}
//					Annotation[] annotations = method.getDeclaredAnnotations();
//					for(Annotation annotation : annotations) {
//						if(annotation.annotationType().equals(EventHandler.class)) {
//							if(method.getParameterCount() > 1) throw new NotOneParameterException();
//							if(method.getParameterCount() == 0) throw new NotExistEventParameterException();
//							Class<?> eventClass = ((EventHandler) annotation).eventClass();
//							if(event.getEventType().equals(eventClass.getSimpleName())) {
//								isExistEventType = true;
//								method.invoke(aggregate, objectMapper.readValue(event.getPayload(), eventClass));
//							}
//						}
//					}
				}
				String msg = "The event type '"+event.getEventType()+"' is not exist in class '"
						+aggregate.getClass().getSimpleName()+"'";
				if(!isExistEventType) throw new NotExistEventTypeException(msg);
			} catch(Exception e) {
				throw new FailedEventSaveException(e);
			}
		}
		return aggregate;
	}
	
}
