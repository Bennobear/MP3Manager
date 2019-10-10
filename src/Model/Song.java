package Model;

public class Song {

    private String artist;
    private String year;
    private String album;
    private String title;
    private String path;

    public Song() {

    }

    public Song(String artist, String year, String album, String title, String path) {
        this.artist = artist;
        this.year = year;
        this.album = album;
        this.title = title;
        this.path = path;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    @Override
    public String toString() {
        return (this.getYear() + "\t\t\t" + this.artist + "\t\t\t" + this.album + "\t\t\t" + this.title);
    }

}