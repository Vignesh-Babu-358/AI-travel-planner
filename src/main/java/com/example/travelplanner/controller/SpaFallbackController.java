package com.example.travelplanner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Lets the React Router handle deep links (e.g. /trips/3, /save) by
 * forwarding any non-API, non-asset GET to index.html. Without this,
 * refreshing on a client-side route would 404 because Spring would look
 * for a matching static file.
 *
 * The regex on the path variable excludes everything Spring should serve
 * itself: API routes, OpenAPI/Swagger, actuator, and static assets that
 * already live under src/main/resources/static.
 */
@Controller
public class SpaFallbackController {

	@GetMapping("/{path:^(?!api|v3|swagger-ui|actuator|assets|favicon\\.svg|icons\\.svg|vite\\.svg).*$}/**")
	public String forward() {
		return "forward:/index.html";
	}
}
