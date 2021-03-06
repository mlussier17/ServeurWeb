import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ServiceWeb implements Runnable{

    //region Constantes
    private final String HEAD = "HEAD";
    private final String GET = "GET";
    private final File INDEX = new File("c:\\www\\index.html");
    private final int CODE_SUCCES_200 = 200;
    private final int CODE_ERROR_403 = 403;
    private final int CODE_ERROR_404 = 404;
    //region Constantes pour tokens
    private final int GET_NAME_REQUEST = 0;
    private final int GET_DOCUMENT_PATH = 1;
    //endregion
    //endregion
    //region Variables
    private Socket cSocket;

    public BufferedReader reader = null;
    public PrintWriter writeFichier = null;
    public PrintWriter writer = null;
    public BufferedInputStream read = null;
    public DataOutputStream sender = null;

    private String ligne = null;
    private String browser = null;
    private String document = null;
    private String[] tokens = null;

    private Date date;

    private File file = null;
    private File fileIndex = null;
    //endregion

    public ServiceWeb(Socket client, String path){
        document = path;
        cSocket = client;
        date = new Date();
    }

    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
            writeFichier = new PrintWriter(new BufferedWriter(new FileWriter("acces.txt", true)), true);

            try {
                writer = new PrintWriter(cSocket.getOutputStream(), true);

                getRequest(reader);
                tokens = ligne.split(" ");
                tokens[GET_NAME_REQUEST] = tokens[GET_NAME_REQUEST].toUpperCase();

                // Vérifier validité requete
                if (tokens[GET_NAME_REQUEST].equals(GET) || tokens[GET_NAME_REQUEST].equals(HEAD)) {
                    // S'il n'y a pas de paramètre par défaut envoyer vers index
                    if (tokens[GET_NAME_REQUEST].equals(GET) && tokens[GET_DOCUMENT_PATH].length() == 1)
                        get(tokens, INDEX, CODE_SUCCES_200);

                    // region If the request has parameters
                    else if (tokens.length > 1) {
                        file = new File(document + tokens[1]);

                        // Si le fichier existe et n'est pas un répertoire
                        if (file.exists() && !file.isDirectory()) {
                            if (tokens[GET_NAME_REQUEST].equals(HEAD)) head(tokens, file, CODE_SUCCES_200);
                            if (tokens[GET_NAME_REQUEST].equals(GET)) {
                                if (tokens[GET_DOCUMENT_PATH].length() > 2) get(tokens, file, CODE_SUCCES_200);
                            }
                        } else {
                            boolean showError403 = true;
                            if (!showIndexWindow() && ServeurWeb.getList()) showFiles(file, tokens);

                            // region Mesages d'erreurs
                            // Quand liste est false Error 403
                            else if (!ServeurWeb.getList()) {
                                tokens[1] = "/403.html";
                                showError(new File("403.html"), tokens, CODE_ERROR_404);
                                showError403 = false;
                            }
                            // Quand le fichier n'existe pas Error 404
                            else if (!ServeurWeb.getList() && !showError403){
                                tokens[1] = "/404.html";
                                showError(new File("404.html"), tokens, CODE_ERROR_404);
                            }
                            // endregion
                        }
                    }
                    //endregion
                } else {
                    error501();
                }
                fileAcces();
            } catch (IOException ioe) {
                System.err.println("Unexpected error");
            }
            finally {
                closeShitUp();
            }

        } catch (Exception e) {
            System.err.println("Client disconnected");
        }
        finally{
            ServeurWeb.connexions--;
        }
    }


    private void showFiles(File file, String [] tokens) {
        File[] listFichier = file.listFiles();
        if(listFichier != null) {
            try {
                head(tokens, file, CODE_ERROR_403);
                if(tokens[GET_NAME_REQUEST].equals(GET)) {
                    writer = new PrintWriter(cSocket.getOutputStream(), true);

                    writer.println("<html>");
                    writer.println("<body> \n");

                    writer.println("<h1>Index of " + file.toString() + "</h1> \n");

                    writer.println("<table style=\"width:100%\"> \n");

                    writer.println("<tr>");
                    writer.println("<td>Name</td>");
                    writer.println("<td>Last modified</td>");
                    writer.println("<td>Size</td>");
                    writer.println("<td>Description</td>");
                    writer.println("</tr> \n");

                    for (int i = 0; i < listFichier.length; ++i) {
                        writer.println("<tr>");
                        writer.println("<td><a href=\"" + tokens[GET_DOCUMENT_PATH] + "/" + listFichier[i].getName() + "\">" + listFichier[i].toString() + "</a></td><td>" + getLastModifiedDateRfc822(listFichier[i]) + "</td><td>" + listFichier[i].length() + "</td>");
                        writer.println("</tr>");
                    }

                    writer.println("\n</table>");
                    writer.println("</body>");
                    writer.println("</html>");

                    writer.close();
                }
            } catch (IOException e) {
                System.err.println("Impossible de recevoir la destination");
            }
        }
        else {
            tokens[1] = "/404.html";
            showError(new File("404.html"), tokens, CODE_ERROR_404);
        }
    }

    private void fileAcces() {
        writeFichier.println("IP: " + cSocket.getInetAddress());
        writeFichier.println("Date: " + getDateRfc822(date));
        writeFichier.println("Requete: " + ligne);
        writeFichier.println("Reponse: HTTP/1.0 200 OK");
        getBrowser(reader);
        writeFichier.println(browser);
        writeFichier.println("\n");
    }
    private void getBrowser(BufferedReader reader) {
        try {
            boolean pasFini = true;

            while (pasFini || (browser = reader.readLine()) != null) {
                if (browser.startsWith("User-Agent: ")) pasFini = false;
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
    private void getRequest(BufferedReader reader) {
        try {
            ligne = reader.readLine();

            //if (ligne != null)
                //System.out.println(ligne);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void showError(File file, String [] tokens, int codeError) {
        try {
            if (tokens[0].equals(HEAD)) head(tokens, file, codeError);
            else get(tokens, file, codeError);
        }
        catch (FileNotFoundException fnfe){
            System.err.println(file + " fichier introuvable");
        }
    }
    private void error501() {
        writer.println("HTTP 501 Not implemented");
        writer.println();
    }
    private boolean showIndexWindow() {
        boolean indexRepertoryExist = false;

        if (tokens[1].length() > 1) {
            if (tokens[1].endsWith("/"))
                fileIndex = new File(document + tokens[1] + "index.html");
            else if (tokens[1].endsWith(""))
                fileIndex = new File(document + tokens[1] + "/index.html");
        }

        if(tokens[1]. length() > 1 && fileIndex.exists()) {
            try {
                tokens[1] += fileIndex;
                if (tokens[GET_NAME_REQUEST].equals(HEAD)) head(tokens, fileIndex, CODE_SUCCES_200);
                if (tokens[GET_NAME_REQUEST].equals(GET)) {
                    if (tokens[GET_DOCUMENT_PATH].length() > 2)
                        get(tokens, fileIndex, CODE_SUCCES_200);
                }
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
            }
            indexRepertoryExist = true;
        }

        return indexRepertoryExist;
    }

    private void head(String[] tokens, File file, int codeError){
        try{
            //region Success
            if (codeError == CODE_SUCCES_200) {
                writer.println("HTTP/1.0 200 OK");
                writer.println("Server: Bulletproof Corporation, CEO:Joaquin, Bitch:Mathieu");
                writer.println("Date: " + getDateRfc822(date));

                if (tokens[GET_DOCUMENT_PATH].endsWith("/")) writer.println("Content-type: text/html");
                else writer.println("Content-type: " + checkContentExtension(tokens));

                writer.println("Last-modified: " + getLastModifiedDateRfc822(file));
                writer.println("Content-length: " + file.length());
            }
            //endregion
            //region Errors
            else if (codeError == CODE_ERROR_403) writer.println("HTTP/1.0 403 UNAUTHORIZED ACCESS.");
            else if (codeError == CODE_ERROR_404) writer.println("HTTP/1.0 404 THE REQUESTED DOCUMENT WAS NOT FOUND.");
            //endregion
            writer.println();
        }
        catch (Exception e){
            System.err.println("Impossible de recevoir la destination");
        }
    }
    private void get(String[] tokens, File file, int codeError) throws FileNotFoundException{
        try {
            head(tokens,file, codeError);
            sender = new DataOutputStream(new BufferedOutputStream(cSocket.getOutputStream()));
            read = new BufferedInputStream(new FileInputStream(file));
            byte[] b = new byte[4096];
            int i = 0;
            while((i = read.read(b,0,4096)) != -1) {
                sender.write(b);
            }
        }
        catch (FileNotFoundException fnfe){
            System.err.println(file + " fichier introuvable");
        }
        catch(IOException ioe){
            System.err.println("Unexpected Error");
        }
    }

    public String checkContentExtension(String[] tokens){
        if (tokens[GET_DOCUMENT_PATH].toLowerCase().endsWith(".html")) return new String ("text/html");
        else if (tokens[GET_DOCUMENT_PATH].toLowerCase().endsWith(".txt"))return new String ("text/plain");
        else if (tokens[GET_DOCUMENT_PATH].toLowerCase().endsWith(".gif"))return new String ("image/gif");
        else if (tokens[GET_DOCUMENT_PATH].toLowerCase().endsWith(".jpg") || tokens[GET_DOCUMENT_PATH].toLowerCase().endsWith(".jpeg"))return new String ("image/jpeg");
        else if (tokens[GET_DOCUMENT_PATH].toLowerCase().endsWith(".png")) return new String ("image/png");
        else return new String("Les extensions de fichiers possible sont .html, .txt, .gif, .jpg, .jpeg, ou .png");
    }

    public void closeShitUp() {
        try {
            read.close();
            sender.close();
            writeFichier.close();
            writer.close();
            reader.close();
            cSocket.close();
        } catch (IOException e) {
            System.err.println("Couldn't close correctly");
        }
    }
    public String getDateRfc822(Date date) {
        SimpleDateFormat formatRfc822 = new SimpleDateFormat(
                "EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'z", Locale.CANADA );
        return formatRfc822.format(date);
    }
    public String getLastModifiedDateRfc822(File file) {
        SimpleDateFormat formatRfc822 = new SimpleDateFormat(
                "EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'z", Locale.CANADA );
        return formatRfc822.format(file.lastModified());
    }
}