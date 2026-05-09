package net.treleas.context.event.pool;

import net.treleas.context.event.EventSubscriber;
import net.treleas.context.event.SubscriberGroup;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A registry responsible for storing and managing event subscribers.
 * <p>
 * The pool handles the association between event classes and their respective
 * listeners, providing efficient lookups for the {@link EventEngine}.
 */
public interface EventPool {

    /**
     * Retrieves all subscribers registered for a specific event type.
     *
     * @param eventClass The class of the event being published.
     * @return An array of {@link EventSubscriber}s, or {@code null} if no subscribers are found.
     */
    @Nullable SubscriberGroup subscribers(@NonNull Class<?> eventClass);

    /**
     * Appends a new subscriber to the registry for a specific event type.
     * <p>
     * Implementation should handle internal sorting (e.g., by priority)
     * to ensure correct delivery order.
     *
     * @param eventClass The class of the event the subscriber is interested in.
     * @param subscriber The subscriber metadata and handle to be stored.
     */
    void appendSubscriber(@NonNull Class<?> eventClass, @NonNull EventSubscriber subscriber);

    /**
     * Removes all subscribers belonging to the specified listener object.
     *
     * @param listener The listener instance to be unregistered from all events.
     */
    void remove(@NonNull Object listener);

    /**
     * Creates a thread-safe implementation of the subscriber pool using concurrent collections.
     *
     * @return A new instance of {@code ConcurrentEventPool}.
     */
    static @NonNull EventPool concurrent() {
        return new ConcurrentEventPool();
    }
}
