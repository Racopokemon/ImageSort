import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.event.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class Main extends Application {

    private File directory; 
    private File delDirectory;
    private ArrayList<String> images = new ArrayList<>();
    private ArrayList<String> allImages = new ArrayList<>();
    private FilenameFilter filenameFilter;

    private String currentImage = null;
    private String lastImageManuallySelected = null;
    private int filter = -1; //-1: No filter. 0: Keep only. 1-3: Only this category. 

    private Hashtable<String, Integer> imageCategory; //images are only added once seen. All not contained images are expected to have category 0. Iterate over allImages for all images instead. 
    private Hashtable<String, RotatedImage> imageBuffer; //holds the current image and some images before and after it, updated with every loadImage. 
    //we use here that javaFX images already load in the background and provide an empty image until they finished loading, so just creating  and 
    //updating references to the (probably still loading) images is enough. 

    private static final int IMAGE_BUFFER_SIZE_MIN = 1; //setting it to 0 ... woah. Well never get to probe the RAM, and if some images load sometimes its still an improvement
    private static final int IMAGE_BUFFER_SIZE_MAX = 4;
    private int imageBufferSize = IMAGE_BUFFER_SIZE_MAX; //this many images before, and also this many after the current are already being loaded
    private int successfullLoadsWithoutMemoryProblems = 0; //we constantly probe if we exceed the memory limits by counting up here, and may increase the buffer

    private double zoom = 2.9;
    private boolean isZooming = false;
    private double mouseRelativeX, mouseRelativeY;
    private static final double MIN_ZOOM = 1.15;
    private static final double MAX_ZOOM = 10;

    private ImageView view;
    private StackPane zoomPane;
    private StackPane root;
    private Text label;
    private static final Color HALF_TRANSPARENT = new Color(1, 1, 1, 0.08);

    private ProgressIndicator progress;
    private Label errorLabel;

    public static void main(String[] args) {        
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        filenameFilter = new FilenameFilter() {
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

        stage.setTitle("Image Sort");

        // listen to del / space (move to folders each)
            // store current file
            // pick next file, show it
            // move file to folder (if not exist, create)
            // update list

        zoomPane = new StackPane();
        view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setCache(true);
        zoomPane.getChildren().add(view);

        zoomPane.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) {
                    nextImage();
                } else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) {
                    prevImage();
                } else if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.W) {
                    incrementCurrentImageCategory();
                } else if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.S) {
                    decrementCurrentImageCategory();
                } else if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
                    deleteImage();
                } else if (event.getCode() == KeyCode.PLUS) {
                    increaseZoom(40);
                } else if (event.getCode() == KeyCode.MINUS) {
                    decreaseZoom(40);
                } else if (event.getCode() == KeyCode.Q) {
                    previousFilter();
                } else if (event.getCode() == KeyCode.E) {
                    nextFilter();
                }
                //else if (event.getCode() == KeyCode.ESCAPE) {
                //    if (!stage.isFullScreen()) {
                //        stage.fireEvent(new WindowEvent(stage,WindowEvent.WINDOW_CLOSE_REQUEST));
                //    }
                //}
            }
        });
        zoomPane.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    setMousePosition(event);
                    zoomIn();
                    event.consume();
                }
                //now theres a fancy context menu for it
                //else if (event.getButton() == MouseButton.SECONDARY) {
                //    showInExplorer();
                //}
            }//
        });
        zoomPane.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                if (isZooming) {
                    if (event.getDeltaY() > 0) {
                        increaseZoom(event.getDeltaY());
                    } else if (event.getDeltaY() < 0) {
                        decreaseZoom(-event.getDeltaY());
                    }
                } else {
                    if (event.getDeltaY() >= 4) {
                        prevImage();
                    } else if (event.getDeltaY() <= -4) {
                        nextImage();
                    }
                }
                event.consume();
            }
        });
        zoomPane.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    setMousePosition(event);
                    zoomIn();
                }
            }
        });
        zoomPane.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    zoomOut();
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
        
        Rectangle scrollAbsorber = new Rectangle(200, 70, Color.TRANSPARENT);
        StackPane.setAlignment(scrollAbsorber, Pos.BOTTOM_RIGHT);
        scrollAbsorber.setOnScroll((event) -> {
            if (event.getDeltaY() >= 4) {
                incrementCurrentImageCategory();
            } else if (event.getDeltaY() <= -4) {
                decrementCurrentImageCategory();
            }
        });
        scrollAbsorber.setOnMouseEntered((event) -> {label.setStroke(Color.GRAY);});
        scrollAbsorber.setOnMouseExited((event) -> {label.setStroke(Color.BLACK);});
        scrollAbsorber.setOnMousePressed((event) -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                incrementCurrentImageCategory();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                decrementCurrentImageCategory();
            }
        });
        
        errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);
        errorLabel.setFont(new Font(15));
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        progress = new ProgressIndicator(0.2);
        progress.setMaxSize(50, 50);
        progress.setVisible(false);
        
        MenuItem menuShowFile = new MenuItem("Show in explorer");
        menuShowFile.setOnAction((event) -> {showInExplorer();});
        MenuItem menuDelete = new MenuItem("Move to '/delete'");
        menuDelete.setOnAction((event) -> {deleteImage();});
        
        Rectangle invisibleContextMenuSource = new Rectangle();
        invisibleContextMenuSource.setVisible(false);
        ContextMenu contextMenu = new ContextMenu(menuShowFile, menuDelete);
        view.setOnContextMenuRequested((event) -> {
            contextMenu.show(invisibleContextMenuSource, event.getScreenX(), event.getScreenY());
            //note that we set invisibleContextMenuSource as anchor and not the view. This is because the context menu hides when
            //the ancor loses focus, and while zooming and scrolling on the view it doesnt so the context menu stays
            //so this is a slight hack, just use anything else thats probably not even visible that will instantly lose focus
        });
        contextMenu.setAutoHide(true);
        
        root = new StackPane();
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        root.getChildren().add(invisibleContextMenuSource);
        root.getChildren().add(progress);
        root.getChildren().add(errorLabel);
        root.getChildren().add(zoomPane);

        new LRButton(root, true);
        new LRButton(root, false);
        
        root.getChildren().add(label);
        root.getChildren().add(scrollAbsorber);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.maximizedProperty().addListener((observable) -> {if (stage.isMaximized()) stage.setFullScreen(true);});
        stage.setOnCloseRequest((event) -> {
            boolean unsavedChanges = false;
            for (Map.Entry<String, Integer> entry : imageCategory.entrySet()) {
                if (entry.getValue() != 0) {
                    unsavedChanges = true;
                    break;
                }
            }
            if (unsavedChanges) {
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
            }
        });
        stage.show();

        DirectoryChooser ch = new DirectoryChooser();
        directory = ch.showDialog(stage);
        //directory = new File("D:\\Mein Terrarium\\Bilder\\Art\\barasui");
        delDirectory = new File(directory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + "delete");

        view.requestFocus();

        updateFilesList();
        
        updateFilter(); // includes a loadImage() call

        Alert useInfo = new Alert(AlertType.NONE, null, ButtonType.OK);
        useInfo.setHeaderText("How to use");
        useInfo.setContentText(
            "Arrow keys or WASD to look through images and change target folder (keep in current or move to \\1, \\2 or \\3). \n"+
            "Del or Backspace to instantly move to a 'delete' folder. \n"+
            "Right click to show in explorer. Click to zoom. \n"+
            "Scroll, + and - to change the zoom strength. \n"+
            "Close the window to perform the file operations (you will be asked for confirmation).\n\n"+
            "Find this project on github.com/Racopokemon/ImageSort"
        );
        useInfo.showAndWait();

        //ToDo: Dont do this automatically, have two modes
        stage.setFullScreen(true);
    }

    private class LRButton extends Rectangle {
        private boolean left;
        public LRButton(StackPane root, boolean left) {
            super(100, 10, Color.TRANSPARENT);
            this.left = left;
            StackPane.setAlignment(this, left ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
            heightProperty().bind(root.heightProperty());
            root.getChildren().add(this);
            setOnMouseEntered((event) -> {setFill(HALF_TRANSPARENT);});
            setOnMouseExited((event) -> {setFill(Color.TRANSPARENT);});
            setOnMouseClicked((event) -> {
                if (left) {
                    prevImage();
                } else {
                    nextImage();
                }
            });
        }
    }

    // Zooms into the whole scene (the easy way, just setting the scale properties)
    private void zoomIn() {
        double width = zoomPane.getWidth();
        double height = zoomPane.getHeight();
        double spaceX = (width*zoom) - (width);
        double spaceY = (height*zoom) - (height);
        double relTransX = -(mouseRelativeX - 0.5);
        double relTransY = -(mouseRelativeY - 0.5);

        zoomPane.setScaleX(zoom);
        zoomPane.setScaleY(zoom);

        zoomPane.setTranslateX(spaceX * relTransX);
        zoomPane.setTranslateY(spaceY * relTransY);

        zoomPane.setCursor(Cursor.NONE);
        isZooming = true;
    }
    // Resets the zoom to the usual 1:1 in the app
    private void zoomOut() {
        zoomPane.setScaleX(1);
        zoomPane.setScaleY(1);
        zoomPane.setTranslateX(0);
        zoomPane.setTranslateY(0);

        zoomPane.setCursor(Cursor.DEFAULT);
        isZooming = false;
    }
    private void increaseZoom(double scale) {
        zoom *= Math.pow(1.01, scale);
        if (zoom > MAX_ZOOM) {
            zoom = MAX_ZOOM;
        }
        if (isZooming) {
            zoomIn();
        }
    }
    private void decreaseZoom(double scale) {
        zoom /= Math.pow(1.01, scale);
        if (zoom < MIN_ZOOM) {
            zoom = MIN_ZOOM;
        }
        if (isZooming) {
            zoomIn();
        }
    }
    //Sets the mouse position from the given MouseEvent. 
    //We store it externally and dont simply pass it over when zooming in,
    //because when the zoom (scale) changes, we don't get new information on
    //the mouse, but need to update the zoom based on the mouse position
    private  void setMousePosition(MouseEvent event) {
        mouseRelativeX = event.getSceneX() / zoomPane.getWidth();
        mouseRelativeY = event.getSceneY() / zoomPane.getHeight();
        mouseRelativeX = Math.max(Math.min(mouseRelativeX, 1), 0);
        mouseRelativeY = Math.max(Math.min(mouseRelativeY, 1), 0);
    }

    private void showInExplorer() {
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

    private ChangeListener<? super Number> numberListener = (a, b, c) -> {updateImageStatus();};
    private ChangeListener<? super Boolean> booleanListener = (a, b, c) -> {updateImageStatus();};
    private void loadImage() {
        //Preprocessing: Checking all currently contained images for loading errors due to memory limitations, and mitigation
        ArrayList<String> entriesToDelete = new ArrayList<>();
        for (Map.Entry<String, RotatedImage> entry : imageBuffer.entrySet()) {
            Exception e = entry.getValue().getException();
            if (e != null && isMemoryException(e)) {
                entriesToDelete.add(entry.getKey());
            }
        }
        if (!entriesToDelete.isEmpty()) {
            //Memory errors occured
            for (String s : entriesToDelete) {
                imageBuffer.remove(s);
            }
            handleMemoryError();
            successfullLoadsWithoutMemoryProblems--;
        } else {
            if (imageBufferSize == 0 || (imageBuffer.containsKey(currentImage) && imageBuffer.get(currentImage).getHeight() > 0)) {
                //height > 0 means the image has finished loading
                if (imageBufferSize < IMAGE_BUFFER_SIZE_MAX && ++successfullLoadsWithoutMemoryProblems > 4) {
                    imageBufferSize++;
                    successfullLoadsWithoutMemoryProblems = 0;
                    System.out.println("Incremented imageBufferSize to " + imageBufferSize);
                }
            }
        }
        //End preprocessing. ... never had these issues on my machine, for reconstruction i limited the jvm heap space

        //Step 1: We update the imageBuffer, which essentially does the actual image loading
        //If nothing has changed, thats no problem at all, but if it has, its already launched here

        int currentImageIndex = getCurrentImageIndex();
        Hashtable<String, RotatedImage> newImageBuffer = new Hashtable<>();

        for (int cursor = -imageBufferSize; cursor <= imageBufferSize; cursor++) {
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
        //initially we were cool and didnt have loop, but so it really help when scrolling rapidly through tie pics.
        for (Map.Entry<String, RotatedImage> e : imageBuffer.entrySet()) {
            if (!newImageBuffer.containsKey(e.getKey())) {
                //will be garbage collected. Lets stop the thread manually.
                e.getValue().cancel();
            }
        }
        imageBuffer = newImageBuffer; //this also discards all images only on the old imageBuffer 

        ///Even though we have this buffering and everything runs in background, it sometimes still lags for a sec until we get the next pic. 
        ///I think this comes from something I cant influence, since it also happens with pics that should be constantly loaded already 
        ///and the pictures dont show up from black, but just late. maybe the images are loaded onto the gpu for hardware acc rendering or so.

        //Step 2: the imageBuffer also contains the current image. If it was just added, or already
        //preloaded before, or still loading, who cares. We just show it! (If its still loading, it will show once its ready)
        RotatedImage img = imageBuffer.get(currentImage);
        Image oldImage = view.getImage();
        //unregister old image listeners (what if theyre still called?.. okay change the img once i guess and its fixed)
        if (oldImage != null) {
            oldImage.progressProperty().removeListener(numberListener);
            oldImage.heightProperty().removeListener(numberListener);
            oldImage.errorProperty().removeListener(booleanListener);
        }

        if (!img.isError() && img.getHeight() == 0) {
            //means: still loading, need to register listeners because the view might change
            img.progressProperty().addListener(numberListener);
            img.heightProperty().addListener(numberListener);
            img.errorProperty().addListener(booleanListener);
        }
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
        updateImageStatus();
    }
    
    private void updateImageStatus() {
        Image img = view.getImage();
        if (img.getHeight() < 0) {
            //image already loaded
            progress.setVisible(false);
            errorLabel.setVisible(false);
        } else if (img.isError()) {
            //error while loading
            progress.setVisible(false);
            errorLabel.setVisible(true);
            if (isMemoryException(img.getException())) {
                errorLabel.setText("Sadly, we could not load " + currentImage + " because there is not enough memory.\n"+
                "We are already decreasing the number of preloaded images to counter this, so that you hopfeully \n" +
                "won't see this message again. ");
                handleMemoryError();
                loadImage();
            } else {
                errorLabel.setText("We could not load " + currentImage + ": \n" + img.getException().getLocalizedMessage());
                System.out.println("Error occured when loading image " + currentImage);
                img.getException().printStackTrace();
            }
        } else {
            //just still loading
            progress.setVisible(true);
            errorLabel.setVisible(false);
            progress.setProgress(img.getProgress());
        }
    }

    private boolean isMemoryException(Exception e) {
        return e.getMessage().startsWith("java.lang.OutOfMemoryError");
    }

    //On machines with small amounts of RAM (?) and with big photos, 
    //loading 4+4+1 images quickly runs into the heap space limitations. 
    //We check for this issue every time a new image is shown as well as when the current image runs into this error during loading. 
    //Then this method is called to decrease the buffer number. 
    //This does however not reload any images or refresh the buffer, that needs to be done in addition to calling here. 
    private void handleMemoryError() {
        if (--imageBufferSize < IMAGE_BUFFER_SIZE_MIN) {
            imageBufferSize = IMAGE_BUFFER_SIZE_MIN;
        }
        System.out.println("Reduced imageBufferSize to "+imageBufferSize);
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
    private void nextFilter() {
        if (++filter > 3) {
            filter = -1;
        }
        updateFilter();
    }
    private void previousFilter() {
        if (--filter < -1) {
            filter = 3;
        }
        updateFilter();
    }

    private void updateFilter() {
        //load subset of images, store it. Thats the obvious part. 
        images.clear();
        if (filter == -1) {
            //no filter
            images.addAll(allImages);
        } else {
            for (String i : allImages) {
                Integer category = imageCategory.get(i);
                if (category == null) {
                    category = 0;
                }
                if (category == filter) {
                    images.add(i);
                }
            }
        }

        //actually, this place is the only one where no images can occur, and also the only place where there are images again. 
        //so we do all the basic organization around it
        if (images.isEmpty()) {
            //hide < and > bars
            currentImage = null;
        } else {
            //show < and > bars
        }
        //What if the subset is empty? Then there is no image selected, this will cause bugs if we dont search for it in detail. 
        //how do we even store that there is no image. null string or ""?
            //getCurrentImageIndex already has a handleNoImages(); call, this needs to be changed
        //In general, which image do we start with? From the cursor the first to the left or right? What if there is nothing to the left? --> go to the right
        //We also need to recache the images (as usual, the already preloaded ones will survive)
        //another TODO: if we open a dir and there are no pics, still show the error and exit. 

        //do we need this call here? 
        //does it stand showing the same image without reloading it?
        //what about the fact that the call could occur any time (esp after deleting)
        //rn im saying yes, replacing loadImage in the init with this call. 
        loadImage();
    }

    //in images, not allImages. 
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
        lastImageManuallySelected = currentImage;
        loadImage();
    }

    //Updates currentFile and shows the image
    private void prevImage() {
        int index = getCurrentImageIndex();
        if (--index < 0) {
            index = images.size() - 1;
        }
        currentImage = images.get(index);
        lastImageManuallySelected = currentImage;
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
        //String sep = FileSystems.getDefault().getSeparator();
        //files.addAll(directory.list(filter)); //again, this should work. 
        //files = new ArrayList<String>(directory.list(filter)); //or this
        allImages.clear();
        String[] newDir = directory.list(filenameFilter);
        Collections.addAll(allImages, newDir);
        Collections.sort(allImages); //My directory was sorted already, but idk if its always like that, also on other OS
        updateFilter();
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
