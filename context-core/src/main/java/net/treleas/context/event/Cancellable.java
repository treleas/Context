package net.treleas.context.event;

public interface Cancellable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}
