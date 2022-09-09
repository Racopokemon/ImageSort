import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import javafx.application.Application;
import javafx.event.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application {

    private File directory; 
    private File delDirectory;
    private ArrayList<String> images = new ArrayList<>();
    private FilenameFilter filter;
    private String currentImage = "";

    private Hashtable<String, Integer> imageCategory;
    private Hashtable<String, RotatedImage> imageBuffer; //holds the current image and some images before and after it, updated with every loadImage. 
    //we use here that javaFX images already load in the background and provide an empty image until they finished loading, so just creating  and 
    //updating references to the (probably still loading) images is enough. 

    private ImageView view;
    private StackPane root;
    private Text label;

    public static void main(String[] args) {        
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        filter = new FilenameFilter() {
            @Override
            public boolean accept(File f, String name) {
                String[] split = name.split("[.]");
                String suffix = split[split.length-1];
                return (suffix.equalsIgnoreCase("jpg") || 
                        suffix.equalsIgnoreCase("jpeg"));
                //I see the advantages of python and probably other high level languages ...
                //where you would have a ends with ignore case ...
                //or could access the last element of split in one line [-1] or .last (maybe c#)
                //... this is too much writing for something trivial
            }
        };
        imageCategory = new Hashtable<>();

        primaryStage.setTitle("Image Sort");

        // listen to del / space (move to folders each)
            // store current file
            // pick next file, show it
            // move file to folder (if not exist, create)
            // update list

        root = new StackPane();
        view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setCache(true);
        root.getChildren().add(view);

        root.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.RIGHT) {
                    nextImage();
                } else if (event.getCode() == KeyCode.LEFT) {
                    prevImage();
                } else if (event.getCode() == KeyCode.UP) {
                    incrementCurrentImageCategory();
                } else if (event.getCode() == KeyCode.DOWN) {
                    decrementCurrentImageCategory();
                } else if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
                    deleteImage();
                }
            }
        });
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    view.setViewport(new Rectangle2D(0, 0, 500, 500));
                }
            }
        });
        root.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    view.setViewport(null);
                }
            }
        });
        label = new Text("bottom text");
        label.setFont(new Font(38));
        label.setFill(Color.WHITE);
        label.setStroke(Color.BLACK);
        label.setStrokeWidth(1.5);
        StackPane.setAlignment(label, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(label, new Insets(5, 10, 5, 5));
        root.getChildren().add(label);


        Scene scene = new Scene(root, 800, 600);
        scene.setFill(Color.BLACK);
        primaryStage.setScene(scene);
        primaryStage.maximizedProperty().addListener((observable) -> {if (primaryStage.isMaximized()) primaryStage.setFullScreen(true);});
        primaryStage.show();

        //DirectoryChooser ch = new DirectoryChooser();
        //directory = ch.showDialog(primaryStage);
        directory = new File("D:\\Mein Terrarium\\Bilder\\Fotos\\2022_09 Hartzerwoche in Aachen - Kopie");
        delDirectory = new File(directory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + "delete");

        view.requestFocus();

        updateFilesList();
        
        nextImage();

        //ToDo: Dont do this automatically, have two modes
        primaryStage.setFullScreen(true);
    }

    private void loadImage() {
        // loading itself is simple
        File img = new File (getFullPathForImage(currentImage));
        view.setImage(new Image(img.toURI().toString(), false));
        // but for weird reasons javafx does not support the exif orientation flag, which my cam already uses by 2014. 
        // (and which is also automatically used by windows in all previews and viewers)
        // ... so we have an external lib for it, and adapt the behavior of the image view to it
        
        boolean ninetyDegrees = false;
        switch (orientation) {
            case 1: case 2:
                view.setRotate(0);
                ninetyDegrees = false;
                break;
            case 3: case 4:
                view.setRotate(180);
                ninetyDegrees = false;
                break;
            case 5: case 6:
                view.setRotate(90);
                ninetyDegrees = true;
                break;
            case 7: case 8:
                view.setRotate(-90);
                ninetyDegrees = true;
                break;
            default:
                view.setRotate(0);
                ninetyDegrees = false;
                break;
        }
        view.fitHeightProperty().unbind();
        view.fitWidthProperty().unbind();
        if (ninetyDegrees) {
            view.fitWidthProperty().bind(root.heightProperty());
            view.fitHeightProperty().bind(root.widthProperty());
        } else {
            view.fitWidthProperty().bind(root.widthProperty());
            view.fitHeightProperty().bind(root.heightProperty());
        }

        updateLabel();
    }

    private void updateLabel() {
        if (!imageCategory.containsKey(currentImage)) {
            imageCategory.put(currentImage, 0);
        }
        int index = imageCategory.get(currentImage);
        if (index == 0) {
            label.setText("keep");
        } else {
            label.setText("move to "+index);
        }
    }
    private void incrementCurrentImageCategory() {
        int current = imageCategory.get(currentImage);
        if (++current > 3) {
            current = 0;
        }
        imageCategory.put(currentImage, current);
        updateLabel();
    }
    private void decrementCurrentImageCategory() {
        int current = imageCategory.get(currentImage);
        if (--current < 0) {
            current = 3;
        }
        imageCategory.put(currentImage, current);
        updateLabel();    
    }

    private int getCurrentImageIndex() {
        int index = images.indexOf(currentImage);
        if (index == -1) {
            if (images.isEmpty()) {
                handleNoImages();
            } else {
                currentImage = images.get(0);
                return 0;
            }
        }
        return index;
    }

    //Updates currentFile and shows the image
    private void nextImage() {
        int index = getCurrentImageIndex();
        if (++index >= images.size()) {
            index = 0;
        }
        currentImage = images.get(index);
        loadImage();
    }

    //Updates currentFile and shows the image
    private void prevImage() {
        int index = getCurrentImageIndex();
        if (--index < 0) {
            index = images.size() - 1;
        }
        currentImage = images.get(index);
        loadImage();
    }

    private void deleteImage() {
        try {
            //if folder doesnt exist, create it
            if (!delDirectory.exists()) {
                delDirectory.mkdir();
            }
            //move file there
            String path = getFullPathForImage(currentImage);
            File origin = new File(path);
            File dest = new File(delDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + currentImage);
            origin.renameTo(dest);
        } catch (Exception e) {
            System.out.println("Could not move image "+currentImage+"to 'deleted' folder: ");
            e.printStackTrace();
        }

        nextImage();
        updateFilesList();
    }

    private String getFullPathForImage(String image) {
        return directory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + image;
    }

    private void updateFilesList() {
        String sep = FileSystems.getDefault().getSeparator();
        //files.addAll(directory.list(filter)); //again, this should work. 
        //files = new ArrayList<String>(directory.list(filter)); //or this
        images.clear();
        String[] newDir = directory.list(filter);
        Collections.addAll(images, newDir);
        Collections.sort(images); //My directory was sorted already, but idk if its always like that, also on other OS
    }

    private void handleNoImages() {
        //ToDo put a real handling here..
        new Alert(AlertType.INFORMATION, "No images in the directory. Closing now.").showAndWait();
        System.exit(0);
    }
}
