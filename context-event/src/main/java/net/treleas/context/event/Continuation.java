package net.treleas.context.event;

/**
 * A handle to control the flow of asynchronous event processing.
 * <p>
 * This interface is used when a subscriber performs non-blocking or long-running
 * operations and needs to signal back to the {@code EventBus} when to continue
 * or abort the execution chain.
 */
public interface Continuation {

    /**
     * Resumes the event delivery chain.
     * <p>
     * Calling this method signals that the current subscriber has successfully
     * finished its task and the bus can now proceed to the next subscriber.
     */
    void resume();

    /**
     * Aborts the event delivery chain with an error.
     * <p>
     * The associated {@code CompletableFuture} will be completed exceptionally,
     * and subsequent subscribers will not be executed for this event.
     *
     * @param throwable the cause of the failure.
     */
    void fail(Throwable throwable);
}
