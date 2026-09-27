package com.bankflow.fraud.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window velocity and profile tracker for accounts.
 * In full production, this is backed by Redis or Apache Flink stream state.
 */
@Component
public class AccountVelocityTracker {

    private final Map<String, List<Instant>> accountTxnTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Double> account30DayAverages = new ConcurrentHashMap<>();

    public AccountVelocityTracker() {
        // Seed default baseline averages for simulated accounts
        account30DayAverages.put("default", 15000.0);
    }

    public int getVelocityLastHour(String accountId) {
        List<Instant> timestamps = accountTxnTimestamps.get(accountId);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        synchronized (timestamps) {
            timestamps.removeIf(t -> t.isBefore(oneHourAgo));
            return timestamps.size();
        }
    }

    public void recordTransaction(String accountId, Instant timestamp) {
        accountTxnTimestamps.computeIfAbsent(accountId, k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(timestamp != null ? timestamp : Instant.now());
    }

    public double getHistorical30DayAverage(String accountId) {
        return account30DayAverages.getOrDefault(accountId, 15000.0);
    }

    public void updateHistoricalAverage(String accountId, double newAverage) {
        account30DayAverages.put(accountId, newAverage);
    }
}
