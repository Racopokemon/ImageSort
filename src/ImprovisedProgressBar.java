import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class ImprovisedProgressBar extends StackPane {

    private double width;
    private Text label;
    Rectangle left, right;

    public ImprovisedProgressBar(double w, double h) {
        width = w;

        label = new Text("11/10");
        //label.setStrokeWidth(0.5);
        //label.setStroke(Color.BLACK);
        label.setFill(Color.BLACK);
        label.setFont(new Font(22));
        label.setTextAlignment(TextAlignment.CENTER);

        setOpacity(0.1);
        setOnMouseEntered((event) -> {setOpacity(1.0);});
        setOnMouseExited((event) -> {setOpacity(0.1);});

        HBox progressItself = new HBox();

        left = new Rectangle(w / 2, h, Color.GREY);
        right = new Rectangle(w / 2, h, Color.gray(0.85));
        progressItself.getChildren().add(left);
        progressItself.getChildren().add(right);

        getChildren().add(progressItself);
        getChildren().add(label);
        setMaxSize(w, h);
    }

    public void setProgress(int value, int outOf) {
        label.setText((value+1) + " / " + outOf);
        double percentage = outOf == 1 ? 1 : (double) value / (outOf - 1);
        left.setWidth(width * percentage);
        right.setWidth(width * (1-percentage));
    }
}