package com.github.racopokemon.imagesort;

import javafx.event.EventHandler;
import javafx.scene.input.ScrollEvent;

public abstract class ScrollEventHandler implements EventHandler<ScrollEvent> {

    @Override
    public void handle(ScrollEvent event) {
        raw(event.getDeltaY());
        if (event.getDeltaY() <= -4) {
            down();
        } else if (event.getDeltaY() >= 4) {
            up();
        }
        event.consume();
    }

    public void up() {}

    public void down() {}

    //always called, additionally to the preprocessed single up() and down() calls
    public void raw(double amount) {}
    
} 