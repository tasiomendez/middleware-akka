package it.polimi.middleware.akka.node;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.join.MasterNotificationMessage;
import it.polimi.middleware.akka.messages.storage.GathererStorageMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionGetterResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionResponseMessage;
import it.polimi.middleware.akka.messages.storage.PropagateMessage;
import it.polimi.middleware.akka.node.cluster.ClusterManager;
import it.polimi.middleware.akka.node.storage.Storage;
import it.polimi.middleware.akka.node.storage.StorageManager;

/**
 * Main supervisor of node actors. It is the main entry point to the node. It supervises the {@link ClusterManager}
 * actor and the {@link Storage} actor.
 */
public class Node extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorRef clusterManager = getContext().actorOf(ClusterManager.props(), "clusterManager");
    private final ActorRef storageManager = getContext().actorOf(StorageManager.props(clusterManager), "storageManager");

    public static Props props() {
        return Props.create(Node.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()

                .match(MasterNotificationMessage.class, msg -> clusterManager.forward(msg, getContext()))

                .match(PropagateMessage.class, msg -> storageManager.forward(msg, getContext()))
                .match(GetPartitionResponseMessage.class, msg -> storageManager.forward(msg, getContext()))
                .match(GetPartitionGetterResponseMessage.class, msg -> storageManager.forward(msg, getContext()))

                .match(GathererStorageMessage.class, msg -> storageManager.forward(msg, getContext()))

                .matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }

}
