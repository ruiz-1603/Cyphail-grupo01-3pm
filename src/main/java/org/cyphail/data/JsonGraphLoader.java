package org.cyphail.data;

import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Carga datos de grafos desde archivos JSON en disco (carpeta ./data,
 * relativa al directorio desde donde se ejecuta `cyphail`). Se lee de
 * disco real (no del classpath/jar) para que los cambios en el JSON se
 * reflejen sin recompilar ni reempaquetar, tal como pide el SPEC.
 */
public final class JsonGraphLoader {

    private static final Path DATA_DIR = Path.of("data");

    private JsonGraphLoader() {}

    public static GraphData loadGraph(String graphName) throws IOException {
        Path file = DATA_DIR.resolve(graphName + ".json");

        if (!Files.exists(file)) {
            throw new IOException("Graph file not found: " + file.toAbsolutePath());
        }

        String jsonContent = Files.readString(file, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();

        String description = root.has("description")
                ? root.get("description").getAsString()
                : graphName;

        String[][] nodes = parseNodes(root);
        String[][] relationships = parseRelationships(root);

        return new GraphData(description, nodes, relationships);
    }

    public static List<String> listAvailableGraphs() throws IOException {
        if (!Files.exists(DATA_DIR)) return List.of();
        try (var stream = Files.list(DATA_DIR)) {
            return stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .sorted()
                    .toList();
        }
    }

    private static String[][] parseNodes(JsonObject root) {
        JsonArray nodesArray = root.getAsJsonArray("nodes");
        if (nodesArray == null) return new String[0][0];

        List<String> headers = new ArrayList<>();
        List<String[]> rows = new ArrayList<>();

        for (int i = 0; i < nodesArray.size(); i++) {
            JsonObject node = nodesArray.get(i).getAsJsonObject();
            if (i == 0) headers.addAll(node.keySet());

            String[] row = new String[headers.size()];
            for (int j = 0; j < headers.size(); j++) {
                JsonElement elem = node.get(headers.get(j));
                row[j] = elem != null ? elem.getAsString() : "";
            }
            rows.add(row);
        }

        String[][] result = new String[rows.size() + 1][];
        result[0] = headers.toArray(new String[0]);
        for (int i = 0; i < rows.size(); i++) result[i + 1] = rows.get(i);
        return result;
    }

    private static String[][] parseRelationships(JsonObject root) {
        JsonArray relsArray = root.getAsJsonArray("relationships");
        if (relsArray == null) return new String[0][0];

        List<String> headers = Arrays.asList("from", "type", "to");
        List<String[]> rows = new ArrayList<>();

        for (int i = 0; i < relsArray.size(); i++) {
            JsonObject rel = relsArray.get(i).getAsJsonObject();
            String[] row = new String[3];
            row[0] = rel.has("from") ? rel.get("from").getAsString() : "";
            row[1] = rel.has("type") ? rel.get("type").getAsString() : "";
            row[2] = rel.has("to") ? rel.get("to").getAsString() : "";
            rows.add(row);
        }

        String[][] result = new String[rows.size() + 1][];
        result[0] = headers.toArray(new String[0]);
        for (int i = 0; i < rows.size(); i++) result[i + 1] = rows.get(i);
        return result;
    }

    public record GraphData(String description, String[][] nodes, String[][] relationships) {}
}