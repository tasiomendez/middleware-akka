package it.polimi.middleware.akka.node.storage;

import akka.cluster.ddata.ReplicatedData;

import java.io.Serializable;

/**
 * Encapsulates a conflict-free replicated data type of a strings, that resolves conflict by simply discarding the
 * current value over the new one.
 */
public class CrdtString implements ReplicatedData, Serializable {

    private static final long serialVersionUID = 1L;

    private final String string;

    private CrdtString(String string) {
        this.string = string;
    }

    /**
     * Creates an instance of {@link CrdtString} from the provided string.
     *
     * @param string the string to encapsulate in the instance of {@link CrdtString}
     * @return a new instance of {@link CrdtString}
     */
    public static CrdtString of(String string) {
        return new CrdtString(string);
    }

    public String getString() {
        return string;
    }

    @Override
    public ReplicatedData merge(ReplicatedData that) {
        return that;
    }

    @Override
    public String toString() {
        return string;
    }
}
