package com.jun.event.util;

import org.springframework.util.StringUtils;

public class ExtStringUtils extends StringUtils {
	/**
	 * ī�����̽� -> ������ھ� ��ȯ
	 * @param str
	 * @return
	 */
	public static String camelCaseToUnderScore(String str) {
		StringBuilder sb = new StringBuilder(str);
		for(int i=0; i<sb.length(); i++) {
			if(Character.isUpperCase(sb.charAt(i))) {
				sb.insert(i, "_");
				i++;
			};
		}
		return sb.toString().toLowerCase();
	}
	
	/**
	 * ������ھ� -> ī�����̽� ��ȯ
	 * @param str
	 * @return
	 */
	public static String UnderScoreToCamelCase(String str) {
		StringBuilder underScoreSb = new StringBuilder(str);
		StringBuilder camelCaseSb = new StringBuilder();
		for(int i=0; i<underScoreSb.length(); i++) {
			if(underScoreSb.charAt(i) == '_') {
				camelCaseSb.append(Character.toString(underScoreSb.charAt(i+1)).toUpperCase());
				i++;
			}else {
				camelCaseSb.append(underScoreSb.charAt(i));
			}
		}
		return camelCaseSb.toString();
	}
}
