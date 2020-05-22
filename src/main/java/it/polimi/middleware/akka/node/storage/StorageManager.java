package it.polimi.middleware.akka.node.storage;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.model.StatusCodes;
import it.polimi.middleware.akka.messages.api.ErrorMessage;
import it.polimi.middleware.akka.messages.api.ReplyMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetterMessage;
import it.polimi.middleware.akka.messages.storage.PutterMessage;

/**
 * Actor in charge of managing data partitions. When data partition is needed to be updated,
 * it handles all the necessary operations. 
 */
public class StorageManager extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private final Cluster cluster = Cluster.get(getContext().getSystem());
	private final Storage storage = Storage.get(getContext().getSystem());
	
	public void onGetByKey(GetterMessage msg) {
		ReplyMessage reply = storage.get(msg.getKey());		
		sender().tell(reply, self());
	}
	
	public void onGetAll(GetterMessage msg) {
		ReplyMessage reply = storage.getAll();
		sender().tell(reply, self());
	}
	
	public void onPut(PutterMessage msg) {
		if (!cluster.selfMember().hasRole("master")) {
			ReplyMessage reply = new ErrorMessage(StatusCodes.FORBIDDEN);
			sender().tell(reply, self());

		} else {
			log.debug("Put request received. Asking PartitionManager for partition.");
			getContext().getParent().tell(new GetPartitionRequestMessage(msg, sender()), self());
		}
	}
	
	public void onGetPartitionResponse(GetPartitionResponseMessage msg) {
		ReplyMessage reply = storage.put(msg.getEntry().getKey(), msg.getEntry().getValue());
		msg.getReplyTo().tell(reply, self());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				
				.match(GetterMessage.class, msg -> !msg.isAll(), this::onGetByKey)
				.match(GetterMessage.class, msg -> msg.isAll(),  this::onGetAll)
				
				.match(PutterMessage.class, this::onPut)
				
				.match(GetPartitionResponseMessage.class, this::onGetPartitionResponse)

				.matchAny(msg -> log.warning("Received unknown message: {}", msg))
				.build();
	}

	public static Props props() {
		return Props.create(StorageManager.class);
	}

}
