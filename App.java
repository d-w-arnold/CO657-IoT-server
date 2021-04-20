import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
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
    private static final int NEW_READ_TIMEOUT = 3000;
    private static final String LIGHT_IP_ADDRESS = "10.150.46.108";
    private static final String API_KEY = "70617373776f7264";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("(dd/MM/yyyy) HH:mm:ss");
    private static final Pattern VALID_IPV6_PATTERN = Pattern.compile("[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]", Pattern.CASE_INSENSITIVE);
    // Zigbee
    private static final int BAUD_RATE = 9600;
    private static final String COMM_PORT_PATH = "/dev/tty.usbserial-A9Z2T81O";
    // Direct, no Zigbee
    // private static final int baudRate = 115200;
    // private static final String commPortPath = "/dev/tty.usbserial-1410";
    private static final HashMap<String, String> BLE_DEVICE_NAMES = new HashMap<>();
    private static final HashMap<String, Light> LIVING_ROOM_MAP = new HashMap<>();
    private static final HashMap<String, Light> BEDROOM_MAP = new HashMap<>();
    private static final HashMap<String, Light> KITCHEN_MAP = new HashMap<>();
    private static boolean infoSent = false;

    public static void main(String[] args) throws SQLException
    {
        // Serial Comm Port Setup
        SerialPort commPort = SerialPort.getCommPort(COMM_PORT_PATH);
        commPort.setBaudRate(BAUD_RATE);
        commPort.openPort();
        commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, NEW_READ_TIMEOUT, 0);
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
                                String timestamp = SIMPLE_DATE_FORMAT.format(new Timestamp(System.currentTimeMillis()));
                                System.out.println(timestamp + " - Welcome home! " + BLE_DEVICE_NAMES.get(bleMACAddress));
                                try {
                                    if (sendPingRequest()) {
                                        Light currentState = getLight();
                                        if (Objects.equals(currentState, Light.OFF)) {
                                            if (!Objects.equals(currentState, LIVING_ROOM_MAP.get(bleMACAddress))) {
                                                setLight(Light.TOGGLE);
                                            }
                                        }
                                    }
                                } catch (IOException ignored) {
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
        for (String bleDevice : BLE_DEVICE_NAMES.keySet()) {
            String bleDeviceInfoToSend = bleDevice + "+" + BLE_DEVICE_NAMES.get(bleDevice) + "*";
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
            // Step 1: "Load" the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
            // Step 2: Establish the connection to the database
            String url = "jdbc:mysql://dragon.kent.ac.uk:3306/dwa4";
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
            BLE_DEVICE_NAMES.put(bleMacAddress, rs.getString("name"));
            LIVING_ROOM_MAP.put(bleMacAddress, Light.values()[rs.getInt("living_room")]);
            BEDROOM_MAP.put(bleMacAddress, Light.values()[rs.getInt("bedroom")]);
            KITCHEN_MAP.put(bleMacAddress, Light.values()[rs.getInt("kitchen")]);
        }
        rs.close();
        stmt.close();
    }

    // Sends ping request to a provided IP address
    private static boolean sendPingRequest() throws IOException
    {
        InetAddress geek = InetAddress.getByName(App.LIGHT_IP_ADDRESS);
        System.out.println("Sending Ping request to smart home light: " + App.LIGHT_IP_ADDRESS);
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
            if (!BLE_DEVICE_NAMES.containsKey(bleDevice)) {
                rs.close();
                stmt.close();
                return true;
            }
            bleDevices.add(bleDevice);
        }
        if (bleDevices.size() != BLE_DEVICE_NAMES.size()) {
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
        BLE_DEVICE_NAMES.clear();
        LIVING_ROOM_MAP.clear();
        BEDROOM_MAP.clear();
        KITCHEN_MAP.clear();
    }

    private static Light getLight() throws IOException
    {
        URL url = new URL("http://10.150.46.108/api/relay/0?apikey=70617373776f7264"); // The Shed smart light URL
        URLConnection yc = url.openConnection();
        int res = getResponse(yc.getInputStream());
        return res < 0 ? null : Light.values()[res];
    }

    private static void setLight(Light value) throws IOException
    {
        // curl -X PUT -H "Accept: application/json" http://10.150.46.108/api/relay/0 --data "apikey=70617373776f7264&value=2"
        String parameters = "apikey=" + API_KEY + "&" + "value=" + value.ordinal();
        String url = "http://" + LIGHT_IP_ADDRESS + "/api/relay/0";
        String[] command = {"curl", "-X", "PUT", "H", "Accept: application/json", url, "--data", parameters};
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        getResponse(process.getInputStream());
        process.destroy();
    }

    private static int getResponse(InputStream inputStream)
    {
        int res = -1;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
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
        return res;
    }

    public enum Light
    {OFF, ON, TOGGLE}
}