import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Future;

public class ServeurWeb {
    //("\\config.txt") on console ------- ("\\src\\config.txt") on IjIdea
    private static final String DEFAULT_CONFIG = ("\\src\\config.txt");

    // region Variables
    private static int portNumber;
    private static String rootPath;
    private static String indexFile;
    public static Boolean list;
    private static int connNumber;
    private ServerSocket sSocket;
    private boolean run = true;
    public Thread t;
    private static String currentDirectory;
    private static File projectpath;
    private static File configFile;
    private static File configPath;
    private BufferedReader reader;
    public static int connexions = 0;
    // endregion

    public static void main (String args[]){
        //region Zero Parameter
        if(args.length == 0) {
            setUpConfig();
            if (configPath.exists()) {
                new ServeurWeb().readConfig(configPath, false, false, args);
                new ServeurWeb().connection();
            } else {
                System.err.println("Configuration file not found");
                System.exit(1);
            }
        }
        //endregion

        //region One Parameter
        if (args.length == 1){
            try {
                 int port = Integer.parseInt(args[0]);
                if (port > 0 && port < 65535) {
                    setUpConfig();
                    if (configPath.exists()) {
                            new ServeurWeb().readConfig(configPath, true, false, args);
                            new ServeurWeb().connection();
                    } else {
                        System.err.println("Configuration file not found");
                        System.exit(1);
                    }
                } else {
                    System.err.println(port + " is an invalid port. Port range(1, 65535)");
                    System.exit(1);
                }
            }
            catch (NumberFormatException nfe){
                System.err.println("Port must be numbers");
                System.exit(1);
            }
        }
        //endregion

        //region Two Parameters
        if (args.length == 2){
            try {
                int port = Integer.parseInt(args[0]);
                File directory = new File(args[1]);
                if (port > 0 && port < 65535) {
                    setUpConfig();
                    if (configPath.exists()) {
                        if (directory.isDirectory() && directory.exists()) {
                            new ServeurWeb().readConfig(configPath, true, true, args);
                            new ServeurWeb().connection();
                        }
                        else{
                            System.err.println(directory + " directory doesn't exist or is not a directory");
                            System.exit(1);
                        }
                    } else {
                        System.err.println("Configuration file not found");
                        System.exit(1);
                    }
                } else {
                    System.err.println(port + " Invalid port. Range(1, 65535)");
                    System.exit(1);
                }
            }
            catch (NumberFormatException nfe){
                System.err.println("Port must be numbers");
                System.exit(1);
            }
        }
        //endregion
    }

    public static void setUpConfig(){
        try {
            projectpath = new File(".");
            configFile = new File(DEFAULT_CONFIG);
            currentDirectory = projectpath.getCanonicalPath() + configFile;
            configPath = new File(currentDirectory);
        }
        catch (IOException ioe) {
            System.err.println("Can't retrieve the project path.");
            System.exit(1);
        }

    }

    public void connection(){
        try{
            sSocket = new ServerSocket(portNumber);
            reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Port = " + portNumber);
            System.out.println("Directory = " + rootPath);
            System.out.println("Server online waiting for request ...");
            System.out.println("Connexions accpeted = " + connNumber);
            Listener listener = new Listener();
            Thread worker = new Thread(listener);
            worker.start();

            while(worker.isAlive()){
                Socket cSocket = sSocket.accept();
                if(connexions == connNumber || !worker.isAlive())cSocket.close();
                if(connexions < connNumber && worker.isAlive()) {
                    System.out.println("Client connected.");
                    ServiceWeb conn = new ServiceWeb(cSocket, rootPath);
                    t = new Thread(conn);
                    t.start();
                    connexions++;
                }
            }
            sSocket.close();
            System.out.println("Server is closed");
        }
        catch (BindException be){
            System.err.println("Port is already in use by another program.");
            System.exit(1);
        }
        catch(IOException ioe){
            System.err.println("Unexpected error on the server");
            System.exit(1);
        }
    }

    public void readConfig(File config, Boolean port, Boolean path, String[] args){
        String line;
        String[] tokens;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(config));

            while((line = reader.readLine()) != null){
                tokens = line.split("=");
                switch(tokens[0]){
                    case "port":
                        portNumber = port ? Integer.parseInt(args[0]) : Integer.parseInt(tokens[1]);
                        break;
                    case "root":
                        File check = path ? new File(args[1]) : new File(tokens[1]);
                        if (check.exists())
                            rootPath = path ? args[1] : tokens[1];
                        else {
                            System.err.println("Specified directory doesn't exist");
                            System.exit(1);
                        }
                        break;
                    case "index":
                        indexFile = tokens[1];
                        break;
                    case "list":
                        list = tokens[1].equals("true");
                        break;
                    case "connexion":
                        connNumber = Integer.parseInt(tokens[1]);
                        break;
                    default:
                        break;
                }
            }
        }
        catch (IOException ioe){
            System.err.println("Unable to read the config file.");
            System.exit(1);
        }
    }

    public static boolean getList(){return list;}
}
