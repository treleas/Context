package net.treleas.context.engine;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import net.treleas.context.event.EventSubscriber;
import net.treleas.context.event.SubscriberDispatcher;
import net.treleas.context.event.SubscriberGroup;
import net.treleas.context.event.engine.EventEngine;
import net.treleas.context.event.pool.EventPool;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class DisruptorEngine implements EventEngine {

    private final WaitStrategy strategy;
    private final int workers;

    private EventPool pool;
    private Disruptor<EventHolder> disruptor;
    private RingBuffer<EventHolder> ringBuffer;

    DisruptorEngine(WaitStrategy strategy, int workers) {
        this.strategy = strategy;
        this.workers = workers;
    }

    @Override
    public void initialize(@NonNull EventPool pool, @NonNull ExecutorService executor, @NonNull SubscriberDispatcher dispatcher) {
        this.pool = pool;

        this.disruptor = new Disruptor<>(
                EventHolder::new,
                64 * 1024,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI,
                strategy
        );

        EventWorker[] workersArray = new EventWorker[workers];
        for (int i = 0; i < workers; i++) {
            workersArray[i] = new EventWorker(i, workers, dispatcher, executor);
        }

        this.disruptor.handleEventsWith(workersArray);
        this.ringBuffer = disruptor.getRingBuffer();
        this.disruptor.start();
    }

    @Override
    public @NonNull CompletableFuture<Void> post(@NonNull Object event) {
        SubscriberGroup group = pool.subscribers(event.getClass());
        if (group == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        ringBuffer.publishEvent((holder, _, e, s, f) -> {
            holder.set(e, s, f);
        }, event, group.subscribers(), future);

        return future;
    }

    @Override
    public void unmount() {
        if (disruptor != null) {
            disruptor.shutdown();
        }
    }

    private record EventWorker(int workerId, int totalWorkers, SubscriberDispatcher dispatcher,
                               ExecutorService executor) implements EventHandler<EventHolder> {

        private static final int CHUNK_SIZE = 100;

        @Override
            public void onEvent(EventHolder holder, long sequence, boolean endOfBatch) {
                if (sequence % totalWorkers == workerId) {
                    final Object event = holder.event();
                    final EventSubscriber[] subs = holder.subscribers();
                    final CompletableFuture<Void> future = holder.future();

                    if (event != null && subs != null) {
                        if (subs.length <= CHUNK_SIZE) {
                            dispatcher.dispatch(event, subs, 0, future);
                        } else {
                            dispatchParallel(event, subs, future);
                        }
                    } else if (future != null) {
                        future.complete(null);
                    }
                    holder.clear();
                }
            }

        private void dispatchParallel(Object event, EventSubscriber[] subs, CompletableFuture<Void> future) {
            int total = subs.length;
            int taskCount = (total + CHUNK_SIZE - 1) / CHUNK_SIZE;
            var remaining = new AtomicInteger(taskCount);

            for (int i = 0; i < total; i += CHUNK_SIZE) {
                final int start = i;
                final int end = Math.min(start + CHUNK_SIZE, total);

                executor.execute(() -> {
                    try {
                        EventSubscriber[] chunk = Arrays.copyOfRange(subs, start, end);

                        dispatcher.dispatch(event, chunk, 0, new CompletableFuture<>() {
                            @Override
                            public boolean complete(Void value) {
                                if (remaining.decrementAndGet() == 0) {
                                    future.complete(null);
                                }
                                return true;
                            }

                            @Override
                            public boolean completeExceptionally(Throwable ex) {
                                future.completeExceptionally(ex);
                                remaining.decrementAndGet();
                                return true;
                            }
                        });
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                        remaining.decrementAndGet();
                    }
                });
            }
        }
    }

    public static @NonNull EventEngine engine(@NonNull WaitStrategy strategy, int workers) {
        return new DisruptorEngine(strategy, workers);
    }
}
