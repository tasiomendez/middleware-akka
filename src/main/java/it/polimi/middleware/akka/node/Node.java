package it.polimi.middleware.akka.node;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.join.MasterNotificationMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetterMessage;
import it.polimi.middleware.akka.messages.storage.PutterMessage;
import it.polimi.middleware.akka.node.cluster.ClusterManager;
import it.polimi.middleware.akka.node.storage.Storage;
import it.polimi.middleware.akka.node.storage.StorageManager;

/**
 * Main supervisor of node actors. It is the main entry point to the node.
 * It supervises the {@link ClusterManager} actor and the {@link Storage} actor.
 */
public class Node extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().getSystem());
	private final ActorRef clusterManager = getContext().actorOf(ClusterManager.props(), "clusterManager");
	private final ActorRef storageManager = getContext().actorOf(StorageManager.props(), "storageManager");

	@Override
	public Receive createReceive() {
		return receiveBuilder()

				.match(MasterNotificationMessage.class, msg -> clusterManager.forward(msg, getContext()))
				
				// Storage messages
				.match(GetterMessage.class, msg -> storageManager.forward(msg, getContext()))
				.match(PutterMessage.class, msg -> storageManager.forward(msg, getContext()))

				.match(GetPartitionRequestMessage.class,
						() -> cluster.selfMember().hasRole("master"),
						(msg) -> clusterManager.forward(msg, getContext()))

				.matchAny(msg -> log.warning("Received unknown message: {}", msg))
				.build();
	}

	public static Props props() {
		return Props.create(Node.class);
	}

}
