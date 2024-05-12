import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.Semaphore;


public class client {
    private static PriorityQueue<Map.Entry<Integer, String>> priorityQueue = new PriorityQueue<>(
            (e1, e2) -> {
                // Compare keys of the Map.Entry objects
                if (e1.getKey() > e2.getKey()) {
                    return 1; // e1 has higher priority
                } else if (e1.getKey() < e2.getKey()) {
                    return -1; // e2 has higher priority
                } else {
                    // Keys are equal, compare values by converting to hash
                    int hash1 = generateHash(e1.getValue());
                    int hash2 = generateHash(e2.getValue());
                    return Integer.compare(hash1, hash2);
                }
            }
    );
    public static String localip;
    public static ArrayList<String> history=new ArrayList<>();
    public static int portno, port1, port2,port_cs;
    public static String add1, add2,add_cs;
    private static Socket client1=null,client2=null;
    public static ServerSocket serverSocket;
    private static int resno;
    private static int timeStamp=0;
    private static final Object lock = new Object();
    private static final Object res_lock = new Object();

    private static int events=0;
    private static boolean cs_flag=false;
    private static Semaphore semaphore = new Semaphore(0);

    public static int generateHash(String input) {
        int hash = 0;
        for (int i = 0; i < input.length(); i++) {
            hash += input.charAt(i) * (i + 1); // Multiply by position (1-indexed)
        }
        return hash;
    }

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("google.com", 80));
        System.out.println("System running on"+socket.getLocalAddress().toString().substring(1));
        localip=socket.getLocalAddress().toString().substring(1);
        socket.close();
        Scanner scanner = new Scanner(System.in);
        if(args.length==7){
            portno=Integer.parseInt(args[0]);
            add1=args[1];
            port1=Integer.parseInt(args[2]);
            add2=args[3];
            port2=Integer.parseInt(args[4]);
            add_cs=args[5];
            port_cs=Integer.parseInt(args[6]);
        }
        else {
            System.out.println("Enter Portno of your listener:");
            portno = scanner.nextInt();
            scanner.nextLine();
            System.out.println("Enter IP address of client1:");
            add1 = scanner.nextLine();
            System.out.println("Enter portno of client1:");
            port1 = scanner.nextInt();
            scanner.nextLine();
            System.out.println("Enter IP address of client1:");
            add2 = scanner.nextLine();
            System.out.println("Enter portno of client 2:");
            port2 = scanner.nextInt();
            scanner.nextLine();
            System.out.println("Enter IP address of Critical_section server:");
            add_cs = scanner.nextLine();
            System.out.println("Enter port no of Critical_section server: ");
            port_cs = scanner.nextInt();

        }
        if(add1.equals("localhost") || add1.equals("127.0.0.1"))
            add1=localip;
        if(add2.equals("localhost") || add2.equals("127.0.0.1"))
            add2=localip;
        if(add_cs.equals("localhost") || add_cs.equals("127.0.0.1"))
            add_cs=localip;
        new Thread(() -> {
            try {
                server();
            } catch (IOException e) {
                e.printStackTrace(); // Log the exception instead of throwing a RuntimeException
            }
        }).start();

        commun();
//        server.close();
    }
    public static void enter_cs() throws IOException {
        Socket client= null;
        client = waitForServer(add_cs,port_cs);
        DataOutputStream write=new DataOutputStream(client.getOutputStream());
        DataInputStream read=new DataInputStream(client.getInputStream());
        write.writeUTF(localip+String.valueOf(portno));
        String message = null;
        message=read.readUTF();
//        System.out.println(message+" CS");
    }
    public static void req_handler() throws IOException {
        int t;

        synchronized (lock) {
            events++;
            timeStamp++;
            t = timeStamp;
            history.add(String.valueOf(events)+" -> "+String.valueOf(timeStamp)+" -> sending REQ");
        }
        synchronized (res_lock){
            resno=0;
        }
        synchronized (priorityQueue) {
            priorityQueue.add(Map.entry(t, localip+portno));
        }

        common_msg("REQ_"+t+"_"+localip+"_"+portno);
        try {
            semaphore.acquire(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Entering critical Section");
        try {
            enter_cs();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("exiting CS");
//            semaphore.release();
//        semaphore.release();
        semaphore = new Semaphore(0);
        synchronized (priorityQueue){
            priorityQueue.poll();
        }
        synchronized (lock){
            timeStamp++;
            events++;
            t=timeStamp;
            history.add(String.valueOf(events)+" -> "+String.valueOf(timeStamp)+" ->sending REL");
        }
        common_msg("REL_"+t+"_"+localip+"_"+portno);
    }
    public static void commun() throws IOException {
        while (true) {
            System.out.println("Enter the type of task(enter the number 1 or 2 or 3): \n 1.Create an Event \n 2.Request CS \n 3.timestamp \n 4. top of the queue \n");
            Scanner scanner = new Scanner(System.in);
            int task = scanner.nextInt();
            if (task == 1) {
                synchronized (lock) {
                    events++;
                    timeStamp++;
                    history.add(String.valueOf(events)+" -> "+String.valueOf(timeStamp)+" -> new event");
                }
            } else if (task == 2) {
                cs_flag=true;
//                   new Thread(this::req_handler).start();
                req_handler();
                cs_flag=false;
            }
            else if(task==3)
            {
                for (int i = 0; i < history.size(); i++) {
                    System.out.println(history.get(i));
                }
            }
            else if(task==4){
                synchronized (priorityQueue){
                    if( priorityQueue.peek() != null)
                        System.out.println("top element in priorityQueue is:"+ priorityQueue.peek().getValue());
                    else
                        System.out.println("Queue is empty");
                }
            }
            else{
                break;
            }
        }
        System.out.println("Exiting cli interface");
        if(serverSocket!=null)
            serverSocket.close();
        if(client1!=null)
            client1.close();
        if(client2!=null)
            client2.close();
    }



    public static void common_msg(String message) throws IOException {

        if(client1==null){
            client1 = waitForServer(add1, port1);
        }
        if(client2==null){
            client2 = waitForServer(add2, port2);
        }

        DataOutputStream dout1 = new DataOutputStream(client1.getOutputStream());
        DataOutputStream dout2 = new DataOutputStream(client2.getOutputStream());
        try {
            dout1.writeUTF(message);
            dout1.flush();
        }
        catch(SocketException s){
            System.out.println("entering catch");
            client1= waitForServer(add1,port1);
            dout1 = new DataOutputStream(client1.getOutputStream());
            dout1.writeUTF(message);
            dout1.flush();
        }
        try{
            dout2.writeUTF(message);
            dout2.flush();}
        catch(SocketException s){
            client2= waitForServer(add2,port2);
            dout2 = new DataOutputStream(client2.getOutputStream());
            dout2.writeUTF(message);
            dout2.flush();
        }

    }
    public static void write(String add,int port,String message){
        try {
            System.out.println("Sending RES request to"+port+add);
            Socket client=null;
            if(add.equals(add1)&& port==port1) {
                client = client1;
                if(client==null){
                    client=waitForServer(add,port);
                    client1=client;
                }
            }
            else if(add.equals(add2) && port==port2) {
                client = client2;
                if(client==null){
                    client=waitForServer(add,port);
                    client2=client;
                }
            }
            else
                client=new Socket(add,port);

            DataOutputStream dout = new DataOutputStream(client.getOutputStream());
            Random random = new Random();
            int randomNumber = random.nextInt(10);
            Thread.sleep(1000*randomNumber);
            try{
                dout.writeUTF(message);
                dout.flush();
            }
            catch(IOException e){
                System.out.println(add+port+"server disconnected");
                client=waitForServer(add,port);
                dout= new DataOutputStream(client.getOutputStream());
                dout.writeUTF(message);
                dout.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        synchronized (lock) {
            events++;
            timeStamp++;
            history.add(String.valueOf(events)+" -> "+String.valueOf(timeStamp)+" Sending RES");
        }
    }
    public static Socket waitForServer(String add,int port) throws IOException {
        boolean serverOpen = false;
        Socket socket=null;
        while (!serverOpen) {
            try {
                // Attempt to connect to the server
                socket = new Socket(add, port);
                serverOpen = true;

            } catch (IOException e) {
                // Server is not reachable or closed, wait and retry
                System.out.println("Server is not open yet. Retrying in " + 5000 + " ms.");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    System.err.println("Interrupted while waiting for server.");
                }
            }
        }
        if(socket.isConnected()){
            DataOutputStream out=new DataOutputStream(socket.getOutputStream());
            out.writeUTF(add+"_"+String.valueOf(portno));
        }
        return socket;

    }
    public static void server() throws IOException {
        serverSocket = new ServerSocket(portno);
        while (true) {
            Socket clientSocket=null;
            try {
                clientSocket = serverSocket.accept();
                System.out.println("connected_______________");
            }
            catch(SocketException s){
                System.out.println("exiting programme");
                break;
            }
            Socket finalClientSocket = clientSocket;
            new Thread(() -> {
                try {
                    handleClient(finalClientSocket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        serverSocket.close();
    }

    private static void handleClient(Socket clientSocket) throws IOException {
        DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
        String clientName;
        String address;

        String m=dis.readUTF();
//                System.out.println(m+" first msg");
        int clientport;
        //     message pattern recieved is of  type_time_ip_port
        while (true) {
            try {
                String message = dis.readUTF();
                String[] mess=message.split("_");
                String type;
                type=mess[0];
                address=mess[2];
                clientport=Integer.parseInt(mess[3]);
                clientName=address+clientport;
                int t, h;
                int client_clock;
                try {
                    client_clock = Integer.parseInt(mess[1]);
                } catch (NumberFormatException n) {
                    System.out.println("error in the message:" + message + " " + clientport);
                    return;
                }

                synchronized (lock) {
                    events++;
                    h = timeStamp;
                    timeStamp = Math.max(timeStamp, client_clock) + 1;
                    t = timeStamp;
                    history.add(String.valueOf(events) + " -> " + String.valueOf(t) + " -> " + type + "  " + client_clock);

                }
                if (type.endsWith("REQ")) {
                    synchronized (priorityQueue) {
                        priorityQueue.add(Map.entry(client_clock, clientName));
                    }
                    write(address, clientport, "RES_" + (t + 1) + "_" + localip + "_" + portno);
                } else if (type.endsWith("RES")) {

                    synchronized (res_lock) {
                        resno++;
                        System.out.println("res section");
                        if (resno == 2) {
                            semaphore.release();
                            resno = 0;
                            if (priorityQueue.peek().getValue().equals(localip+portno))
                                semaphore.release();
                        }

                    }

                } else if (type.endsWith("REL")) {
                    synchronized (priorityQueue) {
                        if (!Objects.requireNonNull(priorityQueue.poll()).getValue().equals(clientName)) {
                            System.out.println("Error in the queue");
                        }
                        if (cs_flag) {
                            assert priorityQueue.peek() != null;
                            if (priorityQueue.peek().getValue().equals(localip+portno)) {
                                semaphore.release();
                            }
                        }
                    }
                }


                System.out.println("Message received: " + message + " from " + clientport);

            } catch (IOException i) {
                System.out.println("client disconnected ");
                String[] client_add = m.split("_");
                if(client_add.length>=2){
                    if(client_add[0].equals(add1) && client_add[1].equals(String.valueOf(port1)))
                        client1=null;
                    if(client_add[0].equals(add2) && client_add[1].equals(String.valueOf(port2)))
                        client2=null;
                }
                clientSocket.close();
                return ;
            }
        }
    }

}
