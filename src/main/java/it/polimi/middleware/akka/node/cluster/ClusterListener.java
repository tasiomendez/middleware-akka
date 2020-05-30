package it.polimi.middleware.akka.node.cluster;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.join.MasterNotificationMessage;

/**
 * The ClusterListener actor is in charge of listening events in the cluster and notifies
 * the corresponding actors.
 */
public class ClusterListener extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final Cluster cluster = Cluster.get(getContext().system());

	/**
	 * A new member has joined the cluster. When joined, the master node 
	 * notifies the new member who is the master.
	 * 
	 * @param msg message
	 */
	private void onMemberUp(MemberUp msg) {
		log.info("Node {} - Member is Up: {}", msg.member().address(), msg.member());
		if (cluster.selfMember().hasRole("master") && !msg.member().hasRole("master")) {
			// Notify new member who is the master
			final ActorSelection member = getContext().getSystem().actorSelection(msg.member().address() + "/user/node");
			member.tell(new MasterNotificationMessage(getContext().getParent()), self());
		}
	}

	/**
	 * A member has been detected as unreachable. If the member detected is the master, the system
	 * is terminated. A message to the {@link ClusterManager} is sent to handle the reachability.
	 * 
	 * @param msg
	 */
	private void onUnreachableMember(UnreachableMember msg) {
		log.info("Node {} - Member detected as Unreachable: {}", msg.member().address(), msg.member());
		if (msg.member().hasRole("master")) {
			log.error("Master Node detected as Unreachable. Shutting down system.");
			getContext().getSystem().terminate();
		} else {
			// Send message to ClusterManager
			getContext().getParent().tell(msg, self());
		}
	}

	/**
	 * A member has been removed from the cluster. No longer is in the cluster.
	 * 
	 * @param msg
	 */
	private void onMemberRemoved(MemberRemoved msg) {
		log.info("Node {} - Member is Removed: {}", msg.member().address(), msg.member());
		if (msg.member().hasRole("master")) {
			log.error("Master Node detected as Unreachable. Shutting down system.");
			getContext().getSystem().terminate();
		} else {
			// Send message to ClusterManager
			getContext().getParent().tell(msg, self());
		}
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
				.match(MemberUp.class,          this::onMemberUp)
				.match(UnreachableMember.class, this::onUnreachableMember)
				.match(MemberRemoved.class,     this::onMemberRemoved)
				.match(MemberEvent.class,       this::onMemberEvent)
				.build();
	}

	public static Props props() {
		return Props.create(ClusterListener.class);
	}

}
