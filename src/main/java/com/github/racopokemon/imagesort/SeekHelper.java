package com.github.racopokemon.imagesort;

import javafx.geometry.Point2D;

public class SeekHelper {
    private class Segment {

    }

    private int count, current, initialIndex;
    private Point2D initialPos;
    private double virtualPosition = 0;
    private double offset;

    public SeekHelper(Point2D initial, int imageCount, int currentImage) {
        count = imageCount;
        initialIndex = currentImage;
        current = initialIndex;
        initialPos = new Point2D(initial.getX(),initial.getY());
        offset = 0;
    }

    //Call getIndex() afterwards to see if the index has changed
    public void update(double mouseX, double mouseY) {
        double relativeMouse = mouseX-initialPos.getX();
        virtualPosition = relativeMouse + offset;

        if (relativeMouse > 70 || relativeMouse < -70) {
            //Lets just hope / assume that the robot works instant or at least in the next frame
            offset += relativeMouse;
            Common.setMouseScreenPos(initialPos);
        }
        System.out.println(virtualPosition + " " + offset);

        current = (int)(virtualPosition / 15) + initialIndex;
        if (current >= count) current = count-1;
        if (current < 0) current = 0;
    }

    public int getIndex() {
        return current;
    }
}
