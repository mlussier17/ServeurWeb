import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by 196128636 on 2016-04-01.
 */
public class ServeurWeb {
    // region Constantes
    private final int POS_PORT = 1;
    private final int POS_REPERTOIRE = 2;
    private static final int DEFAULT_PORT = 80;
    private static final String DEFAULT_PATH = ("c:\\www");
    // endregion
    // region Variables
    private ServerSocket sSocket;
    private boolean run = true;
    private static int port;
    private static String file = null;
    public Thread t;
    // endregion

    public static void main (String args[]){
        if(args.length == 0){ new ServeurWeb().connection(DEFAULT_PORT, DEFAULT_PATH); }

        if (args.length == 1){
            try {
                port = Integer.parseInt(args[0]);
                if (port > 0 && port < 65535) {
                    new ServeurWeb().connection(Integer.parseInt(args[0]), DEFAULT_PATH);
                }
                else throw new NumberFormatException();
            }
            catch (NumberFormatException nfe){ System.err.println("Port invalide"); }
        }

        if (args.length == 2){
            try{
                port = Integer.parseInt(args[0]);
                if (port > 0 && port < 65535) {
                    file = args[1];
                    new ServeurWeb().connection(Integer.parseInt(args[0]), file);
                }
                else throw new NumberFormatException();
            }
            catch (NumberFormatException nfe){ System.err.println("Port invalide"); }
        }
    }

    public void connection(int port, String path){
        try{
            sSocket = new ServerSocket(port);
            System.out.println("Port = " + port);
            System.out.println("Dossier = " + path);
            System.out.println("Serveur en attente");

            while(run){
                Socket cSocket = sSocket.accept();
                System.out.println("Client connecte");
                ServiceWeb conn = new ServiceWeb(cSocket, path);
                t = new Thread(conn);
                t.start();
            }
            sSocket.close();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
}
