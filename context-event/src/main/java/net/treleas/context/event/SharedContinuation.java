package net.treleas.context.event;

import java.util.concurrent.CompletableFuture;

final class SharedContinuation implements Continuation {

    private final EventBus bus;
    final Object event;
    final EventSubscriber[] subs;
    final CompletableFuture<Object> future;
    final boolean isCancellable;
    final int limit;
    private int index;

    SharedContinuation(EventBus bus, Object event, EventSubscriber[] subs, int startIndex, int limit, CompletableFuture<Object> future) {
        this.bus = bus;
        this.event = event;
        this.subs = subs;
        this.future = future;
        this.index = startIndex;
        this.limit = limit;
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
