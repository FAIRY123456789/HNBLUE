package com.example.jpaspringboot.service.ai;

/**
 * Streaming filter for model reasoning blocks.
 *
 * <p>The filter only recognizes lowercase/uppercase variants of
 * {@code <think>} and {@code </think>}. It keeps at most a tag-length tail
 * across chunks, so memory usage is constant and normal Markdown content is
 * emitted immediately.</p>
 */
public class ThinkTagFilter {
    private static final String START_TAG = "<think>";
    private static final String END_TAG = "</think>";

    private enum State {
        VISIBLE,
        MATCHING_START,
        INSIDE_THINK,
        MATCHING_END
    }

    private State state = State.VISIBLE;
    private final StringBuilder pending = new StringBuilder();

    public String filter(String delta) {
        if (delta == null || delta.isEmpty()) return "";

        StringBuilder out = new StringBuilder(delta.length());
        for (int i = 0; i < delta.length(); i++) {
            char ch = delta.charAt(i);
            switch (state) {
                case VISIBLE -> handleVisible(ch, out);
                case MATCHING_START -> handleMatchingStart(ch, out);
                case INSIDE_THINK -> handleInsideThink(ch);
                case MATCHING_END -> handleMatchingEnd(ch);
            }
        }
        return out.toString();
    }

    public String finish() {
        if (state == State.MATCHING_START) {
            String tail = pending.toString();
            pending.setLength(0);
            state = State.VISIBLE;
            return tail;
        }
        pending.setLength(0);
        state = State.VISIBLE;
        return "";
    }

    public boolean isInsideThink() {
        return state == State.INSIDE_THINK || state == State.MATCHING_END;
    }

    private void handleVisible(char ch, StringBuilder out) {
        if (ch == '<') {
            pending.setLength(0);
            pending.append(ch);
            state = State.MATCHING_START;
            return;
        }
        out.append(ch);
    }

    private void handleMatchingStart(char ch, StringBuilder out) {
        pending.append(ch);
        String lower = pending.toString().toLowerCase();
        if (START_TAG.equals(lower)) {
            pending.setLength(0);
            state = State.INSIDE_THINK;
            return;
        }
        if (START_TAG.startsWith(lower)) return;

        out.append(pending);
        pending.setLength(0);
        state = State.VISIBLE;
    }

    private void handleInsideThink(char ch) {
        if (ch == '<') {
            pending.setLength(0);
            pending.append(ch);
            state = State.MATCHING_END;
        }
    }

    private void handleMatchingEnd(char ch) {
        pending.append(ch);
        String lower = pending.toString().toLowerCase();
        if (END_TAG.equals(lower)) {
            pending.setLength(0);
            state = State.VISIBLE;
            return;
        }
        if (END_TAG.startsWith(lower)) return;

        if (ch == '<') {
            pending.setLength(0);
            pending.append(ch);
            state = State.MATCHING_END;
            return;
        }
        pending.setLength(0);
        state = State.INSIDE_THINK;
    }
}
