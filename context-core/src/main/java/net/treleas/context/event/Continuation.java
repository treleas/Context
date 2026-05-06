package net.treleas.context.event;

public interface Continuation {
    void resume();
    void fail(Throwable throwable);
}
