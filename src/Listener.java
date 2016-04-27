import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by math on 2016-04-27.
 */
public class Listener implements Runnable {
    private String line;
    private BufferedReader reader;
    public void run(){
        reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            while ((line = reader.readLine()) != null) {
                if (line.trim().toUpperCase().equals("Q")) break;
            }
            reader.close();
        }
        catch (IOException ioe){
            System.err.println(ioe.getMessage());
        }
    }
}
