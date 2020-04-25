package it.polimi.middleware.akka.api;

import java.util.concurrent.CompletionStage;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

public class HttpManager {
	
	private LoggingAdapter log;
	
	private ActorSystem system = ActorSystem.create();
	private Http http;
	private ActorMaterializer materializer;
	private final Router router = new Router();
	
	private Flow<HttpRequest, HttpResponse, NotUsed> routeFlow;
	private CompletionStage<ServerBinding> binding; 
	
	private static HttpManager instance; 
	
	private HttpManager() { }
	
	public static HttpManager getInstance() {
		if(instance == null)
            instance = new HttpManager();
        return instance;
	}
	
	/**
	 * Mandatory before performing any operation. If it is not called,
	 * its own ActorSystem instance is used.
	 * 
	 * @param system Customized ActorSystem
	 * @return instance
	 */
	public HttpManager setSystem(ActorSystem system) {
		this.system = system;
		this.initializeHttpManager(system);
		this.initializeLogging(system);
		return instance;
	}
	
	private void initializeLogging(ActorSystem system) {
		this.log = Logging.getLogger(system, this);
	}
	
	private void initializeHttpManager(ActorSystem system) {
		this.http = Http.get(system);
		this.materializer = ActorMaterializer.create(system);
	}
	
	public ActorSystem getSystem() {
		return this.system;
	}
	
	/**
	 * Starts a new Akka HTTP Server which listens on the port
	 * provided on the configuration file.
	 * 
	 * @return instance
	 */
	public HttpManager start() {
		this.routeFlow = router.createRouter().flow(system, materializer);
		log.debug("Routes flow created successfully");
		
		final String hostname = this.system.settings().config().getString("akka.http.hostname");
		final int port = this.system.settings().config().getInt("akka.http.port");
		
		this.http.bindAndHandle(routeFlow, ConnectHttp.toHost(hostname, port), materializer);
		log.info("HttpManager listening on {}:{}", hostname, port);
		return instance;
	}
	
	/**
	 * Stops an existing Akka HTTP Server and unbinds the port 
	 * which is used.
	 * 
	 * @return instance
	 */
	public HttpManager stop() {
		this.binding.thenCompose(ServerBinding::unbind);
		return instance;
	}
	
	/**
	 * Import routes from another Router into the existing router. All the
	 * routes are combined and will be accessible
	 * 
	 * @param route routes to import
	 * @return instance
	 */
	public HttpManager importRoutes(Route route) {
		this.router.imports(route);
		log.debug("Routes imported into HttpManager");
		return instance;
	}

}
