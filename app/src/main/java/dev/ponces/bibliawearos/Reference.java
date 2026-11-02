package dev.ponces.bibliawearos;

import java.util.List;
import java.util.Objects;

/** One-based book, chapter and verse numbers, independent of Android. */
public final class Reference {
    public final int book;
    public final int chapter;
    public final int verse;

    public Reference(int book, int chapter, int verse) {
        this.book = book;
        this.chapter = chapter;
        this.verse = verse;
    }

    public static Reference parse(String value) {
        String[] parts = value.split(":");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid reference");
        return new Reference(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
    }

    public boolean isValid(List<int[]> counts) {
        return book >= 1 && book <= counts.size() && chapter >= 1
                && chapter <= counts.get(book - 1).length && verse >= 1
                && verse <= counts.get(book - 1)[chapter - 1];
    }

    /** Returns null at either end of the Bible; never wraps around. */
    public Reference move(int direction, List<int[]> counts) {
        if (!isValid(counts) || (direction != -1 && direction != 1))
            throw new IllegalArgumentException("Invalid navigation");
        int b = book, c = chapter, v = verse + direction;
        if (v > counts.get(b - 1)[c - 1]) {
            v = 1;
            if (++c > counts.get(b - 1).length) { b++; c = 1; }
        } else if (v < 1) {
            if (--c < 1) {
                if (--b < 1) return null;
                c = counts.get(b - 1).length;
            }
            v = counts.get(b - 1)[c - 1];
        }
        return b > counts.size() ? null : new Reference(b, c, v);
    }

    /** Adjacent chapter, starting at verse one, including across book boundaries. */
    public Reference moveChapter(int direction, List<int[]> counts) {
        if (!isValid(counts) || (direction != -1 && direction != 1))
            throw new IllegalArgumentException("Invalid navigation");
        int boundary = direction == 1 ? counts.get(book - 1)[chapter - 1] : 1;
        Reference adjacent = new Reference(book, chapter, boundary).move(direction, counts);
        return adjacent == null ? null : new Reference(adjacent.book, adjacent.chapter, 1);
    }

    @Override public String toString() { return book + ":" + chapter + ":" + verse; }
    @Override public boolean equals(Object other) {
        return other instanceof Reference && toString().equals(other.toString());
    }
    @Override public int hashCode() { return Objects.hash(book, chapter, verse); }
}
