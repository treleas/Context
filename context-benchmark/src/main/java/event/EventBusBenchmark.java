package event;

import com.lmax.disruptor.YieldingWaitStrategy;
import net.treleas.context.engine.DisruptorEngine;
import net.treleas.context.event.EventBus;
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
        int threads = Runtime.getRuntime().availableProcessors();
        var strategy = new YieldingWaitStrategy();

        executor = Executors.newFixedThreadPool(threads);
        eventBus = EventBus.create(executor, FastutilEventPool.eventPool(), DisruptorEngine.engine(strategy, threads));

        virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        virtualEventBus = EventBus.create(virtualExecutor, FastutilEventPool.eventPool(), DisruptorEngine.engine(strategy, threads));

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

    /*
    @Benchmark
    @Threads(Threads.MAX)
    public void testContented(Blackhole bh) throws Exception {
        bh.consume(eventBus.post(event).get());
    }

    @Benchmark
    @Threads(Threads.MAX)
    public void testVirtual(Blackhole bh) throws Exception {
        bh.consume(virtualEventBus.post(event).get());
    }
     */

    @Benchmark
    @Threads(Threads.MAX)
    public void testContentedFireAndForget(Blackhole bh) {
        bh.consume(eventBus.post(event));
    }

    @Benchmark
    @Threads(Threads.MAX)
    public void testVirtualFireAndForget(Blackhole bh) {
        bh.consume(virtualEventBus.post(event));
    }
}
