package com.example.travelplanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * {@link RestClient} bean for the OpenRouteService HTTP API. The API key is
 * sent in the {@code Authorization} header on every request (ORS does not use
 * a {@code Bearer} prefix). When {@code ORS_API_KEY} is unset the bean is
 * still created so the app starts; calls fail at request time with a clear
 * 503 from {@link com.example.travelplanner.service.routing.RoutingService}.
 */
@Configuration
public class RoutingConfig {

	@Bean
	RestClient openRouteServiceClient(
			@Value("${app.routing.ors.base-url:https://api.openrouteservice.org}") String baseUrl,
			@Value("${app.routing.ors.api-key:}") String apiKey) {
		return RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader("Authorization", apiKey)
				.defaultHeader("Accept", "application/json, application/geo+json")
				.requestFactory(httpRequestFactory())
				.build();
	}

	private static org.springframework.http.client.ClientHttpRequestFactory httpRequestFactory() {
		var f = new org.springframework.http.client.SimpleClientHttpRequestFactory();
		f.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
		f.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
		return f;
	}
}
