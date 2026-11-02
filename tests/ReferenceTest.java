package dev.ponces.bibliawearos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReferenceTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        List<int[]> counts = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(args[0]))) {
            String[] cells = line.split(",");
            int[] chapters = new int[cells.length];
            for (int i = 0; i < cells.length; i++) chapters[i] = Integer.parseInt(cells[i]);
            counts.add(chapters);
        }
        Reference first = new Reference(1, 1, 1);
        check(first.move(-1, counts) == null, "Start of Bible");
        check(!new Reference(0, 1, 1).isValid(counts), "Invalid book");
        check(!new Reference(67, 1, 1).isValid(counts), "Invalid last book");
        check(!new Reference(1, 0, 1).isValid(counts), "Invalid chapter");
        check(!new Reference(1, 51, 1).isValid(counts), "Invalid chapter range");
        check(!new Reference(1, 1, 0).isValid(counts), "Invalid verse");
        check(!new Reference(1, 1, 32).isValid(counts), "Invalid verse range");
        Reference current = first;
        int visited = 0;
        while (current != null) {
            check(current.isValid(counts), "Valid traversal");
            check(Reference.parse(current.toString()).equals(current), "Round-trip reference");
            Reference next = current.move(1, counts);
            if (next != null) check(next.move(-1, counts).equals(current), "Reversible traversal");
            visited++;
            if (next == null) check(current.equals(new Reference(66, 22, 21)), "Last verse");
            current = next;
        }
        check(visited == 31105, "All verses visited: " + visited);
        check(first.moveChapter(-1, counts) == null, "First chapter boundary");
        check(new Reference(66, 22, 21).moveChapter(1, counts) == null, "Last chapter boundary");
        check(new Reference(1, 50, 26).moveChapter(1, counts).equals(new Reference(2, 1, 1)),
                "Next book starts at first verse");
        check(new Reference(2, 1, 15).moveChapter(-1, counts).equals(new Reference(1, 50, 1)),
                "Previous book starts at first verse of final chapter");
        current = first;
        int chapters = 0;
        while (current != null) {
            check(current.isValid(counts) && current.verse == 1, "Valid chapter start");
            Reference next = current.moveChapter(1, counts);
            if (next != null) check(next.moveChapter(-1, counts).equals(current), "Reversible chapters");
            chapters++;
            current = next;
        }
        check(chapters == 1189, "Every chapter visited: " + chapters);
        try {
            Reference.parse("bad");
            throw new AssertionError("Invalid parse must fail");
        } catch (IllegalArgumentException expected) { /* expected */ }
        try {
            first.move(0, counts);
            throw new AssertionError("Invalid direction must fail");
        } catch (IllegalArgumentException expected) { /* expected */ }
        try {
            first.moveChapter(0, counts);
            throw new AssertionError("Invalid chapter direction must fail");
        } catch (IllegalArgumentException expected) { /* expected */ }
        try {
            new Reference(0, 1, 1).moveChapter(1, counts);
            throw new AssertionError("Invalid chapter reference must fail");
        } catch (IllegalArgumentException expected) { /* expected */ }
        System.out.println("OK: navigation through all " + visited + " verses, boundaries and parsing");
        System.out.println("OK: navigation through all " + chapters + " chapters and book boundaries");
    }
}
