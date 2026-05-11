package net.treleas.context.di;

import net.treleas.context.Lifecycle;
import net.treleas.context.di.injector.Injector;
import net.treleas.context.di.pool.BeanPool;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

/**
 * The core engine of the DI system.
 * Responsible for wiring dependencies, managing bean lifecycles, and coordinating with the pool.
 *
 * @param beanPool The storage for registered components.
 * @param injector The strategy used to perform field injection (e.g., VarHandle or Reflection).
 */
public record Di(@NonNull BeanPool beanPool, @NonNull Injector injector) {
    private static final Logger LOGGER = LoggerFactory.getLogger(Di.class);

    /**
     * Retrieves a bean registered by its class type.
     *
     * @param service the class of the service to look up
     * @param <T>     the type of the service
     * @return the bean instance, or {@code null} if not found
     */
    public <T> @Nullable T classifiedBean(@NonNull Class<T> service) {
        return beanPool.classifiedBean(service);
    }

    /**
     * Retrieves a bean registered by its class type, wrapped in an {@link Optional}.
     *
     * @param service the class of the service to look up
     * @param <T>     the type of the service
     * @return an Optional containing the bean, or empty if not found
     */
    public <T> @NonNull Optional<T> safeBean(@NonNull Class<T> service) {
        return Optional.ofNullable(classifiedBean(service));
    }

    /**
     * Retrieves a bean registered with a specific string tag.
     *
     * @param tag the identifier for the dependency
     * @param <T> the expected type of the service
     * @return the bean instance, or {@code null} if not found
     */
    public <T> @Nullable T taggedBean(@NonNull String tag) {
        return beanPool.taggedBean(tag);
    }

    /**
     * Retrieves a bean registered with a specific string tag, wrapped in an {@link Optional}.
     *
     * @param tag the identifier for the dependency
     * @param <T> the expected type of the service
     * @return an Optional containing the bean, or empty if not found
     */
    public <T> @NonNull Optional<T> safeBean(@NonNull String tag) {
        return Optional.ofNullable(taggedBean(tag));
    }

    /**
     * Retrieves a bean using a dual-lookup strategy.
     *
     * <p>It first attempts to find a bean associated with the provided {@code tag}.
     * If no such bean exists, it falls back to looking up the bean by its
     * {@code service} class type.</p>
     *
     * @param service the class type of the service to look up
     * @param tag     the unique string identifier (tag) for the dependency
     * @param <T>     the type of the service
     * @return the found bean instance, or {@code null} if no match is found by either tag or class
     */
    public <T> @Nullable T bean(@NonNull Class<T> service, @NonNull String tag) {
        T taggedBean = taggedBean(tag);
        if (taggedBean != null) {
            return taggedBean;
        }

        return classifiedBean(service);
    }

    public <T> @NonNull Optional<T> safeBean(@NonNull Class<T> service, @NonNull String tag) {
        T taggedBean = taggedBean(tag);
        if (taggedBean != null) {
            return Optional.of(taggedBean);
        }

        return safeBean(service);
    }

    /**
     * Injects dependencies into the bean, triggers {@link Lifecycle#mount()},
     * and registers it in the pool under the specified owner.
     *
     * @param owner   the object that owns this bean (e.g., a Plugin instance).
     * @param service the class type to register the bean as.
     * @param tag     an optional identifier for named dependencies.
     * @param bean    the instance to be processed and stored.
     * @return the current {@link Di} instance for method chaining
     */
    public <T> @NonNull Di appendBean(@NonNull Object owner, @NonNull Class<T> service, @Nullable String tag, @NonNull T bean) {
        injector.inject(this, bean);

        if (bean instanceof Lifecycle lifecycle) {
            try {
                lifecycle.mount();
            } catch (Throwable t) {
                LOGGER.error("Error during Lifecycle.mount() for {}", bean.getClass().getName(), t);
            }
        }

        beanPool.appendBean(owner, service, tag, bean);
        return this;
    }

    /**
     * Registers a bean into the system using its class type as the primary key.
     * <p>
     * This is a convenience method that calls {@link #appendBean(Object, Class, String, Object)}
     * with a {@code null} tag.
     * </p>
     *
     * @param owner   the object that owns this bean (e.g., a Plugin instance).
     * @param service the class type to register the bean as.
     * @param bean    the instance to be processed and stored.
     * @return the current {@link Di} instance for method chaining
     */
    public <T> @NonNull Di appendBean(@NonNull Object owner, @NonNull Class<T> service, @NonNull T bean) {
        return appendBean(owner, service, null, bean);
    }

    /**
     * Unregisters all beans associated with the given owner and triggers
     * {@link Lifecycle#unmount()} for each.
     *
     * @param owned The owner whose beans should be removed.
     * @return collection of removed bean instances, or {@code null} if none were found.
     */
    public @Nullable Collection<?> removeOwned(@NonNull Object owned) {
        Collection<?> beans = beanPool.removeOwned(owned);
        if (beans == null) {
            return null;
        }

        for (Object bean : beans) {
            if (bean instanceof Lifecycle lifecycle) {
                try {
                    lifecycle.unmount();
                } catch (Throwable t) {
                    LOGGER.error("Error during Lifecycle.unmount() for {}", bean.getClass().getName(), t);
                }
            }
        }

        return beans;
    }

    public static <T> @NullUnmarked T mark() {
        return null;
    }

    public static @NonNull Di create(@NonNull BeanPool pool, @NonNull Injector injector) {
        return new Di(pool, injector);
    }

    public static @NonNull Di standard() {
        return new Di(BeanPool.concurrent(), Injector.varHandle());
    }

    public static @NonNull Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private BeanPool beanPool;
        private Injector injector;

        private Builder() {
        }

        public @NonNull Builder beanPool(@NonNull BeanPool beanPool) {
            this.beanPool = beanPool;
            return this;
        }

        public @NonNull Builder injector(@NonNull Injector injector) {
            this.injector = injector;
            return this;
        }

        public @NonNull Di build() {
            if (beanPool == null) {
                throw new IllegalStateException("beanPool has not been set");
            }

            if (injector == null) {
                throw new IllegalStateException("injector has not been set");
            }

            return new Di(beanPool, injector);
        }
    }
}
