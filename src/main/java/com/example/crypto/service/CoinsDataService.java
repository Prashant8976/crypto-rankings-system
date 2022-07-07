package com.example.crypto.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.crypto.model.CoinData;
import com.example.crypto.model.CoinInfo;
import com.example.crypto.model.CoinPriceHistory;
import com.example.crypto.model.CoinPriceHistoryExchangeRate;
import com.example.crypto.model.Coins;
import com.example.crypto.utils.HttpUtils;

import io.github.dengliming.redismodule.redisjson.RedisJSON;
import io.github.dengliming.redismodule.redisjson.args.GetArgs;
import io.github.dengliming.redismodule.redisjson.args.SetArgs;
import io.github.dengliming.redismodule.redisjson.utils.GsonUtils;
import io.github.dengliming.redismodule.redistimeseries.DuplicatePolicy;
import io.github.dengliming.redismodule.redistimeseries.RedisTimeSeries;
import io.github.dengliming.redismodule.redistimeseries.Sample;
import io.github.dengliming.redismodule.redistimeseries.Sample.Value;
import io.github.dengliming.redismodule.redistimeseries.TimeSeriesOptions;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CoinsDataService {
	public static final String GET_COINS_API="https://coinranking1.p.rapidapi.com/coins?referenceCurrencyUuid=yhjMzLPhuIDl&timePeriod=24h&tiers%5B0%5D=1&orderBy=marketCap&orderDirection=desc&limit=50&offset=0";
	public static final String GET_COIN_HISTORY_API = "https://coinranking1.p.rapidapi.com/coin/";
    public static final String COIN_HISTORY_TIME_PERIOD_PARAM = "/history?timePeriod=";
	public static final String REDIS_KEY_COINS="coins";
	public static final List<String> timePeriods=new ArrayList<>(Arrays.asList("24h","7d","30d","3m","1y","3y","5y"));
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private RedisJSON redisJSON;
	
	@Autowired
	private RedisTimeSeries redisTimeSeries;
	public void fetchCoins() {
		log.info("inside fetchCoins()");
		ResponseEntity<Coins> coinsEntity=restTemplate.exchange(GET_COINS_API, HttpMethod.GET,
				HttpUtils.getHttpEntity(),
				Coins.class
				);
		storeCoinsToRedisJSON(coinsEntity.getBody());
	}
	
	public void fetchCoinHistory() {
		log.info("Inside FetchCoinHistory");
		List<CoinInfo> allCoins=getAllCoinsFromRedisJSON();
		allCoins.forEach(coinInfo->{
			timePeriods.forEach(s->{
				fetchCoinHistoryForTimePeriod(coinInfo,s);
			});
		});
	}

	private void fetchCoinHistoryForTimePeriod(CoinInfo coinInfo, String timePeriod) {
        log.info("Fetching Coin History of {} for Time Period {}", coinInfo.getName(), timePeriod);
        String url = GET_COIN_HISTORY_API + coinInfo.getUuid() + COIN_HISTORY_TIME_PERIOD_PARAM + timePeriod;
        ResponseEntity<CoinPriceHistory> coinPriceHistoryResponseEntity =
                restTemplate.exchange(url,
                        HttpMethod.GET,
                        HttpUtils.getHttpEntity(),
                        CoinPriceHistory.class);

        log.info("Data Fetched From API for Coin History of {} for Time Period {}", coinInfo.getName(), timePeriod);

        storeCoinHistoryToRedisTS(coinPriceHistoryResponseEntity.getBody(), coinInfo.getSymbol(), timePeriod);
    }
	
	private void storeCoinHistoryToRedisTS(CoinPriceHistory body, String symbol, String timePeriod) {
		List<CoinPriceHistoryExchangeRate> rate=body.getData().getHistory();
		//BTC:24h
		rate.stream()
		.filter(ch -> ch.getPrice()!=null && ch.getTimestamp()!=null)
		.forEach(ch -> {
            redisTimeSeries.add(new Sample(symbol + ":" + timePeriod, Sample.Value.of(Long.valueOf(ch.getTimestamp()),
                    Double.valueOf(ch.getPrice()))), new TimeSeriesOptions()
                    .unCompressed()
                    .duplicatePolicy(DuplicatePolicy.LAST));
                   // .labels(new Label(symbol, timePeriod)));
        });
		log.info("Save");
	}

	private List<CoinInfo> getAllCoinsFromRedisJSON() {
		CoinData coinData=redisJSON.get(REDIS_KEY_COINS, CoinData.class,
				new GetArgs().path(".data").indent("\t").newLine("\n").space(" ")
				);
		return coinData.getCoins();
	}

	private void storeCoinsToRedisJSON(Coins coins) {
		redisJSON.set(REDIS_KEY_COINS, SetArgs.Builder.create(".",GsonUtils.toJson(coins)));
	}

	public List<CoinInfo>  fetAllCoinsFromRedisJSON() {
		return getAllCoinsFromRedisJSON();
	}

	public List<Sample.Value> fetchCoinHistoyPerTimePeriodFromRedisTS(String symbol, String timePeriod) {
		Map<String,Object> tsInfo=fetchTSInfoForSymbol(symbol,timePeriod);
		Long firstTimestamp = Long.valueOf(tsInfo.get("firstTimestamp").toString());
        Long lastTimestamp = Long.valueOf(tsInfo.get("lastTimestamp").toString());
        List<Sample.Value> coinsTSData =
                fetchTSDataForCoin(symbol, timePeriod, firstTimestamp, lastTimestamp);
        return coinsTSData;
	}

	private List<Value> fetchTSDataForCoin(String symbol, String timePeriod, Long fromTimestamp, Long toTimestamp) {
		String key=symbol+":"+timePeriod;
		return redisTimeSeries.range(key, fromTimestamp, toTimestamp);
	}

	private Map<String, Object> fetchTSInfoForSymbol(String symbol, String timePeriod) {
		return redisTimeSeries.info(symbol+":"+timePeriod);
	}
}
