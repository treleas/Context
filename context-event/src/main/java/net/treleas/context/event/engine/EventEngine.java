package net.treleas.context.event.engine;

import net.treleas.context.Lifecycle;
import net.treleas.context.event.SubscriberDispatcher;
import net.treleas.context.event.pool.EventPool;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Defines the core execution logic for event publication and distribution.
 * <p>
 * Implementations of this interface determine how an event is queued,
 * ordered, and handed over to the {@link SubscriberDispatcher}.
 */
public interface EventEngine extends Lifecycle {

    /**
     * Initializes the engine with the required infrastructure.
     *
     * @param pool       The subscriber pool used to retrieve listeners for events.
     * @param executor   The executor service for handling asynchronous tasks.
     * @param dispatcher The dispatcher logic used to invoke subscribers.
     */
    void initialize(@NonNull EventPool pool, @NonNull ExecutorService executor, @NonNull SubscriberDispatcher dispatcher);

    /**
     * Submits an event into the engine for processing.
     *
     * @param event The event object to be published.
     * @return A future that completes when the engine has finished delivering the event to all subscribers.
     */
    @NonNull CompletableFuture<Void> post(@NonNull Object event);

    /**
     * Creates a synchronous engine that dispatches events immediately in the calling thread
     * or via direct executor tasks, bypassing any ring buffers.
     *
     * @return A new instance of {@code DirectEngine}.
     */
    static @NonNull EventEngine direct() {
        return new DirectEngine();
    }
}
