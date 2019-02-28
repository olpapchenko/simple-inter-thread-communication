import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ExchangerTest {
    private final int RING_BUFFER_SIZE = 10_000;

    @Test
    public void testExchangeWithoutWrapping() throws InterruptedException {
        //given
        AtomicBoolean allValuesConsumed = new AtomicBoolean(true);
        AtomicInteger consumedValue = new AtomicInteger(0);

        Exchanger<Value> valueExchanger = new Exchanger<>(RING_BUFFER_SIZE,
                Executors.newSingleThreadExecutor(),
                () -> new Value(0));

        valueExchanger.addEventHandler(value -> {
            if (value.value != consumedValue.get()) {
                allValuesConsumed.set(false);
            }
            consumedValue.incrementAndGet();
        });
        RingBuffer<Value> ringBuffer = valueExchanger.start();


        //when
        int idx = 0;
        while (idx < RING_BUFFER_SIZE) {
            long availableIdx = ringBuffer.next();
            Value value = ringBuffer.get(availableIdx);
            value.value = idx;
            ringBuffer.publish(idx);
            idx++;
        }

        //then
        Thread.sleep(1000);
        assertTrue(allValuesConsumed.get());
        assertEquals(RING_BUFFER_SIZE, consumedValue.get());
    }
}
