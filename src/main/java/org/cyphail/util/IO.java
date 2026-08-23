package org.cyphail.util;

import java.util.Scanner;

public class IO {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void println(String msg) {
        System.out.println(msg);
    }

    public static void println() {
        System.out.println();
    }

    public static void print(String msg) {
        System.out.print(msg);
    }

    public static String readLine() {
        return SCANNER.nextLine();
    }

    public static void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}