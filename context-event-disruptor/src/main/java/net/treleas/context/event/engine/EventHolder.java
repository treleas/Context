package net.treleas.context.event.engine;

import net.treleas.context.event.EventSubscriber;
import org.jspecify.annotations.NullUnmarked;

import java.util.concurrent.CompletableFuture;

public class EventHolder {

    private Object event;
    private EventSubscriber[] subscribers;
    private CompletableFuture<Object> future;

    public void set(Object event, EventSubscriber[] subscribers, CompletableFuture<Object> future) {
        this.event = event;
        this.subscribers = subscribers;
        this.future = future;
    }

    public void clear() {
        this.event = null;
        this.subscribers = null;
        this.future = null;
    }

    public @NullUnmarked Object event() {
        return event;
    }

    public @NullUnmarked EventSubscriber[] subscribers() {
        return subscribers;
    }

    public @NullUnmarked CompletableFuture<Object> future() {
        return future;
    }
}
