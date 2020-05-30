package it.polimi.middleware.akka.node.storage;

import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.api.ReplyMessage;
import it.polimi.middleware.akka.messages.storage.GathererStorageMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionGetterRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionGetterResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetterBackupMessage;
import it.polimi.middleware.akka.messages.storage.GetterMessage;
import it.polimi.middleware.akka.messages.storage.PropagateMessage;
import it.polimi.middleware.akka.messages.storage.PutterMessage;
import it.polimi.middleware.akka.messages.storage.RestoreRequestMessage;

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

    public void onGetByKey(GetPartitionGetterResponseMessage msg) {
    	log.debug("Get request for key [{}]", msg.getEntry().getKey());
        ReplyMessage reply = storage.get(msg.getEntry().getKey());
        msg.getReplyTo().tell(reply, self());
    }

    public void onGetBackupAll(GetterBackupMessage msg) {
        ReplyMessage reply = storage.getNodeAll();
        sender().tell(reply, self());
    }

    private void onGet(GetterMessage msg) {
        log.debug("Get request received. Asking for partition.");
        clusterManager.tell(new GetPartitionGetterRequestMessage(msg, sender()), self());
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

    public void onGetAll(GetterMessage msg) {
        log.debug("Get request received for all keys. Starting loop...");
        clusterManager.tell(new GathererStorageMessage(storage.getAll(), sender()), self());
    }
    
    public void onGathererStorage(GathererStorageMessage msg) {
    	sender().tell(msg.sum(this.storage.getAll()), self());
    }
    
    public void onRestoreRequest(RestoreRequestMessage msg) {
    	log.info("Restoring lost data from unreachable node");
    	HashMap<String, String> restores = storage.removePartition(msg.getAddress());
    	for (Map.Entry<String, String> entry : restores.entrySet()) {
    		clusterManager.tell(new GetPartitionRequestMessage(new PutterMessage(entry.getKey(), entry.getValue()), sender()), self());
    	}
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()

                .match(GetterMessage.class, msg -> !msg.isAll() && storage.contains(msg.getKey()), this::onGetByKey)
                .match(GetterMessage.class, msg -> !msg.isAll(), this::onGet)
                .match(GetterMessage.class, msg -> msg.isAll(), this::onGetAll)
                .match(GetPartitionGetterResponseMessage.class, this::onGetByKey)
                .match(GetterBackupMessage.class, this::onGetBackupAll)

                .match(PutterMessage.class, this::onPut)

                .match(PropagateMessage.class, this::onPropagateMessage)

                .match(GetPartitionResponseMessage.class, this::onGetPartitionResponse)

                .match(GathererStorageMessage.class, this::onGathererStorage)

                .match(RestoreRequestMessage.class, (msg) -> storage.containsPartition(msg.getAddress()), this::onRestoreRequest)

                .matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }

}
