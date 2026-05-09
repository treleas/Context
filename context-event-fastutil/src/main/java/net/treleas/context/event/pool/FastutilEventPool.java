package net.treleas.context.event.pool;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.treleas.context.event.EventSubscriber;
import net.treleas.context.event.SubscriberGroup;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FastutilEventPool implements EventPool {

    private volatile Reference2ObjectOpenHashMap<Class<?>, EventSubscriber[]> rawRegistry = new Reference2ObjectOpenHashMap<>();

    private volatile Reference2ObjectOpenHashMap<Class<?>, SubscriberGroup> flattenedCache = new Reference2ObjectOpenHashMap<>();

    private final Map<Class<?>, Class<?>[]> hierarchyCache = new ConcurrentHashMap<>();


    private FastutilEventPool() {
    }

    @Override
    public @Nullable SubscriberGroup subscribers(@NonNull Class<?> eventClass) {
        SubscriberGroup cached = flattenedCache.get(eventClass);
        if (cached != null) {
            return cached;
        }

        return computeFlattened(eventClass);
    }


    private synchronized SubscriberGroup computeFlattened(@NonNull Class<?> eventClass) {
        // Double-check locking
        SubscriberGroup cached = flattenedCache.get(eventClass);
        if (cached != null) {
            return cached; // cached before
        }

        Class<?>[] parents = hierarchyCache.computeIfAbsent(eventClass, this::scanHierarchy);
        List<EventSubscriber> combined = new ArrayList<>();

        Reference2ObjectOpenHashMap<Class<?>, EventSubscriber[]> currentRegistry = rawRegistry;

        for (Class<?> clazz : parents) {
            EventSubscriber[] subs = currentRegistry.get(clazz);
            if (subs != null) {
                for (EventSubscriber s : subs) {
                    combined.add(s);
                }
            }
        }

        if (combined.isEmpty()) {
            return null;
        }

        combined.sort(Comparator.naturalOrder());
        EventSubscriber[] resultArray = combined.toArray(new EventSubscriber[0]);

        // Pre-compile async flag
        boolean hasAsync = false;
        for (EventSubscriber s : resultArray) {
            if (s.type() != EventSubscriber.SYNC) {
                hasAsync = true;
                break;
            }
        }

        SubscriberGroup newGroup = new SubscriberGroup(resultArray, hasAsync);

        // Update via Copy-On-Write
        Reference2ObjectOpenHashMap<Class<?>, SubscriberGroup> newCache = new Reference2ObjectOpenHashMap<>(flattenedCache);
        newCache.put(eventClass, newGroup);
        this.flattenedCache = newCache;

        return newGroup;
    }

    @Override
    public synchronized void appendSubscriber(@NonNull Class<?> eventClass, @NonNull EventSubscriber subscriber) {
        Reference2ObjectOpenHashMap<Class<?>, EventSubscriber[]> newRegistry = new Reference2ObjectOpenHashMap<>(rawRegistry);

        EventSubscriber[] oldArray = newRegistry.get(eventClass);
        EventSubscriber[] newArray;
        if (oldArray == null) {
            newArray = new EventSubscriber[]{subscriber};
        } else {
            newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
            newArray[oldArray.length] = subscriber;
            Arrays.sort(newArray);
        }

        newRegistry.put(eventClass, newArray);
        this.rawRegistry = newRegistry;

        // invalided cache group
        this.flattenedCache = new Reference2ObjectOpenHashMap<>();
    }

    @Override
    public synchronized void remove(@NonNull Object listener) {
        Reference2ObjectOpenHashMap<Class<?>, EventSubscriber[]> newRegistry = new Reference2ObjectOpenHashMap<>(rawRegistry);
        boolean modified = false;

        var iterator = newRegistry.reference2ObjectEntrySet().fastIterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            EventSubscriber[] currentArray = entry.getValue();

            int countToRemove = 0;
            for (EventSubscriber sub : currentArray) {
                if (sub.instance() == listener) {
                    countToRemove++;
                }
            }

            if (countToRemove > 0) {
                modified = true;

                if (countToRemove == currentArray.length) {
                    iterator.remove();
                } else {
                    EventSubscriber[] newArray = new EventSubscriber[currentArray.length - countToRemove];
                    int index = 0;
                    for (EventSubscriber sub : currentArray) {
                        if (sub.instance() != listener) {
                            newArray[index++] = sub;
                        }
                    }
                    entry.setValue(newArray);
                }
            }
        }

        if (modified) {
            this.rawRegistry = newRegistry;
            this.flattenedCache = new Reference2ObjectOpenHashMap<>();
        }
    }

    private Class<?>[] scanHierarchy(Class<?> clazz) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        walk(clazz, classes);
        return classes.toArray(new Class<?>[0]);
    }

    private void walk(Class<?> clazz, Set<Class<?>> set) {
        if (clazz == null || clazz == Object.class) {
            return;
        }

        set.add(clazz);
        for (Class<?> iface : clazz.getInterfaces()) {
            walk(iface, set);
        }
        walk(clazz.getSuperclass(), set);
    }

    public static @NonNull EventPool eventPool() {
        return new FastutilEventPool();
    }
}
