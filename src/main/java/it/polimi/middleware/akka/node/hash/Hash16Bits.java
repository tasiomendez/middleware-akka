package it.polimi.middleware.akka.node.hash;

public class Hash16Bits implements HashFunction {

    /**
     * Returns a 16-bit hash based on the input string: since the return type of the function is {@code int} (32 bits
     * signed), this ensures that the returned value will always be a positive integer.
     *
     * @param string the string to compute the hash on
     * @return a 16-bit hash based on the input string
     */
    @Override
    public int hash(String string) {
        int hash = 0;

        for (int i = 0; i < string.length(); i++) {
            hash = hash + ((hash) << 5) + string.charAt(i) + (string.charAt(i) << 7);
        }

        return ((hash) ^ (hash >> 16)) & 0xffff;
    }
}
