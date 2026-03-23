package com.github.reporter.service;

import com.github.reporter.dto.AccessReportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CacheService {

    @Value("${github.cache.ttl-minutes}")
    private int ttlMinutes;

    // Stores the cached report
    private final Map<String, AccessReportResponse> cache = new ConcurrentHashMap<>();

    // Stores when each entry was cached
    private final Map<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────
    // Returns cached report if it exists AND hasn't expired
    // ─────────────────────────────────────────────────────────────
    public AccessReportResponse get(String org) {
        String key = org.toLowerCase();

        if (!cache.containsKey(key)) {
            log.debug("Cache MISS for org: {}", org);
            return null;
        }

        LocalDateTime cachedAt = cacheTimestamps.get(key);
        LocalDateTime expiresAt = cachedAt.plusMinutes(ttlMinutes);

        if (LocalDateTime.now().isAfter(expiresAt)) {
            log.info("Cache EXPIRED for org: {} (cached at: {})", org, cachedAt);
            cache.remove(key);
            cacheTimestamps.remove(key);
            return null;
        }

        log.info("Cache HIT for org: {} (expires at: {})", org, expiresAt);
        return cache.get(key);
    }

    // ─────────────────────────────────────────────────────────────
    // Saves a report into cache with current timestamp
    // ─────────────────────────────────────────────────────────────
    public void put(String org, AccessReportResponse response) {
        String key = org.toLowerCase();
        cache.put(key, response);
        cacheTimestamps.put(key, LocalDateTime.now());
        log.info("Cache SET for org: {} (TTL: {} minutes)", org, ttlMinutes);
    }

    // ─────────────────────────────────────────────────────────────
    // Manually clear cache for an org (useful for testing)
    // ─────────────────────────────────────────────────────────────
    public void evict(String org) {
        String key = org.toLowerCase();
        cache.remove(key);
        cacheTimestamps.remove(key);
        log.info("Cache EVICTED for org: {}", org);
    }

    public int size() {
        return cache.size();
    }
}