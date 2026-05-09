package net.treleas.context.event;

public record SubscriberGroup(
        EventSubscriber[] subscribers,
        boolean hasAsyncOrTask
) {}
