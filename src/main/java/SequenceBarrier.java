public class SequenceBarrier {
    private Sequence producerSequence;

    private SequenceBarrier() {

    }

    SequenceBarrier(Sequence producerSequence) {
        this.producerSequence = producerSequence;
    }

    public long waitFor(long index) {
        long availableSequence = producerSequence.get();
        while(index > availableSequence) {
            availableSequence = producerSequence.get();
        }

        return availableSequence;
    }
}
