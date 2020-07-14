package it.polimi.middleware.akka.node.storage;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.api.ReplyMessage;
import it.polimi.middleware.akka.messages.join.MoveStorageMessage;
import it.polimi.middleware.akka.messages.join.MoveStorageRequestMessage;
import it.polimi.middleware.akka.messages.storage.GathererStorageMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionBackupRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionBackupResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionGetterRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionGetterResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetterBackupMessage;
import it.polimi.middleware.akka.messages.storage.GetterMessage;
import it.polimi.middleware.akka.messages.storage.PropagateBackupMessage;
import it.polimi.middleware.akka.messages.storage.PropagateMessage;
import it.polimi.middleware.akka.messages.storage.PutterMessage;
import it.polimi.middleware.akka.messages.storage.RestoreRequestMessage;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Actor in charge of managing data partitions. When data partition is needed to be updated, it handles all the
 * necessary operations.
 */
public class StorageManager extends AbstractActor {
	
	private static final Duration BACKUP_MAX_TIME_DURATION = Duration.ofSeconds(30);
    private final int PARTITION_NUMBER = getContext().getSystem().settings().config().getInt("clustering.partition.max");
    private final int FINGER_TABLE_SIZE = (int) (Math.log(this.PARTITION_NUMBER) / Math.log(2));

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Storage storage = Storage.get(getContext().getSystem());
    
    private final ActorRef clusterManager;
    
    private final ConcurrentHashMap<Address, Long> backups = new ConcurrentHashMap<>();
    
    private final Supervisor supervisor = Supervisor.get(getContext().getSystem());
    
    public StorageManager(ActorRef clusterManager) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
    	this.clusterManager = clusterManager;
    	log.info("Starting backup supervisor");
    	this.supervisor.start(this::supervisor);
    }

    public static Props props(ActorRef clusterManager) {
        return Props.create(StorageManager.class, clusterManager);
    }
	
	private void supervisor() {
		final PropagateBackupMessage message = new PropagateBackupMessage(storage.getAll());
		clusterManager.tell(message, self());
	}

    private void onMoveStorage(MoveStorageMessage msg) {
        final Map<String, String> move = this.storage.getKeySpace(msg.getFromKey(), msg.getToKey());
        this.storage.remove(move);
        log.debug("Moving [{}] to {}", move, msg.getDestination());
        msg.getDestination().tell(new MoveStorageRequestMessage(move), self());
    }

    private void onMoveStorageRequest(MoveStorageRequestMessage msg) {
        this.storage.put(msg.getMove());
        log.debug("Imported storage [{}] from {}", msg.getMove(), sender());
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
        clusterManager.tell(new GetPartitionGetterRequestMessage(FINGER_TABLE_SIZE + 1, msg, sender()), self());
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
    	this.storage.addToPartition(msg.getAddress(), msg.getBackup());
        this.backups.put(msg.getAddress(), System.currentTimeMillis());
        
        for (Address address : this.backups.keySet()) {
        	if (System.currentTimeMillis() - this.backups.get(address) > BACKUP_MAX_TIME_DURATION.toMillis()) {
        		this.backups.remove(address);
        		this.storage.removePartition(address);
        	}
        }  
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
    	clusterManager.tell(new GetPartitionBackupRequestMessage(storage.getPartition(msg.getAddress())), self());
    }

    private void onGetPartitionBackupResponse(GetPartitionBackupResponseMessage msg) {
        log.info("Received restore request");
        storage.put(msg.getBackup());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()

                .match(MoveStorageMessage.class, this::onMoveStorage)
                .match(MoveStorageRequestMessage.class, this::onMoveStorageRequest)

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

                .match(GetPartitionBackupResponseMessage.class, this::onGetPartitionBackupResponse)

                .matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }

}
