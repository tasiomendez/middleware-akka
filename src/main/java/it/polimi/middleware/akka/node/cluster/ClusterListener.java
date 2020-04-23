package it.polimi.middleware.akka.node.cluster;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ClusterListener extends AbstractActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final Cluster cluster = Cluster.get(getContext().system());
	
	@Override
	public void preStart() {
		cluster.subscribe(self(), ClusterEvent.initialStateAsEvents(), MemberEvent.class, UnreachableMember.class);
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
				.build();
	}
	
	private final void onMemberUp(MemberUp msg) {
		log.info("Node {} - Member is Up: {}", msg.member().address(), msg.member());
	}
	
	private final void onUnreachableMember(UnreachableMember msg) {
		log.info("Node {} - Member detected as Unreachable: {}", msg.member().address(), msg.member());
	}
	
	private final void onMemberRemoved(MemberRemoved msg) {
		log.info("Node {} - Member is Removed: {}", msg.member().address(), msg.member());
	}
	
	private final void onMemberEvent(MemberEvent msg) { }
	
	public static Props props() {
		return Props.create(ClusterListener.class);
	}

}
