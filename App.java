import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashMap;

/**
 * IoT Server
 *
 * @author David W. Arnold
 * @version 30/10/2019
 */
public class App
{
    private static HashMap<String, String> bleDeviceNames = new HashMap<>();
    private static HashMap<String, String> living_roomMap = new HashMap<>();
    private static HashMap<String, String> bedroomMap = new HashMap<>();
    private static HashMap<String, String> kitchenMap = new HashMap<>();

    public static void main(String[] args) throws SQLException
    {
        // Serial Port Setup
        SerialPort commPort = SerialPort.getCommPort("/dev/tty.usbserial-A9Z2T81O");
        commPort.setBaudRate(9600);
        commPort.openPort();
        commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        // JDBC connection to my MySQL database
        Connection conn = connectToDatabase();

        // SELECT * FROM iot
        assert conn != null;
        populateMaps(conn);

        // Reading from Serial Port
        try (BufferedReader in = new BufferedReader(new InputStreamReader(commPort.getInputStream()))) {
            while (true) {
                System.out.println(in.readLine());
            }
        } catch (Exception e) {
            System.err.println("** No Serial Input **");
        }

        // Close JDBC connection to my MySQL database
        conn.close();

        // Close Serial Port
        commPort.closePort();
    }

    private static Connection connectToDatabase()
    {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance(); // Step 1: "Load" the JDBC driver
            String url = "jdbc:mysql://dragon.kent.ac.uk:3306/dwa4"; // Step 2: Establish the connection to the database
            return DriverManager.getConnection(url, "dwa4", "i9sipin");
        } catch (Exception e) {
            System.err.println("** Cannot connect to database **");
        }
        return null;
    }

    private static void populateMaps(Connection conn) throws SQLException
    {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM iot");
        while (rs.next()) {
            String bleMacAddress = rs.getString("mac");
            bleDeviceNames.put(bleMacAddress, rs.getString("name"));
            living_roomMap.put(bleMacAddress, rs.getString("living_room"));
            bedroomMap.put(bleMacAddress, rs.getString("bedroom"));
            kitchenMap.put(bleMacAddress, rs.getString("kitchen"));
        }
        rs.close();
        stmt.close();
    }

//    // The Shed - example of getting the String representation of the source code at a URL
//    public static void light()
//    {
//        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL("https://www.google.com").openConnection().getInputStream()))) {
//            String line;
//            while ((line = in.readLine()) != null) {
//                System.out.println(line);
//            }
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
}
