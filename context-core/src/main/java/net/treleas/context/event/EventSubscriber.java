package net.treleas.context.event;

import java.lang.invoke.MethodHandle;

record EventSubscriber(
        Object instance,
        MethodHandle methodHandle,
        int priority,
        boolean ignoreCancelled
) implements Comparable<EventSubscriber> {
    @Override
    public int compareTo(EventSubscriber o) {
        return Integer.compare(o.priority, this.priority);
    }
}
