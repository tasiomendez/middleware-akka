package it.polimi.middleware.akka.api;

import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.http.javadsl.server.Route;
import akka.management.cluster.javadsl.ClusterHttpManagementRoutes;
import akka.management.javadsl.AkkaManagement;

public class ManagementManager {
	
	private ActorSystem system;
	private AkkaManagement management;
	private static ManagementManager instance; 
	
	private ManagementManager() { }
	
	public static ManagementManager getInstance() {
		if(instance == null)
            instance = new ManagementManager();
        return instance;
	}
	
	public ManagementManager setSystem(ActorSystem system) {
		this.system = system;
		this.management = AkkaManagement.get(this.system);
		return instance;
	}
	
	public ActorSystem getSystem() {
		return this.system;
	}
	
	public AkkaManagement start() {
		this.management.start();
		return this.management;
	}
	
	public AkkaManagement stop() {
		this.management.stop();
		return this.management;
	}
	
	public Route exportRoutes() {
		return ClusterHttpManagementRoutes.all(Cluster.get(this.system));
	}

}
