package com.github.racopokemon.imagesort;

import javafx.geometry.Point2D;

public class SeekHelper {
    private class Segment {

    }

    private int count, current;
    private Point2D initialPos;
    private double virtualPosition = 0;

    public SeekHelper(Point2D initial, int imageCount, int currentImage) {
        count = imageCount;
        current = currentImage;
        initialPos = new Point2D((int)initial.getX(),(int)initial.getY());
    }

    //Call getIndex() afterwards to see if the index has changed
    public void update(double mouseX, double mouseY) {
        Point2D p = Common.getMouseScreenPos();
        mouseX = p.getX();
        mouseY = p.getY();
        if (mouseX != initialPos.getX() || mouseY != initialPos.getY()) {
            virtualPosition += mouseX - initialPos.getX();
            Common.setMouseScreenPos(initialPos);
            if (virtualPosition > 10) {
                virtualPosition -= 20;
                current++;
                if (current >= count) current = count - 1;
            } else if (virtualPosition < -10) {
                virtualPosition += 20;
                current --;
                if (current < 0) current = 0;
            }
            System.out.println(initialPos.getX() + " " + (mouseX - initialPos.getX()));
        }
    }

    public int getIndex() {
        return current;
    }
}
