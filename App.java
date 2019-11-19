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
        light();
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
