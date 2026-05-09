import net.treleas.context.event.Subscribe;

import java.util.concurrent.atomic.AtomicInteger;

public class TestListener {
    public final AtomicInteger callCount = new AtomicInteger(0);

    @Subscribe
    public void event(TestEvent event) {
        callCount.incrementAndGet();
    }
}