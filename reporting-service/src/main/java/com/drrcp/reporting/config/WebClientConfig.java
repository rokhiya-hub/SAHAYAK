package com.drrcp.reporting.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean("victimClient")
    public WebClient victimClient(WebClient.Builder builder,
                                  @Value("${reporting.services.victims-url}") String url,
                                  @Value("${reporting.connect-timeout}") Duration connectTimeout,
                                  @Value("${reporting.response-timeout}") Duration responseTimeout) {
        return createClient(builder, url, connectTimeout, responseTimeout);
    }

    @Bean("shelterClient")
    public WebClient shelterClient(WebClient.Builder builder,
                                   @Value("${reporting.services.shelters-url}") String url,
                                   @Value("${reporting.connect-timeout}") Duration connectTimeout,
                                   @Value("${reporting.response-timeout}") Duration responseTimeout) {
        return createClient(builder, url, connectTimeout, responseTimeout);
    }

    @Bean("resourceClient")
    public WebClient resourceClient(WebClient.Builder builder,
                                    @Value("${reporting.services.resources-url}") String url,
                                    @Value("${reporting.connect-timeout}") Duration connectTimeout,
                                    @Value("${reporting.response-timeout}") Duration responseTimeout) {
        return createClient(builder, url, connectTimeout, responseTimeout);
    }

    @Bean("volunteerClient")
    public WebClient volunteerClient(WebClient.Builder builder,
                                     @Value("${reporting.services.volunteers-url}") String url,
                                     @Value("${reporting.connect-timeout}") Duration connectTimeout,
                                     @Value("${reporting.response-timeout}") Duration responseTimeout) {
        return createClient(builder, url, connectTimeout, responseTimeout);
    }

    private WebClient createClient(WebClient.Builder builder, String url, Duration connectTimeout,
                                   Duration responseTimeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeout.toMillis()))
                .responseTimeout(responseTimeout);
        return builder.clone()
                .baseUrl(url)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}