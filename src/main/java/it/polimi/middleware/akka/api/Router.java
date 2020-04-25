package it.polimi.middleware.akka.api;

import static akka.http.javadsl.server.PathMatchers.segment;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;

public class Router extends AllDirectives {
	
	// Initialize by default in case there are no imports
	private Route[] imports = {};
	
	private static Router instance;
	private ActorSystem system;
	private LoggingAdapter log;
	
	private Router(ActorSystem system) {
		this.system = system;
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
		return ignoreTrailingSlash(() -> concat(this.database(), imports));

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
	
	
	/**
	 * Create handlers for URIs that starts like /database.
	 * 
	 * @return Route object
	 */
	private Route database() {	

		return pathPrefix("database", 
				() -> get(
						() -> concat(
								path(segment("get"), this::onGetRequest),									// database/get
								path(segment("get").slash(segment()), this::onGetRequest), 					// database/get/:key
								path(segment("put").slash(segment()).slash(segment()), this::onPutRequest)  // database/put/:key/:value
								)
						)
				);
	}
	
	/**
	 * Handler which returns a list with all the keys stored
	 * in the database.
	 * Path - /database/get
	 * 
	 * @return Route object
	 */
	private Route onGetRequest() {
		log.debug("Request received on /database/get");
		system.actorSelection("/user/node").tell("test", ActorRef.noSender());
		return complete("All keys");
	}
	
	/**
	 * Handler which search for a unique key on the database
	 * Path - /database/get/:key
	 * 
	 * @param key 
	 * @return Route object
	 */
	private Route onGetRequest(String key) {
		log.debug("Request received on /database/get/{}", key);
		system.actorSelection("/user/node").tell("test", ActorRef.noSender());
		return complete(key);
	}
	
	/**
	 * Add a new key-value to the database.
	 * Path - /database/put/:key/:value
	 * 
	 * @param key
	 * @param value
	 * @return Route object
	 */
	private Route onPutRequest(String key, String value) {
		log.debug("Request received on /database/put/{}/{}", key, value);
		system.actorSelection("/user/node").tell("test", ActorRef.noSender());
		return complete(key + " " + value);
	}

}
