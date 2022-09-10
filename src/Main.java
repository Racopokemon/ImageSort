import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Optional;

import javafx.application.Application;
import javafx.event.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

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
    private static final int IMAGE_BUFFER_SIZE = 4; //this many images before, and also this many after the current are already being loaded

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
        imageBuffer = new Hashtable<>();

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
                } else if (event.getButton() == MouseButton.SECONDARY) {
                    //Desktop.getDesktop().browseFileDirectory(<file>) would be better, cross platform, but requires java 9 and im too lazy to install now
                    if (System.getProperty("os.name").startsWith("Windows")) {
                        try {
                            Runtime.getRuntime().exec("explorer.exe /select," + getFullPathForImage(currentImage));
                        } catch (IOException e) {
                            System.out.println("Could not show file " + currentImage + " in explorer:");
                            e.printStackTrace();
                        }
                    }
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
        label.setStrokeWidth(1.1);
        StackPane.setAlignment(label, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(label, new Insets(5, 10, 5, 5));
        root.getChildren().add(label);

        Scene scene = new Scene(root, 800, 600);
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        primaryStage.setScene(scene);
        primaryStage.maximizedProperty().addListener((observable) -> {if (primaryStage.isMaximized()) primaryStage.setFullScreen(true);});
        primaryStage.setOnCloseRequest((event) -> {
            Alert closeAlert = new Alert(AlertType.NONE, "Move all files now before closing?\n" +
            "'No' keeps the files unchanged, but discards your work here.",
            ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            closeAlert.setHeaderText("Move files now?");
            Optional<ButtonType> result = closeAlert.showAndWait();
            if (!result.isPresent() || result.get() == ButtonType.CANCEL) {
                //prevent close
                event.consume();
            } else if (result.get() == ButtonType.YES) {
                //rename (closes automatically on return)
                moveAllFiles();
                new Alert(AlertType.NONE, "Consider that other file types (videos) might also be in this folder.", ButtonType.OK).showAndWait();
            }
        });
        primaryStage.show();

        //DirectoryChooser ch = new DirectoryChooser();
        //directory = ch.showDialog(primaryStage);
        directory = new File("D:\\Mein Terrarium\\Bilder\\Fotos\\2022_09 Hartzerwoche in Aachen - Kopie");
        delDirectory = new File(directory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + "delete");

        view.requestFocus();

        updateFilesList();
        
        loadImage();

        Alert useInfo = new Alert(AlertType.NONE, null, ButtonType.OK);
        useInfo.setHeaderText("How to use");
        useInfo.setContentText(
            "Arrow keys to look through images and change target folder. \n"+
            "Del or Backspace to instantly move to a 'delete' folder. \n"+
            "Click to zoom. Right click to show in explorer. \n"+
            "Close the window to apply all moves (with confirmation).\n"
        );
        useInfo.showAndWait();

        //ToDo: Dont do this automatically, have two modes
        primaryStage.setFullScreen(true);
    }

    private void loadImage() {
        //Step 1: We update the imageBuffer, which essentially does the actual image loading
        //If nothing has changed, thats no problem at all, but if it has, its already launched here

        int currentImageIndex = getCurrentImageIndex();
        Hashtable<String, RotatedImage> newImageBuffer = new Hashtable<>();

        for (int cursor = -IMAGE_BUFFER_SIZE; cursor <= IMAGE_BUFFER_SIZE; cursor++) {
            int actualIndex = (currentImageIndex + cursor) % images.size();
            if (actualIndex < 0) {
                actualIndex += images.size();
            }
            String imageNameAtCursor = images.get(actualIndex);
            if (imageBuffer.containsKey(imageNameAtCursor)) {
                newImageBuffer.put(imageNameAtCursor, imageBuffer.get(imageNameAtCursor));
            } else {
                newImageBuffer.put(imageNameAtCursor, new RotatedImage(new File (getFullPathForImage(imageNameAtCursor))));
            }
        }
        imageBuffer = newImageBuffer; //this also discards all images only on the old imageBuffer

        ///Even though we have this buffering and everything runs in background, it sometimes still lags for a sec until we get the next pic. 
        ///I think this comes from something I cant influence, since it also happens with pics that should be constantly loaded already 
        ///and the pictures dont show up from black, but just late. maybe the images are loaded onto the gpu for hardware acc rendering or so.

        //Step 2: the imageBuffer also contains the current image. If it was just added, or already
        //preloaded before, or still loading, who cares. We just show it! (If its still loading, it will show once its ready)
        RotatedImage img = imageBuffer.get(currentImage);
        view.setImage(img);
        
        boolean ninetyDegrees = false;
        switch (img.getOrientation()) {
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
            imageCategory.remove(currentImage);
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

    //The final call, that actually executes all the move operations. 
    //Right now it is only possible to do this on close (which makes sense, after that the program would close anyway)
    private void moveAllFiles() {
        for (String key : imageCategory.keySet()) {
            int category = imageCategory.get(key);
            if (category != 0) {
                //if folder doesnt exist: create. 
                String dirPath = directory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + category;
                File dir = new File(dirPath);
                if (!dir.exists()) {
                    try {
                        dir.mkdir();
                    } catch (Exception e) {
                        System.out.println("Could not create folder for category " + category);
                        e.printStackTrace();
                    }
                }
                File origin = new File(getFullPathForImage(key));
                File dest = new File(dirPath + FileSystems.getDefault().getSeparator() + key);
                try {
                    origin.renameTo(dest);
                } catch (Exception e) {
                    System.out.println("Could not move file " + key + " to category folder "+ category);
                    e.printStackTrace();
                }
            }
        }
    }
}
