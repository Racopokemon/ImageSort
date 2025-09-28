package com.github.racopokemon.imagesort;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

class LRButton extends StackPane {
    
    static final Color HALF_TRANSPARENT = new Color(1, 1, 1, 0.08);
    static final Color FLASH_COLOR = new Color(1, 1, 1, 0.09);
    static final Color ARROW_COLOR = new Color(0, 0, 0, 0.5);
    static final double BUTTON_WIDTH = 100;

    private final Gallery gallery;
    private boolean left;

    private Timeline holdTimeline;
    private Timeline repeatTimeline;
    private Timeline flashTimeline;

    private Rectangle flashRect;


    public LRButton(Gallery gallery, StackPane root, boolean left) {
        this.gallery = gallery;
        Rectangle rect = new Rectangle(BUTTON_WIDTH, 10, HALF_TRANSPARENT);
        this.left = left;
        StackPane.setAlignment(this, left ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        setMaxWidth(BUTTON_WIDTH);
        maxHeightProperty().bind(root.heightProperty());
        rect.heightProperty().bind(root.heightProperty());
        root.getChildren().add(this);

        flashRect = new Rectangle(BUTTON_WIDTH, 10, FLASH_COLOR);
        flashRect.heightProperty().bind(root.heightProperty());
        
        double mirror = left ? 1 : -1;
        double scale = 10;
        Polyline arrow = new Polyline(
            scale*mirror, -scale*2, 
            -scale*mirror, 0,
            scale*mirror, scale*2);
        arrow.setStrokeWidth(3);
        arrow.setStrokeLineCap(StrokeLineCap.ROUND);
        arrow.setStrokeLineJoin(StrokeLineJoin.ROUND);
        arrow.setStroke(ARROW_COLOR);

        setOnMouseEntered((event) -> {
            rect.setOpacity(1.0);
            arrow.setOpacity(1.0);
        });
        setOnMouseExited((event) -> {
            rect.setOpacity(0.0);
            arrow.setOpacity(0.0);
            stopTimers(); //rather a security measure id say
        });
        setOnMousePressed((event) -> {
            fire();
            stopTimers();
            holdTimeline.play();
        });
        setOnMouseReleased((event) -> stopTimers());

        getChildren().addAll(flashRect, rect, arrow);
        for (Node n : getChildren()) n.setOpacity(0.0);

        repeatTimeline = new Timeline(new KeyFrame(Duration.seconds(0.11), e -> fire()));
        repeatTimeline.setCycleCount(Timeline.INDEFINITE);
        holdTimeline = new Timeline(new KeyFrame(Duration.seconds(0.32), e -> repeatTimeline.play()));
        holdTimeline.setCycleCount(1);
        flashTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(flashRect.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.9), new KeyValue(flashRect.opacityProperty(), 0.0))
        );
    }

    private void fire() {
        if (left) {
            this.gallery.prevImage();
        } else {
            this.gallery.nextImage();
        }
    }

    private void stopTimers() {
        holdTimeline.stop();
        repeatTimeline.stop();
    }

    public void flash() {
        flashTimeline.playFromStart();
    }

    public void stopFlashing() {
        flashTimeline.jumpTo(Duration.seconds(0.9));
    }
}