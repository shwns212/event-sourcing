package com.jun.event.util;

import org.springframework.util.StringUtils;

public class ExtStringUtils extends StringUtils {
	/**
	 * 카멜케이스 -> 언더스코어 변환
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
	 * 언더스코어 -> 카멜케이스 변환
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
