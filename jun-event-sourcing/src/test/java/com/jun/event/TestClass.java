package com.jun.event;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.jun.event.annotation.Identifier;
import com.jun.event.model.Event;

public class TestClass {

	@Test
	public void test3() {
		StringBuilder sb = new StringBuilder("ag_id_no");
		StringBuilder newSb = new StringBuilder();
		for(int i=0; i<sb.length(); i++) {
			if(sb.charAt(i) == '_') {
				newSb.append(Character.toString(sb.charAt(i+1)).toUpperCase());
				i++;
			}else {
				newSb.append(sb.charAt(i));
			}
		}
		System.out.println(newSb);
	}
	
//	@Test
	public void test2() {
		String sql = "select * from event where aggregate_type = :aggregateType and aggregate_id = :aggregateId order by version desc limit 1";
		int paramIndex = 0;
		List<String> param = new ArrayList<>();
		while(true) {
			paramIndex = sql.indexOf(":",paramIndex+1);
			if(paramIndex == -1) break;
			int space = sql.indexOf(" ", paramIndex+1);
			param.add(sql.substring(paramIndex+1, space));
		}
		System.out.println(param);
//		System.out.println(sql.indexOf(":"));
//		System.out.println(sql.indexOf(" ", 43));
//		System.out.println(sql.indexOf(":", 44));
	}
	
	
	@Test
	public void test() throws IllegalArgumentException, IllegalAccessException {
		Event event = new Event("Product", "11", "ProductCreated", 1L, "{}", LocalDateTime.now());
		
		List<String> fieldList = new ArrayList<>();
		List<Object> valueList = new ArrayList<>();
		List<String> paramList = new ArrayList<>();
		
		Field[] fields = event.getClass().getDeclaredFields();
		StringBuilder sb = new StringBuilder();
		for(Field field : fields) {
			field.setAccessible(true);
			
			System.out.println(field.getDeclaredAnnotation(org.springframework.data.annotation.Id.class));
			System.out.println(field.getDeclaredAnnotation(Identifier.class));
			
			StringBuilder b = new StringBuilder(field.getName());
			for(int i=0; i<b.length(); i++) {
				if(Character.isUpperCase(b.charAt(i))) {
					b.insert(i, "_");
					i++;
				};
			}
			
			fieldList.add(b.toString().toLowerCase());
			valueList.add(field.get(event));
			paramList.add("?");
		}
		sb.append("insert into event ")
		.append("(")
		.append(String.join(",", fieldList))
		.append(") ")
		.append(" values")
		.append("(")
		.append(String.join(",",paramList))
		.append(")");
//		GenericExecuteSpec s = dbClient.sql(sb.toString());
//		int cnt = 1;
//		for(Object obj : valueList) {
//			s.bind(cnt, obj);
//			cnt++;
//		}
	}
}
