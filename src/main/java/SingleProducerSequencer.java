import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

/**
 * The class that manages producer sequence. Responsible for update of producer sequence.
 * Prevents wrapping up ring buffer by tracking consumer sequences
 */
class SingleProducerSequencer implements Sequencer {
    private CopyOnWriteArrayList<Sequence> gatingSequences = new CopyOnWriteArrayList<>();
    private Sequence producerSequence = new Sequence();
    private int bufferSize;

    /**
     *
     * @param bufferSize - size of ring buffer
     */
    SingleProducerSequencer(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Acquire index to next for publisher free entity, may block till all consumer thread
     * processed next entry in order to prevent event loss
     * @return sequence to be used to publish new entity
     */
    @Override
    public long next() {
        long nextVal = producerSequence.get() + 1;
        long wrapPoint = nextVal - bufferSize;
        long minGatingSequence = getMinGatingSequence(nextVal);

        while(minGatingSequence <= wrapPoint) {
            LockSupport.parkNanos(1L);
            minGatingSequence = getMinGatingSequence(nextVal);
        }

        return nextVal;
    }


    /**
     * Performs publishing of ready for consumption entity,
     * this call includes storestore barrier all stores before this call happens before
     * @param sequenceValue
     */
    @Override
    public void publish(long sequenceValue) {
        producerSequence.set(sequenceValue);
    }

    /**
     * Add consumer sequence to track in order to prevent buffer wrapping
     * @param sequence
     */
    @Override
    public void addGatingSequence(Sequence sequence) {
        gatingSequences.add(sequence);
    }

    /**
     * Returns producer sequence
     * @return - producer sequence
     */
    @Override
    public Sequence getProducerSequence() {
        return producerSequence;
    }

    private long getMinGatingSequence(final long initialMinValue) {
        long min = initialMinValue;
        for (int i = 0; i < gatingSequences.size(); i++) {
            min = Math.min(min, gatingSequences.get(i).get());
        }

        return min;
    }
}
