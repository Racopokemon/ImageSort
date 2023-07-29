package com.github.racopokemon.imagesort;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.robot.Robot;

public class TickLabel extends InteractiveLabel {

    private int tickNumber, numberOfTicks;

    public TickLabel(int tickNumber, int numberOfTicks, Pos alignment, Action action) {
        super(28, 150, Gallery.TICK_LABEL_HEIGHT, alignment, action, action);
        this.tickNumber = tickNumber;
        this.numberOfTicks = numberOfTicks;

        scrollUp = () -> {
            Robot r = new Robot();
            double skipSize;
            if (tickNumber <= 0) {
                skipSize = Gallery.TICK_LABEL_HEIGHT * (numberOfTicks-1);
            } else {
                skipSize = -Gallery.TICK_LABEL_HEIGHT;
            }
            Point2D p = r.getMousePosition().add(new Point2D(0, skipSize));
            r.mouseMove(p);
        };
        scrollDown = () -> {
            Robot r = new Robot();
            double skipSize;
            if (tickNumber >= numberOfTicks-1) {
                skipSize = -Gallery.TICK_LABEL_HEIGHT * (numberOfTicks-1);
            } else {
                skipSize = Gallery.TICK_LABEL_HEIGHT;
            }
            Point2D p = r.getMousePosition().add(new Point2D(0, skipSize));
            r.mouseMove(p);
            //this has been brought to you by github copilot, "the best AI ever" (that also came as suggestion lol)
        };
    }
    
}