import java.util.function.Consumer;

/**
 * Event processor - the wrapper around user provided event handlers.
 * The main purpose of the class is to wait till there available next entry for consumption on the ring buffer.
 * Also this class updates consumed event sequence number.
 * For each event handler provided by user there is separate instance of this class. Each instance of the class is
 * run in separate thread
 *
 * @param <T>
 */
class EventProcessor<T> implements Runnable {
    private Sequence sequence;
    private Consumer<T> consumer;
    private SequenceBarrier producerSequenceBarrier;
    private RingBuffer<T> ringBuffer;

    /**
     * @param consumerSequence        - sequence of given consumer, shows index consumer wait to consume
     * @param consumer                - event handler submitted by the user for event handling
     * @param producerSequenceBarrier - barrier of the producer - used to wait for event to be ready for consumption
     * @param ringBuffer              - ring buffer
     */
    EventProcessor(Sequence consumerSequence,
                   Consumer<T> consumer,
                   SequenceBarrier producerSequenceBarrier,
                   RingBuffer<T> ringBuffer) {
        this.sequence = consumerSequence;
        this.consumer = consumer;
        this.producerSequenceBarrier = producerSequenceBarrier;
        this.ringBuffer = ringBuffer;
    }

    @Override
    public void run() {
        long nextEntryIndexForConsumption = sequence.get() + 1;

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            long lastAvailableIndexForConsumption = producerSequenceBarrier.waitFor(nextEntryIndexForConsumption);

            while (nextEntryIndexForConsumption <= lastAvailableIndexForConsumption) {
                T entry = ringBuffer.get(nextEntryIndexForConsumption);
                consumer.accept(entry);
                nextEntryIndexForConsumption++;
            }
            sequence.set(nextEntryIndexForConsumption);
        }
    }
}
