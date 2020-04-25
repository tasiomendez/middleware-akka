package it.polimi.middleware.akka.api;

import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.http.javadsl.server.Route;
import akka.management.cluster.javadsl.ClusterHttpManagementRoutes;
import akka.management.javadsl.AkkaManagement;

public class ManagementManager {
	
	private ActorSystem system = ActorSystem.create();
	private AkkaManagement management;
	private static ManagementManager instance; 
	
	private ManagementManager() { }
	
	public static ManagementManager getInstance() {
		if(instance == null)
            instance = new ManagementManager();
        return instance;
	}
	
	/**
	 * Mandatory before performing any operation. If it is not called,
	 * its own ActorSystem instance is used.
	 * 
	 * @param system Customized ActorSystem
	 * @return instance
	 */
	public ManagementManager setSystem(ActorSystem system) {
		this.system = system;
		this.management = AkkaManagement.get(this.system);
		return instance;
	}
	
	public ActorSystem getSystem() {
		return this.system;
	}
	
	/**
	 * Starts an Akka HTTP server and hosts the Cluster HTTP Routes.
	 * 
	 * @return AkkaManagement instance
	 */
	public AkkaManagement start() {
		this.management.start();
		return this.management;
	}
	
	/**
	 * Stop the Akka HTTP server created
	 * 
	 * @return AkkaManagement instance
	 */
	public AkkaManagement stop() {
		this.management.stop();
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
