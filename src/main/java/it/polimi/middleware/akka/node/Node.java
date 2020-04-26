package it.polimi.middleware.akka.node;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.GetterMessage;
import it.polimi.middleware.akka.messages.PutterMessage;
import it.polimi.middleware.akka.messages.ReplyMessage;
import it.polimi.middleware.akka.node.cluster.ClusterManager;

public class Node extends AbstractActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private final Cluster cluster = Cluster.get(getContext().system());
	private final ActorRef clusterManager = getContext().actorOf(ClusterManager.props(), "clusterManager");
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(GetterMessage.class, msg -> !msg.isAll(), this::onGetByKey)
				.match(GetterMessage.class, msg -> msg.isAll(), this::onGetAll)
				.match(PutterMessage.class, this::onPut)
				.matchAny(O -> log.info("Received unknown message"))
				.build();
	}

	private final void onGetByKey(GetterMessage msg) {
		log.debug("Get request received for key: {}", msg.getKey());
		final String address = cluster.selfMember().address().toString();
		sender().tell(new ReplyMessage(msg.getKey(), "test value", address), self());
	};
	
	private final void onGetAll(GetterMessage msg) {
		// TODO
	};
	
	private final void onPut(PutterMessage msg) {
		// TODO
	};
	
	public static Props props() {
		return Props.create(Node.class);
	}

}
