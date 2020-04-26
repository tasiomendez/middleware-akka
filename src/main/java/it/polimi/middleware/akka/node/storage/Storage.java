package it.polimi.middleware.akka.node.storage;

import java.util.HashMap;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.GetterMessage;
import it.polimi.middleware.akka.messages.PutterMessage;
import it.polimi.middleware.akka.messages.api.SuccessMessage;

public class Storage extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private final Cluster cluster = Cluster.get(getContext().system());
	private HashMap<String, String> storage = new HashMap<>();

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(GetterMessage.class, msg -> !msg.isAll(), this::onGetByKey)
				.match(GetterMessage.class, msg -> msg.isAll(), this::onGetAll)
				.match(PutterMessage.class, this::onPut)
				.matchAny(O -> log.warning("Received unknown message"))
				.build();
	} 
	
	public void onGetByKey(GetterMessage msg) {
		log.debug("Get request received for key: {}", msg.getKey());
		final String address = cluster.selfMember().address().toString();
		final String value = this.storage.get(msg.getKey());
		final SuccessMessage reply = new SuccessMessage(msg.getKey(), value, address);
		sender().tell(reply, self());
	}
	
	public void onGetAll(GetterMessage msg) {
		log.debug("Get request received for all keys");
		final SuccessMessage reply = new SuccessMessage(this.storage);
		sender().tell(reply, self());
	}
	
	public void onPut(PutterMessage msg) {
		log.debug("Put request received for <key,value>: <{},{}>", msg.getKey(), msg.getValue());
		this.storage.put(msg.getKey(), msg.getValue());
		final String address = cluster.selfMember().address().toString();
		final SuccessMessage reply = new SuccessMessage(msg.getKey(), msg.getValue(), address);
		sender().tell(reply, self());
	}

	public static Props props() {
		return Props.create(Storage.class);
	}

}
