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
    //endregion
    //region Variables
    private Socket cSocket;
    public BufferedReader reader= null;
    public PrintWriter writer= null;
    public BufferedInputStream read= null;
    public DataOutputStream sender = null;
    private String ligne = null;
    private String document = null;
    private Date date;
    private File file = null;
    private File fileIndex = null;
    private boolean fileExist = false;
    String[] tokens = null;
    //endregion

    public  ServiceWeb(Socket client, String path){
        document = path;
        cSocket = client;
        date = new Date();
    }

    public void run(){
        try {
            reader = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));

            try {
                writer = new PrintWriter(cSocket.getOutputStream(), true);
                ligne = reader.readLine();

                if (ligne != null)
                    System.out.println(ligne);

                tokens = ligne.split(" ");
                if(tokens[0].equals(GET) || tokens[0].equals(HEAD)) {
                    if (tokens[0].equals(GET) && tokens[1].length() == 1) get(tokens, INDEX);

                    else if (tokens.length > 1) {
                        file = new File(document + tokens[1]);
                        if (tokens[1].endsWith("/"))
                            fileIndex = new File(document + tokens[1] + "index.html");
                        else if (tokens[1].endsWith(""))
                            fileIndex = new File(document + tokens[1] + "/index.html");


                        if (file.exists() && !file.isDirectory()) {
                            fileExist = true;
                            if (tokens[0].equals(HEAD)) head(tokens, file);
                            if (tokens[0].equals(GET)) {
                                if (tokens[1].length() > 2) get(tokens, file);
                            }
                        }

                        if (!fileExist) {
                            if(fileIndex.exists()) {
                                if (tokens[0].equals(HEAD)) head(tokens, fileIndex);
                                if (tokens[0].equals(GET)) {
                                    if (tokens[1].length() > 2) get(tokens, fileIndex);
                                }
                            }
                            else if(ServeurWeb.getList()) {
                                Afficher_Fichiers(file, tokens);
                            }

                            // region Mesages d'erreurs
                            // Quand liste est false Error 403
                            else if (file.isDirectory()) {
                                tokens[1] = "/403.html";
                                File fichierErreur403 = new File("403.html");
                                Afficher_Erreur(fichierErreur403, tokens);
                            }
                            // Quand le fichier n'existe pas Error 404
                            else {
                                tokens[1] = "/404.html";
                                File fichierErreur404 = new File("404.html");
                                Afficher_Erreur(fichierErreur404, tokens);
                            }
                            // endregion
                        }
                    }
                }
                else {
                    writer.println("HTTP 501 Not implemented");
                    writer.println();
                }
            }
            catch (FileNotFoundException fnfe){
//                writer.println("HTTP/1.1 404 Not Found");
//                writer.println();
//                writer.flush();
//                writer.close();
            }
            catch (IOException ioe) {
                System.err.println("Unexpected error");
            }
            read.close();
            sender.close();
            writer.close();
            reader.close();

            System.out.println("Client deconnecte");
            cSocket.close();
        } catch (Exception e) {
            System.err.println("Client deconnecte");
        }
    }

    private void Afficher_Fichiers(File file, String [] tokens) {
//        String lien = file.getPath().replaceFirst(repertoire, "") + "\\" + s;
        File[] listFichier = file.listFiles();
        if(listFichier != null) {
            try {
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
                    writer.println("<td><a href=\"" + tokens[1] + "/" + listFichier[i].getName() + "\">" + listFichier[i].toString() + "</a></td><td>" + getLastModifiedDateRfc822(listFichier[i]) + "</td><td>" + listFichier[i].length() + "</td>");
                    writer.println("</tr>");
                }

                writer.println("\n</table>");
                writer.println("</body>");
                writer.println("</html>");

                writer.close();
            } catch (IOException e) {
                System.err.println("Impossible de recevoir la destination");
            }
        }
        else {
            tokens[1] = "/404.html";
            File fichierErreur404 = new File("404.html");
            Afficher_Erreur(fichierErreur404, tokens);
        }
    }

    private void Afficher_Erreur(File file, String [] tokens) {
        try {
            get(tokens, file);
        }
        catch (FileNotFoundException fnfe){
            System.err.println(file + " fichier introuvable");
        }
    }

    private void head(String[] tokens, File file){
        try{
            writer.println("HTTP/1.0 200 OK");
            writer.println("Server: Bulletproof Corporation, CEO:Joaquin, Bitch:Mathieu");
            writer.println("Date: " + getDateRfc822(date));

            if (tokens[1].endsWith("/")) writer.println("Content-type: text/html");
            else writer.println("Content-type: " + checkContentExtension(tokens));

            writer.println("Last-modified: " + getLastModifiedDateRfc822(file));
            writer.println("Content-length: " + file.length());
            writer.println();
        }
        catch (Exception e){
            System.err.println("Impossible de recevoir la destination");
        }
    }

    private void get(String[] tokens, File file) throws FileNotFoundException{
        try{
            head(tokens,file);
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
        if (tokens[1].toLowerCase().endsWith(".html")) return new String ("text/html");
        if (tokens[1].toLowerCase().endsWith(".txt"))return new String ("text/plain");
        if (tokens[1].toLowerCase().endsWith(".gif"))return new String ("image/gif");
        if (tokens[1].toLowerCase().endsWith(".jpg") || tokens[1].toLowerCase().endsWith(".jpeg"))return new String ("image/jpeg");
        if (tokens[1].toLowerCase().endsWith(".png")) return new String ("image/png");
        else return new String("Les extensions de fichiers possible sont .html, .txt, .gif, .jpg, .jpeg, ou .png");
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