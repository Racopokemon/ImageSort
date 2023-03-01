package com.github.racopokemon.imagesort;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class ImprovisedProgressBar extends VBox {

    private double width, height;
    private Text percentageText;
    private VBox propertiesBox; 
    private VBox marginPropertiesBox; 
    private Rectangle left, right;

    private static final double SPACING = 3;
    private static final Font FONT = new Font(14);
    private static final Font FONT_BOLD = Font.font(FONT.getFamily(), FontWeight.SEMI_BOLD, 16);
    private static final Color DARK_GREY = Color.gray(0.2);

    public ImprovisedProgressBar(double w, double h) {
        width = w;
        height = h;

        StackPane stack = new StackPane();

        percentageText = new Text("11/10");
        //label.setStrokeWidth(0.5);
        //label.setStroke(Color.BLACK);
        percentageText.setFill(Color.BLACK);
        percentageText.setFont(new Font(22));
        percentageText.setTextAlignment(TextAlignment.CENTER);

        stack.setOnMouseEntered((event) -> {toForeground();});
        stack.setOnMouseExited((event) -> {toBackground();});

        HBox progressItself = new HBox();

        left = new Rectangle(w / 2, h, Color.GREY);
        right = new Rectangle(w / 2, h, Color.gray(0.85));
        progressItself.getChildren().add(left);
        progressItself.getChildren().add(right);
        
        stack.getChildren().add(progressItself);
        stack.getChildren().add(percentageText);
        stack.setMaxSize(w, h);

        propertiesBox = new VBox();
        VBox.setMargin(propertiesBox, new Insets(SPACING));
        propertiesBox.setSpacing(SPACING);
        propertiesBox.setAlignment(Pos.CENTER);
        
        marginPropertiesBox = new VBox(propertiesBox); 
        marginPropertiesBox.setBackground(new Background(new BackgroundFill(Color.color(1, 1, 1, 0.45), null, null)));

        getChildren().add(stack);
        getChildren().add(marginPropertiesBox);
        setMaxSize(0, 0); //this seems to be the better solution if we want the vbox to use the minimum space possible
        //setSpacing(SPACING); //between bar & properties
        setAlignment(Pos.CENTER); //This gets important when the filename or any of the properties is longer than the percentage bar

        propertiesBox.setMouseTransparent(true);

        toBackground();
    }

    private void toForeground() {
        setOpacity(1.0);
        marginPropertiesBox.setVisible(true);
    }

    private void toBackground() {
        setOpacity(0.1);
        marginPropertiesBox.setVisible(false);
    }

    //You can also make newlines in info strings, then the spacing is smaller than with separate array entries
    public void setProgress(int value, int outOf, String[] infos, boolean filtered) {
        percentageText.setText((value+1) + " / " + outOf);
        if (filtered) {
            percentageText.setText(percentageText.getText() + " (filtered)");
        }
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
                info.setFont(FONT);
                info.setFill(DARK_GREY);
            }
            propertiesBox.getChildren().add(info);
        }
    }
}