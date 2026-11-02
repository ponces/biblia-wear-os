package dev.ponces.bibliawearos;

public final class BackSwipeTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static BackSwipe start() { return new BackSwipe(10, 100, 300, 8); }

    public static void main(String[] args) {
        BackSwipe swipe = start();
        check(!swipe.move(14, 102), "Tap jitter must not start dragging");
        check(!swipe.shouldFinish(2000, 600), "A tap must not navigate");
        check(swipe.move(35, 102), "Horizontal drag starts after touch slop");
        check(swipe.distance() == 25, "Page follows finger distance");
        check(!swipe.shouldFinish(0, 600), "Short slow swipe snaps back");
        check(swipe.shouldFinish(1000, 600), "Intentional quick swipe completes");
        check(!swipe.shouldFinish(-1000, 600), "Leftward fling does not complete");
        swipe.move(115, 102);
        check(swipe.shouldFinish(0, 600), "35 percent drag completes without fling");
        swipe.move(500, 102);
        check(swipe.distance() == 300, "Translation is clamped to screen width");
        swipe.move(5, 102);
        check(swipe.distance() == 0 && !swipe.shouldFinish(1000, 600),
                "Dragging back to the origin cancels even at high velocity");

        swipe = start();
        check(!swipe.move(12, 130), "Vertical reading must not be intercepted");
        check(!swipe.move(150, 130), "Vertical gesture cannot later become back");

        swipe = start();
        check(!swipe.move(0, 100), "Leftward gesture must not be intercepted");
        check(!swipe.move(150, 100), "Wrong-direction gesture stays rejected");

        swipe = new BackSwipe(100, 100, 300, 8);
        check(swipe.move(260, 100), "A drag from the centre starts back");
        check(swipe.distance() == 160 && swipe.shouldFinish(0, 600),
                "Centre drag completes using the same distance threshold");
        swipe = new BackSwipe(150, 100, 300, 8);
        check(swipe.move(180, 102), "Short centre drag follows the finger");
        check(!swipe.shouldFinish(0, 600), "Short centre drag snaps back");
        check(swipe.shouldFinish(1000, 600), "Quick centre drag completes");
        swipe = new BackSwipe(150, 100, 300, 8);
        check(!swipe.move(152, 130), "Vertical scrolling from the centre stays vertical");
        check(!swipe.move(290, 130), "Centre vertical scroll cannot become back later");
        swipe = new BackSwipe(250, 100, 300, 8);
        check(swipe.move(290, 100), "A drag can start on the right side too");
        check(swipe.shouldFinish(1000, 600), "Right-side swipe can complete by fling");
        swipe = new BackSwipe(0, 0, 0, 8);
        check(!swipe.move(100, 0), "Unmeasured screen does not start a gesture");

        swipe = start();
        swipe.move(23, 100);
        check(!swipe.shouldFinish(2000, 600), "Tiny flick must not navigate");
        swipe.move(40, 100);
        check(!swipe.shouldFinish(599, 600), "Fling threshold is enforced");

        System.out.println("OK: swipe progress, completion, snap-back, fling and scroll arbitration");
    }
}
