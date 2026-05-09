package net.treleas.context.event;

import java.util.concurrent.CompletableFuture;

/**
 * A functional interface used by the {@link EventEngine} to trigger the event
 * delivery process.
 * <p>
 * This dispatcher bridges the gap between the event publication (handled by the engine)
 * and the actual subscriber invocation logic (handled by the bus).
 */
@FunctionalInterface
public interface SubscriberDispatcher {

    /**
     * Dispatches an event to a specific set of subscribers starting from a given index.
     *
     * @param event  The event object being published.
     * @param subs   The array of subscribers registered for this event type.
     * @param start  The starting index in the subscriber array (used for recursive/async resumption).
     * @param limit  The limit in the subscriber array (used for recursive/async resumption).
     * @param future The future that will be completed once the entire delivery chain is finished.
     */
    void dispatch(Object event, EventSubscriber[] subs, int start, int limit, CompletableFuture<Object> future);
}
