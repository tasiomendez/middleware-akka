package it.polimi.middleware.akka.api;

import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;

public class Router extends AllDirectives {
	
	// Initialize by default in case there are no imports
	private Route[] imports = {};

	public Route createRouter() {
		return concat(
				path("hello", () ->
				get(() ->
				complete("<h1>Say hello to akka-http</h1>"))),
				imports
				);
	}
	
	public Router imports(Route... routes) {
		this.imports = routes;
		return this;
	}

}
