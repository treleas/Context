package net.treleas.context.di.pool;

import it.unimi.dsi.fastutil.objects.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FastutilBeanPool implements BeanPool {
    private final Reference2ObjectOpenHashMap<Class<?>, BeanData<?>> beans =
            new Reference2ObjectOpenHashMap<>(1024, 0.5f);
    private final Object2ObjectOpenHashMap<String, BeanData<?>> tags =
            new Object2ObjectOpenHashMap<>(1024, 0.5f);
    private final Object2ObjectOpenHashMap<Object, ObjectSet<BeanData<?>>> owners =
            new Object2ObjectOpenHashMap<>(1024, 0.5f);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private FastutilBeanPool() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T classifiedBean(@NonNull Class<T> service) {
        readLock.lock();
        try {
            final BeanData<?> data = beans.get(service);
            return data != null ? (T) data.provider : null;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T taggedBean(@NonNull String tag) {
        readLock.lock();
        try {
            final BeanData<?> data = tags.get(tag);
            return data != null ? (T) data.provider : null;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public <T> void appendBean(@NonNull Object owner, @NonNull Class<T> service, @Nullable String tag, @NonNull T provider) {
        final BeanData<T> data = new BeanData<>(service, tag, provider);

        writeLock.lock();
        try {
            if (tag == null || !beans.containsKey(service)) {
                beans.put(service, data);
            }

            if (tag != null && !tag.isEmpty()) {
                tags.put(tag, data);
            }

            owners.computeIfAbsent(owner, k -> new ObjectOpenHashSet<>(4))
                    .add(data);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public @Nullable Collection<?> removeOwned(@NonNull Object owner) {
        writeLock.lock();
        try {
            final ObjectSet<BeanData<?>> ownedData = owners.remove(owner);
            if (ownedData == null) {
                return null;
            }

            final ObjectArrayList<Object> result = new ObjectArrayList<>(ownedData.size());
            for (BeanData<?> d : ownedData) {
                beans.remove(d.service);
                if (d.tag != null) {
                    tags.remove(d.tag);
                }
                result.add(d.provider);
            }
            return result;
        } finally {
            writeLock.unlock();
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static final class BeanData<T> {
        public final Class<?> service;
        public final String tag;
        public final T provider;

        BeanData(Class<?> service, String tag, T provider) {
            this.service = service;
            this.tag = tag;
            this.provider = provider;
        }
    }

    public static @NonNull BeanPool beanPool() {
        return new FastutilBeanPool();
    }
}
