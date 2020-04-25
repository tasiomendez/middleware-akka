package it.polimi.middleware.akka.node;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.node.cluster.ClusterManager;

public class Node extends AbstractActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private final ActorRef clusterManager = getContext().actorOf(ClusterManager.props(), "clusterManager");
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(
			            String.class,
			            s -> {
			              log.info("Received String message: {}", s);
			            })
			        .matchAny(o -> log.info("received unknown message"))
			        .build();
	}
	
	public static Props props() {
		return Props.create(Node.class);
	}

}
