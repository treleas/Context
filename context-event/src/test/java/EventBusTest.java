import net.treleas.context.event.Continuation;
import net.treleas.context.event.EventBus;
import net.treleas.context.event.Subscribe;
import net.treleas.context.event.engine.EventEngine;
import net.treleas.context.event.pool.EventPool;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        ExecutorService syncExecutor = new AbstractExecutorService() {
            private boolean isShutdown = false;

            @Override
            public void execute(Runnable command) {
                command.run();
            }

            @Override
            public void shutdown() {
                isShutdown = true;
            }

            @Override
            public @NonNull List<Runnable> shutdownNow() {
                isShutdown = true;
                return List.of();
            }

            @Override
            public boolean isShutdown() {
                return isShutdown;
            }

            @Override
            public boolean isTerminated() {
                return isShutdown;
            }

            @Override
            public boolean awaitTermination(long t, @NonNull TimeUnit u) {
                return true;
            }
        };
        eventBus = EventBus.create(syncExecutor, EventPool.concurrent(), EventEngine.direct());
    }

    @Test
    void shouldDeliverSimpleEvent() throws Exception {
        TestListener listener = new TestListener();
        eventBus.register(listener);

        eventBus.post(new TestEvent("Hello")).get(5, TimeUnit.SECONDS);

        assertThat(listener.callCount.get()).isEqualTo(1);
    }

    @Test
    void shouldRespectCancellation() throws Exception {
        CancellableEvent event = new CancellableEvent("Cancel me");
        event.setCancelled(true);

        AtomicInteger callCount = new AtomicInteger(0);
        Object listener = new Object() {
            @Subscribe(ignoreCancelled = false)
            public void onEvent(CancellableEvent e) {
                callCount.incrementAndGet();
            }
        };

        eventBus.register(listener);
        eventBus.post(event).get(5, TimeUnit.SECONDS);

        assertThat(callCount.get()).isEqualTo(0);
    }

    @Test
    void shouldHandleAsyncContinuation() throws Exception {
        AtomicInteger step = new AtomicInteger(0);

        Object listener = new Object() {
            @Subscribe
            public void onEvent(TestEvent e, Continuation continuation) {
                step.incrementAndGet();
                continuation.resume();
            }
        };

        eventBus.register(listener);
        eventBus.post(new TestEvent("Async")).get(5, TimeUnit.SECONDS);

        assertThat(step.get()).isEqualTo(1);
    }

    @Test
    void shouldUnregisterListener() throws Exception {
        TestListener listener = new TestListener();
        eventBus.register(listener);
        eventBus.unregister(listener);

        eventBus.post(new TestEvent("Ghost")).get(5, TimeUnit.SECONDS);

        assertThat(listener.callCount.get()).isEqualTo(0);
    }

    @Test
    void shouldFailFutureIfSubscriberThrows() {
        Object evilListener = new Object() {
            @Subscribe
            public void event(TestEvent event) {
                throw new RuntimeException("Boom");
            }
        };

        eventBus.register(evilListener);
        CompletableFuture<Void> future = eventBus.post(new TestEvent("Crash"));

        assertThat(future).failsWithin(5, TimeUnit.SECONDS)
                .withThrowableThat()
                .havingCause()
                .isInstanceOf(RuntimeException.class)
                .withMessage("Boom");
    }
}