package net.treleas.context.event;

import java.util.concurrent.CompletableFuture;

final class SharedContinuation implements Continuation {

    private final EventBus bus;
    final Object event;
    final EventSubscriber[] subs;
    final CompletableFuture<Void> future;
    final boolean isCancellable;
    private int index;

    SharedContinuation(EventBus bus, Object event, EventSubscriber[] subs, CompletableFuture<Void> future) {
        this.bus = bus;
        this.event = event;
        this.subs = subs;
        this.future = future;
        this.index = 0;
        this.isCancellable = event instanceof Cancellable;
    }

    @Override
    public void resume() {
        bus.runLoop(this);
    }

    @Override
    public void fail(Throwable t) {
        future.completeExceptionally(t);
    }

    // Внутренние геттеры для быстрого доступа
    void next() {
        index++;
    }

    int index() {
        return index;
    }
}
