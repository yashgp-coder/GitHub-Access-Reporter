package com.github.reporter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    @Value("${github.threadpool.core-size}")
    private int corePoolSize;

    @Value("${github.threadpool.max-size}")
    private int maxPoolSize;

    @Value("${github.threadpool.queue-capacity}")
    private int queueCapacity;

    @Value("${github.threadpool.thread-name-prefix}")
    private String threadNamePrefix;

    // RestTemplate with timeouts via SimpleClientHttpRequestFactory
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000); // 10 seconds
        factory.setReadTimeout(30_000);    // 30 seconds
        return new RestTemplate(factory);
    }

    // Thread pool — used in Stage 6 for parallel repo processing
@Bean(name = "githubExecutorService")
public ExecutorService githubExecutorService() {
    return new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName(threadNamePrefix + thread.getId());
                thread.setDaemon(true);
                return thread;
            },
            // ✅ Instead of crashing — run the task on the caller's thread
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
}
}