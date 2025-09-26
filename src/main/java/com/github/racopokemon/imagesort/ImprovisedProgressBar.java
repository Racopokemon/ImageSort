package com.github.racopokemon.imagesort;

import java.util.ArrayList;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
    private StackPane percentageBarPane;
    private Text percentageTextNode;
    private Text deletedTextNode;
    private VBox propertiesBox; 
    private VBox marginPropertiesBox; 
    private int currentIndex, outOf, numberDeleted;
    private boolean isForeground;
    private Rectangle leftBox, rightBox, deletedBox;
    private IsAnyTrue zoomIndicatorVisibilityConditions; //0: showing details (foreground), 1: zooming

    private static final double SPACING = 3;
    private static final Font FONT = new Font(14);
    private static final Font FONT_ITALIC = Font.font(FONT.getFamily(), FontWeight.NORMAL, FontPosture.ITALIC, 12);
    private static final Font FONT_BOLD = Font.font(FONT.getFamily(), FontWeight.SEMI_BOLD, 16);
    private static final Color DARK_GREY = Color.gray(0.2);

    public ImprovisedProgressBar(double w, double h, Node resolutionIndicator, Node zoomIndicator) {
        width = w;
        height = h;
        this.resolutionIndicator = resolutionIndicator;

        percentageBarPane = new StackPane();

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
        
        HBox progressItself = new HBox();
        progressItself.setSnapToPixel(false);

        leftBox = new Rectangle(w / 3, h, Color.GREY);
        rightBox = new Rectangle(w / 3, h, Color.gray(0.85));
        deletedBox = new Rectangle(w / 3, h, Color.gray(0.7));
        progressItself.getChildren().add(leftBox);
        progressItself.getChildren().add(rightBox);
        progressItself.getChildren().add(deletedBox);
        
        percentageBarPane.getChildren().add(progressItself);
        percentageBarPane.getChildren().add(percentageTextNode);
        percentageBarPane.getChildren().add(deletedTextNode);
        StackPane.setAlignment(deletedTextNode, Pos.CENTER_RIGHT);
        percentageBarPane.setMaxSize(w, h);

        propertiesBox = new VBox();
        VBox.setMargin(propertiesBox, new Insets(SPACING));
        propertiesBox.setSpacing(SPACING*0.5);
        propertiesBox.setAlignment(Pos.CENTER);
        
        marginPropertiesBox = new VBox(propertiesBox); 
        marginPropertiesBox.setBackground(new Background(new BackgroundFill(Color.color(1, 1, 1, 0.8), new CornerRadii(5), null)));

        getChildren().add(percentageBarPane);
        getChildren().add(zoomIndicator);
        VBox.setMargin(zoomIndicator, new Insets(10,0,0,0));        
        setMaxSize(0, 0); //this seems to be the better solution if we want the vbox to use the minimum space possible
        setAlignment(Pos.CENTER); //This gets important when the filename or any of the properties is longer than the percentage bar

        marginPropertiesBox.setMouseTransparent(true);

        this.setSpacing(3);

        zoomIndicatorVisibilityConditions = new IsAnyTrue(2, (isAnyTrue) -> {
            zoomIndicator.setVisible(isAnyTrue);
            resolutionIndicator.setVisible(isAnyTrue);
        });

        isForeground = true; //otherwise, toBackground instantly returns
        toBackground();
    }

    public void makeVisible(boolean visible) {
        super.setVisible(visible);
        zoomIndicatorVisibilityConditions.updateBlocker(!visible);
    }

    //More precisely: The pane that, if hovered, should cause the bar to show the details (but this is done in the Gallery bc it also depends on keystrokes)
    public Pane getDetailsPane() {
        return percentageBarPane;
    }

    private void toForeground() {
        if (isForeground) {
            return;    
        }

        percentageBarPane.setOpacity(1.0);
        if (getChildren().size() == 2) {
            getChildren().add(1, marginPropertiesBox); //used to just use setVisible() here which is the nicer solution, ...
            //but then the mouse still leaves the pane below and the mouse-auto-hide does actually not hide in this area
            //getChildren().add(resolutionIndicator);
        }
        deletedTextNode.setVisible(true);
        zoomIndicatorVisibilityConditions.update(0, true);

        isForeground = true;
        drawProgress();
    }
    
    private void toBackground() {
        if (!isForeground) {
            return;
        }

        percentageBarPane.setOpacity(0.1);
        if (getChildren().size() > 2) {
            getChildren().remove(marginPropertiesBox);
            //getChildren().remove(resolutionIndicator);
        }
        deletedTextNode.setVisible(false);
        zoomIndicatorVisibilityConditions.update(0, false);

        isForeground = false;
        drawProgress();
    }

    //You can also make newlines in info strings, then the spacing is smaller than with separate array entries
    public void setProgress(int currentIndex, int outOf, int numberDeleted, boolean filtered, ArrayList<String> infos) {
        String percentageText = (currentIndex+1) + " / " + outOf;
        deletedTextNode.setText("");
        if (filtered) {
            percentageText += " (filtered)";
        } else {
            if (numberDeleted > 0) {
                deletedTextNode.setText("("+numberDeleted + " deleted)  ");
            }
        }
        percentageTextNode.setText(percentageText);
        this.currentIndex = currentIndex;
        this.outOf = outOf;
        this.numberDeleted = numberDeleted;
        drawProgress();
    
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

    private void drawProgress() {
        int deletedForThisView = isForeground ? numberDeleted : 0;
        double globalOutOf = (outOf + deletedForThisView - 1);
        double percentL, percentR, percentD;
        if (globalOutOf == 0) {
            percentL = 1;
            percentR = 0;
            percentD = 0;
        } else {
            percentL = currentIndex / globalOutOf;
            percentR = (outOf - currentIndex - 1) / globalOutOf;
            percentD = deletedForThisView / globalOutOf;
        }
        leftBox.setWidth(width * percentL);
        rightBox.setWidth(width * percentR);
        deletedBox.setWidth(width * percentD);
    }

    public void updateShowingDetails(boolean f) {
        if (f) {
            toForeground();
        } else {
            toBackground();
        }
    }

    //if zooming, the zoom indicator must be shown, so tell us :)
    //were doing the work, bc if were showing details it also must be shown. 
    public void setZooming(boolean zooming) {
        zoomIndicatorVisibilityConditions.update(1, zooming);
    }
}