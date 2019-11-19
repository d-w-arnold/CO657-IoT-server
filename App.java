import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author David W. Arnold
 * @version 30/10/2019
 */
public class App
{
    public static void main(String[] args) throws IOException
    {
        SerialPort comPort = SerialPort.getCommPort("/dev/tty.usbserial-A9Z2T81O");
        comPort.setBaudRate(9600);
        comPort.openPort();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(comPort.getInputStream()))) {
            while (true)
            {
                System.out.println(in.readLine());
            }
        } catch (Exception e) { e.printStackTrace(); }
        comPort.closePort();

//        light();
    }

    public static void light() {
        try(BufferedReader in = new BufferedReader(new InputStreamReader(new URL("https://www.google.com").openConnection().getInputStream()))) {
            String line;
            while( (line=in.readLine()) != null ) {
                System.out.println(line);
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }
}
