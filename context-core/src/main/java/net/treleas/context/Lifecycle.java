package net.treleas.context;

/**
 * Interface for managing the lifecycle of components (beans) within the DI container.
 *
 * <p>A bean can implement this interface to receive notifications when it is
 * integrated into the context or removed from it.</p>
 */
public interface Lifecycle {

    /**
     * Called immediately after the bean has been instantiated, injected,
     * and registered in the {@code BeanPool}.
     *
     * <p>Use this method to initialize resources, start tasks, or perform
     * any setup logic that requires dependencies to be fully injected.</p>
     */
    default void mount() {
    }


    /**
     * Called when the bean is being removed from the {@code BeanPool}
     * or when the owner (e.g., a plugin) is being unloaded.
     *
     * <p>Use this method to release resources, stop threads, or close
     * database connections to prevent memory leaks.</p>
     */
    default void unmount() {
    }
}
