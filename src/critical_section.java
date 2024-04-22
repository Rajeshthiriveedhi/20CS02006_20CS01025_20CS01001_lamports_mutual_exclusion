// A Java program for a Server
import java.net.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Scanner;
import java.io.Serializable;
public class critical_section
{
    //initialize socket and input stream
    private Socket		 socket = null;
    private ServerSocket server = null;
    private DataInputStream in	 = null;

    // constructor with port

    private static Object lock=new Object();
    private static int flag=0;
    private static void cs(Socket client) throws IOException {
      DataInputStream dis =new DataInputStream(client.getInputStream());
      String m=dis.readUTF();
      System.out.println(m+" connected");
      String message = dis.readUTF();
        // Get the current date and time
        Date d1 = new Date();

        System.out.println("Entering criticalSection at "+d1+" client :"+message);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    d1 = new Date();

        System.out.println("Exiting Critical Section at "+d1+" client :"+message);
        DataOutputStream write=new DataOutputStream(client.getOutputStream());
        write.writeUTF("exiting");

        client.close();
    }
    public static void main(String[] args) throws IOException {
        int portNumber = 8080; // Port to listen on
        if(args.length==1) {
            portNumber = Integer.parseInt(args[0]);
        }
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("google.com", 80));
//        socket.close();
        System.out.println(socket.getLocalAddress().toString().substring(1));
        socket.close();
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Server started on port " + portNumber);

            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected.");
                 new Thread(()-> {
                     try {
                         cs(client);
                     } catch (IOException e) {
                         throw new RuntimeException(e);
                     }
                 }).start();

        } catch (IOException e) {
            e.printStackTrace(); // Handle exceptions
        }
    }
}

