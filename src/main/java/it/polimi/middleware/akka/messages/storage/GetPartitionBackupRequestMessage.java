package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;
import java.util.Map;

public class GetPartitionBackupRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> backup;

    public GetPartitionBackupRequestMessage(Map<String, String> backup) {
        this.backup = backup;
    }

    public Map<String, String> getBackup() {
        return backup;
    }
}
