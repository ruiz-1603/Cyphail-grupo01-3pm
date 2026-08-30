package org.cyphail.util;

import java.util.Arrays;

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
        if (data == null || data.length == 0) {
            return "";
        }

        // Calculate column widths based on content
        int cols = data[0].length;
        int[] colWidths = new int[cols];

        for (String[] row : data) {
            for (int i = 0; i < cols; i++) {
                colWidths[i] = Math.max(colWidths[i], row[i].length());
            }
        }

        // Build formatted table
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

            // Add separator line after header
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