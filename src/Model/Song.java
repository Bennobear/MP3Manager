package Model;

public class Song {

    private  String artist;
    private  String year;
    private  String album;
    private  String title;

    public Song(){

    }

    public Song(String artist, String year, String album, String title) {
        this.artist = artist;
        this.year = year;
        this.album = album;
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public String getYear() {
        return year;
    }

    public String getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }


}