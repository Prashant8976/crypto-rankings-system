package com.example.crypto.utils;

import java.util.Collections;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class HttpUtils {
	private static String apiHost="coinranking1.p.rapidapi.com";
	private static String apiKey="bd8765cf4cmsha0c047d00260681p12c036jsn0406a58e2a23";

	public static HttpEntity<String> getHttpEntity(){
		HttpHeaders headers=new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set("X-RapidAPI-Host",apiHost);
		headers.set("X-RapidAPI-Key",apiKey);
		return new HttpEntity<>(null,headers);
	}
}
