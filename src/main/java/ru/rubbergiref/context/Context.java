package ru.rubbergiref.context;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Context {
    private final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<Class<?>, Object>();

    /**
     * Get service provider
     */
    @SuppressWarnings("unchecked")
    public <T> T context(final Class<T> service) {
        return (T) Context.SERVICES.get(service);
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
}
