package it.polimi.middleware.akka.node.storage;

import java.time.Duration;

import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Backup supervisor. It uses the {@link Scheduler} from akka in order to supervise that
 * the backup of data is up to date.
 */
public class Supervisor {
	
	private static final Duration INITIAL_DELAY = Duration.ofSeconds(6);
	private static final Duration BETWEEN_DELAY = Duration.ofSeconds(1);
	
	private final ActorSystem system;
	private final LoggingAdapter log;

	private static Supervisor instance; 
    	
	private Supervisor(ActorSystem system) { 
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
	public static Supervisor get(ActorSystem system) {
		if(instance == null)
            instance = new Supervisor(system);
        return instance;
	}	
	
	/**
	 * Only one instance from this class exists. If not ActorSystem 
	 * is provided, its own ActorSystem instance is used.
	 * 
	 * @return instance
	 */
	public static Supervisor get() {
		if(instance == null)
            instance = new Supervisor(ActorSystem.create());
        return instance;
	}
	
	/**
	 * Start supervising the current node backup. Every 500ms,
	 * a message is propagated to update the backup on the 
	 * following nodes.
	 * 
	 * @param runnable
	 */
	public void start(Runnable runnable) {
		log.debug("Starting backup supervisor every {} milliseconds", BETWEEN_DELAY.toMillis());
        this.system.scheduler().schedule(
                INITIAL_DELAY, // initial delay
                BETWEEN_DELAY, // delay between each invocation
                runnable,
                this.system.dispatcher());
	}

}
