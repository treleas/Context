package net.treleas.context.event;

import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

@FunctionalInterface
public interface EventTask {
    void execute(@NonNull Continuation continuation);

    static @NonNull EventTask async(@NonNull Consumer<Continuation> task) {
        return task::accept;
    }
}
