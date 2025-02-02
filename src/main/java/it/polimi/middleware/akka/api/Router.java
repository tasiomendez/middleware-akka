package it.polimi.middleware.akka.api;

import static akka.http.javadsl.server.PathMatchers.segment;
import static akka.pattern.Patterns.ask;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import it.polimi.middleware.akka.messages.storage.GetterBackupMessage;
import it.polimi.middleware.akka.messages.storage.GetterMessage;
import it.polimi.middleware.akka.messages.storage.PutterMessage;

public class Router extends AllDirectives {
	
	// Initialize by default in case there are no imports
	private Route[] imports = {};
	
	// Timeout for discarding a request
	private static final Duration timeout = Duration.ofSeconds(10);
	
	private static Router instance;
	private ActorSystem system;
	private LoggingAdapter log;
	private ActorSelection gateway;
	
	private Router(ActorSystem system) {
		this.system = system;
		this.log = Logging.getLogger(system, this);
		this.gateway = system.actorSelection("/user/node/storageManager");
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
								path(segment("put").slash(segment()).slash(segment()), this::onPutRequest),  // database/put/:key/:value
								path(segment("node").slash(segment("get")), this::onGetBackupRequest) 		// database/node/get
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
		final GetterMessage msg = new GetterMessage();
		return routeGateway(msg);
	}
	
	/**
	 * Handler which search for keys on current node
	 * Path - /database/node/get
	 * 
	 * @return Route object
	 */
	private Route onGetBackupRequest() {
		log.debug("Request received on /database/node/get");
		final GetterBackupMessage msg = new GetterBackupMessage();
		return routeGateway(msg);
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
		final GetterMessage msg = new GetterMessage(key);
		return routeGateway(msg);
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
		final PutterMessage msg = new PutterMessage(key, value);
		return routeGateway(msg);
	}
	
	/**
	 * Send a message to an actor and waits for its reply. The reply is converted
	 * into a JSON object for the HTTP response.
	 * 
	 * @param msg to send
	 * @return Route object
	 */
	private Route routeGateway(Object msg) {
		final CompletionStage<Object> future = ask(this.gateway, msg, timeout)
				.thenApplyAsync(message -> {
					log.debug("Reply received from {}", this.gateway.pathString());
					return message;
				}, this.system.dispatcher());
		return completeOKWithFuture(future, Jackson.marshaller());
	}

}
