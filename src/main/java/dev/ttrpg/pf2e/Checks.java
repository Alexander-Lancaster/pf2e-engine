package dev.ttrpg.pf2e;
import java.util.*;
final class Checks {
    private Checks() {}
    static String text(String text) {
        Objects.requireNonNull(text);
        if (text.isBlank()) throw new IllegalArgumentException("Text must not be blank");
        return text;
    }
    static <T> void unique(List<T> values) {
        if (new HashSet<>(values).size() != values.size())
            throw new IllegalArgumentException("Duplicate IDs");
    }
    static <T> List<T> append(List<T> list, T value) {
        var copy = new ArrayList<>(list); copy.add(value); return List.copyOf(copy);
    }
}
