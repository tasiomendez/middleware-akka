package it.polimi.middleware.akka.node.storage;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.model.StatusCodes;
import it.polimi.middleware.akka.messages.api.ErrorMessage;
import it.polimi.middleware.akka.messages.api.ReplyMessage;
import it.polimi.middleware.akka.messages.api.SuccessMessage;
import it.polimi.middleware.akka.node.cluster.ClusterManager;

import java.util.HashMap;
import java.util.Map;

public class Storage {

	private final ActorSystem system;
	private final LoggingAdapter log;
	private final Cluster cluster;

	private final int PARTITION_NUMBER;

	private static Storage instance; 

	private HashMap<String, String> storage = new HashMap<>();
	private HashMap<Address, HashMap<String, String>> backup = new HashMap<>();
	
	private Storage(ActorSystem system) {
		this.system = system;
		this.log = Logging.getLogger(this.system, this);
		this.cluster = Cluster.get(this.system);
		this.PARTITION_NUMBER = this.system.settings().config().getInt("clustering.partition.max");
	}
	
	/**
	 * Only one instance from this class exists. If not ActorSystem 
	 * is provided, its own ActorSystem instance is used.
	 * 
	 * @param system Customized ActorSystem
	 * @return instance
	 */
	public static Storage get(ActorSystem system) {
		if(instance == null)
            instance = new Storage(system);
        return instance;
	}	
	
	/**
	 * Only one instance from this class exists. If not ActorSystem 
	 * is provided, its own ActorSystem instance is used.
	 * 
	 * @return instance
	 */
	public static Storage get() {
		if(instance == null)
            instance = new Storage(ActorSystem.create());
        return instance;
	}
	
	/**
	 * Add a new key value on the current node. It replies {@link SuccessMessage} if
	 * successful.
	 * 
	 * @param key 
	 * @param value
	 * @return reply message
	 */
	public ReplyMessage put(String key, String value) {
		try {
			log.info("Put request received for <key,value>: <{},{}>", key, value);
			this.storage.put(key, value);
			final String address = cluster.selfMember().address().toString();
			return new SuccessMessage(key, value, address);
		} catch (Exception e) {
			return new ErrorMessage(e);
		}
	}

	public void put(Map<String, String> map) {
		this.storage.putAll(map);
	}
	
	/**
	 * Get value from key. Responds with {@link SuccessMessage} if key exists and
	 * {@link ErrorMessage} otherwise
	 * 
	 * @param key
	 * @return reply message
	 */
	public ReplyMessage get(String key) {
		try {
			log.info("Get request received for key: {}", key);
			String address = cluster.selfMember().address().toString();
			String value = this.storage.get(key);
			if (storage.containsKey(key))
				return new SuccessMessage(key, value, address);
			else return new ErrorMessage(StatusCodes.NOT_FOUND);
		} catch (Exception e) {
			return new ErrorMessage(e);
		}
	}
	
	/**
	 * Remove given keys from storage
	 * 
	 * @param map of keys to remove
	 */
	public void remove(Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet())
        	storage.remove(entry.getKey());
	}
	
	/**
	 * Check if contains a given key
	 * 
	 * @param key
	 * @return true if it is contained, otherwise false
	 */
	public boolean contains(String key) {
		return this.storage.containsKey(key);
	}
	
	/**
	 * Get all keys from the main memory
	 * 
	 * @return HashMap
	 */
	public HashMap<String, String> getAll() {
		return this.storage;
	}
	
	/**
	 * Get all keys and values stored in the current node.
	 * 
	 * @return reply message
	 */
	public ReplyMessage getNodeAll() {
		try {
			log.info("Get request received for all keys");
			return new SuccessMessage(this.storage, this.backup);
		} catch (Exception e) {
			return new ErrorMessage(e);
		}
	}

	public Map<String, String> getKeySpace(int fromKey, int toKey) {
		final Map<String, String> result = new HashMap<>();
		for (Map.Entry<String, String> entry : this.storage.entrySet()) {
			final int key = Math.abs(entry.getKey().hashCode() % PARTITION_NUMBER);
			if (ClusterManager.isBetween(key, fromKey, toKey, false, true)) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}
	
	/**
	 * Add a new partition to the backup from an existing node.
	 * 
	 * @param address address of the backup node
	 * @param storage storage to backup
	 * @return data stored
	 */
	public String addToPartition(Address address, Map.Entry<String,String> entry) {
		log.debug("Add new entry to backup from {}", address);
		if (!this.backup.containsKey(address))
			this.backup.put(address, new HashMap<String, String>());
		return this.backup.get(address).put(entry.getKey(), entry.getValue());
	}
	
	/**
	 * Add a new partition to the backup from an existing node.
	 * 
	 * @param address address of the backup node
	 * @param storage storage to backup
	 * @return data stored
	 */
	public void addToPartition(Address address, Map<String,String> map) {
		if (!this.backup.containsKey(address)) {
			log.info("Add new entry to backup from {}", address);
			this.backup.put(address, new HashMap<String, String>());
		}
		this.backup.get(address).clear();
		this.backup.get(address).putAll(map);
	}
	
	/**
	 * Get an existing partition from the backup of an existing node.
	 * 
	 * @param address address of the backup node
	 * @return data 
	 */
	public HashMap<String, String> getPartition(Address address) {
		log.debug("Get existing partition from backup for {}", address);
		return this.backup.get(address);
	}
	
	/**
	 * Check if an existing backup of an address exists.
	 * 
	 * @param address address of the backup node
	 * @return true if it contains, otherwise false 
	 */
	public boolean containsPartition(Address address) {
		return this.backup.containsKey(address);
	}
	
	/**
	 * Remove an existing partition from the backup of an existing node.
	 * 
	 * @param address address of the backup node
	 * @return data removed
	 */
	public HashMap<String, String> removePartition(Address address) {
		log.debug("Removing existing partition from backup from {}", address);
		return this.backup.remove(address);
	}
	
	/**
	 * Restore a backup of a giving node into current node.
	 * 
	 * @param address
	 * @return the restored data
	 */
	public HashMap<String, String> restore(Address address) {
		HashMap<String, String> restorement = this.removePartition(address);
		this.storage.putAll(restorement);
		return restorement;
	}
	
}
