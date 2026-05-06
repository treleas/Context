package net.treleas.context.di.injector;

import net.treleas.context.di.Di;
import org.jspecify.annotations.NonNull;

/**
 * Strategy interface for performing dependency injection into bean fields.
 *
 * <p>Implementations define how to discover fields annotated with {@code @Inject}
 * and how to assign values to them from the provided {@link Di} context.</p>
 */
public interface Injector {

    /**
     * Creates an injector that uses standard Java Reflection API.
     * <p>This implementation is simple but may be slower due to runtime
     * metadata lookups and access checks.</p>
     *
     * @return a new reflection-based injector instance
     */
    static @NonNull Injector reflection() {
        return new ReflectionInjector();
    }

    /**
     * Creates a high-performance injector using {@link java.lang.invoke.VarHandle}.
     * <p>This implementation caches field accessors and provides performance
     * close to direct field access after a short "warm-up" period.</p>
     *
     * @return a new VarHandle-based injector instance
     */
    static @NonNull Injector varHandle() {
        return new VarHandleInjector();
    }

    /**
     * Injects dependencies into the specified bean instance.
     *
     * @param di   the dependency context used to look up required beans
     * @param bean the target object where dependencies should be injected
     */
    void inject(@NonNull Di di, @NonNull Object bean);
}
