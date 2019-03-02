import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SingleProducerSequencerTest {
    private static final int RING_BUFFER_SIZE = 10;

    @Test
    public void notConsumedEventsAreNotOverride() throws InterruptedException {
        //given
        SingleProducerSequencer sequencer = new SingleProducerSequencer(RING_BUFFER_SIZE);
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
        CountDownLatch consumerLatch = new CountDownLatch(1);
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            consumerSequence.set(1);
            eventConsumed.set(true);
            consumerLatch.countDown();
        });

        consumerLatch.await();
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
        AtomicBoolean allEntriesConsumed = new AtomicBoolean();

        Sequence consumerSequence = new Sequence(0);
        Future<?> consumerFuture = Executors.newSingleThreadExecutor().submit(() -> {
            long nextIdx = consumerSequence.get();

            while (nextIdx < bufferSize) {
                //wait producer
                while (nextIdx != sequencer.getProducerSequence().get()) {
                    // busy spin
                }
                assertEquals(nextIdx, value.value);
                nextIdx++;
                consumerSequence.set(nextIdx);
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
                while (consumerSequence.get() != producerSequenceIndex) {
                    //busy spin
                }
                value.value = producerSequenceIndex;

                //storestore barrier - assign to value.value should happen before
                sequencer.publish(producerSequenceIndex);
                producerSequenceIndex++;
            }
            threadsCompletionFlag.incrementAndGet();
        });

        //then


        Thread.sleep(500);
        producerFeature.cancel(true);
        consumerFuture.cancel(true);
        assertTrue(allEntriesConsumed.get());
        assertEquals(2, threadsCompletionFlag.get());
    }
}
