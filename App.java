import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
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
    final private static String lightIPAddress = "10.150.46.108";
    final private static String apiKey = "70617373776f7264";
    private static HashMap<String, String> bleDeviceNames = new HashMap<>();
    private static HashMap<String, String> living_roomMap = new HashMap<>();
    private static HashMap<String, String> bedroomMap = new HashMap<>();
    private static HashMap<String, String> kitchenMap = new HashMap<>();

    public static void main(String[] args) throws SQLException, IOException
    {
//        System.out.println(getLight());
        setLight(2);

//        // Serial Port Setup
//        SerialPort commPort = SerialPort.getCommPort("/dev/tty.usbserial-A9Z2T81O");
//        commPort.setBaudRate(9600);
//        commPort.openPort();
//        commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
//
//        // JDBC connection to my MySQL database
//        Connection conn = connectToDatabase();
//
//        // SELECT * FROM iot
//        assert conn != null;
//        populateMaps(conn);
//
//        // Send all BLE Device MAC addresses and BLE Device Names
//        Set<String> bleDevices = bleDeviceNames.keySet();
//        for (String bleDevice : bleDevices) {
//            String bleDeviceInfoToSend = bleDevice + "+" + bleDeviceNames.get(bleDevice) + "*";
//            commPort.writeBytes(bleDeviceInfoToSend.getBytes(), bleDeviceInfoToSend.length());
//        }
//
//        // Reading from Serial Port
//        try (BufferedReader in = new BufferedReader(new InputStreamReader(commPort.getInputStream()))) {
//            while (true) {
//                System.out.println(in.readLine());
//
//            }
//        } catch (Exception e) {
//            System.err.println("** No Serial Input **");
//        }
//
//        // Close JDBC connection to my MySQL database
//        conn.close();
//
//        // Close Serial Port
//        commPort.closePort();
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

    private static int getLight() throws IOException
    {
        // curl -H "Accept: application/json" http://10.150.46.108/api/relay/0?apikey=70617373776f7264
        String url = "http://"+ lightIPAddress + "/api/relay/0?apikey="+ apiKey;
        String[] command = {"curl", "-H", "Accept: application/json", url};
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        int res = -1;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    res = Integer.parseInt(line); // Sort this out
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        process.destroy();
        return res;
    }

    private static int setLight(int value) throws IOException
    {
        // curl -X PUT -H "Accept: application/json" http://10.150.46.108/api/relay/0 --data "apikey=70617373776f7264&value=2"
        String parameters = "apikey="+ apiKey + "&" + "value=" + value;
        String url = "http://"+ lightIPAddress + "/api/relay/0";
        String[] command = {"curl", "-X", "PUT", "H", "Accept: application/json", url, "--data", parameters};
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        int res = -1;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    res = Integer.parseInt(line);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        process.destroy();
        return res;
    }
}
