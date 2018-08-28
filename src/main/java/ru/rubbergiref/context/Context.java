package ru.rubbergiref.context;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@SuppressWarnings("unused")
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Context {
    private final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();
    private final Map<Class<?>, Function<Class<?>, ?>> PARENTS = new ConcurrentHashMap<>();

    /**
     * Get service provider
     */
    @SuppressWarnings("unchecked")
    public <T> T context(final Class<T> service) {
        final T provider = (T) Context.SERVICES.get(service);
        if (provider == null) {
            for (final Function<Class<?>, ?> parent : Context.PARENTS.values()) {
                final T parentProvider = (T) parent.apply(service);
                if (parentProvider != null) {
                    return parentProvider;
                }
            }
        }

        return provider;
    }

    /**
     * Register context
     */
    public <T> void wrap(final Class<T> service, final T provider) {
        if (Context.wrapped(service)) {
            throw new IllegalStateException("Provider already registered");
        }

        Context.SERVICES.put(service, provider);
    }

    /**
     * Unregister context
     */
    public <T> void unwrap(final Class<T> service) {
        if (Context.SERVICES.remove(service) == null) {
            throw new IllegalStateException("Provider not found");
        }
    }

    /**
     * All services
     */
    public Set<Class<?>> services() {
        return Context.SERVICES.keySet();
    }

    /**
     * Is service registered
     */
    public boolean wrapped(final Class<?> service) {
        return Context.SERVICES.containsKey(service);
    }

    /**
     * Register parent with get function
     */
    public <T> void warpParent(final Class<T> serviceManager, final Function<Class<?>, ?> parent) {
        Context.PARENTS.put(serviceManager, parent);
    }

    /**
     * Unregister parent
     */
    public <T> void unwrapParent(final Class<T> serviceManager) {
        Context.PARENTS.remove(serviceManager);
    }

    /**
     * All parents
     */
    public Set<Class<?>> parents() {
        return Context.PARENTS.keySet();
    }
}
