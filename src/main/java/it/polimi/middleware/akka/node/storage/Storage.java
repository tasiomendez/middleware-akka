package it.polimi.middleware.akka.node.storage;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.Key;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORMapKey;
import akka.cluster.ddata.Replicator;
import akka.cluster.ddata.SelfUniqueAddress;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.model.StatusCodes;
import it.polimi.middleware.akka.messages.GetterMessage;
import it.polimi.middleware.akka.messages.PutterMessage;
import it.polimi.middleware.akka.messages.api.ErrorMessage;
import it.polimi.middleware.akka.messages.api.ReplyMessage;
import it.polimi.middleware.akka.messages.api.SuccessMessage;
import scala.Option;

import java.time.Duration;
import java.util.Optional;

public class Storage extends AbstractActor {

    private static final Key<ORMap<String, CrdtString>> KEY = ORMapKey.create("storage");

    private final SelfUniqueAddress node = DistributedData.get(getContext().getSystem()).selfUniqueAddress();
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().system());

    /** Reference to the underlying replicator actor used to replicate the data. */
    private final ActorRef replicator = DistributedData.get(getContext().getSystem()).replicator();
    /** Reference to the actor (router) that sends requests to the node. */
    private ActorRef router;

    public static Props props() {
        return Props.create(Storage.class);
    }

    @Override
    public void preStart() {
        Replicator.Subscribe<ORMap<String, CrdtString>> subscribe = new Replicator.Subscribe<>(KEY, getSelf());
        replicator.tell(subscribe, ActorRef.noSender());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                // messages from the router
                .match(GetterMessage.class, msg -> !msg.isAll(), this::onGetByKey)
                .match(PutterMessage.class, this::onPut)
                // messages from the replicator
                .match(Replicator.GetSuccess.class, msg -> msg.key().equals(KEY), this::onGetSuccess)
                .match(Replicator.Changed.class, msg -> msg.key().equals(KEY), this::onGetChanged)
                .match(Replicator.UpdateSuccess.class, msg -> msg.key().equals(KEY), this::onUpdateSuccess)
                .match(Replicator.NotFound.class, msg -> msg.key().equals(KEY), this::onNotFound)
                // unknown messages
                .matchAny(msg -> log.warning("Received unknown message: {}", msg.toString()))
                .build();
    }

    public void onGetByKey(GetterMessage msg) {
        log.debug("Get request received for key: {}", msg.getKey());
        router = getSender();
        replicator.tell(new Replicator.Get<>(
                KEY,
                new Replicator.ReadAll(Duration.ofSeconds(2)),
                Optional.of(msg.getKey()) // the 'request' parameter
        ), getSelf());
        log.debug("Sent get request to replicator, awaiting answer...");
    }

    public void onPut(PutterMessage msg) {
        log.debug("Put request received for <key,value>: <{},{}>", msg.getKey(), msg.getValue());
        router = getSender();
        CrdtString crdtString = CrdtString.of(msg.getValue());
        replicator.tell(new Replicator.Update<>(
                KEY,
                ORMap.create(), // operation to do when creating the key
                new Replicator.WriteAll(Duration.ofSeconds(2)),
                Optional.of(msg), // the 'request' parameter
                curr -> curr.put(node, msg.getKey(), crdtString)
        ), getSelf());
        log.debug("Sent update request to replicator, awaiting answer...");
    }

    private void onGetSuccess(Replicator.GetSuccess<ORMap<String, CrdtString>> msg) {
        ORMap<String, CrdtString> dataValue = msg.dataValue();
        String key = (String) msg.request().get();
        Option<String> value = dataValue.get(key).map(CrdtString::getString);
        if (value.isEmpty()) {
            router.tell(new ErrorMessage(StatusCodes.NOT_FOUND), getSelf());
            return;
        }
        log.debug("Received get-success response from replicator for key {}", key);
        ReplyMessage reply = new SuccessMessage(key, value.get(), cluster.selfMember().address().toString());
        router.tell(reply, self());
    }

    private void onGetChanged(Replicator.Changed<ORMap<String, CrdtString>> msg) {
        log.debug("Received changed response from replicator");
    }

    private void onUpdateSuccess(Replicator.UpdateSuccess<ORMap<String, CrdtString>> msg) {
        final PutterMessage putterMessage = (PutterMessage) msg.request().get();
        log.debug("Received get-success response from replicator for key {}", putterMessage.getKey());
        final String address = cluster.selfMember().address().toString();
        final SuccessMessage reply = new SuccessMessage(putterMessage.getKey(), putterMessage.getValue(), address);
        router.tell(reply, self());
    }

    private void onNotFound(Replicator.NotFound<ORMap<String, CrdtString>> msg) {
        router.tell(new ErrorMessage(StatusCodes.NOT_FOUND), getSelf());
    }
}
