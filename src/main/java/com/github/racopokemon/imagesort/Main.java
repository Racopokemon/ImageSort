package com.github.racopokemon.imagesort;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    
    public static void main(String[] args) {
        System.out.println(System.getProperty("os.name"));
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        new Launcher().start(stage);
    }

}