import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Exchanger<T> {
    private volatile boolean started = false;
    private List<Consumer<T>> eventHandlers = new ArrayList<>();
    private ExecutorService executorService;
    private RingBuffer<T> ringBuffer;

    public Exchanger(int size, ExecutorService executorService, Supplier<T> initializer) {
        this.executorService = executorService;
        this.ringBuffer = new RingBuffer<>(size, initializer);
    }

    /**
     * Add event handler - events will be handled in separate thread
     * @param eventHandler - event handler
     */
    public void addEventHandler(Consumer<T> eventHandler) {
        if (started) {
            throw new IllegalStateException("Exchanger already started, can not add event handler");
        }
        this.eventHandlers.add(eventHandler);
    }

    /**
     * Starts the ring buffer, starts event handler threads
     * @return - ready to use instance of the ring buffer
     */
    public RingBuffer<T> start() {
        if (started) {
            throw new IllegalStateException("Exchanger already started");
        }
        started = true;
        Sequencer sequencer = this.ringBuffer.getSequencer();

        SequenceBarrier producerSequenceBarrier = new SequenceBarrier(sequencer.getProducerSequence());
        eventHandlers.forEach(tConsumer -> {
            Sequence consumerSequence = new Sequence();
            sequencer.addGatingSequence(consumerSequence);
            executorService.execute(new EventProcessor<>(consumerSequence,
                    tConsumer,
                    producerSequenceBarrier,
                    this.ringBuffer));
        });

        return this.ringBuffer;
    }
}
