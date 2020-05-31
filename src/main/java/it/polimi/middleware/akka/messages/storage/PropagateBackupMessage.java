package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;
import java.util.HashMap;

public class PropagateBackupMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final HashMap<String, String> backup;
	
	public PropagateBackupMessage(HashMap<String, String> backup) {
		this.backup = backup;
	}

	public final HashMap<String, String> getBackup() {
		return backup;
	}
	
}
