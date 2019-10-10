package Controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class StartKlasse extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/View/MainMenu.fxml"));
        primaryStage.setTitle("MP3 Depot - Main Menu");
        primaryStage.setScene(new Scene(root, 600, 450));
        primaryStage.getIcons().add(new Image("/View/music.png"));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}