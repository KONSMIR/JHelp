/*
 * Server.java
 *
 */
package JHelp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class sets a network connection between end client's objects
 * of {@link jhelp.Client} type and single {@link jhelp.ServerDb} object.
 * @version 1.0
 * @see jhelp.Client
 * @see jhelp.ClientThread
 * @see jhelp.ServerDb
 */
public class Server implements JHelp {

    /**
     *
     */
    private ServerSocket serverSocket;
    /**
     *
     */
    private Socket clientSocket, dbSocket;
    /**
     *
     */
    private ObjectInputStream input;
    /**
     *
     */
    private ObjectOutputStream output;
    private Item item;
    private Data data;

    /** Creates a new instance of Server */
    public Server() {
        this(DEFAULT_SERVER_PORT, DEFAULT_DATABASE_PORT);
    }

    /**
     *
     * @param port
     * @param dbPort
     */
    public Server(int port, int dbPort) {
        port = JHelp.DEFAULT_SERVER_PORT;
        dbPort = JHelp.DEFAULT_DATABASE_PORT;
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        Server server = new Server();
        if (server.connect() == JHelp.READY) {
            server.disconnect();
        }
    }

    /**
     *
     */
    private void run() {
        System.out.println("SERVER: run");
    }

    /**
     * The method sets connection to database ({@link jhelp.ServerDb} object) and
     * create {@link java.net.ServerSocket} object for waiting of client's
     * connection requests. This method uses default parameters for connection.
     * @return error code. The method returns {@link JHelp#OK} if streams are
     * successfully opened, otherwise the method returns {@link JHelp#ERROR}.
     */
    @Override
    public int connect() {
        try {
            serverSocket = new ServerSocket(JHelp.DEFAULT_SERVER_PORT);
            dbSocket = new Socket(InetAddress.getLocalHost(), JHelp.DEFAULT_DATABASE_PORT);
            output = new ObjectOutputStream(dbSocket.getOutputStream());
            input = new ObjectInputStream(dbSocket.getInputStream()); 
            System.out.println("Server run");
            while (true) {
                clientSocket = serverSocket.accept(); 
                ClientThread ct = new ClientThread(this, clientSocket);
                Thread th = new Thread(ct);
                th.start();
            }   
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        return JHelp.READY;
    }

    /**
     * The method sets connection to database ({@link jhelp.ServerDb} object) and
     * create {@link java.net.ServerSocket} object for waiting of client's
     * connection requests.
     * @param args specifies properties of connection.
     * @return error code. The method returns {@link JHelp#OK} if connection are
     * openeds uccessfully, otherwise the method returns {@link JHelp#ERROR}.
     */
    @Override
    public int connect(String[] args) {
        return OK;
    }

    /**
     * Transports initial {@link Data} object from {@link ClientThread} object to
     * {@link ServerDb} object and returns modified {@link Data} object to
     * {@link ClientThread} object.
     * @param data Initial {@link Data} object which was obtained from client
     * application.
     * @return modified {@link Data} object
     */
    @Override
    public synchronized Data getData(Data data) {
        try {
            output.writeObject(data);
            data = (Data) input.readObject(); 
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        return data;
    }

    /**
     * The method closes connection with database.
     * @return error code. The method returns {@link JHelp#OK} if a connection
     * with database ({@link ServerDb} object) closed successfully,
     * otherwise the method returns {@link JHelp#ERROR} or any error code.
     */
    @Override
    public int disconnect() {
        try {
                item = new Item(JHelp.DISCONNECT);
                data = new Data(JHelp.DISCONNECT, item, JHelp.DEFAULT_VALUES);
                output.writeObject(data);
                serverSocket.close();
                dbSocket.close();
                output.close();
                input.close();
                System.exit(0);
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        return JHelp.DISCONNECT;
    }
}
