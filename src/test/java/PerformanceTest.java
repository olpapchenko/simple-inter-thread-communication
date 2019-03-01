import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Ignore
public class PerformanceTest {
    private static int BUFFER_SIZE = 1024 * 64;
    private static int ITERATIONS = 50_000_000;

    @Test
    public void testOneProducerOneConsumersExchanger() throws InterruptedException {
        warmUpExchanger();
        CountDownLatch consumerLatch = new CountDownLatch(1);
        CountDownLatch startTestsLatch = runExchanger(ITERATIONS, getConsumer(ITERATIONS, consumerLatch));
        startTestsLatch.await();

        long start = System.nanoTime();
        consumerLatch.await();
        long end = System.nanoTime();

        System.out.println("Exchanger exchange of " + ITERATIONS + " events took: " + (TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS)) + " ms");
    }

    @Test
    public void testOneProducerOneConsumerArrayBlockingQueue() throws InterruptedException {
        warmUpQueue();
        ArrayBlockingQueue<Value> queue = new ArrayBlockingQueue<>(BUFFER_SIZE);
        CountDownLatch consumerLatch = new CountDownLatch(1);

        CountDownLatch startTestLatch = runBlockingQueue(ITERATIONS, queue, getConsumer(ITERATIONS, consumerLatch));

        startTestLatch.await();

        long start = System.nanoTime();
        consumerLatch.await();
        long end = System.nanoTime();

        System.out.println("Queue exchange of " + ITERATIONS + " events took: " + (TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS)) + " ms");
    }

    private void warmUpQueue() {
        runBlockingQueue(100_000, new ArrayBlockingQueue<>(BUFFER_SIZE), getConsumer(100_000, new CountDownLatch(1)));
    }

    private void warmUpExchanger() throws InterruptedException {
        runExchanger(100_000, getConsumer(100_000, new CountDownLatch(1)));
    }

    private CountDownLatch runBlockingQueue(int iterations, BlockingQueue<Value> queue, Consumer<Value> eventHandler) {
        CountDownLatch startTestLatch = new CountDownLatch(1);

        Executors.newSingleThreadExecutor().execute(() -> {
            while (true) {
                try {
                    eventHandler.accept(queue.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Executors.newSingleThreadExecutor().execute(() -> {
            startTestLatch.countDown();
            long idx = 0;
            Value preAllocatedValue = new Value(0);
            while (idx < iterations) {
                preAllocatedValue.value = idx;
                try {
                    queue.put(preAllocatedValue);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                idx++;
            }
        });

        return startTestLatch;
    }


    private CountDownLatch runExchanger(int iterations, Consumer<Value>... eventHandlers) throws InterruptedException {
        CountDownLatch startTestsLatch = new CountDownLatch(1);
        Exchanger<Value> exchanger = new Exchanger<>(BUFFER_SIZE, Executors.newCachedThreadPool(), () -> new Value(0));

        Arrays.asList(eventHandlers).forEach(exchanger::addEventHandler);
        RingBuffer<Value> ringBuffer = exchanger.start();

        Thread.sleep(10);
        Executors.newSingleThreadExecutor().execute(() -> {
            startTestsLatch.countDown();

            int idx = 0;
            while (idx < iterations) {
                long next = ringBuffer.next();
                Value value = ringBuffer.get(next);
                value.value = idx;
                ringBuffer.publish(next);
                idx++;
            }
        });

        return startTestsLatch;
    }

    private Consumer<Value> getConsumer(long targetValue, CountDownLatch latch) {
        return value -> {
            if (value.value == targetValue - 1) {
                latch.countDown();
            }
        };
    }
}
