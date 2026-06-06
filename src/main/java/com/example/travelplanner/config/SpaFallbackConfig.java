package com.example.travelplanner.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Lets the React Router handle client-side routes (e.g. /trips/3, /save) by
 * falling back to index.html when no matching static asset exists.
 *
 * Uses Spring's official {@link PathResourceResolver} pattern instead of a
 * regex-mapped controller, which avoids any {@code PathPattern} lookahead
 * pitfalls. @RequestMapping handlers (TripController etc.) still match first,
 * so /api/* and /actuator/* are never served as index.html.
 */
@Configuration
public class SpaFallbackConfig implements WebMvcConfigurer {

	private static final String[] BACKEND_PREFIXES = {
			"api/", "actuator/", "v3/", "swagger-ui"
	};

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						// Never serve index.html for backend paths — let the
						// regular handlers (or a real 404) respond.
						for (String prefix : BACKEND_PREFIXES) {
							if (resourcePath.startsWith(prefix)) {
								return null;
							}
						}
						Resource requested = location.createRelative(resourcePath);
						if (requested.exists() && requested.isReadable()) {
							return requested;
						}
						// SPA fallback: any other unknown path -> index.html so
						// React Router can handle it.
						return new ClassPathResource("/static/index.html");
					}
				});
	}
}
