package it.polimi.middleware.akka.node.cluster;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.IdRequestMessage;
import it.polimi.middleware.akka.messages.IdResponseMessage;

public class ClusterListener extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final Cluster cluster = Cluster.get(getContext().system());
    private final ActorRef nodeParent;
    private final Member selfMember = cluster.selfMember();

    public ClusterListener(ActorRef nodeParent) {
        this.nodeParent = nodeParent;
    }

    public static Props props(ActorRef nodeParent) {
        return Props.create(ClusterListener.class, nodeParent);
    }

    private void onMemberUp(MemberUp msg) {
        if (msg.member().equals(selfMember)) {
            log.info("Changed status to Up, requesting id to master");
            // get reference to the current master
            final Address masterAddress = cluster.state().getRoleLeader("master");
            final ActorSelection master = getContext().getSystem().actorSelection(masterAddress + "/user/master");
            // first tell the Node actor that a new member has joined the cluster (itself) so it will create
            // its finger table, than ask the master for an id
            master.tell(new IdRequestMessage(), nodeParent);
        } else {
            log.info("Node {} - Member is Up: {}", msg.member().address(), msg.member());
        }
    }

    private void onUnreachableMember(UnreachableMember msg) {
        log.info("Node {} - Member detected as Unreachable: {}", msg.member().address(), msg.member());
    }

    private void onMemberRemoved(MemberRemoved msg) {
        log.info("Node {} - Member is Removed: {}", msg.member().address(), msg.member());
    }

    private void onMemberEvent(MemberEvent msg) {
    }

    @Override
    public void preStart() {
        cluster.subscribe(self(), ClusterEvent.initialStateAsEvents(),
                MemberEvent.class,
                UnreachableMember.class);
    }

    @Override
    public void postStop() {
        cluster.unsubscribe(self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(MemberUp.class, this::onMemberUp)
                .match(UnreachableMember.class, this::onUnreachableMember)
                .match(MemberRemoved.class, this::onMemberRemoved)
                .match(MemberEvent.class, this::onMemberEvent)
                .match(IdResponseMessage.class, msg -> nodeParent.tell(msg, self()))
                .build();
    }

}
