import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ExchangerTest {
    private final int RING_BUFFER_SIZE = 10_000;

    @Test
    public void testExchangeWithoutWrapping() {
        testExchanger(RING_BUFFER_SIZE - 100);
    }

    @Test
    public void testExchangeWithWrapping()  {
        testExchanger(RING_BUFFER_SIZE + 100);
    }

    @Test
    public void testSeveralConsumersWithoutWrapping() {
        AtomicBoolean allValuesConsumed = new AtomicBoolean(true);
        AtomicInteger consumedValue = new AtomicInteger(0);

        testExchanger(RING_BUFFER_SIZE - 100, value -> {
            if (value.value != consumedValue.get()) {
                allValuesConsumed.set(false);
            }
            if (consumedValue.get() == 50) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            consumedValue.incrementAndGet();
        });
        assertTrue(allValuesConsumed.get());
        assertEquals(RING_BUFFER_SIZE - 100, consumedValue.get());
    }

    @Test
    public void testSeveralConsumersWithWrapping() {
        AtomicBoolean allValuesConsumed = new AtomicBoolean(true);
        AtomicInteger consumedValue = new AtomicInteger(0);

        testExchanger(RING_BUFFER_SIZE + 100, value -> {
            if (value.value != consumedValue.get()) {
                allValuesConsumed.set(false);
            }
            if (consumedValue.get() == 50) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            consumedValue.incrementAndGet();
        });
        assertTrue(allValuesConsumed.get());
        assertEquals(RING_BUFFER_SIZE + 100, consumedValue.get());
    }

    private void testExchanger(int producedValuesCount, Consumer<Value> ... additionalConsumers) {
        //given
        AtomicBoolean allValuesConsumed = new AtomicBoolean(true);
        AtomicInteger consumedValue = new AtomicInteger(0);

        Exchanger<Value> valueExchanger = new Exchanger<>(RING_BUFFER_SIZE,
                Executors.newCachedThreadPool(),
                () -> new Value(0));

        valueExchanger.addEventHandler(value -> {
            if (value.value != consumedValue.get()) {
                allValuesConsumed.set(false);
            }
            //emulate slow consumption of 50s event
            if (consumedValue.get() == 50) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            consumedValue.incrementAndGet();
        });

        Arrays.asList(additionalConsumers).forEach(valueExchanger::addEventHandler);

        RingBuffer<Value> ringBuffer = valueExchanger.start();

        //when
        int idx = 0;
        while (idx < producedValuesCount) {
            long availableIdx = ringBuffer.next();
            Value value = ringBuffer.get(availableIdx);
            value.value = idx;
            ringBuffer.publish(idx);
            idx++;
        }

        //then
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(allValuesConsumed.get());
        assertEquals(producedValuesCount, consumedValue.get());
    }
}
