package net.treleas.context.engine;

import net.treleas.context.event.EventSubscriber;
import org.jspecify.annotations.NullUnmarked;

import java.util.concurrent.CompletableFuture;

public class EventHolder {

    private Object event;
    private EventSubscriber[] subscribers;
    private CompletableFuture<Void> future;

    public void set(Object event, EventSubscriber[] subscribers, CompletableFuture<Void> future) {
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

    public @NullUnmarked CompletableFuture<Void> future() {
        return future;
    }
}
