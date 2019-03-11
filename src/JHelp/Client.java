/*
 * Client.java
 *
 */
package JHelp;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

/**
 * Client class provides users's interface of the application.
 * @author <strong >Y.D.Zakovryashin, 2009</strong>
 * @version 1.0
 */
public class Client extends JFrame implements JHelp {

    /**
     * Static constant for serialization
     */
    public static final long serialVersionUID = 1234;
    /**
     * Programm properties
     */
    private Properties prop;
    /**
     * Private Data object presents informational data.
     */
    private Data data, ans;
    private Item item, key, value;
    private JTextArea termin, def_area;
    private JLabel term, def, helper;
    private JButton find, add, edit, delete, next, prev, exit;
    private JScrollPane scrollPane;
    private JTabbedPane mainM;
    private JPanel p1, p2, p3;
    private Container cont;
    private Socket clientSocket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    /**
     * Constructor with parameters.
     * @param args Array of {@link String} objects. Each item of this array can
     * define any client's property.
     */
    public Client(String[] args) {
        setSize(600, 600);
        setTitle("JHelp");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addComponents();
        setResizable(false);
        setVisible(true);
    }
    
    public void addComponents() {
        try {
            cont = getContentPane();
            cont.setLayout(null); 
            
            mainM = new JTabbedPane();
            mainM.setBounds(0, 0, 600, 600);
            p1 = new JPanel();
            p1.setLayout(null);
            p2 = new JPanel();
            p2.setLayout(null);
            p3 = new JPanel();
            p3.setLayout(new BorderLayout());
            mainM.addTab("Main", p1);
            mainM.addTab("Help", p2);
            cont.add(mainM);
            
            term = new JLabel("Termin:");
            term.setBounds(20, 15, 50, 30);
            p1.add(term);
            termin = new JTextArea();
            termin.setBounds(80, 15, 360, 30);
            p1.add(termin);
            
            def = new JLabel("Definition:");
            def.setBounds(20, 50, 70, 30);
            p1.add(def);
            def_area = new JTextArea();
            def_area.setLineWrap(true);
            def_area.setWrapStyleWord(true);
            scrollPane = new JScrollPane(def_area);
            scrollPane.setBounds(20, 85, 420, 440);
            p1.add(scrollPane);
            
            find = new JButton("Find");
            find.setBounds(460, 10, 100, 40);
            p1.add(find);
            find.addActionListener(new ClientListener(this));

            add = new JButton("Add");
            add.setBounds(460, 80, 100, 40);
            p1.add(add);
            add.addActionListener(new ClientListener(this));
            
            edit = new JButton("Edit");
            edit.setBounds(460, 130, 100, 40);
            p1.add(edit);
            edit.addActionListener(new ClientListener(this));
            
            delete = new JButton("Delete");
            delete.setBounds(460, 180, 100, 40);
            p1.add(delete);
            delete.addActionListener(new ClientListener(this));
            
            next = new JButton("Next");
            next.setBounds(460, 230, 100, 40);
            p1.add(next);
            next.addActionListener(new ClientListener(this));
            
            prev = new JButton("Previous");
            prev.setBounds(460, 280, 100, 40);
            p1.add(prev);
            prev.addActionListener(new ClientListener(this));
            
            exit = new JButton("Shutdown all");
            exit.setBounds(460, 490, 100, 40);
            p1.add(exit);
            exit.addActionListener(new ClientListener(this));
            
            helper = new JLabel("Here is the space for the short manual");
            p2.add(helper, BorderLayout.NORTH);            
        } catch (NullPointerException e) {
            System.out.println("Error: " + e.getMessage());
            disconnect();
        }
    }

    /**
     * Method for application start
     * @param args agrgument of command string
     */
    static public void main(String[] args) {
        Client client = new Client(args);
        if (client.connect(args) == JHelp.READY) {
        client.run();
        }
    }
    
    public void actions(ActionEvent e) {
        try {
        String buttonName = e.getActionCommand();
        switch(buttonName) {
            case "Find":
                selectSt(JHelp.SELECT, JHelp.SELECT);
                def_area.setText(ans.getKey().getItem());
            break;
            
            case "Add":
                changeDB(JHelp.INSERT, JHelp.INSERT, JHelp.INSERT);
                def_area.setText("Termin " + data.getKey().getItem() + " was added to Data base");
            break;
            
            case "Edit": 
                changeDB(JHelp.UPDATE, JHelp.UPDATE, JHelp.UPDATE);
                def_area.setText("Termin " + data.getKey().getItem() + " was changed");
            break;
            
            case "Delete":
                changeDB(JHelp.DELETE, JHelp.DELETE, JHelp.DELETE);
                def_area.setText("Termin " + data.getKey().getItem() + " was delete from Data base"); 
            break;    
            
            case "Next":
                selectSt(JHelp.NEXT, JHelp.SELECT);
                termin.setText(ans.getKey().getItem());
                def_area.setText(ans.getValue().getItem());
            break; 
            
            case "Previous":
                selectSt(JHelp.PREVIOUS, JHelp.SELECT);
                termin.setText(ans.getKey().getItem());
                def_area.setText(ans.getValue().getItem());    
            break; 
            
            case "Shutdown all":
                disconnect();
            break;
        }
        } catch (NullPointerException ex) {
            def_area.setText("Termin can not be empty, error: " + ex.getMessage());
        }
    }
    
    private void selectSt(int itemState, int dataOperation) {
        String req = termin.getText();
        if (req.isEmpty()) {
            def_area.setText("Termin can not be empty");
        } else {
            item = new Item(req, itemState);
            data = new Data(dataOperation, item, JHelp.DEFAULT_VALUES);
            ans = getData(data);
        }
    }
    
    private void changeDB (int keyState, int valueState, int dataOperation) {
        try {
            String req = termin.getText();
                if (req.isEmpty()) {
                    def_area.setText("Termin can not be empty");
                } else {
                    key = new Item (termin.getText(), keyState);
                    value = new Item (def_area.getText(), valueState);
                    data = new Data(dataOperation, key, value);
                    output.writeObject(data);
                }
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    /**
     * Method define main job cycle
     */
    public void run() {  
        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            disconnect();
        }
    }

    /**
     * Method set connection to default server with default parameters
     * @return error code
     */
    @Override
    public int connect() {
        return JHelp.OK;
    }

    /**
     * Method set connection to server with parameters defines by argument 
     * <code>args</code>
     * @return error code
     */
    @Override
    public int connect(String[] args) {
        try {
            clientSocket = new Socket(InetAddress.getLocalHost(), JHelp.DEFAULT_SERVER_PORT);
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            disconnect();
            return JHelp.ERROR;
        }
        return JHelp.READY;
    }

    /**
     * Method gets data from data source
     * @param data initial object (template)
     * @return new object
     */
    @Override
    public Data getData(Data data) {
        try {
            output.writeObject(data);
            ans = (Data) input.readObject();
            output.flush();
        } catch (IOException | ClassNotFoundException | NullPointerException ex) {
            System.out.println("Error: " + ex.getMessage());
            disconnect();
        } 
        return ans;
    }

    /**
     * Method disconnects client and server
     * @return error code
     */
    @Override
    public int disconnect() {
        try {
            item = new Item(JHelp.DISCONNECT);
            data = new Data(JHelp.DISCONNECT, item, JHelp.DEFAULT_VALUES);
            output.writeObject(data);
            output.close();
            input.close();
            clientSocket.close();
            System.out.println("Client disconnected, all sockets were closed");
            dispose();
        } catch (IOException | NullPointerException ex) {
            System.out.println("Error: " + ex.getMessage());
            dispose();
        }
        return JHelp.DISCONNECT;
    }
}
