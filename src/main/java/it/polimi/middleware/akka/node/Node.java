package it.polimi.middleware.akka.node;

import java.util.HashMap;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.GetterMessage;
import it.polimi.middleware.akka.messages.PutterMessage;
import it.polimi.middleware.akka.messages.api.SuccessMessage;
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
				.matchAny(O -> log.warning("Received unknown message"))
				.build();
	}

	private final void onGetByKey(GetterMessage msg) {
		log.debug("Get request received for key: {}", msg.getKey());
		final String address = cluster.selfMember().address().toString();
		final SuccessMessage reply = new SuccessMessage(msg.getKey(), "test value", address);
		sender().tell(reply, self());
	};
	
	private final void onGetAll(GetterMessage msg) {
		log.debug("Get request received for all keys");
		
		// TEST HashMap
		final HashMap<String, String> storage = new HashMap<String, String>();
		storage.put("hey", "yo");
		storage.put("hello", "bye");
		storage.put("how are you?", "fine!");
		
		final SuccessMessage reply = new SuccessMessage(storage);
		sender().tell(reply, self());
	};
	
	private final void onPut(PutterMessage msg) {
		log.debug("Put request received for <key,value>: <{},{}>", msg.getKey(), msg.getValue());
		final String address = cluster.selfMember().address().toString();
		final SuccessMessage reply = new SuccessMessage(msg.getKey(), msg.getValue(), address);
		sender().tell(reply, self());
	};
	
	public static Props props() {
		return Props.create(Node.class);
	}

}
