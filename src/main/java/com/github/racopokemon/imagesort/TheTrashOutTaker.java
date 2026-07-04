package com.github.racopokemon.imagesort;


import javafx.stage.Stage;

import java.awt.Desktop;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class TheTrashOutTaker {
    /**
     * Moves the /delete folder to system trash (if supported and theres such a folder)
     * Shows an alert if not successful (the you need to delete manually, you'll manage)
     */
    public static void takeOutTheTrash(Stage stage, File containingDir) {
        File trashFolder = new File(containingDir, "delete");
        if (!trashFolder.exists() || !trashFolder.isDirectory()) {
            return;
        }

        if (!java.awt.Desktop.isDesktopSupported() ||
            !java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.MOVE_TO_TRASH)) {
            return;
        }

        //String timestamp = LocalDateTime.now()
        //    .format(DateTimeFormatter.ofPattern(" yyyy_MM_dd-HH_mm_ss"));
        //File renamed = new File(containingDir, "deleted " + containingDir.getName() + timestamp);
        File renamed = new File(containingDir, JobDelete.generateFolderName("delete"));

        String errorMessage = null;
        try {
            Files.move(trashFolder.toPath(), renamed.toPath());
        } catch (Exception e) {
            errorMessage = "Failed to rename the temporary 'delete' folder before moving it to system trash. "+
            "You may manually delete it yourself. \n\nHere is the error message: \n\n" + e.toString();
        }

        if (errorMessage == null) {
            try {
                boolean moved = Desktop.getDesktop().moveToTrash(renamed);
                if (!moved) {
                    errorMessage = "Could not move the temporary 'delete' folder to system trash. " +
                    "The folder remains as '" + renamed.getName() +"' in your directory, you may manually delete it.";
                }
            } catch (Exception e) {
                errorMessage = "Error while moving the temporary 'delete' folder to system trash. You may manually delete the directory '" + renamed.getName() + "'. \n\nThis is the error message: \n\n"+e.toString();
            }
        }

        if (errorMessage != null) {
            Alert alert = new Alert(AlertType.NONE, errorMessage, ButtonType.OK);
            alert.setTitle("Cannot take out trash");
            alert.initOwner(stage);
            alert.showAndWait();
        }
        return;
    }
}
