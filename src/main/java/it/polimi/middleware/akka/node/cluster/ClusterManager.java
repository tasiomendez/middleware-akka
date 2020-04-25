package it.polimi.middleware.akka.node.cluster;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;

public class ClusterManager extends AbstractActor {

	private final Cluster cluster = Cluster.get(getContext().system());
	private final ActorRef listener = getContext().actorOf(ClusterListener.props(), "clusterListener");
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().build();
	}
	
	public static Props props() {
		return Props.create(ClusterManager.class);
	}

}
