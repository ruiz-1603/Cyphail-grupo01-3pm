package org.cyphail.util;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

public class TableFormatter {

    public static String formatTable(String[][] data) {
        if (data == null || data.length == 0 || data[0].length == 0) {
            return "";
        }

        int cols = data[0].length;
        int[] colWidths = IntStream.range(0, cols)
                .map(col -> Arrays.stream(data)
                        .mapToInt(row -> col < row.length && row[col] != null ? row[col].length() : 0)
                        .max()
                        .orElse(0))
                .toArray();

        String headerLine = formatRow(data[0], colWidths);
        String separatorLine = IntStream.range(0, cols)
                .mapToObj(i -> "-".repeat(colWidths[i]))
                .collect(Collectors.joining("  "));

        String bodyLines = Arrays.stream(data)
                .skip(1)
                .map(row -> formatRow(row, colWidths))
                .collect(Collectors.joining("\n"));

        return bodyLines.isEmpty()
                ? headerLine + "\n" + separatorLine + "\n"
                : headerLine + "\n" + separatorLine + "\n" + bodyLines + "\n";
    }

    private static String formatRow(String[] row, int[] colWidths) {
        return IntStream.range(0, colWidths.length)
                .mapToObj(i -> {
                    String cell = (i < row.length && row[i] != null) ? row[i] : "";
                    return String.format("%-" + colWidths[i] + "s", cell);
                })
                .collect(Collectors.joining("  "));
    }
}
