package net.treleas.context.event;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import net.treleas.context.di.Lifecycle;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

public class EventBus implements Lifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);

    private final Map<Class<?>, EventSubscriber[]> registry = new ConcurrentHashMap<>();

    private final ExecutorService executor;
    private final Disruptor<EventHolder> disruptor;
    private final RingBuffer<EventHolder> ringBuffer;

    private EventBus(@NonNull ExecutorService executor) {
        this.executor = executor;

        this.disruptor = new Disruptor<>(
                EventHolder::new,
                1024 * 64,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        );

        this.disruptor.handleEventsWith((holder, _, _) -> {
            Object event = holder.event();
            CompletableFuture<Void> future = holder.future();
            holder.set(null, null);

            EventSubscriber[] subscribers = this.registry.get(event.getClass());

            if (subscribers == null || subscribers.length == 0) {
                future.complete(null);
                return;
            }

            processNext(event, subscribers, 0, future);
        });

        this.ringBuffer = this.disruptor.start();
    }

    private void processNext(Object event, EventSubscriber[] subs, int index, CompletableFuture<Void> future) {
        // Termination condition: all subscribers processed
        if (index >= subs.length) {
            future.complete(null);
            return;
        }

        EventSubscriber sub = subs[index];

        // Check for cancellation before executing
        if (event instanceof Cancellable c && c.isCancelled() && !sub.ignoreCancelled()) {
            processNext(event, subs, index + 1, future);
            return;
        }

        this.executor.execute(() -> {
            try {
                MethodHandle handle = sub.methodHandle();
                int paramCount = handle.type().parameterCount();

                if (paramCount == 2) {
                    handle.invoke(event, new Continuation() {
                        @Override
                        public void resume() {
                            processNext(event, subs, index + 1, future);
                        }

                        @Override
                        public void fail(Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                    return; // Stop here, wait for resume()
                }

                Object result = handle.invoke(event);

                if (result instanceof EventTask task) {
                    task.execute(new Continuation() {
                        @Override
                        public void resume() {
                            processNext(event, subs, index + 1, future);
                        }

                        @Override
                        public void fail(Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                } else {
                    // Standard synchronous listener, move to next immediately
                    processNext(event, subs, index + 1, future);
                }

            } catch (Throwable t) {
                LOGGER.error("Event delivery failed for {}", sub.instance().getClass().getSimpleName(), t);
                future.completeExceptionally(t);
            }
        });
    }

    @Override
    public void unmount() {
        this.disruptor.shutdown();
    }

    public void register(@NonNull Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            Subscribe sub = method.getAnnotation(Subscribe.class);
            if (sub == null) {
                continue;
            }

            int paramCount = method.getParameterCount();
            // Allow both (Event) and (Event, Continuation)
            if (paramCount < 1 || paramCount > 2) {
                LOGGER.warn("Method {} in {} has invalid parameter count for @Subscribe",
                        method.getName(), listener.getClass().getSimpleName());
                continue;
            }

            try {
                method.setAccessible(true);
                MethodHandle handle = MethodHandles.lookup().unreflect(method).bindTo(listener);
                Class<?> eventType = method.getParameterTypes()[0];

                this.registry.compute(eventType, (k, currentArray) -> {
                    List<EventSubscriber> list = currentArray == null
                            ? new ArrayList<>()
                            : new ArrayList<>(Arrays.asList(currentArray));

                    list.add(new EventSubscriber(listener, handle, sub.priority(), sub.ignoreCancelled()));
                    Collections.sort(list);
                    return list.toArray(new EventSubscriber[0]);
                });

                LOGGER.debug("Registered: {} for {}", method.getName(), eventType.getSimpleName());
            } catch (Exception e) {
                LOGGER.error("Registration failed", e);
            }
        }
    }

    public void unregister(@NonNull Object listener) {
        for (Class<?> eventType : this.registry.keySet()) {
            this.registry.computeIfPresent(eventType, (_, currentArray) -> {
                List<EventSubscriber> list = new ArrayList<>(Arrays.asList(currentArray));

                boolean removed = list.removeIf(sub -> sub.instance() == listener);

                if (!removed) {
                    return currentArray;
                }

                if (list.isEmpty()) {
                    return null;
                }

                return list.toArray(new EventSubscriber[0]);
            });
        }
        LOGGER.debug("Unregistered all listeners for: {}", listener.getClass().getSimpleName());
    }

    public @NonNull CompletableFuture<Void> post(@NonNull Object event) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        long sequence = this.ringBuffer.next();
        try {
            EventHolder holder = this.ringBuffer.get(sequence);
            holder.set(event, future);
        } finally {
            this.ringBuffer.publish(sequence);
        }

        return future;
    }

    public static @NonNull EventBus create(@NonNull ExecutorService executor) {
        return new EventBus(executor);
    }
}
