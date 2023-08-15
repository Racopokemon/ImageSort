package com.github.racopokemon.imagesort;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class InteractiveLabel extends StackPane {

    public interface Action {
        public void call();
    }

    protected Action scrollUp, scrollDown, primary, secondary, mid;

    private Text label;
    protected Rectangle scrollAbsorber;

    private double unhoverOpacity = 1.0;
    private boolean hovered = false;
    
    public InteractiveLabel(double textSize, double width, double height, Pos alignment, Action up, Action down, Action midAction) {
        this(textSize, width, height, alignment, up, down, up, down, midAction);
    }

    public InteractiveLabel(double textSize, double width, double height, Pos alignment, Action primary, Action secondary, Action scrollUp, Action scrollDown, Action midAction) {

        label = new Text("bottom text");
        label.setFont(new Font(textSize));
        label.setFill(Color.WHITE);
        label.setStroke(Color.BLACK);
        label.setStrokeWidth(1.1);

        this.scrollUp = scrollUp;
        this.scrollDown = scrollDown;
        this.primary = primary;
        this.secondary = secondary;
        this.mid = midAction;
        
        scrollAbsorber = new Rectangle(width, height, Color.TRANSPARENT);
        scrollAbsorber.setOnScroll((event) -> {
            if (event.getDeltaY() >= 4) {
                this.scrollUp.call();
            } else if (event.getDeltaY() <= -4) {
                this.scrollDown.call();
            }
        });
        scrollAbsorber.setOnMouseEntered((event) -> {
            hovered = true;
            updateOpacity();
        });
        scrollAbsorber.setOnMouseExited((event) -> {
            hovered = false;
            updateOpacity();
        });
        scrollAbsorber.setOnMousePressed((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (primary != null) primary.call();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                if (secondary != null) secondary.call();
            } else if (event.getButton() == MouseButton.MIDDLE) {
                if (mid != null) mid.call();
            }
        });

        StackPane.setAlignment(label, alignment);
        StackPane.setMargin(label, new Insets(2, 10, 2, 10));
        StackPane.setAlignment(scrollAbsorber, alignment);

        getChildren().addAll(label, scrollAbsorber);
        setMaxSize(0, 0);
    }

    public void setText(String text) {
        label.setText(text);
    }

    //set the opacity for when the InteractiveLabel is not hovered by the mouse (on hover its always 1.0)
    public void setUnhoverOpacity(double op) {
        unhoverOpacity = op;
        updateOpacity();
    }

    private void updateOpacity() {
        if (hovered) {
            label.setOpacity(1.0);
            label.setStroke(Color.GRAY); 
        } else {
            label.setOpacity(unhoverOpacity);
            label.setStroke(Color.BLACK); 
        }
    }
}
