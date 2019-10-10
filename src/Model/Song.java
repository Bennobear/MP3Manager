package Model;

/* Song Object Class*/
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return (this.getYear() + "\t\t" + this.artist + "\t\t" + this.album + "\t\t" + this.title);
    }

}