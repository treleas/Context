package net.treleas.context.di.pool;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * A centralized repository for storing and managing bean instances.
 *
 * <p>The pool maintains mappings for class-based lookups, tagged lookups,
 * and tracks bean ownership for bulk lifecycle management (e.g., unloading plugins).</p>
 */
public interface BeanPool {

    /**
     * Retrieves a bean registered by its class type.
     *
     * @param service the class of the service to look up
     * @param <T>     the type of the service
     * @return the bean instance, or {@code null} if not found
     */
    <T> @Nullable T classifiedBean(@NonNull Class<T> service);

    /**
     * Retrieves a bean registered with a specific string tag.
     *
     * @param tag the identifier for the dependency
     * @param <T> the expected type of the service
     * @return the bean instance, or {@code null} if not found
     */
    <T> @Nullable T taggedBean(@NonNull String tag);

    /**
     * Registers a new bean into the pool and associates it with an owner.
     *
     * @param owner    the object that owns this bean (used for cleanup)
     * @param service  the class type under which this bean should be indexed
     * @param tag      an optional unique string identifier
     * @param provider the actual bean instance to store
     * @param <T>      the type of the service
     */
    <T> void appendBeam(@NonNull Object owner, @NonNull Class<T> service, @Nullable String tag, @NonNull T provider);

    /**
     * Removes all beans associated with the specified owner.
     *
     * <p>This is typically called when a module or plugin is disabled to
     * prevent memory leaks and clear the registry.</p>
     *
     * @param owner the owner whose beans should be removed
     * @return a collection of removed bean instances, or {@code null} if no beans were owned
     */
    @Nullable Collection<?> removeOwned(@NonNull Object owner);

    /**
     * Creates a thread-safe implementation of the pool based on {@code ConcurrentHashMap}.
     * <p>This is the recommended implementation for high-concurrency environments
     * and asynchronous bean initialization.</p>
     *
     * @return a new thread-safe bean pool instance
     */
    static @NonNull BeanPool concurrent() {
        return new ConcurrentBeanPool();
    }
}