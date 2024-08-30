package com.github.racopokemon.imagesort;

import javafx.event.EventHandler;
import javafx.scene.input.ScrollEvent;

public abstract class ScrollEventHandler implements EventHandler<ScrollEvent> {

    private static final double FIRST_THRESHOLD = 12;
    private static final double REST_THRESHOLD = 19;
    private static double scrolledDistance;
    private static boolean noThresholdReachedYet = true;
    private long lastScroll = 0;

    @Override
    public void handle(ScrollEvent event) {
        raw(event.getDeltaY());
        long timestamp = System.currentTimeMillis();

        if (System.currentTimeMillis()-lastScroll >= 2000) {
            scrolledDistance = event.getDeltaY();
            noThresholdReachedYet = true;
        }
        lastScroll = timestamp;

        scrolledDistance += event.getDeltaY();

        if (noThresholdReachedYet) {
            if (scrolledDistance <= -FIRST_THRESHOLD) {
                while (scrolledDistance <= -FIRST_THRESHOLD) {
                    scrolledDistance += FIRST_THRESHOLD;
                }
                noThresholdReachedYet = false;
                down();
            } else if (scrolledDistance >= FIRST_THRESHOLD) {
                while (scrolledDistance >= FIRST_THRESHOLD) {
                    scrolledDistance -= FIRST_THRESHOLD;
                }
                noThresholdReachedYet = false;
                up();
            }    
        } else {
            if (scrolledDistance <= -REST_THRESHOLD) {
                while (scrolledDistance <= -REST_THRESHOLD) {
                    scrolledDistance += 2*REST_THRESHOLD;
                }
    
                down();
            } else if (scrolledDistance >= REST_THRESHOLD) {
                while (scrolledDistance >= REST_THRESHOLD) {
                    scrolledDistance -= 2*REST_THRESHOLD;
                }
    
                up();
            }
        }

        event.consume();
   }

    public void up() {}

    public void down() {}

    //always called, additionally to the preprocessed single up() and down() calls
    public void raw(double amount) {}

    public static double getPosition() {
        return scrolledDistance;
    }
    
} 