import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBConnection {

    public static void main(String[] args) {
        System.setProperty("user.timezone", "UTC");
       String url ="jdbc:postgresql://localhost:5434/sql_visualizer?options=-c%20TimeZone=UTC";
        String user = "postgres";
        String password = "postgres";

        try {
            Connection conn = DriverManager.getConnection(url, user, password);

            System.out.println("Database Connected Successfully!");

            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT * FROM students");

            while (rs.next()) {
                System.out.println(
                        rs.getInt("id") + " "
                        + rs.getString("name") + " "
                        + rs.getInt("marks"));
            }

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}