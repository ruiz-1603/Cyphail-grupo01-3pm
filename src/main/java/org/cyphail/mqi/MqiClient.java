package org.cyphail.mqi;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

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
 * Minimalist MQI Client over already opened streams (TCP socket).
 * Format for each message in both directions: {@code <size in bytes>.\n<message bytes>.\n}
 * Adapted from course MQI Demo (EIF400-II-2026).
 */
public final class MqiClient implements AutoCloseable {

    private final InputStream in;
    private final OutputStream out;
    private boolean closed = false;

    public MqiClient(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    /** First required message: password printed by SWI-Prolog MQI server. */
    public void authenticate(String password) throws IOException {
        send(password);
        PrologResult r = MqiResponseParser.parse(receive());
        if (r instanceof PrologResult.PrologError err) {
            throw new IOException("Authentication rejected: " + err.term());
        }
    }

    /** Runs goal and returns all solutions. No timeout = -1. */
    public PrologResult run(String goal) throws IOException {
        return run(goal, -1);
    }

    /**
     * Runs goal with a timeout in seconds (-1 = no limit).
     * Double parentheses wrap the goal in case of commas so goal is a single argument of run/2.
     */
    public PrologResult run(String goal, int timeoutSeconds) throws IOException {
        send("run((" + goal + "), " + timeoutSeconds + ")");
        return MqiResponseParser.parse(receive());
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        send("close");
        receive();
    }

    // ---------- framing ----------

    void send(String message) throws IOException {
        byte[] payload = (message + ".\n").getBytes(StandardCharsets.UTF_8);
        out.write((payload.length + ".\n").getBytes(StandardCharsets.UTF_8));
        out.write(payload);
        out.flush();
    }

    String receive() throws IOException {
        StringBuilder len = new StringBuilder();
        int c;
        while ((c = in.read()) != -1 && c != '.') {
            len.append((char) c);
        }
        if (c == -1) {
            throw new EOFException("Connection closed before receiving size");
        }
        in.read(); // consume the '\n' after the '.'

        int n = Integer.parseInt(len.toString().trim());
        byte[] buf = in.readNBytes(n);
        if (buf.length < n) {
            throw new EOFException("Truncated message: " + buf.length + "/" + n);
        }

        String s = new String(buf, StandardCharsets.UTF_8).strip();
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
