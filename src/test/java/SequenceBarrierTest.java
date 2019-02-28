import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertFalse;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SequenceBarrierTest {

    @Mock
    private Sequence sequence;

    @Test
    public void testSequenceBarrierIsWaitingForProducer() throws InterruptedException {
        //given
        AtomicBoolean barrierUnlocked = new AtomicBoolean(false);
        SequenceBarrier sequenceBarrier = new SequenceBarrier(sequence);
        when(sequence.get()).thenAnswer(invocation -> {
            Thread.sleep(1000);
            return invocation.getArgumentAt(0, Long.class);
        });

        //when
        Future<?> waiterThread = Executors.newSingleThreadExecutor().submit(() -> {
            sequenceBarrier.waitFor(0);
            barrierUnlocked.set(true);
        });

        //then
        Thread.sleep(100);
        waiterThread.cancel(true);
        assertFalse(barrierUnlocked.get());
    }

    @Test
    public void testBarrierUnlocked() throws InterruptedException {
        //given
        AtomicBoolean barrierUnlocked = new AtomicBoolean(false);
        SequenceBarrier sequenceBarrier = new SequenceBarrier(sequence);
        when(sequence.get()).thenAnswer(invocation -> {
            Thread.sleep(50);
            return invocation.getArgumentAt(0, Long.class);
        });

        //when
        Future<?> waiterThread = Executors.newSingleThreadExecutor().submit(() -> {
            sequenceBarrier.waitFor(0);
            barrierUnlocked.set(true);
        });

        //then
        Thread.sleep(100);
        waiterThread.cancel(true);
        assertFalse(barrierUnlocked.get());

    }
}
