package it.polimi.middleware.akka.node.storage;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.api.ReplyMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetterBackupMessage;
import it.polimi.middleware.akka.messages.storage.GetterMessage;
import it.polimi.middleware.akka.messages.storage.PropagateMessage;
import it.polimi.middleware.akka.messages.storage.PutterMessage;

/**
 * Actor in charge of managing data partitions. When data partition is needed to be updated, it handles all the
 * necessary operations.
 */
public class StorageManager extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Storage storage = Storage.get(getContext().getSystem());
    
    private final ActorRef clusterManager;
    
    public StorageManager(ActorRef clusterManager) {
    	this.clusterManager = clusterManager;
    }

    public static Props props(ActorRef clusterManager) {
        return Props.create(StorageManager.class, clusterManager);
    }

    public void onGetByKey(GetterMessage msg) {
        ReplyMessage reply = storage.get(msg.getKey());
        sender().tell(reply, self());
    }

    public void onGetAll(GetterMessage msg) {
        // TODO
    }

    public void onGetBackupAll(GetterBackupMessage msg) {
        ReplyMessage reply = storage.getAll();
        sender().tell(reply, self());
    }

    private void onPut(PutterMessage msg) {
        log.debug("Put request received. Asking PartitionManager for partition.");
        clusterManager.tell(new GetPartitionRequestMessage(msg, sender()), self());
    }

    public void onGetPartitionResponse(GetPartitionResponseMessage msg) {
        ReplyMessage reply = storage.put(msg.getEntry().getKey(), msg.getEntry().getValue());
        msg.getReplyTo().tell(reply, self());
    }

    private void onPropagateMessage(PropagateMessage msg) {
    	log.debug("Put request received for backup of {}", msg.getAddress());
        storage.addToPartition(msg.getAddress(), msg.getEntry().toHashMapEntry());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()

                .match(GetterMessage.class, msg -> !msg.isAll(), this::onGetByKey)
                .match(GetterMessage.class, msg -> msg.isAll(), this::onGetAll)
                .match(GetterBackupMessage.class, this::onGetBackupAll)

                .match(PutterMessage.class, this::onPut)

                .match(PropagateMessage.class, this::onPropagateMessage)

                .match(GetPartitionResponseMessage.class, this::onGetPartitionResponse)

                .matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }

}
