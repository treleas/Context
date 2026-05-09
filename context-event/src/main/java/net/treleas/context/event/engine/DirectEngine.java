package net.treleas.context.event.engine;

import net.treleas.context.event.EventSubscriber;
import net.treleas.context.event.SubscriberDispatcher;
import net.treleas.context.event.SubscriberGroup;
import net.treleas.context.event.pool.EventPool;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class DirectEngine implements EventEngine {

    private EventPool pool;
    private SubscriberDispatcher dispatcher;

    DirectEngine() {
    }

    @Override
    public void initialize(@NonNull EventPool pool, @NonNull SubscriberDispatcher dispatcher) {
        this.pool = pool;
        this.dispatcher = dispatcher;
    }

    @Override
    public @NonNull CompletableFuture<Object> post(@NonNull Object event) {
        SubscriberGroup group = pool.subscribers(event.getClass());
        if (group == null) {
            return CompletableFuture.completedFuture(event);
        }

        EventSubscriber[] subscribers = group.subscribers();
        CompletableFuture<Object> future = new CompletableFuture<>();
        dispatcher.dispatch(event, subscribers, 0, subscribers.length, future);

        return future;
    }
}
