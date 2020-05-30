package it.polimi.middleware.akka.node.cluster;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.time.Duration;

public class HeartBeat {
	
	private static final Duration INITIAL_DELAY = Duration.ofSeconds(6);
	private static final Duration BETWEEN_DELAY = Duration.ofSeconds(6);
	
	private final ActorSystem system;
	private final LoggingAdapter log;

	private boolean started = false;

	private static HeartBeat instance; 
	
	private HeartBeat(ActorSystem system) { 
		this.system = system;
		this.log = Logging.getLogger(this.system, this);
	}
	
	/**
	 * Only one instance from this class exists. If not ActorSystem 
	 * is provided, its own ActorSystem instance is used.
	 * 
	 * @param system Customized ActorSystem
	 * @return instance
	 */
	public static HeartBeat get(ActorSystem system) {
		if(instance == null)
            instance = new HeartBeat(system);
        return instance;
	}	
	
	/**
	 * Only one instance from this class exists. If not ActorSystem 
	 * is provided, its own ActorSystem instance is used.
	 * 
	 * @return instance
	 */
	public static HeartBeat get() {
		if(instance == null)
            instance = new HeartBeat(ActorSystem.create());
        return instance;
	}
	
	/**
	 * Start heartbeat messaging. The function passed as parameter is
	 * executed every X seconds.
	 * 
	 * @param runnable
	 */
	public void start(Runnable runnable) {
		log.debug("Starting heartbeat");
        this.system.scheduler().schedule(
                INITIAL_DELAY, // initial delay
                BETWEEN_DELAY, // delay between each invocation
                runnable,
                this.system.dispatcher());
        this.started = true;
	}
	
	/**
	 * Execute a function on the next loop of the heartbeat.
	 *  
	 * @param runnable
	 */
	public void execute(Runnable runnable) {
		this.system.scheduler().scheduleOnce(
				INITIAL_DELAY, 
				runnable, 
				this.system.dispatcher());
	}
	
	/**
	 * Execute a function on the next loop of the heartbeat by
	 * by delaying the number of steps given.
	 *  
	 * @param runnable
	 */
	public void execute(Runnable runnable, int offset) {
		this.system.scheduler().scheduleOnce(
				INITIAL_DELAY.multipliedBy(offset), 
				runnable, 
				this.system.dispatcher());
	}

	public boolean started() {
		return this.started;
	}
}
