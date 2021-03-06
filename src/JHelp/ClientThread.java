/*
 * Class ClientThread.
 */
package JHelp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This class provides a network connection between end client of
 * {@link jhelp.Client} type and {@link jhelp.Server} object. Every object of
 * this class may work in separate thread.
 * @version 1.0
 * @see jhelp.Client
 * @see jhelp.Server
 */
public class ClientThread implements JHelp, Runnable {

    /**
     *
     */
    private Server server;
    /**
     *
     */
    private Socket clientSocket;
    /**
     *
     */
    private ObjectInputStream input;
    /**
     *
     */
    private ObjectOutputStream output;
    private Data data;

    /**
     * Creates a new instance of Client
     * @param server reference to {@link Server} object.
     * @param clientSocket
     * @param socket reference to {@link java.net.Socket} object for connection
     * with client application.
     */
    public ClientThread(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    /**
     * The method defines main job cycle for the object.
     */
    public void run() {
        if (connect() == JHelp.READY) {
            try {
                while(true) {
                    data = (Data) input.readObject();
                    if (data.getKey().getState() == JHelp.DISCONNECT) {
                        server.disconnect();
                    }
                    data = server.getData(data);
                    output.writeObject(data);
                }
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    /**
     * Opens input and output streams for data interchanging with
     * client application.  The method uses default parameters.
     * @return error code. The method returns {@link JHelp#OK} if streams are
     * successfully opened, otherwise the method returns {@link JHelp#ERROR}.
     */
    public int connect() {
        try {
            input = new ObjectInputStream(clientSocket.getInputStream());
            output = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            return JHelp.ERROR;
        }
        return JHelp.READY;
    }

    /**
     * Opens input and output streams for data interchanging with
     * client application. This method uses parameters specified by parameter
     * <code>args</code>.
     * @param args defines properties for input and output streams.
     * @return error code. The method returns {@link JHelp#OK} if streams are
     * successfully opened, otherwise the method returns {@link JHelp#ERROR}.
     */
    public int connect(String[] args) {
        System.out.println("MClient: connect");
        return JHelp.OK;
    }

    /**
     * Transports {@link Data} object from client application to {@link Server}
     * and returns modified {@link Data} object to client application.
     * @param data {@link Data} object which was obtained from client
     * application.
     * @return modified {@link Data} object
     */
    public Data getData(Data data) {
        System.out.println("Client: getData");
        return null;
    }

    /**
     * The method closes connection with client application.
     * @return error code. The method returns {@link JHelp#OK} if input/output 
     * streams and connection with client application was closed successfully,
     * otherwise the method returns {@link JHelp#ERROR}.
     */
    public int disconnect() {
        System.out.println("Client: disconnect");
        return JHelp.OK;
    }
}
