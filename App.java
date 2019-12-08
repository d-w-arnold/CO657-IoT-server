import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IoT Server
 *
 * @author David W. Arnold
 * @version 30/10/2019
 */
public class App
{
    final private static int newReadTimeout = 3000;
    final private static String lightIPAddress = "10.150.46.108";
    final private static String apiKey = "70617373776f7264";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("(dd/MM/yyyy) HH:mm:ss");
    private static final Pattern VALID_IPV6_PATTERN = Pattern.compile("[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]", Pattern.CASE_INSENSITIVE);
    // Zigbee
    final private static int baudRate = 9600;
    final private static String commPortPath = "/dev/tty.usbserial-A9Z2T81O";
    // Direct, no Zigbee
//    final private static int baudRate = 115200;
//    final private static String commPortPath = "/dev/tty.usbserial-1410";
    private static HashMap<String, String> bleDeviceNames = new HashMap<>();
    private static HashMap<String, Light> living_roomMap = new HashMap<>();
    private static HashMap<String, Light> bedroomMap = new HashMap<>();
    private static HashMap<String, Light> kitchenMap = new HashMap<>();
    private static boolean infoSent = false;

    public static void main(String[] args) throws SQLException, IOException
    {
        // Serial Comm Port Setup
        SerialPort commPort = SerialPort.getCommPort(commPortPath);
        commPort.setBaudRate(baudRate);
        commPort.openPort();
        commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, newReadTimeout, 0);
        System.out.println("** Serial Comm Port Setup Complete **");

        // JDBC connection to my MySQL database
        Connection conn = connectToDatabase();

        // SELECT * FROM iot
        assert conn != null;
        populateMaps(conn);
        System.out.println("** BLE Device Info retrieved from Database **");

        // Send all BLE Device MAC addresses and BLE Device Names
        sendAll(commPort);

        // Reading from Serial Port
        try (BufferedReader in = new BufferedReader(new InputStreamReader(commPort.getInputStream()))) {
            String bleMACAddress;
            while (true) {
                try {
                    if (commPort.getInputStream().available() > 0) {
                        String readLine = in.readLine();
//                        System.out.println("Readline from IoT Device : " + readLine);
                        if (readLine == null) {
                            continue;
                        }
//                        if (readLine.contains("info")) {
//                            sendAll(commPort);
//                        }
                        if (infoSent) {
                            bleMACAddress = checkIsIPv6MACAddress(readLine);
                            if (!bleMACAddress.equals("")) {
                                System.out.println("\nIoT Device has detected the BLE Device: " + bleMACAddress);
                                String timestamp = sdf.format(new Timestamp(System.currentTimeMillis()));
                                System.out.println(timestamp + " - Welcome home! " + bleDeviceNames.get(bleMACAddress));
                                try {
                                    if (sendPingRequest(lightIPAddress)) {
                                        Light currentState = getLight();
                                        if (Objects.equals(currentState, Light.OFF)) {
                                            if (!Objects.equals(currentState, living_roomMap.get(bleMACAddress))) {
                                                setLight(Light.TOGGLE);
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                }
                            }
                        }
                    }
                } catch (SerialPortTimeoutException se) {
//                    System.out.println("SerialPortTimeoutException ..."); // TODO Remove after testing
                }
                if (requireUpdatesFromDatabase(conn)) {
                    emptyMaps();
                    populateMaps(conn);
                    System.out.println("** Updated BLE Device Info retrieved from Database **");
                }
            }
        } catch (Exception e) {
            System.err.println("** No Serial Input **");
            e.printStackTrace();
        }

        // Close JDBC connection to my MySQL database
        conn.close();

        // Close Serial Port
        commPort.closePort();
    }

    private static void sendAll(SerialPort commPort)
    {
        for (String bleDevice : bleDeviceNames.keySet()) {
            String bleDeviceInfoToSend = bleDevice + "+" + bleDeviceNames.get(bleDevice) + "*";
            commPort.writeBytes(bleDeviceInfoToSend.getBytes(), bleDeviceInfoToSend.length());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("** Sent BLE Device Info to IoT Device **");
        infoSent = true;
    }

    // Check for BLE IPv6 MAC Address
    private static String checkIsIPv6MACAddress(String input)
    {
        Matcher m2 = VALID_IPV6_PATTERN.matcher(input);
        boolean result = m2.find();
        if (result) {
//            System.out.println("** Found IPv6 Address **");
            return m2.group(0);
        } else {
//            System.out.println("** Incorrect IPv6 Address **");
            return "";
        }
    }

    private static Connection connectToDatabase()
    {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance(); // Step 1: "Load" the JDBC driver
            String url = "jdbc:mysql://dragon.kent.ac.uk:3306/dwa4"; // Step 2: Establish the connection to the database
            Connection conn = DriverManager.getConnection(url, "dwa4", "i9sipin");
            System.out.println("** Connected to Database **");
            return conn;
        } catch (Exception e) {
            System.err.println("** Cannot connect to Database **");
            System.exit(1);
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

    // Sends ping request to a provided IP address
    private static boolean sendPingRequest(String ipAddress) throws UnknownHostException, IOException
    {
        InetAddress geek = InetAddress.getByName(ipAddress);
        System.out.println("Sending Ping request to smart home light: " + ipAddress);
        if (geek.isReachable(2000)) {
            System.out.println("Smart home light is reachable\n");
            return true;
        } else {
            System.out.println("Sorry, smart home light is NOT reachable\n");
            return false;
        }
    }

    private static boolean requireUpdatesFromDatabase(Connection conn) throws SQLException
    {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM iot");
        ArrayList<String> bleDevices = new ArrayList<>();
        while (rs.next()) {
            String bleDevice = rs.getString("mac");
            if (!bleDeviceNames.containsKey(bleDevice)) {
                rs.close();
                stmt.close();
                return true;
            }
            bleDevices.add(bleDevice);
        }
        if (bleDevices.size() != bleDeviceNames.size()) {
            rs.close();
            stmt.close();
            return true;
        }
        rs.close();
        stmt.close();
        return false;
    }

    private static void emptyMaps()
    {
        bleDeviceNames.clear();
        living_roomMap.clear();
        bedroomMap.clear();
        kitchenMap.clear();
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