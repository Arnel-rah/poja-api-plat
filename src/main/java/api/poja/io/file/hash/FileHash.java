package api.poja.io.file.hash;

import api.poja.io.PojaGenerated;

@PojaGenerated
public record FileHash(FileHashAlgorithm algorithm, String value) {}
