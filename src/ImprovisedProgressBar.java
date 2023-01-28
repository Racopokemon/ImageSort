import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class ImprovisedProgressBar extends VBox {

    private double width, height;
    private Text label;
    private Text filename;
    private StackPane filenameStack; 
    Rectangle left, right;

    public ImprovisedProgressBar(double w, double h) {
        width = w;
        height = h;

        StackPane stack = new StackPane();

        label = new Text("11/10");
        //label.setStrokeWidth(0.5);
        //label.setStroke(Color.BLACK);
        label.setFill(Color.BLACK);
        label.setFont(new Font(22));
        label.setTextAlignment(TextAlignment.CENTER);

        stack.setOnMouseEntered((event) -> {toForeground();});
        stack.setOnMouseExited((event) -> {toBackground();});

        HBox progressItself = new HBox();

        left = new Rectangle(w / 2, h, Color.GREY);
        right = new Rectangle(w / 2, h, Color.gray(0.85));
        progressItself.getChildren().add(left);
        progressItself.getChildren().add(right);

        filename = new Text(".png");
        filename.setFont(new Font(16));
        StackPane.setAlignment(filename, Pos.CENTER);
        Rectangle filenameBackground = new Rectangle(w, h, Color.WHITE);
        filenameBackground.setOpacity(0.3f);
        filenameStack = new StackPane(filenameBackground, filename);

        stack.getChildren().add(progressItself);
        stack.getChildren().add(label);
        stack.setMaxSize(w, h);

        getChildren().add(stack);
        getChildren().add(filenameStack);
        setMaxSize(0, 0); //this seems to be the better solution if we want the vbox to use the minimum space possible

        filenameStack.setMouseTransparent(true);

        toBackground();
    }

    private void toForeground() {
        setOpacity(1.0);
        filenameStack.setVisible(true);
    }

    private void toBackground() {
        setOpacity(0.1);
        filenameStack.setVisible(false);
    }

    public void setProgress(int value, int outOf, String fName, boolean filtered) {
        label.setText((value+1) + " / " + outOf);
        if (filtered) {
            label.setText(label.getText() + " (filtered)");
        }
        double percentage = outOf == 1 ? 1 : (double) value / (outOf - 1);
        left.setWidth(width * percentage);
        right.setWidth(width * (1-percentage));
        filename.setText(fName);
    }



    /*
     * 
[Exif IFD0] Date/Time - 2022:02:11 15:40:04
[Exif SubIFD] Date/Time Original - 2022:02:11 15:40:04
[File] File Modified Date - Mo Dez 19 01:46:59 +01:00 2022

[Exif SubIFD] ISO Speed Ratings - 50

[Exif SubIFD] Focal Length - 27 mm
[Exif SubIFD] Focal Length 35 - 40 mm ???
     * 
     * 
     */
}