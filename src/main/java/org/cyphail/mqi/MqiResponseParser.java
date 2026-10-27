package org.cyphail.mqi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Proyecto Cyphail
 * Grupo 01-3pm
 *
 * Autores:
 * - Priscilla Murillo Romero
 * - Aaron Ruiz Medina
 * - Samael Sanchez Mora
 * - Daniel Villarroel Abaduca
 * - Nicolás Zárate Hernández
 */

/**
 * Converts MQI JSON responses into a {@link PrologResult}.
 * Adapted from course MQI Demo (EIF400-II-2026).
 *
 * Expected response forms:
 *   false                                             -> PrologResult.Failure
 *   true                                              -> PrologResult.Success (empty binding map)
 *   {"functor":"true","args":[ [ [bind,...], ... ] ]} -> PrologResult.Success
 *   {"functor":"exception","args":[ term ]}           -> PrologResult.PrologError
 * where each binding is {"functor":"=","args":["Var", value]}.
 */
public final class MqiResponseParser {

    private MqiResponseParser() {}

    /** Models a Prolog compound term, e.g. foo(bar, 1). */
    public record Compound(String functor, List<Object> args) {}

    public static PrologResult parse(String json) {
        JsonElement root = JsonParser.parseString(json);

        if (root.isJsonPrimitive() && root.getAsJsonPrimitive().isBoolean()) {
            return root.getAsBoolean()
                    ? new PrologResult.Success(List.of(Map.of()))
                    : new PrologResult.Failure();
        }

        if (root.isJsonObject()) {
            JsonObject obj = root.getAsJsonObject();
            String functor = obj.has("functor") ? obj.get("functor").getAsString() : "";
            JsonArray args = obj.has("args") ? obj.getAsJsonArray("args") : new JsonArray();

            return switch (functor) {
                case "true" -> new PrologResult.Success(parseSolutions(args.get(0).getAsJsonArray()));
                case "exception" -> new PrologResult.PrologError(toJava(args.get(0)));
                default -> new PrologResult.Failure();
            };
        }

        return new PrologResult.Failure();
    }

    private static List<Map<String, Object>> parseSolutions(JsonArray solutions) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonElement solution : solutions) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (JsonElement binding : solution.getAsJsonArray()) {
                JsonArray pair = binding.getAsJsonObject().getAsJsonArray("args");
                row.put(pair.get(0).getAsString(), toJava(pair.get(1)));
            }
            rows.add(row);
        }
        return rows;
    }

    /** Maps a JsonElement to a Java Object: null, Boolean, Long, Double, String, List or Compound. */
    public static Object toJava(JsonElement e) {
        if (e == null || e.isJsonNull()) {
            return null;
        }

        if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isBoolean()) {
                return p.getAsBoolean();
            }
            if (p.isNumber()) {
                String s = p.getAsString();
                if (s.contains(".") || s.contains("e") || s.contains("E")) {
                    return p.getAsDouble();
                }
                return p.getAsLong();
            }
            return p.getAsString();
        }

        if (e.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement item : e.getAsJsonArray()) {
                list.add(toJava(item));
            }
            return list;
        }

        JsonObject jobj = e.getAsJsonObject();
        List<Object> args = new ArrayList<>();
        if (jobj.has("args") && jobj.get("args").isJsonArray()) {
            for (JsonElement arg : jobj.getAsJsonArray("args")) {
                args.add(toJava(arg));
            }
        }
        String functor = jobj.has("functor") ? jobj.get("functor").getAsString() : "";
        return new Compound(functor, args);
    }
}
