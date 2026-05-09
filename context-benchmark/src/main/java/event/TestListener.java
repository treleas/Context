package event;

import net.treleas.context.event.Subscribe;
import org.openjdk.jmh.infra.Blackhole;

public class TestListener {
    @Subscribe
    public void onEvent(TestEvent event) {
        //Blackhole.consumeCPU(10000);
    }
}
