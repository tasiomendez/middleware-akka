package it.polimi.middleware.akka.api;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;

public class Router extends AllDirectives {
	
	// Initialize by default in case there are no imports
	private Route[] imports = {};
	
	private static Router instance;
	private LoggingAdapter log;
	
	private Router(ActorSystem system) {
		this.log = Logging.getLogger(system, this);
	}
	
	public static Router get(ActorSystem system) {
		if(instance == null)
            instance = new Router(system);
        return instance;
	}

	/**
	 * Create the router with all the paths, the ones defined and
	 * the imported ones
	 * 
	 * @return Route object
	 */
	public Route createRouter() {
		return concat(
				path("hello", () ->
				get(() ->
				complete("<h1>Say hello to akka-http</h1>"))),
				imports
				);
	}
	
	/**
	 * Imports routes into the existing router. Routes must be imported
	 * before creating the router.
	 * 
	 * @param routes to import
	 * @return instance
	 */
	public Router imports(Route... routes) {
		this.imports = routes;
		return this;
	}

}
