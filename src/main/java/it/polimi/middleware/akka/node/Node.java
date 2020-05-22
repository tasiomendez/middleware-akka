package it.polimi.middleware.akka.node;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.join.IdResponseMessage;
import it.polimi.middleware.akka.messages.join.MasterNotificationMessage;
import it.polimi.middleware.akka.messages.storage.GetterMessage;
import it.polimi.middleware.akka.messages.storage.PutterMessage;
import it.polimi.middleware.akka.node.cluster.ClusterManager;
import it.polimi.middleware.akka.node.storage.Storage;

/**
 * Main supervisor of node actors. It is the main entry point to the node.
 * It supervises the {@link ClusterManager} actor and the {@link Storage} actor.
 */
public class Node extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private final Cluster cluster = Cluster.get(getContext().getSystem());
	private final ActorRef clusterManager = getContext().actorOf(ClusterManager.props(), "clusterManager");
	private final ActorRef storage = getContext().actorOf(Storage.props(), "storage");

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(GetterMessage.class, msg -> storage.forward(msg, getContext()))
				.match(PutterMessage.class, () -> cluster.selfMember().hasRole("master"), msg -> storage.forward(msg, getContext()))

				.match(MasterNotificationMessage.class, msg -> clusterManager.forward(msg, getContext()))

				.matchAny(msg -> log.warning("Received unknown message: {}", msg))
				.build();
	}

	public static Props props() {
		return Props.create(Node.class);
	}

}
