package event;

import net.treleas.context.event.EventBus;
import net.treleas.context.event.engine.EventEngine;
import net.treleas.context.event.pool.FastutilEventPool;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 5)
@Fork(value = 1, jvmArgs = {"-Xms2g", "-Xmx2g"})
public class EventBusBenchmark {

    @Param({"1", "10", "50", "250"})
    private int listenersCount;

    private EventBus eventBus;
    private EventBus virtualEventBus;
    private ExecutorService executor;
    private ExecutorService virtualExecutor;
    private TestEvent event;

    @Setup
    public void setup() {
        executor = Executors.newFixedThreadPool(Math.min(1, Runtime.getRuntime().availableProcessors() - 2));
        eventBus = EventBus.create(executor, FastutilEventPool.eventPool(), EventEngine.direct());

        virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        virtualEventBus = EventBus.create(virtualExecutor, FastutilEventPool.eventPool(), EventEngine.direct());

        event = new TestEvent();

        for (int i = 0; i < listenersCount; i++) {
            TestListener listener = new TestListener();
            eventBus.register(listener);
            virtualEventBus.register(listener);
        }
    }

    @TearDown
    public void tearDown() {
        eventBus.unmount();
        virtualEventBus.unmount();
        executor.shutdownNow();
        virtualExecutor.shutdownNow();
    }

    @Benchmark
    @Threads(Threads.MAX)
    public void testContentedFireAndForget(Blackhole bh) {
        eventBus.fireAndForget(event);
    }

    @Benchmark
    @Threads(Threads.MAX)
    public void testVirtualFireAndForget(Blackhole bh) {
        virtualEventBus.fireAndForget(event);
    }
}
