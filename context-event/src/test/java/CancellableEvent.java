import net.treleas.context.event.Cancellable;

public record CancellableEvent(String message) implements Cancellable {
    private static boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean v) {
        cancelled = v;
    }
}