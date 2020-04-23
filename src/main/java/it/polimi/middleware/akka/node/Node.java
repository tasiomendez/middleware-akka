package it.polimi.middleware.akka.node;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.polimi.middleware.akka.node.cluster.ClusterManager;

public class Node extends AbstractActor {

	private final ActorRef clusterManager = getContext().actorOf(ClusterManager.props(), "ClusterManager");
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().build();
	}
	
	public static Props props() {
		return Props.create(Node.class);
	}

}
