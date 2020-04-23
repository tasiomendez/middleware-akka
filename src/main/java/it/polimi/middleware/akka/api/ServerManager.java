package it.polimi.middleware.akka.api;

import akka.actor.ActorSystem;
import akka.management.javadsl.AkkaManagement;

public class ServerManager {
	
	private ActorSystem system;
	private AkkaManagement management;
	private static ServerManager instance; 
	
	private ServerManager() { }
	
	public static ServerManager getInstance() {
		if(instance == null)
            instance = new ServerManager();
        return instance;
	}
	
	public ServerManager setSystem(ActorSystem system) {
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

}
