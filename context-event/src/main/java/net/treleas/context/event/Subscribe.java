package net.treleas.context.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event subscriber.
 * <p>
 * The annotated method must be public and have either:
 * <ul>
 *     <li>One parameter: the event object.</li>
 *     <li>Two parameters: the event object and a {@code Continuation} for async flow.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /**
     * Defines the order in which this subscriber receives events relative to others.
     * <p>
     * Subscribers with a higher priority value are executed first.
     * If priorities are equal, the order depends on the registration sequence.
     *
     * @return the execution priority.
     */
    int priority() default 0;

    /**
     * Determines if this subscriber should receive the event even if it has been
     * marked as cancelled by a previous subscriber.
     * <p>
     * Default is {@code false}, meaning cancelled events are skipped.
     *
     * @return {@code true} to receive cancelled events, {@code false} otherwise.
     */
    boolean ignoreCancelled() default false;
}
