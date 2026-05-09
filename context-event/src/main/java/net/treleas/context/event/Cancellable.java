package net.treleas.context.event;

/**
 * Represents an event that can be cancelled by a subscriber.
 * <p>
 * If an event is cancelled, subsequent subscribers will not receive it
 * unless they have specified {@code ignoreCancelled = true} in their
 * {@code @Subscribe} annotation.
 */
public interface Cancellable {

    /**
     * Checks whether the event has been cancelled.
     *
     * @return {@code true} if the event is cancelled, {@code false} otherwise.
     */
    boolean isCancelled();

    /**
     * Sets the cancellation state of the event.
     *
     * @param cancelled {@code true} to cancel the event, {@code false} to resume.
     */
    void setCancelled(boolean cancelled);
}
