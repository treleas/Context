package net.treleas.context.event;

public class EventPriority {
    /**
     * The event is processed last.
     * Useful for final logging or actions that depend on the final state of the event.
     */
    public static final int LOWEST = -2000;

    /**
     * The event is processed after the normal listeners.
     */
    public static final int LOW = -1000;

    /**
     * The default priority.
     */
    public static final int NORMAL = 0;

    /**
     * The event is processed before the normal listeners.
     */
    public static final int HIGH = 1000;

    /**
     * The event is processed first.
     * Ideal for intercepting or cancelling events early.
     */
    public static final int HIGHEST = 2000;

    /**
     * The event is processed after all other priorities.
     * Intended strictly for monitoring the outcome of an event.
     * Listeners at this priority should not modify or cancel the event.
     */
    public static final int MONITOR = Integer.MIN_VALUE;
}
