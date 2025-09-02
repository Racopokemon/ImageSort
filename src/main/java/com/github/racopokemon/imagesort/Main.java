package com.github.racopokemon.imagesort;

import javafx.application.Application;
import javafx.stage.Stage;
import openize.heic.decoder.HeicImage;
import openize.io.IOFileStream;
import openize.io.IOMode;

import java.io.IOException;

//import org.im4java.core.*;

public class Main extends Application {
    
    public static void main(String[] args) {
        System.out.println("imglib");

//        // create command
//        ConvertCmd cmd = new ConvertCmd();
//
//        // create the operation, add images and operators/options
//        IMOperation op = new IMOperation();
//        op.addImage("myimage.jpg");
//        op.resize(800,600);
//        op.addImage("myimage_small.jpg");
//
//        // execute the operation
//        try {
//            cmd.run(op);
//        } catch (IOException | InterruptedException | IM4JavaException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }


        try (IOFileStream fs = new IOFileStream("filename.heic", IOMode.READ))
        {
            HeicImage image = HeicImage.load(fs);
            int[] pixels = image.getInt32Array(openize.heic.decoder.PixelFormat.Argb32);
        }


        System.out.println("imgsort");
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        new Launcher().start(stage);
    }
}