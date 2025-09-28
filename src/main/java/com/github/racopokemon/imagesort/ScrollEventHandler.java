package com.github.racopokemon.imagesort;

import javafx.event.EventHandler;
import javafx.scene.input.ScrollEvent;

public abstract class ScrollEventHandler implements EventHandler<ScrollEvent> {

    private static final double FIRST_THRESHOLD = 24;
    private static final double REST_THRESHOLD = 13; //actually factor 2
    private static final double MIN_FIRE_INTERVAL = 38; //In ms. For touch pad scrolling this is awesome, for mouse wheel scrolling it literally discards single quick scrolls which feels bad. Maybe this feature is better omitted by setting it to 0. 
    private static double scrolledDistance;
    private static boolean noThresholdReachedYet = true;
    private static long lastScroll;
    private static long lastFire;

    @Override
    public void handle(ScrollEvent event) {
        raw(event.getDeltaY());
        long timestamp = System.currentTimeMillis();

        if (System.currentTimeMillis() - lastScroll >= 2000) {
            scrolledDistance = event.getDeltaY();
            noThresholdReachedYet = true;
        }
        lastScroll = timestamp;

        scrolledDistance += event.getDeltaY();
        //System.out.println(scrolledDistance);
        System.out.println(event.getDeltaY());

        if (noThresholdReachedYet) {
            if (scrolledDistance <= -FIRST_THRESHOLD) {
                while (scrolledDistance <= -FIRST_THRESHOLD) {
                    scrolledDistance += FIRST_THRESHOLD;
                }
                noThresholdReachedYet = false;
                lastFire = timestamp;
                down();
            } else if (scrolledDistance >= FIRST_THRESHOLD) {
                while (scrolledDistance >= FIRST_THRESHOLD) {
                    scrolledDistance -= FIRST_THRESHOLD;
                }
                noThresholdReachedYet = false;
                lastFire = timestamp;
                up();
            }
        } else {
            if (scrolledDistance <= -REST_THRESHOLD) {
                if (timestamp - lastFire < MIN_FIRE_INTERVAL) {
                    scrolledDistance = -REST_THRESHOLD + 1;
                } else {
                    while (scrolledDistance <= -REST_THRESHOLD) {
                        scrolledDistance += 2 * REST_THRESHOLD;
                    }
                    lastFire = timestamp;
                    down();
                }
            } else if (scrolledDistance >= REST_THRESHOLD) {
                if (timestamp - lastFire < MIN_FIRE_INTERVAL) {
                    scrolledDistance = REST_THRESHOLD - 1;
                } else {
                    while (scrolledDistance >= REST_THRESHOLD) {
                        scrolledDistance -= 2 * REST_THRESHOLD;
                    }
                    lastFire = timestamp;
                    up();
                }
            }
        }

        event.consume();
    }

    public void up() {}

    public void down() {}

    // always called, additionally to the preprocessed single up() and down() calls
    public void raw(double amount) {}

    public static double getPosition() {
        return scrolledDistance;
    }
}