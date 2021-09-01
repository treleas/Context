package net.treleas.context;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "unused"})
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Context {
    public final Object ANNOTATE_OBJECT = new Object();

    private final Map<Class<?>, Object> SERVICES = MapUtil.synchronizedMap();
    private final Map<Class<?>, Function<Class<?>, ?>> PARENTS = MapUtil.synchronizedMap();

    /**
     * Get service provider
     */
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
     * Get service provider with throw
     */
    public <T> @NotNull T unsafeContext(final @NotNull Class<T> service) {
        final T provider = context(service);
        if (provider == null) {
            throw new IllegalStateException("Provider not found");
        }

        return provider;
    }

    /**
     * Get service provider optional
     */
    public <T> @NotNull Optional<T> safeContext(final @NotNull Class<T> service) {
        return Optional.ofNullable(context(service));
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

    /**
     * Inject contexts into all annotated filed
     */
    @SuppressWarnings("UnusedReturnValue")
    public <T> @NotNull T injectAnnotations(final @NotNull T t) {
        final Class<?> clazz = t.getClass();

        for (final Field field : clazz.getDeclaredFields()) {
            for (final Annotation annotation : field.getAnnotations()) {
                if (!annotation.annotationType().equals(ContextInjectable.class)) {
                    continue;
                }

                try {
                    if (field.get(t) != Context.ANNOTATE_OBJECT) {
                        continue;
                    }

                    final Object provider = context(field.getType());
                    if (provider == null) {
                        continue;
                    }

                    field.setAccessible(true);
                    field.set(t, provider);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException("Error inject provider", e);
                }
            }
        }

        return t;
    }

    public <T> void train(final @NotNull T t) {
        final Class<?> clazz = t.getClass();
        final ClassLoader classLoader = clazz.getClassLoader();

        try {
            classLoader.loadClass(clazz.getName());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Error to train class", e);
        }
    }
}
