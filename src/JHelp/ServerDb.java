/*
 * ServerDb.java
 *
 */
package JHelp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * This class presents server directly working with database.
 * The complete connection string should take the form of:<br>
 * <code><pre>
 *     jdbc:subprotocol://servername:port/datasource:user=username:password=password
 * </pre></code>
 * Sample for using MS Access data source:<br>
 * <code><pre>
 *  private static final String accessDBURLPrefix
 *      = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
 *  private static final String accessDBURLSuffix
 *      = ";DriverID=22;READONLY=false}";
 *  // Initialize the JdbcOdbc Bridge Driver
 *  try {
 *         Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
 *      } catch(ClassNotFoundException e) {
 *         System.err.println("JdbcOdbc Bridge Driver not found!");
 *      }
 *
 *  // Example: method for connection to a Access Database
 *  public Connection getAccessDBConnection(String filename)
 *                           throws SQLException {
 *       String databaseURL = accessDBURLPrefix + filename + accessDBURLSuffix;
 *       return DriverManager.getConnection(databaseURL, "", "");
 *   }
 *</pre></code>
 *  @author <strong >Y.D.Zakovryashin, 2009</strong>
 */
public class ServerDb implements JHelp {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Properties prop;
    private Data data;
    private Connection conn;
    private Statement st;
    private ResultSet rs;
    private Item item, key, value;

    /**
     * Creates a new instance of <code>ServerDb</code> with default parameters.
     * Default parameters are:<br>
     * <ol>
     * <li><code>ServerDb</code> host is &laquo;localhost&raquo;;</li>
     * <li>{@link java.net.ServerSocket} is opened on
     * {@link jhelp.JHelp#DEFAULT_DATABASE_PORT};</li>
     * </ol>
     */
    public ServerDb() {
        this(DEFAULT_DATABASE_PORT);
    }

    /**
     * Constructor creates new instance of <code>ServerDb</code>. 
     * @param port defines port for {@link java.net.ServerSocket} object.
     */
    public ServerDb(int port) {
    }

    /**
     * Constructor creates new instance of <code>ServerDb</code>. 
     * @param args array of {@link java.lang.String} type contains connection
     * parameters.
     */
    public ServerDb(String[] args) {
    }

    /**
     * Start method for <code>ServerDb</code> application.
     * @param args array of {@link java.lang.String} type contains connection
     * parameters.
     */
    public static void main(String[] args) {
        ServerDb serverDb = new ServerDb();
        if (serverDb.connect(args) == JHelp.READY) {
            serverDb.run();
        }
    }

    /**
     * Method defines job cycle for client request processing.
     */
    private void run() {
        while(true) {
            try {
                data = (Data) input.readObject();
                if (data.getKey().getState() == JHelp.DISCONNECT) {
                        disconnect();
                    }
                Data answer = getData(data);
                output.writeObject(answer); 
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    /**
     *
     * @return error code. The method returns {@link JHelp#OK} if streams are
     * opened successfully, otherwise the method returns {@link JHelp#ERROR}.
     */
    @Override
    public int connect() {
        System.out.println("SERVERDb: connect");
        return JHelp.READY;
    }

    /**
     * Method sets connection to database and create {@link java.net.ServerSocket}
     * object for waiting of client's connection requests.
     * @return error code. Method returns {@link jhelp.JHelp#READY} in success
     * case. Otherwise method return {@link jhelp.JHelp#ERROR} or error code.
     */
    @Override
    public int connect(String[] args) {
        try {
            String url = getProperties(args).getProperty("url");
            String user = getProperties(args).getProperty("user");
            String password = getProperties(args).getProperty("password");
            conn = DriverManager.getConnection(url, user, password);
            st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            System.out.println("Database connection is OK");
            serverSocket = new ServerSocket(JHelp.DEFAULT_DATABASE_PORT);
            clientSocket = serverSocket.accept();
            input = new ObjectInputStream(clientSocket.getInputStream());
            output = new ObjectOutputStream(clientSocket.getOutputStream());     
        } catch (SQLException | IOException ex) {
            System.out.println("Database connection error: " + ex.getMessage());
            return JHelp.ERROR;
        }
        return JHelp.READY;
    }
    
    public Properties getProperties (String[] args) {
        try {
        String file = args.length > 0 ? args[0] : "/samples/conf.cfg";
        prop = new Properties();
        prop.load(new FileInputStream(new File(file)));
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        return prop;
    }
    
    public String stSelect (String key) {
        String sql = "select tblDefinitions.definition AS Def from tblDefinitions inner join tblTerms on tblDefinitions.TERM_ID = tblTerms.ID where "
                       + "tblTerms.TERM = '" + key + "'";
        return sql;
    }
    
    public String stSelectNext (String key) {
        String sql = "select tblTerms.TERM, tblDefinitions.DEFINITION as DEF from tblDefinitions inner join tblTerms on tblDefinitions.TERM_ID = tblTerms.ID " +
                     "where tblDefinitions.id = ((select tblTerms.ID from tblTerms where tblTerms.TERM = '" + key + "') + 1)";
        return sql;
    }
    
     public String stSelectPrevious (String key) {
        String sql = "select tblTerms.TERM, tblDefinitions.DEFINITION as DEF from tblDefinitions inner join tblTerms on tblDefinitions.TERM_ID = tblTerms.ID " +
                     "where tblDefinitions.id = ((select tblTerms.ID from tblTerms where tblTerms.TERM = '" + key + "') - 1)";
        return sql;
    }
     
    public int nextId(String tblName) {
        int id = 0;
        try {
            String sql = "select max(" + tblName + ".ID) from " + tblName;
            rs = st.executeQuery(sql);
            rs.absolute(1); 
            id = rs.getInt(1);
        } catch (SQLException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        return (id + 1);
     }
     
    public int nextIdFk() {
        int id = 0;
        try {
            String sql = "select tblDefinitions.TERM_ID from tblDefinitions where tblDefinitions.ID = (select max(tblDefinitions.ID) from tblDefinitions)";
            rs = st.executeQuery(sql);
            rs.absolute(1);
            id = rs.getInt(1);
        } catch (SQLException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        return (id + 1);
    }
    
    public String stInsert(int id, String key) {
        String sql = "insert into tblTerms values (" +id+ ", '" + key + "')";
        return sql;
    }
    
    public String stInsert(int id, String value, int fk) {
        String sql = "insert into tblDefinitions values (" +id + ", '" + value + "' ," + fk + ")";
        return sql;
    }

    /**
     * Method returns result of client request to a database.
     * @param data object of {@link jhelp.Data} type with request to database.
     * @return object of {@link jhelp.Data} type with results of request to a
     * database.
     * @see Data
     * @since 1.0
     */
    @Override
    public Data getData(Data data) {
        String sql, sql2;
        try {
            int state = data.getKey().getState();
            switch(state) {
                case(JHelp.SELECT):
                    sql = stSelect(data.getKey().getItem());
                    rs = st.executeQuery(sql);
                    if (rs.next()) {
                        String answer = rs.getString(1);
                        item = new Item(answer, JHelp.SELECT);
                    } else {
                        item = new Item("This termin was not found, please, try again");
                    }
                    data = new Data(JHelp.SELECT, item, JHelp.DEFAULT_VALUES);
                break;  
                
                case(JHelp.INSERT):
                    sql = stInsert(nextId("tblTerms"), data.getKey().getItem());
                    st.addBatch(sql);
                    sql2 = stInsert(nextId("tblDefinitions"), data.getValue().getItem(), nextIdFk());
                    st.addBatch(sql2);
                    st.executeBatch();
                break;   
                
                case(JHelp.UPDATE):
                    sql = "update tblDefinitions set tblDefinitions.DEFINITION = '" + data.getValue().getItem() + "' where tblDefinitions.TERM_ID = "
                        + "(select tblTerms.ID from tblTerms where tblTerms.TERM = '" + data.getKey().getItem() + "')";
                    st.addBatch(sql);
                    st.executeBatch();  
                break;
                
                case(JHelp.DELETE):
                    sql = "delete from tblDefinitions where tblDefinitions.DEFINITION = '" + data.getValue().getItem() + "'";
                    st.addBatch(sql);
                    sql2 = "delete from tblTerms where tblTerms.TERM = '" + data.getKey().getItem() + "'";
                    st.addBatch(sql2);
                    st.executeBatch();
                break;
                
                case(JHelp.NEXT):
                    sql = stSelectNext(data.getKey().getItem());
                    rs = st.executeQuery(sql);
                    rs.absolute(1);
                    key = new Item(rs.getString(1));
                    value = new Item(rs.getString(2));
                    data = new Data(JHelp.SELECT, key, value);    
                break;  
                
                case(JHelp.PREVIOUS):
                    sql = stSelectPrevious(data.getKey().getItem());
                    rs = st.executeQuery(sql);
                    rs.absolute(1);
                    key = new Item(rs.getString(1));
                    value = new Item(rs.getString(2));
                    data = new Data(JHelp.SELECT, key, value); 
                break;    
            }
        } catch (SQLException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        return data;
    }

    /**
     * Method disconnects <code>ServerDb</code> object from a database and closes
     * {@link java.net.ServerSocket} object.
     * @return disconnect result. Method returns {@link #DISCONNECT} value, if
     * the process ends successfully. Othewise the method returns error code,
     * for example {@link #ERROR}.
     * @see jhelp.JHelp#DISCONNECT
     * @since 1.0
     */
    @Override
    public int disconnect() {
        try {
            st.close();
            conn.close();
            input.close();
            output.close();
            clientSocket.close();
            System.exit(0);
        } catch (SQLException | IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        return JHelp.DISCONNECT;
    }
}
