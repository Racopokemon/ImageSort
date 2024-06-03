package com.github.racopokemon.imagesort;

import java.util.ArrayList;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class ImprovisedProgressBar extends VBox {

    private double width, height;
    private Node resolutionIndicator; //making use of very good OO, we also receive a random node that should also be made visible on mouse hover
    private Text percentageTextNode;
    private Text deletedTextNode;
    private VBox propertiesBox; 
    private VBox marginPropertiesBox; 
    private Rectangle left, right;

    private static final double SPACING = 3;
    private static final Font FONT = new Font(14);
    private static final Font FONT_ITALIC = Font.font(FONT.getFamily(), FontWeight.NORMAL, FontPosture.ITALIC, 12);
    private static final Font FONT_BOLD = Font.font(FONT.getFamily(), FontWeight.SEMI_BOLD, 16);
    private static final Color DARK_GREY = Color.gray(0.2);

    public ImprovisedProgressBar(double w, double h, Node resolutionIndicator) {
        width = w;
        height = h;
        this.resolutionIndicator = resolutionIndicator;

        StackPane stack = new StackPane();

        percentageTextNode = new Text("11/10");
        //label.setStrokeWidth(0.5);
        //label.setStroke(Color.BLACK);
        percentageTextNode.setFill(Color.BLACK);
        percentageTextNode.setFont(new Font(22));
        percentageTextNode.setTextAlignment(TextAlignment.CENTER);

        deletedTextNode = new Text("del'd");
        deletedTextNode.setFill(Color.BLACK);
        deletedTextNode.setFont(new Font(15));
        deletedTextNode.setTextAlignment(TextAlignment.RIGHT);

        stack.setOnMouseEntered((event) -> {toForeground();});
        stack.setOnMouseExited((event) -> {toBackground();});

        HBox progressItself = new HBox();
        progressItself.setSnapToPixel(false);

        left = new Rectangle(w / 2, h, Color.GREY);
        right = new Rectangle(w / 2, h, Color.gray(0.85));
        progressItself.getChildren().add(left);
        progressItself.getChildren().add(right);
        
        stack.getChildren().add(progressItself);
        stack.getChildren().add(percentageTextNode);
        stack.getChildren().add(deletedTextNode);
        stack.setAlignment(deletedTextNode, Pos.CENTER_RIGHT);
        stack.setMaxSize(w, h);

        propertiesBox = new VBox();
        VBox.setMargin(propertiesBox, new Insets(SPACING));
        propertiesBox.setSpacing(SPACING*0.5);
        propertiesBox.setAlignment(Pos.CENTER);
        
        marginPropertiesBox = new VBox(propertiesBox); 
        marginPropertiesBox.setBackground(new Background(new BackgroundFill(Color.color(1, 1, 1, 0.8), new CornerRadii(5), null)));

        getChildren().add(stack);
        //getChildren().add(marginPropertiesBox);
        setMaxSize(0, 0); //this seems to be the better solution if we want the vbox to use the minimum space possible
        //setSpacing(SPACING); //between bar & properties
        setAlignment(Pos.CENTER); //This gets important when the filename or any of the properties is longer than the percentage bar

        marginPropertiesBox.setMouseTransparent(true);

        this.setSpacing(3);

        toBackground();
    }

    private void toForeground() {
        setOpacity(1.0);
        if (getChildren().size() == 1) {
            getChildren().add(marginPropertiesBox); //used to just use setVisible() here which is the nicer solution, ...
            //but then the mouse still leaves the pane below and the mouse-auto-hide does actually not hide in this area
            //getChildren().add(resolutionIndicator);
        }
        resolutionIndicator.setVisible(true);
        deletedTextNode.setVisible(true);
    }
    
    private void toBackground() {
        setOpacity(0.1);
        if (getChildren().size() > 1) {
            getChildren().remove(marginPropertiesBox);
            //getChildren().remove(resolutionIndicator);
        }
        resolutionIndicator.setVisible(false);
        deletedTextNode.setVisible(false);
    }

    //You can also make newlines in info strings, then the spacing is smaller than with separate array entries
    public void setProgress(int value, int outOf, ArrayList<String> infos, boolean filtered, int deleted) {
        String percentageText = (value+1) + " / " + outOf;
        deletedTextNode.setText("");
        if (filtered) {
            percentageText += " (filtered)";
        } else {
            if (deleted > 0) {
                deletedTextNode.setText("("+deleted + " deleted)  ");
            }
        }
        percentageTextNode.setText(percentageText);
        double percentage = outOf == 1 ? 1 : (double) value / (outOf - 1);
        left.setWidth(width * percentage);
        right.setWidth(width * (1-percentage));
    
        propertiesBox.getChildren().clear();
        int index = 0;
        for (String s : infos) {
            Text info = new Text(s);
            info.setTextAlignment(TextAlignment.CENTER);
            if (index++ == 0) {
                info.setFont(FONT_BOLD);
            } else {
                info.setFont(infos.size() == 3 ? (index == 2 ? FONT_ITALIC : FONT) : FONT);
                info.setFill(DARK_GREY);
            }
            propertiesBox.getChildren().add(info);
        }
    }
}