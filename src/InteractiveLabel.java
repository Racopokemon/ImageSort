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

    private Text label;
    private Rectangle scrollAbsorber;

    private double unhoverOpacity = 1.0;
    private boolean hovered = false;
    
    public InteractiveLabel (double textSize, double width, Pos alignment, Action up, Action down) {

        double height = 70;

        label = new Text("bottom text");
        label.setFont(new Font(textSize));
        label.setFill(Color.WHITE);
        label.setStroke(Color.BLACK);
        label.setStrokeWidth(1.1);
        
        scrollAbsorber = new Rectangle(width, height, Color.TRANSPARENT);
        scrollAbsorber.setOnScroll((event) -> {
            if (event.getDeltaY() >= 4) {
                up.call();
            } else if (event.getDeltaY() <= -4) {
                down.call();
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
                up.call();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                down.call();
            }
        });

        StackPane.setAlignment(label, alignment);
        StackPane.setMargin(label, new Insets(5, 10, 5, 10));
        StackPane.setAlignment(scrollAbsorber, alignment);

        getChildren().addAll(label, scrollAbsorber);
        setMaxSize(width, height);
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
