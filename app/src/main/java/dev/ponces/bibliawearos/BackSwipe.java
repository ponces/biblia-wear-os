package dev.ponces.bibliawearos;

/** Touch arbitration independent of Android, so vertical scrolling stays untouched. */
final class BackSwipe {
    private final float startX, startY, width, slop;
    private boolean candidate;
    private boolean dragging;
    private float distance;

    BackSwipe(float x, float y, float width, float slop) {
        startX = x;
        startY = y;
        this.width = width;
        this.slop = slop;
        candidate = width > 0 && x >= 0 && x <= width;
    }

    boolean move(float x, float y) {
        float dx = x - startX;
        float dy = Math.abs(y - startY);
        if (!dragging && candidate) {
            if (dy > slop && dy >= Math.abs(dx)) candidate = false;
            else if (dx < -slop) candidate = false;
            else if (dx > slop && dx > dy * 1.5f) dragging = true;
        }
        if (dragging) distance = Math.max(0, Math.min(width, dx));
        return dragging;
    }

    float distance() { return distance; }
    boolean isDragging() { return dragging; }

    boolean shouldFinish(float velocity, float minimumFling) {
        return dragging && (distance >= width * .35f
                || (distance > slop * 2 && velocity >= minimumFling));
    }
}
