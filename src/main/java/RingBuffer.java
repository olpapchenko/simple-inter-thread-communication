import java.util.function.Supplier;

/**
 * Primary storage for events
 * @param <T> - type of the event to be stored
 */
public class RingBuffer<T> {

    private final Object[] ringBuffer;
    private final Sequencer sequencer;

    private final int bufferSize;

    /**
     *
     * @param capacity - capacity of the ring buffer
     * @param initializer - initializer of the ring buffer
     */
    RingBuffer(int capacity, Supplier<T> initializer) {
        ringBuffer = new Object[capacity];
        bufferSize = capacity;
        for (int i = 0; i < capacity; i++) {
            ringBuffer[i] = initializer.get();
        }
        sequencer = new SingleProducerSequencer(bufferSize);
    }


    /**
     * Returns sequencer for given publisher
     * @return sequencer
     */
    Sequencer getSequencer() {
        return sequencer;
    }

    /**
     * Acquire index to next for publisher free entity, may block till all consumer thread
     * processed next entry in order to prevent event loss
     * @return sequence to be used to publish new entity
     */
    public long next() {
        return sequencer.next();
    }

    /**
     * Returns event entity for given index, use this method after calling {@link RingBuffer#next()}
     * @param index - index of entity to get
     * @return event entity
     */
    public T get(long index) {
       return (T) ringBuffer[(int)index % bufferSize];
    }

    /**
     * Performs publishing of ready for consumption entity,
     * this call includes storestore barrier all stores before this call happens before
     * @param sequenceValue
     */
    public void publish(long sequenceValue) {
        sequencer.publish(sequenceValue);
    }
}
