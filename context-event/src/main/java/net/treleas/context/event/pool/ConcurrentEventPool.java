package net.treleas.context.event.pool;

import net.treleas.context.event.EventSubscriber;
import net.treleas.context.event.SubscriberGroup;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class ConcurrentEventPool implements EventPool {

    private volatile Map<Class<?>, SubscriberGroup> registry = new HashMap<>();

    @Override
    public @Nullable SubscriberGroup subscribers(@NonNull Class<?> eventClass) {
        // Абсолютно wait-free чтение. HashMap.get() без конкуренции — это O(1)
        return registry.get(eventClass);
    }

    @Override
    public synchronized void appendSubscriber(@NonNull Class<?> eventClass, @NonNull EventSubscriber subscriber) {
        Map<Class<?>, SubscriberGroup> newRegistry = new HashMap<>(registry);

        SubscriberGroup group = newRegistry.get(eventClass);
        EventSubscriber[] newArray;

        if (group == null) {
            newArray = new EventSubscriber[]{subscriber};
        } else {
            EventSubscriber[] oldArray = group.subscribers();
            newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
            newArray[oldArray.length] = subscriber;
            Arrays.sort(newArray);
        }

        newRegistry.put(eventClass, new SubscriberGroup(newArray, checkHasAsync(newArray)));
        this.registry = newRegistry;
    }

    @Override
    public synchronized void remove(@NonNull Object listener) {
        Map<Class<?>, SubscriberGroup> newRegistry = new HashMap<>(registry);
        boolean modified = false;

        var it = newRegistry.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            EventSubscriber[] array = entry.getValue().subscribers();

            int count = 0;
            for (EventSubscriber s : array) {
                if (s.instance() == listener) count++;
            }

            if (count > 0) {
                modified = true;
                if (array.length == count) {
                    it.remove();
                } else {
                    EventSubscriber[] filtered = new EventSubscriber[array.length - count];
                    int idx = 0;
                    for (EventSubscriber s : array) {
                        if (s.instance() != listener) filtered[idx++] = s;
                    }
                    entry.setValue(new SubscriberGroup(filtered, checkHasAsync(filtered)));
                }
            }
        }

        if (modified) {
            this.registry = newRegistry;
        }
    }

    private boolean checkHasAsync(EventSubscriber[] subs) {
        for (EventSubscriber s : subs) {
            if (s.type() != EventSubscriber.SYNC) return true;
        }
        return false;
    }
}
