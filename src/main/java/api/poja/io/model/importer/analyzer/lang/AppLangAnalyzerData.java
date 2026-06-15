package api.poja.io.model.importer.analyzer.lang;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.nio.file.Path;

public record AppLangAnalyzerData(
    @JsonSerialize(using = ToStringSerializer.class) Path mainClassPath) {}
