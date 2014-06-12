import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;



public class Handler extends Thread {
    private static final String ROOTDIR = "/Users/tuvialerea/code/web/coins/";
    private static final int BUFFERSIZE = 4096;
    private Socket socket;
    private ArrayList<String> versions;
    private BufferedReader in;
    private PrintStream out;
    
    public Handler(Socket s) {
        this.versions = initVersions();
        this.socket = s;
        try {
            in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            out = new PrintStream(new BufferedOutputStream(this.socket.getOutputStream()));
        } catch (IOException e) {
            System.err.println(e);
        }
        this.start();
    }
    
    public void run() {
        try {
            String request = in.readLine();
            if (request == null) {
                badRequest("null request");
                return;
            }
            System.out.println(new Date() + ": "+ request);
            String[] requestPieces = request.split(" ");
            if (requestPieces.length != 3) {
                badRequest("");
                return;
            }
            if (! checkVersion(requestPieces[2])) {
                badRequest("Invalid HTTP Version");
                return;
            }
            String method = requestPieces[0];
            if (method.equals("GET")) {
                processGet(requestPieces);
            } else if (method.equals("POST")) {
                processPost(requestPieces);
            } else {
                
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    private void processGet(String[] pieces) {
        String path = pieces[1];
        String params = "";
        int indexOfParams = path.indexOf('?');
        if (indexOfParams > 0) {
            params = path.substring(indexOfParams);
            path = path.substring(0,indexOfParams);
        }
        File file = parsePath(path);
        if (file == null) {
            fileNotFound(path);
            return;
        }
        String type = fileType(file);
        String contentType = contentType(type);
        if (type.equals("php")) {
            setupPHP(params, file);
        } else {
            try {
                okHeader(contentType);
                InputStream inFile=new FileInputStream(file);

                int n;
                byte[] bytes = new byte[BUFFERSIZE];

                while ((n = inFile.read(bytes)) > 0) {
                    out.write(bytes, 0, n);
                }
                inFile.close();
            } catch (FileNotFoundException e) {
                System.err.println(e);
            } catch (IOException e) {
                System.err.println(e);
            }

            out.close();
        }
        
    }
    
    private void processPost(String[] pieces) {
        
    }
    
    private void setupPHP(String params, File file) {
        if (params.length() == 1) {
            badRequest("trailing ?");
        } else if (params.equals("")) {
            runPHP(new String[] {"php", file.getAbsolutePath()});
        } else {
            params = params.substring(1);
            String command = "";
            String key, val;
            String[] paramPieces = params.split("&");
            for (String s : paramPieces) {
                String[] keyVal = s.split("=");
                key = keyVal[0];
                val = keyVal[1];
                command += "$_GET['" + key + "']=\"" + val + "\"; ";
            }
            command += "include(\"" + file.getAbsolutePath() + "\");";
            runPHP(new String[] {"php", "-r", command});
        }
    }
    
    private void runPHP(String[] runCommands) {
        InputStream in;
        okHeader("text/plain");
        try {
            in = Runtime.getRuntime().exec(runCommands).getInputStream();
            int n;
            byte[] bytes = new byte[BUFFERSIZE];
            while ((n = in.read(bytes)) > 0) {
                out.write(bytes, 0, n);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    private String contentType(String type) {
        switch(type) {
            case "html":
                return "text/html";
            case "jpg":
                return "image/jpeg";
            case "txt":
                return "text/plain";
            case "css":
                return "text/css";
            case "js":
                return "text/javascript";
            case "php":
                return "text/plain";
            default:
                return "text/plain";
            
        }
    }
    
    private String fileType(File file) {
        String name = file.getName();
        String[] pieces = name.split("\\.");
        String type = pieces[pieces.length-1];
        return type;
    }
    
    private File parsePath(String path) {
        // add index.html for directories
        if (path.endsWith("/")) {
            path += "index.html";
        }
        // remove leading '/'
        while (path.indexOf("/")==0) {
            path = path.substring(1);
        }
        // make absolute path
        path = ROOTDIR + path;
        // if directory requested without ending '/'
        File file = new File(path);
        if (file.isDirectory()) {
            path += "/index.html";
        }
        file = new File(path);
        if (! file.exists()) {
            return null;
        }
        return file;
    }
    
    private boolean checkVersion(String version) {
        String[] parsed = version.split("/");
        if (parsed.length != 2) {
            return false;
        }
        if (! parsed[0].equals("HTTP")) {
            return false;
        }
        if (! this.versions.contains(parsed[1])) {
            return false;
        }
        return true;
    }
    
    private ArrayList<String> initVersions() {
        ArrayList<String> versions = new ArrayList<String>();
        versions.add("1.1");
        
        return versions;
    }
    
    
    private void badRequest(String message) {
      out.print("HTTP/1.1 400 Bad Request\n");
      out.print("Content-type: text/html\n");
      out.print("\n<html> <body> <p> 400 Bad Request <br>" + message + "</p> </body> </html>\n");
      out.close();
    }
    
    private void fileNotFound(String message) {
        out.print("HTTP/1.1 404 Not Found\n");
        out.print("Content-type: text/html\n");
        out.print("\n<html><body><p> 404 File Not Found <br>" + message + "</p></body></html>\n");
        out.close();
    }
    
    private void okHeader(String type) {
        out.print("HTTP/1.1 200 OK\n");
        out.print("Content-type: " + type + "\n");
        out.print("\n");
    }
}
