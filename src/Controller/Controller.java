package Controller;

import Model.ConnectionDAO;
import Model.Song;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/* Main Working Class */

public class Controller implements Initializable {
    /* Get Dao access & the Mediaplayer ready */
    ConnectionDAO dao = new ConnectionDAO();
    MediaPlayer mediaPlayer;

    /* MainMenu */
    @FXML
    Button btnInput;
    @FXML
    Button btnOutput;

    /* Input */
    @FXML
    TextField txtInput;
    @FXML
    Button btnSearch;

    /* Output */
    @FXML
    ListView<Song> listSongs = new ListView<>();

    @FXML
    Button btnPlay = new Button();

    @FXML
    Button btnPause = new Button();

    /* Input & Output */
    @FXML
    Button btnBack;

    /* Refreshes the ListView everytime the Window changes */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listSongs.getItems().addAll(dao.findAll());
        btnPlay.disableProperty().bind(Bindings.isEmpty(listSongs.getSelectionModel().getSelectedItems()));
        btnPause.setDisable(true);
    }


    /* Play the selected Song with the push of a Button */
    @FXML
    private void playMusic() {
        String path = listSongs.getSelectionModel().getSelectedItem().getPath();
        Media hit = new Media(new File(path).toURI().toString());
        btnPause.setDisable(false);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer = new MediaPlayer(hit);
            mediaPlayer.play();
        } else {
            mediaPlayer = new MediaPlayer(hit);
            mediaPlayer.play();
        }
    }

    /* Pause or Resume the Song (Resumes at the right time) */
    @FXML
    private void pauseMusic() {
        MediaPlayer.Status currentStatus = mediaPlayer.getStatus();
        if (currentStatus == MediaPlayer.Status.PLAYING){
            mediaPlayer.pause();
            mediaPlayer.setOnPaused(() -> System.out.println("Paused at: " + mediaPlayer.getCurrentTime()));
        }
        else if (currentStatus == MediaPlayer.Status.PAUSED || currentStatus == MediaPlayer.Status.STOPPED) {
            System.out.println("Player will start at: " + mediaPlayer.getCurrentTime());
            mediaPlayer.play();
        }
    }

    /* Change Stage to Input */
    @FXML
    private void openInput() {
        try {
            Stage oldStage = (Stage) btnInput.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/View/Input.fxml"));
            Parent root1 = fxmlLoader.load();
            Stage stage = new Stage();
            stage.setTitle("MP3 Depot - Input");
            stage.setScene(new Scene(root1, 600, 450));
            stage.getIcons().add(new Image("/View/music.png"));
            oldStage.hide();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Change Stage to Output */
    @FXML
    private void openOutput() {
        try {
            Stage oldStage = (Stage) btnOutput.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/View/Output.fxml"));
            Parent root1 = fxmlLoader.load();
            Stage stage = new Stage();
            stage.setTitle("MP3 Depot - Output");
            stage.setScene(new Scene(root1, 600, 450));
            stage.getIcons().add(new Image("/View/music.png"));
            oldStage.hide();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Searches the path the user wrote in the textField for any MP3s if they have MetaData they'll be added to the Database */
    @FXML
    private void searchD() throws InvalidDataException, IOException, UnsupportedTagException {
        File folder = new File(txtInput.getText());
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            File file = listOfFiles[i];
            if (file.isFile() && file.getName().endsWith(".mp3")) {
                Mp3File mp3file = new Mp3File(folder + "\\" + file.getName());
                if (mp3file.hasId3v1Tag()) {
                    ID3v1 id3v1Tag = mp3file.getId3v1Tag();
                    dao.add(id3v1Tag.getYear(), id3v1Tag.getArtist(), id3v1Tag.getAlbum(), id3v1Tag.getTitle(), folder + "\\" + file.getName());
                } else System.out.println("No Metadata");
            }
        }
    }

    /* Change Scene to MainMenu */
    @FXML
    private void getBack() {
        try {
            Stage oldStage = (Stage) btnBack.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/View/MainMenu.fxml"));
            Parent root1 = fxmlLoader.load();
            Stage stage = new Stage();
            stage.setTitle("MP3 Depot - Main Menu");
            stage.setScene(new Scene(root1, 600, 450));
            stage.getIcons().add(new Image("/View/music.png"));
            oldStage.hide();
            stage.show();
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
