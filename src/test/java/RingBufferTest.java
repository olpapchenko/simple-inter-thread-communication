import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class RingBufferTest {

    private static final int RING_BUFFER_SIZE = 100;

    @Test
    public void testInitializeRingBuffer() {
        //when
        RingBuffer<Value> valueRingBuffer = new RingBuffer<>(RING_BUFFER_SIZE, () -> new Value(0));

        //then
        for (long i = 0; i < RING_BUFFER_SIZE; i++) {
            assertEquals(0, valueRingBuffer.get(i).value);
        }
    }

    @Test
    public void getEntityWithWrappingIndex() {
        //given
        RingBuffer<Value> valueRingBuffer = new RingBuffer<>(RING_BUFFER_SIZE, () -> new Value(0));

        //when
        for (int i = 0; i < RING_BUFFER_SIZE; i++) {
            Value value = valueRingBuffer.get(i);
            value.value = i;
        }

        //then
        for (int i = 0; i < RING_BUFFER_SIZE * 2; i++) {
            assertEquals(i % (RING_BUFFER_SIZE), valueRingBuffer.get(i).value);
        }
    }
}
