package it.polimi.middleware.akka.node.hash;

/** Interface used to declare hash functions that map keys ({@link String}) to {@code int} values. */
public interface HashFunction {

    int hash(String string);
}
