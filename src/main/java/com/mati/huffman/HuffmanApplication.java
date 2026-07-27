package com.mati.huffman;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HuffmanApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HuffmanApplication.class.getResource("ui/main-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 1180, 800);
        stage.setTitle("Huffman Visualizer");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }
}
