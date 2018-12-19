package ru.rubbergiref.context;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Context {
    private final Map<Class<?>, Object> SERVICES = MapUtil.synchronizedMap();
    private final Map<Class<?>, Function<Class<?>, ?>> PARENTS = MapUtil.synchronizedMap();

    /**
     * Get service provider
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T context(final @NotNull Class<T> service) {
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
     * Get or register context
     */
    public <T> @NotNull T contextIfAbsent(final @NotNull Class<T> service, final @NotNull Supplier<T> supplier) {
        final T provider = Context.context(service);
        if (provider != null) {
            return provider;
        }

        final T newProvider = supplier.get();
        Context.wrap(service, newProvider);
        return newProvider;
    }

    /**
     * Register context
     */
    public <T> void wrap(final @NotNull Class<T> service, final @NotNull T provider) {
        if (Context.wrapped(service)) {
            throw new IllegalStateException("Provider already registered");
        }

        Context.SERVICES.put(service, provider);
    }

    /**
     * Unregister context
     */
    public <T> void unwrap(final @NotNull Class<T> service) {
        if (Context.SERVICES.remove(service) == null) {
            throw new IllegalStateException("Provider not found");
        }
    }

    /**
     * All services
     */
    public @NotNull Set<Class<?>> services() {
        return Context.SERVICES.keySet();
    }

    /**
     * Is service registered
     */
    public boolean wrapped(final @NotNull Class<?> service) {
        return Context.SERVICES.containsKey(service);
    }

    /**
     * Register parent with get function
     */
    public <T> void warpParent(final @NotNull Class<T> serviceManager, final @NotNull Function<Class<?>, ?> parent) {
        Context.PARENTS.put(serviceManager, parent);
    }

    /**
     * Unregister parent
     */
    public <T> void unwrapParent(final @NotNull Class<T> serviceManager) {
        Context.PARENTS.remove(serviceManager);
    }

    /**
     * All parents
     */
    public @NotNull Set<Class<?>> parents() {
        return Context.PARENTS.keySet();
    }
}
