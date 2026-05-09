package net.treleas.context.di.future;

import net.treleas.context.di.Di;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * A reactive pipeline for sequential bean registration and injection.
 *
 * <p>FutureChain ensures that dependencies are processed in a strict order,
 * even when running on asynchronous executors (like Virtual Threads).
 * This prevents race conditions during complex system initialization.</p>
 */
public interface FutureChain {

    /**
     * Schedules a bean for registration and injection after the current
     * stage of the chain completes.
     *
     * @param owner   the object that owns this bean
     * @param service the class type to register the bean as
     * @param tag     an optional string identifier
     * @param bean    the instance to be processed and stored
     * @param <T>     the type of the service
     * @return a new FutureChain stage representing the updated pipeline
     */
    <T> @NonNull FutureChain thenAppend(@NonNull Object owner, @NonNull Class<T> service, @Nullable String tag, @NonNull T bean);

    <T> @NonNull FutureChain thenLazy(@NonNull Object owner, @NonNull Class<T> service, @Nullable String tag, @NonNull Supplier<T> bean);

    /**
     * Schedules a bean for registration using its class type as the primary key.
     * <p>Convenience method for {@link #thenAppend(Object, Class, String, Object)}
     * with a {@code null} tag.</p>
     *
     * @param owner   the object that owns this bean
     * @param service the class type to register the bean as
     * @param bean    the instance to be processed and stored
     * @param <T>     the type of the service
     * @return a new FutureChain stage representing the updated pipeline
     */
    default <T> @NonNull FutureChain thenAppend(@NonNull Object owner, @NonNull Class<T> service, @NonNull T bean) {
        return thenAppend(owner, service, null, bean);
    }

    default <T> @NonNull FutureChain thenLazy(@NonNull Object owner, @NonNull Class<T> service, @NonNull Supplier<T> bean) {
        return thenLazy(owner, service, null, bean);
    }

    @NonNull FutureChain thenCompose(@NonNull FutureChain chain);

    /**
     * Returns a {@link CompletableFuture} that completes when all beans
     * in the chain have been successfully injected and registered.
     *
     * @return the completion handle for the entire chain
     */
    @NonNull CompletableFuture<Void> future();

    // Implementation
    record Impl(@NonNull ExecutorService executor, @NonNull CompletableFuture<Void> future, @NonNull Di di) implements FutureChain {
        @Override
        public @NonNull <T> FutureChain thenAppend(@NonNull Object owner, @NonNull Class<T> service, @Nullable String tag, @NonNull T bean) {
            return new Impl(executor, future.thenRunAsync(() -> di.appendBean(owner, service, tag, bean), executor), di);
        }

        @Override
        public @NonNull <T> FutureChain thenLazy(@NonNull Object owner, @NonNull Class<T> service, @Nullable String tag, @NonNull Supplier<T> bean) {
            return new Impl(executor, future.thenRunAsync(() -> di.appendBean(owner, service, tag, bean.get()), executor), di);
        }

        @Override
        public @NonNull FutureChain thenCompose(@NonNull FutureChain chain) {
            return new Impl(executor, chain.future(), di);
        }
    }

    // Empty implementation
    record Empty(@NonNull ExecutorService executor, @NonNull Di di) implements FutureChain {
        @Override
        public @NonNull <T> FutureChain thenAppend(@NonNull Object owner, @NonNull Class<T> service, @Nullable String tag, @NonNull T bean) {
            var future = CompletableFuture.runAsync(() -> di.appendBean(owner, service, tag, bean), executor);
            return new Impl(executor, future, di);
        }

        @Override
        public @NonNull <T> FutureChain thenLazy(@NonNull Object owner, @NonNull Class<T> service, @Nullable String tag, @NonNull Supplier<T> bean) {
            var future = CompletableFuture.runAsync(() -> di.appendBean(owner, service, tag, bean.get()), executor);
            return new Impl(executor, future, di);
        }

        @Override
        public @NonNull FutureChain thenCompose(@NonNull FutureChain chain) {
            return new Impl(executor, chain.future(), di);
        }

        @Override
        public @NonNull CompletableFuture<Void> future() {
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Starts a new, empty asynchronous chain using a custom executor.
     *
     * @param executor the service used to run injection and registration tasks
     * @param di       the DiWire instance used to process the beans
     * @return a new empty FutureChain stage
     */
    static @NonNull FutureChain chain(@NonNull ExecutorService executor, @NonNull Di di) {
        return new Empty(executor, di);
    }

    /**
     * Wraps an existing future into a FutureChain, allowing subsequent DI operations
     * to wait for its completion.
     *
     * @param executor the service used to run the next stages of the chain
     * @param future   the future to synchronize with
     * @param di       the DiWire instance used to process the beans
     * @return a new FutureChain stage linked to the provided future
     */
    static @NonNull FutureChain chain(@NonNull ExecutorService executor, @NonNull CompletableFuture<Void> future, @NonNull Di di) {
        return new Impl(executor, future, di);
    }
}
