package it.polimi.middleware.akka.node.hash;

public class DefaultHash implements HashFunction {

    @Override
    public int hash(String string) {
        return Math.abs(string.hashCode());
    }
}
