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
    public static void main(String[] args)
    {
//        System.out.println(System.getProperty("java.library.path"));

        Communicator main = new Communicator();
        main.initialize();
        Thread t = new Thread() {
            public void run() {
                //the following line will keep this app alive for 1000 seconds,
                //waiting for events to occur and responding to them (printing incoming messages to console).
                try {Thread.sleep(1000000);} catch (InterruptedException ie) {}
            }
        };
        t.start();
        System.out.println("Started");



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
