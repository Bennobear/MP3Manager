package Model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ConnectionDAO {
    Connection connection = null;
    PreparedStatement ptmt = null;
    ResultSet resultSet = null;
    private List<Song> songList;
    public ConnectionDAO() {
    }

    private Connection getConnection() throws SQLException {
        Connection conn;
        conn = ConnectionFactory.getInstance().getConnection();
        return conn;
    }


    public void add(String year, String artist , String album, String title) {
        try {
            String i = null;
            String queryString = "INSERT INTO songs(id, year, artist, album, title) VALUES(?,?,?,?,?)";
            connection = getConnection();
            ptmt = connection.prepareStatement(queryString);
            ptmt.setString(1, i);
            ptmt.setString(2, year);
            ptmt.setString(3,artist);
            ptmt.setString(4, album);
            ptmt.setString(5, title);
            ptmt.executeUpdate();
            System.out.println("Data Added Successfully");
        } catch (SQLException e) {
            System.out.println(e);
        } finally {
            try {
                if (ptmt != null)
                    ptmt.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                System.out.println(e);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public List<Song> findAll() {
        try {
            String queryString = "SELECT * FROM songs";
            connection = getConnection();
            ptmt = connection.prepareStatement(queryString);
            resultSet = ptmt.executeQuery();
            while (resultSet.next()) {
                Song s = new Song();
                s.setYear(resultSet.getString("year"));
                s.setArtist(resultSet.getString("artist"));
                s.setAlbum(resultSet.getString("album"));
                s.setTitle(resultSet.getString("title"));
                songList.add(s);
            }
        } catch (SQLException e) {
            System.out.println(e);
        } finally {
            try {
                if (resultSet != null)
                    resultSet.close();
                if (ptmt != null)
                    ptmt.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                System.out.println(e);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        return songList;
    }
}
