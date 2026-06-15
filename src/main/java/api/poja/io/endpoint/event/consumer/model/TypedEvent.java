package api.poja.io.endpoint.event.consumer.model;

import api.poja.io.PojaGenerated;
import api.poja.io.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
