import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

/**
 * IoT Server
 *
 * @author David W. Arnold
 * @version 30/10/2019
 */
public class App
{
    final private static int baudRate = 9600;
    final private static String commPortPath = "/dev/tty.usbserial-A9Z2T81O";
    final private static String lightIPAddress = "10.150.46.108";
    final private static String apiKey = "70617373776f7264";
    private static HashMap<String, String> bleDeviceNames = new HashMap<>();
    private static HashMap<String, Light> living_roomMap = new HashMap<>();
    private static HashMap<String, Light> bedroomMap = new HashMap<>();
    private static HashMap<String, Light> kitchenMap = new HashMap<>();

    public static void main(String[] args) throws SQLException, IOException
    {
        // Serial Port Setup
        SerialPort commPort = SerialPort.getCommPort(commPortPath);
        commPort.setBaudRate(baudRate);
        commPort.openPort();
        commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        // JDBC connection to my MySQL database
        Connection conn = connectToDatabase();

        // SELECT * FROM iot
        assert conn != null;
        populateMaps(conn);

        // Send all BLE Device MAC addresses and BLE Device Names
        Set<String> bleDevices = bleDeviceNames.keySet();
        for (String bleDevice : bleDevices) {
            String bleDeviceInfoToSend = bleDevice + "+" + bleDeviceNames.get(bleDevice) + "*";
            commPort.writeBytes(bleDeviceInfoToSend.getBytes(), bleDeviceInfoToSend.length());
        }

        // Reading from Serial Port
        try (BufferedReader in = new BufferedReader(new InputStreamReader(commPort.getInputStream()))) {
            Light currentState = getLight();
            while (true) {
                String bleMACAddress = in.readLine();
                if (Objects.equals(currentState, Light.OFF)) {
                    if (!Objects.equals(currentState, living_roomMap.get(bleMACAddress))) {
                        currentState = setLight(Light.TOGGLE);
                    }
                }
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
            living_roomMap.put(bleMacAddress, Light.values()[rs.getInt("living_room")]);
            bedroomMap.put(bleMacAddress, Light.values()[rs.getInt("bedroom")]);
            kitchenMap.put(bleMacAddress, Light.values()[rs.getInt("kitchen")]);
        }
        rs.close();
        stmt.close();
    }

    private static Light getLight() throws IOException
    {
        URL url = new URL("http://10.150.46.108/api/relay/0?apikey=70617373776f7264");
        URLConnection yc = url.openConnection();
        int res = -1;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()))) {
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
        return res < 0 ? null : Light.values()[res];
    }

    private static Light setLight(Light value) throws IOException
    {
        // curl -X PUT -H "Accept: application/json" http://10.150.46.108/api/relay/0 --data "apikey=70617373776f7264&value=2"
        String parameters = "apikey=" + apiKey + "&" + "value=" + value.ordinal();
        String url = "http://" + lightIPAddress + "/api/relay/0";
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
        return res < 0 ? null : Light.values()[res];
    }

    public enum Light
    {OFF, ON, TOGGLE}
}
