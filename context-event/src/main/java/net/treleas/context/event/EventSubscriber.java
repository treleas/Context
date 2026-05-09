package net.treleas.context.event;

import java.lang.invoke.MethodHandle;

public record EventSubscriber(
        Object instance,
        MethodHandle methodHandle,
        int priority,
        boolean ignoreCancelled,
        byte type
) implements Comparable<EventSubscriber> {
    public static final byte SYNC = 0, ASYNC = 1, TASK = 2;

    @Override
    public int compareTo(EventSubscriber o) {
        int res = Integer.compare(o.priority, this.priority);
        return res != 0 ? res : Integer.compare(this.hashCode(), o.hashCode());
    }
}
