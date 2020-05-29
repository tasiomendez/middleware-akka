package it.polimi.middleware.akka.node.cluster.master;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.CreateRingMessage;
import it.polimi.middleware.akka.messages.join.IdRequestMessage;
import it.polimi.middleware.akka.messages.join.IdResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionResponseMessage;
import it.polimi.middleware.akka.node.Reference;

/**
 * The PartitionManager actor belongs to the master node. It is in charge of keeping track of the members in the cluster
 * and their ids. It will provide new unique ids when needed.
 */
public class PartitionManager extends AbstractActor {

    private final int PARTITION_NUMBER = getContext().getSystem().settings().config().getInt("clustering.partition.max");

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().getSystem());

    // Random ID generator
    private final Random generator = new Random();

    // The members tree stores id and actors, while the ids tree 
    // associates each address to an id.
    private final TreeMap<Integer, Reference> members = new TreeMap<>();
    private final TreeMap<Integer, Integer> ids = new TreeMap<>();

    public PartitionManager() {
    	final int masterId = generator.nextInt(PARTITION_NUMBER);
        // Create ring at beginning
        getContext().getParent().tell(new CreateRingMessage(masterId), self());

        // Actor stored references ClusterManager
        final Reference node = new Reference(masterId, getContext().getParent());
        this.members.put(node.getId(), node);
        this.ids.put(getContext().self().path().address().hashCode(), node.getId());
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


    private void onGetPartitionRequest(GetPartitionRequestMessage msg) {
        final int key = msg.getEntry().getKey().hashCode() % PARTITION_NUMBER;
        final Reference partition = getCeilingReference(key);
        log.debug("Partition for key [{}] is [{}]", msg.getEntry().getKey(), partition.getActor().path());
        partition.getActor().tell(new GetPartitionResponseMessage(msg.getEntry(), msg.getReplyTo()), self());
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

                .match(GetPartitionRequestMessage.class, this::onGetPartitionRequest)

                .matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }
}
