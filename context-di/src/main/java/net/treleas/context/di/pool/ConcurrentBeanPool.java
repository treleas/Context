package net.treleas.context.di.pool;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ConcurrentBeanPool implements BeanPool {
    private static final Function<Object, Set<BeanData<?>>> OWNING_SET = _ -> ConcurrentHashMap.newKeySet();

    private final Map<Class<?>, BeanData<?>> beans = new ConcurrentHashMap<>();
    private final Map<String, BeanData<?>> tags = new ConcurrentHashMap<>();
    private final Map<Object, Set<BeanData<?>>> owners = new ConcurrentHashMap<>();

    ConcurrentBeanPool() {
    }

    @Override
    public <T> @Nullable T classifiedBean(@NonNull Class<T> service) {
        //noinspection unchecked
        BeanData<T> data = (BeanData<T>) beans.get(service);
        if (data != null) {
            return data.provider;
        }

        return null;
    }

    @Override
    public <T> @Nullable T taggedBean(@NonNull String name) {
        //noinspection unchecked
        BeanData<T> data = (BeanData<T>) tags.get(name);
        if (data != null) {
            return data.provider;
        }

        return null;
    }

    @Override
    public <T> void appendBeam(@NonNull Object owner, @NonNull Class<T> service, @Nullable String tag, @NonNull T provider) {
        BeanData<T> data = new BeanData<>(service, tag, provider);

        if (tag == null || !beans.containsKey(service)) {
            beans.put(service, data);
        }

        if (tag != null && !tag.isEmpty()) {
            tags.put(tag, data);
        }

        owners.computeIfAbsent(owner, OWNING_SET)
                .add(data);
    }

    @Override
    public @Nullable Collection<?> removeOwned(@NonNull Object owner) {
        Set<BeanData<?>> ownedData = owners.remove(owner);
        if (ownedData == null) {
            return null;
        }

        List<Object> removedProviders = new ArrayList<>(ownedData.size());

        for (BeanData<?> data : ownedData) {
            beans.remove(data.service(), data);
            if (data.tag() != null) {
                tags.remove(data.tag(), data);
            }
            removedProviders.add(data.provider());
        }

        return removedProviders;
    }

    private record BeanData<T>(Class<?> service, @Nullable String tag, T provider) {
    }
}
