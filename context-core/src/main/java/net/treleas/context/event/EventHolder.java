package net.treleas.context.event;

import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class EventHolder {
    private Object event;
    private CompletableFuture<Void> future;

    public void set(@Nullable Object event, @Nullable CompletableFuture<Void> future) {
        this.event = event;
        this.future = future;
    }

    public void clear() {
        this.event = null;
        this.future = null;
    }

    public @NullUnmarked Object event() {
        return this.event;
    }

    public @NullUnmarked CompletableFuture<Void> future() {
        return this.future;
    }
}
