package com.example.crypto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.example.crypto.service.CoinsDataService;

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {
	@Autowired
	private CoinsDataService service;
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		service.fetchCoins();
		service.fetchCoinHistory();
	}

}
