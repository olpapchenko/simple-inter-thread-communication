import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SingleProducerSequencerTest {
    private static final int RING_BUFFER_SIZE = 10;

    @Test
    public void notConsumedEventsAreNotOverride() {
        //given
        SingleProducerSequencer sequencer = new SingleProducerSequencer(
                RING_BUFFER_SIZE);
        Sequence consumerSequence = new Sequence();
        consumerSequence.set(0);
        sequencer.addGatingSequence(consumerSequence);

        //when
        for (int i = 0; i < RING_BUFFER_SIZE; i++) {
            long next = sequencer.next();
            assertEquals(i, next);
            sequencer.publish(next);
        }

        AtomicBoolean eventConsumed = new AtomicBoolean();
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            consumerSequence.set(1);
            eventConsumed.set(true);
        });

        long next = sequencer.next();
        assertTrue(eventConsumed.get());
        assertEquals(RING_BUFFER_SIZE, next);
    }

    @Test
    public void testPublishSequence() throws InterruptedException {
        //given
        int bufferSize = 100_000;
        SingleProducerSequencer sequencer = new SingleProducerSequencer(bufferSize);

        Value value = new Value(0);
        AtomicInteger threadsCompletionFlag = new AtomicInteger(0);

        //when

        //start consumer thread
        AtomicInteger consumerIdx = new AtomicInteger(0);
        AtomicBoolean allEntriesConsumed = new AtomicBoolean();

        Future<?> consumerSequence = Executors.newSingleThreadExecutor().submit(() -> {

            while (consumerIdx.get() < bufferSize) {
                //wait producer
                while (consumerIdx.get() != sequencer.getProducerSequence().get()) {
                    // busy spin
                }
                assertEquals(consumerIdx.get(), value.value);
                consumerIdx.incrementAndGet();
            }

            allEntriesConsumed.set(true);
            threadsCompletionFlag.incrementAndGet();
        });

        //start producer thread
        Future<?> producerFeature = Executors.newSingleThreadExecutor().submit(() -> {
            Sequence producerSequence = sequencer.getProducerSequence();
            int producerSequenceIndex = 0;

            while (producerSequenceIndex < bufferSize) {
                //wait consumer
                while (consumerIdx.get() != producerSequenceIndex) {
                    //busy spin
                }
                value.value = producerSequenceIndex;

                //storestore barrier - value.value should happen before
                producerSequence.set(producerSequenceIndex);
                producerSequenceIndex++;
            }
            threadsCompletionFlag.incrementAndGet();
        });

        //then


        Thread.sleep(100);
        producerFeature.cancel(true);
        consumerSequence.cancel(true);
        assertTrue(allEntriesConsumed.get());
        assertEquals(2, threadsCompletionFlag.get());
    }
}
