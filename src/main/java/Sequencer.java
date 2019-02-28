interface Sequencer {
    long next();
    void publish(long sequenceValue);
    void addGatingSequence(Sequence sequence);
    Sequence getProducerSequence();
}
