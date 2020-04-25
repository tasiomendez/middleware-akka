package it.polimi.middleware.akka.api;

import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.server.Route;
import akka.management.cluster.javadsl.ClusterHttpManagementRoutes;
import akka.management.javadsl.AkkaManagement;

public class ManagementManager {
	
	private ActorSystem system;
	private AkkaManagement management;
	
	private LoggingAdapter log;
	
	private static ManagementManager instance; 
	
	private ManagementManager(ActorSystem system) {
		this.system = system;
		this.management = AkkaManagement.get(this.system);
		this.log = Logging.getLogger(this.system, this);
	}
	
	/**
	 * Only one instance from this class exists. If not ActorSystem 
	 * is provided, its own ActorSystem instance is used.
	 * 
	 * @param system Customized ActorSystem
	 * @return instance
	 */
	public static ManagementManager get(ActorSystem system) {
		if(instance == null)
            instance = new ManagementManager(system);
        return instance;
	}
	
	/**
	 * Only one instance from this class exists. If not ActorSystem 
	 * is provided, its own ActorSystem instance is used.
	 * 
	 * @return instance
	 */
	public static ManagementManager get() {
		if(instance == null)
            instance = new ManagementManager(ActorSystem.create());
        return instance;
	}
	
	/**
	 * Starts an Akka HTTP server and hosts the Cluster HTTP Routes.
	 * 
	 * @return AkkaManagement instance
	 */
	public AkkaManagement start() {
		this.management.start();
		log.info("HTTP Server for Akka Management started successfully");
		return this.management;
	}
	
	/**
	 * Stop the Akka HTTP server created
	 * 
	 * @return AkkaManagement instance
	 */
	public AkkaManagement stop() {
		this.management.stop();
		log.info("HTTP Server for Akka Management stopped successfully");
		return this.management;
	}
	
	/**
	 * Exports the routes of the Cluster HTTP Module in order to include
	 * them on an existing Akka HTTP Server.
	 * 
	 * @return the exported routes
	 */
	public Route exportRoutes() {
		return ClusterHttpManagementRoutes.all(Cluster.get(this.system));
	}

}
