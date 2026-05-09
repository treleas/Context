package net.treleas.context.event.engine;

import net.treleas.context.event.SubscriberDispatcher;
import net.treleas.context.event.SubscriberGroup;
import net.treleas.context.event.pool.EventPool;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DirectEngine implements EventEngine {

    private EventPool pool;
    private SubscriberDispatcher dispatcher;

    DirectEngine() {
    }

    @Override
    public void initialize(@NonNull EventPool pool, @NonNull ExecutorService executor, @NonNull SubscriberDispatcher dispatcher) {
        this.pool = pool;
        this.dispatcher = dispatcher;
    }


    @Override
    public @NonNull CompletableFuture<Void> post(@NonNull Object event) {
        SubscriberGroup group = pool.subscribers(event.getClass());
        if (group == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        dispatcher.dispatch(event, group.subscribers(), 0, future);

        return future;
    }
}