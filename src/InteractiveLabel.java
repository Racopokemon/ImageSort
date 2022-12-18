import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class InteractiveLabel extends StackPane {

    public interface Action {
        public void call();
    }

    private static double WIDTH = 200;
    private static double HEIGHT = 70;

    private Text label;
    private Rectangle scrollAbsorber;
    
    public InteractiveLabel (Pos alignment, Action up, Action down) {
        label = new Text("bottom text");
        label.setFont(new Font(38));
        label.setFill(Color.WHITE);
        label.setStroke(Color.BLACK);
        label.setStrokeWidth(1.1);
        
        scrollAbsorber = new Rectangle(WIDTH, HEIGHT, Color.TRANSPARENT);
        scrollAbsorber.setOnScroll((event) -> {
            if (event.getDeltaY() >= 4) {
                up.call();
            } else if (event.getDeltaY() <= -4) {
                down.call();
            }
        });
        scrollAbsorber.setOnMouseEntered((event) -> {label.setStroke(Color.GRAY);});
        scrollAbsorber.setOnMouseExited((event) -> {label.setStroke(Color.BLACK);});
        scrollAbsorber.setOnMousePressed((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                up.call();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                down.call();
            }
        });

        StackPane.setAlignment(label, alignment);
        StackPane.setMargin(label, new Insets(5, 10, 5, 10));
        StackPane.setAlignment(scrollAbsorber, alignment);

        getChildren().addAll(label, scrollAbsorber);
        setMaxSize(WIDTH, HEIGHT);
    }

    public void setText(String text) {
        label.setText(text);
    }
}
