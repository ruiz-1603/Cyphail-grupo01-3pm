package org.cyphail.util;

import java.util.Arrays;

public class TableFormatter {

    public static String formatTable(String[][] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        // Calcular ancho de cada columna
        int cols = data[0].length;
        int[] colWidths = new int[cols];

        for (String[] row : data) {
            for (int i = 0; i < cols; i++) {
                colWidths[i] = Math.max(colWidths[i], row[i].length());
            }
        }

        // Construir tabla
        StringBuilder sb = new StringBuilder();
        boolean isHeader = true;

        for (String[] row : data) {
            for (int i = 0; i < cols; i++) {
                String cell = row[i];
                sb.append(String.format("%-" + colWidths[i] + "s", cell));
                if (i < cols - 1) {
                    sb.append("  ");
                }
            }
            sb.append("\n");

            // Línea separadora después del header
            if (isHeader) {
                for (int i = 0; i < cols; i++) {
                    sb.append("-".repeat(colWidths[i]));
                    if (i < cols - 1) {
                        sb.append("  ");
                    }
                }
                sb.append("\n");
                isHeader = false;
            }
        }

        return sb.toString();
    }
}