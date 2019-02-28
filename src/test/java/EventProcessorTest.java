import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventProcessorTest {
    @Mock
    private RingBuffer<Value> ringBuffer;

    @Mock
    private SequenceBarrier sequenceBarrier;

    @Test
    public void testEventsHandler() throws InterruptedException {
        int producedEventsCount = 1000;
        AtomicInteger expectedValue = new AtomicInteger(0);
        AtomicBoolean ok = new AtomicBoolean(true);

        //given
        EventProcessor<Value> eventProcessor = new EventProcessor<Value>(new Sequence(), event -> {
            if (expectedValue.get() != event.value) {
                ok.set(false);
            }
            expectedValue.incrementAndGet();
        }, sequenceBarrier,
                ringBuffer);

        when(sequenceBarrier.waitFor(anyLong())).thenAnswer(invocation -> {
            Long index = invocation.getArgumentAt(0, Long.class);
            if (index == 500) {
                Thread.sleep(50);
            }
            if (index > producedEventsCount) {
                //force event producer stop at this point
                Thread.sleep(200);
            }
            return index;
        });
        when(ringBuffer.get(anyLong())).thenAnswer(invocation -> new Value(invocation.getArgumentAt(0, Long.class)));

        //when
        Future<?> processor = Executors.newSingleThreadExecutor().submit(eventProcessor);

        //then
        Thread.sleep(200);
        processor.cancel(true);
        assertTrue(ok.get());
        assertEquals(producedEventsCount + 1, expectedValue.get());
    }
}
