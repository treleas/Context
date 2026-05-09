package net.treleas.context.event;

import net.treleas.context.Lifecycle;
import net.treleas.context.event.engine.EventEngine;
import net.treleas.context.event.pool.EventPool;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * A high-performance event bus built on top of pluggable engines (EventEngine).
 * Supports synchronous and asynchronous event handling, Continuation-based flows,
 * and integrations via specialized engines.
 *
 * @param executor The execution service used for asynchronous subscribers and tasks.
 * @param pool     The subscriber registry where listeners are stored.
 * @param engine   The publication engine that defines the event delivery strategy.
 */
public record EventBus(@NonNull ExecutorService executor, @NonNull EventPool pool, @NonNull EventEngine engine) implements Lifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);

    /**
     * Constructs a new EventBus instance and initializes the selected engine.
     */
    public EventBus(@NonNull ExecutorService executor, @NonNull EventPool pool, @NonNull EventEngine engine) {
        this.executor = executor;
        this.pool = pool;
        this.engine = engine;

        this.engine.initialize(pool, executor, this::processNext);
    }

    private void processNext(Object event, EventSubscriber[] subs, int index, CompletableFuture<Void> future) {
        runLoop(new SharedContinuation(this, event, subs, future));
    }

    void runLoop(SharedContinuation ctx) {
        final EventSubscriber[] subs = ctx.subs;
        final Object event = ctx.event;
        final boolean isCancellable = ctx.isCancellable;

        try {
            for (int i = ctx.index(); i < subs.length; i++) {
                EventSubscriber sub = subs[i];
                ctx.next(); // Shifting the index for a possible resume()

                // Check for cancellation before executing
                if (isCancellable && ((Cancellable) event).isCancelled() && !sub.ignoreCancelled()) {
                    continue;
                }

                switch (sub.type()) {
                    case EventSubscriber.SYNC -> sub.methodHandle().invoke(event);
                    case EventSubscriber.ASYNC -> {
                        this.executor.execute(() -> {
                            try { sub.methodHandle().invoke(event, ctx); }
                            catch (Throwable t) { ctx.fail(t); }
                        });
                        return; // wait for resume()
                    }
                    case EventSubscriber.TASK -> {
                        ((EventTask) sub.methodHandle().invoke(event)).execute(ctx);
                        return; // wait for resume()
                    }
                }
            }
            ctx.future.complete(null);
        } catch (Throwable t) {
            ctx.fail(t);
        }
    }

    @Override
    public void unmount() {
        this.engine.unmount();
    }

    /**
     * Registers all methods of the listener marked with the {@link Subscribe} annotation.
     * <p>
     * Methods must have either 1 parameter (the event) or 2 parameters (event and {@code Continuation}).
     *
     * @param listener The listener object whose methods will be registered.
     */
    public void register(@NonNull Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            Subscribe sub = method.getAnnotation(Subscribe.class);
            if (sub == null) {
                continue;
            }

            int paramCount = method.getParameterCount();
            if (paramCount < 1 || paramCount > 2) {
                LOGGER.warn("Method {} in {} has invalid parameter count for @Subscribe",
                        method.getName(), listener.getClass().getSimpleName());
                continue;
            }

            try {
                method.setAccessible(true);
                MethodHandle handle = MethodHandles.lookup().unreflect(method).bindTo(listener);
                Class<?> eventType = method.getParameterTypes()[0];

                byte type;
                if (method.getParameterCount() == 2) {
                    type = EventSubscriber.ASYNC;
                } else if (EventTask.class.isAssignableFrom(method.getReturnType())) {
                    type = EventSubscriber.TASK;
                } else {
                    type = EventSubscriber.SYNC;
                }

                EventSubscriber subscriber = new EventSubscriber(
                        listener,
                        handle,
                        sub.priority(),
                        sub.ignoreCancelled(),
                        type
                );

                this.pool.appendSubscriber(eventType, subscriber);

                LOGGER.debug("Registered: {} for {}", method.getName(), eventType.getSimpleName());
            } catch (Exception e) {
                LOGGER.error("Registration failed", e);
            }
        }
    }

    /**
     * Unregisters all listener methods for the specified object across all event types.
     *
     * @param listener The listener object to be removed.
     */
    public void unregister(@NonNull Object listener) {
        this.pool.remove(listener);

        LOGGER.debug("Unregistered all listeners for: {}", listener.getClass().getSimpleName());
    }

    /**
     * Publishes an event to the bus and returns a {@link CompletableFuture}
     * that completes when all subscribers have finished processing the event.
     *
     * @param event The event object to publish.
     * @return A future representing the event delivery status.
     */
    public @NonNull CompletableFuture<Void> post(@NonNull Object event) {
        SubscriberGroup group = pool.subscribers(event.getClass());
        if (group == null) {
            return CompletableFuture.completedFuture(null);
        }

        EventSubscriber[] subs = group.subscribers();

        if (!group.hasAsyncOrTask()) {
            try {
                boolean isCancellable = event instanceof Cancellable;
                for (int i = 0; i < subs.length; i++) {
                    EventSubscriber sub = subs[i];
                    if (isCancellable && ((Cancellable) event).isCancelled() && !sub.ignoreCancelled()) {
                        continue;
                    }
                    sub.methodHandle().invoke(event);
                }
                return CompletableFuture.completedFuture(null);
            } catch (Throwable t) {
                CompletableFuture<Void> fail = new CompletableFuture<>();
                fail.completeExceptionally(t);
                return fail;
            }
        }

        return engine.post(event);
    }

    /**
     * Publishes an event using the "fire and forget" principle.
     * The result of the processing is ignored, and errors are logged internally.
     *
     * @param event The event object to publish.
     */
    public void postAndForget(@NonNull Object event) {
        engine.post(event);
    }

    /**
     * Publishes an event and executes the specified callback once all
     * subscribers have finished processing.
     *
     * @param event    The event object to publish.
     * @param callback A consumer to be notified of completion, receiving a throwable if an error occurred.
     */
    public void postThen(@NonNull Object event, @Nullable Consumer<Throwable> callback) {
        engine.post(event).whenComplete((_, throwable) -> {
            if (callback != null) {
                callback.accept(throwable);
            }
        });
    }

    /**
     * Static factory method to create an {@link EventBus} instance.
     *
     * @param executor The execution service.
     * @param pool     The subscriber pool.
     * @param engine   The publication engine.
     * @return A new instance of the event bus.
     */
    public static @NonNull EventBus create(@NonNull ExecutorService executor, @NonNull EventPool pool, @NonNull EventEngine engine) {
        return new EventBus(executor, pool, engine);
    }

    public static EventBus.@NonNull Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ExecutorService executor;
        private EventPool pool;
        private EventEngine engine;

        private Builder() {
        }

        public @NonNull Builder executor(@NonNull ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public @NonNull Builder pool(@NonNull EventPool pool) {
            this.pool = pool;
            return this;
        }

        public @NonNull Builder engine(@NonNull EventEngine engine) {
            this.engine = engine;
            return this;
        }

        public @NonNull EventBus build() {
            if (executor == null) {
                throw new IllegalStateException("executor has not been set");
            }

            if (pool == null) {
                throw new IllegalStateException("pool has not been set");
            }

            if (engine == null) {
                throw new IllegalStateException("engine has not been set");
            }

            return new EventBus(executor, pool, engine);
        }
    }
}
