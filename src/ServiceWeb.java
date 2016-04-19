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
                        } else if (!fileIndex.exists() && file.isDirectory()) {
                            tokens[1] = "/403.html";
                            File fichierErreur403 = new File("c:\\www\\403.html");
                            writer.println("HTTP/1.1 403 Acces refuse");
                            writer.println();
                            Afficher_Erreur403(fichierErreur403, tokens);
                            //writer.flush();
                            //writer.close();
                        }

                        if (fileIndex.exists() && !fileExist) {
                            if (tokens[0].equals(HEAD)) head(tokens, fileIndex);
                            if (tokens[0].equals(GET)) {
                                if (tokens[1].length() > 2) get(tokens, fileIndex);
                            }
                        } else if (!fileExist && !file.exists()) {
                            // if (lister) {
                            Afficher_Fichiers(file, tokens);
                            //}
                            //else {
                            // tokens[1] = "/403.html";
                            // File fichierErreur403 = new File("c:\\www\\403.html");
                            // Afficher_Erreur403(fichierErreur403, tokens);
                            //}
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
                writer.println("<body>");

                writer.println("<h1>Index of " + file.toString() + "</h1>");

                writer.print("<table style=\"width:100%\">");

                writer.print("<tr>");
                writer.print("<td>Name</td>");
                writer.print("<td>Last modified</td>");
                writer.print("<td>Size</td>");
                writer.print("<td>Description</td>");
                writer.print("</tr>");

                for (int i = 0; i < listFichier.length; ++i) {
                    //String lien = file.getPath().replaceFirst(document, "") + "\\" + listFichier[i].getName();
                    writer.print("<tr>");
                    writer.println("<td><a href=\"" + tokens[1] + "/" + listFichier[i].getName() + "\">" + listFichier[i].toString() + "</a></td><td>" + getLastModifiedDateRfc822(listFichier[i]) + "</td><td>" + listFichier[i].length() + "</td>");
                    writer.print("</tr>");
                }

                writer.print("</table>");
                writer.println("</body>");
                writer.println("</html>");
                //writer.flush();
                writer.close();
            } catch (IOException e) {
                //e.printStackTrace();
                System.err.println("Impossible de recevoir la destination");
            }
        }
        else {
            tokens[1] = "/404.html";
            File fichierErreur404 = new File("404.html");
            Afficher_Erreur403(fichierErreur404, tokens);
            System.err.println("Client deconnecte");
        }
    }

    private void Afficher_Erreur403(File file, String [] tokens) {
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