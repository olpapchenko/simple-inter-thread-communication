import sun.misc.Unsafe;

/**
 * Class that encapsulates sequence number
 * Updates sequence number and make it visible for other threads
 */
class Sequence {
    private volatile long value;

    private static final long INITIAL_VALUE = -1L;
    private static final Unsafe UNSAFE;
    private static final long VALUE_OFFSET;

    static {
        UNSAFE = Utils.getUnsafe();
        try {
            VALUE_OFFSET = UNSAFE.objectFieldOffset(Sequence.class.getDeclaredField("value"));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    Sequence() {
        this(INITIAL_VALUE);
    }

    Sequence(long initialValue) {
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, initialValue);
    }


    long get() {
        return value;
    }

    /**
     * Set new sequence value, includes store store barrier, meaning
     * new value will be eventually visible for other threads. All store operation before call of this method
     * happens before sequence update
     * @param value - new sequence value
     */
    void set(long value) {
        Utils.getUnsafe().putOrderedLong(this, VALUE_OFFSET, value);
    }
}
