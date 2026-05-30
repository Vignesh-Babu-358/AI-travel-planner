package com.example.travelplanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * {@link RestClient} bean for the Google Maps Platform HTTP APIs
 * (Directions + Geocoding). The API key is passed as a query parameter on
 * each request (not a header), so this bean only configures base URL and
 * timeouts. Missing key is detected later in the client so the app starts
 * fine — calls fail at request time with a clear 503.
 */
@Configuration
public class RoutingConfig {

	@Bean
	RestClient googleMapsClient(
			@Value("${app.routing.google.base-url:https://maps.googleapis.com}") String baseUrl) {
		return RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader("Accept", "application/json")
				.requestFactory(httpRequestFactory())
				.build();
	}

	private static ClientHttpRequestFactory httpRequestFactory() {
		SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
		f.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
		f.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
		return f;
	}
}
