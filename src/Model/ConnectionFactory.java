package Model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/* Handles Database connection*/
public class ConnectionFactory {
    private static ConnectionFactory connectionFactory = null;
    String driverClassName = "com.mysql.cj.jdbc.Driver";
    String connectionUrl = "jdbc:mysql://localhost:3306/mp3manager";
    String dbUser = "java";
    String dbPwd = "snoopy";

    private ConnectionFactory() {
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static ConnectionFactory getInstance() {
        if (connectionFactory == null) {
            connectionFactory = new ConnectionFactory();
        }
        return connectionFactory;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = null;
        conn = DriverManager.getConnection(connectionUrl, dbUser, dbPwd);
        return conn;
    }
}
