package it.polimi.middleware.akka.node.cluster.master;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.CreateRingMessage;
import it.polimi.middleware.akka.messages.join.IdRequestMessage;
import it.polimi.middleware.akka.messages.join.IdResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionBackupRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionBackupResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionResponseMessage;
import it.polimi.middleware.akka.messages.update.NewSuccessorRequestMessage;
import it.polimi.middleware.akka.messages.update.NewSuccessorResponseMessage;
import it.polimi.middleware.akka.node.Reference;
import it.polimi.middleware.akka.node.hash.HashFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * The PartitionManager actor belongs to the master node. It is in charge of keeping track of the members in the cluster
 * and their ids. It will provide new unique ids when needed.
 */
public class PartitionManager extends AbstractActor {

    private final int PARTITION_NUMBER = getContext().getSystem().settings().config().getInt("clustering.partition.max");

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().getSystem());

    private final HashFunction hashFunction;

    // Random ID generator
    private final Random generator = new Random();

    // The members tree stores id and actors, while the ids tree 
    // associates each address to an id.
    private final TreeMap<Integer, Reference> members = new TreeMap<>();
    private final TreeMap<Integer, Integer> ids = new TreeMap<>();

    public PartitionManager() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    	final int masterId = generator.nextInt(PARTITION_NUMBER);
        // Create ring at beginning
        getContext().getParent().tell(new CreateRingMessage(masterId), self());

        // Actor stored references ClusterManager
        final Reference node = new Reference(masterId, getContext().getParent());
        this.members.put(node.getId(), node);
        this.ids.put(getContext().self().path().address().hashCode(), node.getId());
        Class<?> hashFunctionClass = Class.forName(getContext().getSystem().settings().config().getString("clustering.hash-function"));
        this.hashFunction = (HashFunction) hashFunctionClass.getConstructor().newInstance();
    }

    public static Props props() {
        return Props.create(PartitionManager.class);
    }

    private void onIdRequest(IdRequestMessage msg) {
        int id;
        do {
        	id = generator.nextInt(PARTITION_NUMBER);
        } while (members.containsKey(id));
        
        log.info("Received id request from {}, assigning id {}", sender().path().address(), id);

        final Reference entry = this.getRandomMember();
        log.debug("Assigning successor with id {}, path {}", entry.getId(), entry.getActor().path());
        sender().tell(new IdResponseMessage(id, entry), self());

        // Actor stored references ClusterManager
        final Reference node = new Reference(id, sender());
        this.members.put(node.getId(), node);
        this.ids.put(msg.getMember().address().hashCode(), node.getId());
    }

    private void onUnreachableMember(UnreachableMember msg) {
    	cluster.down(msg.member().address());
    	log.info("Member {} marked as down", msg.member().address());
    	final int id = this.ids.remove(msg.member().address().hashCode());
    	this.members.remove(id);
    }

    private void onNewSuccessorRequest(NewSuccessorRequestMessage msg) {
    	final Reference entry = getRandomMember();
    	sender().tell(new NewSuccessorResponseMessage(entry), self());
    }

    private void onGetPartitionRequest(GetPartitionRequestMessage msg) {
    	final int key = this.hashFunction.hash(msg.getEntry().getKey()) % PARTITION_NUMBER;
        final Reference partition = getCeilingReference(key);
        log.debug("Partition for key [{}] with hash [{}] is [{}]", msg.getEntry().getKey(), key, partition.getId());
        partition.getActor().tell(new GetPartitionResponseMessage(msg.getEntry(), msg.getReplyTo()), self());
    }

    private void onGetPartitionBackupRequest(GetPartitionBackupRequestMessage msg) {
        log.debug("Got request to restore [{}]", msg.getBackup());
        // map the reference of the actor to the backup they will have to store
        final Map<Reference, Map<String, String>> partitions = new HashMap<>();
        for (Map.Entry<String, String> entry : msg.getBackup().entrySet()) {
            // compute the key and get the reference
            final int key = this.hashFunction.hash(entry.getKey()) % PARTITION_NUMBER;
            final Reference node = getCeilingReference(key);
            // if the map doesn't have an entry for that key, create a new HashMap
            partitions.putIfAbsent(node, new HashMap<>());
            final Map<String, String> partition = partitions.get(node);
            partition.put(entry.getKey(), entry.getValue());
        }
        // send the message to every actor
        for (Map.Entry<Reference, Map<String, String>> partition : partitions.entrySet()) {
            final Object message = new GetPartitionBackupResponseMessage(partition.getValue());
            partition.getKey().getActor().tell(message, self());
        }
    }

    /**
     * Get random member from the TreeMap.
     *
     * @return the member
     */
    private Reference getRandomMember() {
        // Get random member
        final Object[] entries = this.members.values().toArray();
        final Random generator = new Random();
        return (Reference) entries[generator.nextInt(entries.length)];
    }

    /**
     * Searches the {@link PartitionManager#members} {@link TreeMap} for an entry whose key is greater or at most equal
     * to the given key. Normally the method {@link TreeMap#ceilingEntry(Object)} will return {@code null} if the
     * requested key is the biggest among the entries of the map, but in a circular ring this is not the expected
     * behaviour (e.g. id=2 should be the result when looking for key=6 if no other nodes lie between the interval 6-2),
     * so the search is repeated starting from 0.
     *
     * @param key the {@link Integer} key to look for
     * @return an entry whose key is greater or at most equal to the given key
     */
    private Reference getCeilingReference(int key) {
        final Map.Entry<Integer, Reference> entry = this.members.ceilingEntry(key);
        return (entry == null) ? this.members.ceilingEntry(0).getValue() : entry.getValue();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(IdRequestMessage.class, this::onIdRequest)
                .match(UnreachableMember.class, this::onUnreachableMember)
                
                .match(NewSuccessorRequestMessage.class, this::onNewSuccessorRequest)

                .match(GetPartitionRequestMessage.class, this::onGetPartitionRequest)

                .match(GetPartitionBackupRequestMessage.class, this::onGetPartitionBackupRequest)

                .matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }
}
