package com.example.crypto.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.crypto.model.CoinInfo;
import com.example.crypto.model.HistoryData;
import com.example.crypto.service.CoinsDataService;
import com.example.crypto.utils.Utility;

import io.github.dengliming.redismodule.redistimeseries.Sample;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/coins")
@Slf4j
@CrossOrigin(value="http://localhost:3000")
public class CoinsRankingController {

	@Autowired
	private CoinsDataService coinsDataService;
	@GetMapping
	public ResponseEntity<List<CoinInfo>> fetchAllCoins(){
		return ResponseEntity.ok()
				.body(coinsDataService.fetAllCoinsFromRedisJSON());
	}
	@GetMapping("/{symbol}/{timePeriod}")
	public List<HistoryData> fetchCoinHistoryPerTimePeriod(
			@PathVariable String symbol,
			@PathVariable String timePeriod
			){
			List<Sample.Value> coinsTSData=
					coinsDataService.fetchCoinHistoyPerTimePeriodFromRedisTS(symbol,timePeriod);
			
			List<HistoryData> coinHistoryDatas=
					coinsTSData
					.stream()
					.map(value-> new HistoryData(
							Utility.convertUnixTimeToDate(value.getTimestamp()),
							Utility.round(value.getValue(),2)
							))
							.collect(Collectors.toList());
			return coinHistoryDatas;
		
	}
}
