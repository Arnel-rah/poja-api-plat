package api.poja.io.model;

public record StackStatus(boolean isCancellable, boolean isUpdatable, CancelMethod cancelMethod) {}
