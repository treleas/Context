package net.treleas.context.event.engine;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import net.treleas.context.event.EventSubscriber;
import net.treleas.context.event.SubscriberDispatcher;
import net.treleas.context.event.SubscriberGroup;
import net.treleas.context.event.pool.EventPool;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

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
    public void initialize(@NonNull EventPool pool, @NonNull SubscriberDispatcher dispatcher) {
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
            workersArray[i] = new EventWorker(i, workers, dispatcher);
        }

        this.disruptor.handleEventsWith(workersArray);
        this.ringBuffer = disruptor.getRingBuffer();
        this.disruptor.start();
    }

    @Override
    public @NonNull CompletableFuture<Object> post(@NonNull Object event) {
        SubscriberGroup group = pool.subscribers(event.getClass());
        if (group == null) {
            return CompletableFuture.completedFuture(event);
        }

        CompletableFuture<Object> future = new CompletableFuture<>();
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

    private record EventWorker(int workerId, int totalWorkers, SubscriberDispatcher dispatcher) implements EventHandler<EventHolder> {

        private static final int CHUNK_SIZE = 1000;

        @Override
        public void onEvent(EventHolder holder, long sequence, boolean endOfBatch) {
            if ((sequence % totalWorkers) != workerId) {
                return;
            }

            final Object event = holder.event();
            final EventSubscriber[] subs = holder.subscribers();

            if (event != null && subs != null) {
                dispatcher.dispatch(event, subs, 0, subs.length, holder.future());
            }

            holder.clear();
        }
    }

    public static @NonNull EventEngine engine(@NonNull WaitStrategy strategy, int workers) {
        return new DisruptorEngine(strategy, workers);
    }
}
