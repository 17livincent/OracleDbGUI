/**
 * DbGUI.java
 * @author Vincent Li <vincentl@asu.edu>
 * A graphical user interface to interact with one of the tables of the CSE412 DB project.
 * The table it interacts with is the "employee" table.
 */
package honorsproject;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.ArrayList;

public class DbGUI {
    private static final String CONNECTIONSTRING = "jdbc:oracle:thin:@acaddb2.asu.edu:1521:orcl";
    private static final String USERNAME = "vincentl";
    private static final String PW = "vincentl";

    private static final String TABLENAME = "employee";
    private static final String[] COLUMNS = {"employeeID", "fName", "mI", "lName"};

    private Connection con;
    private Statement stmt;

    private String[][] data;
    private String[][] dataCopy;

    private JFrame frame;
    private JScrollPane scroll;
    private JTable table;
    private JButton refreshButton;
    private JButton updateButton;
    private JTextField insertEmpID;
    private JTextField insertFName;
    private JTextField insertMI;
    private JTextField insertLName;
    private JButton insertButton;
    private JTextArea queryInput;
    private JButton runQueryButton;
    
    /**
     * constructor
     */
    public DbGUI() {
        try {
            // register
            Class.forName("oracle.jdbc.driver.OracleDriver");
            // connect
            con = DriverManager.getConnection(CONNECTIONSTRING, USERNAME, PW);
            stmt = con.createStatement();
        }
        catch(ClassNotFoundException e) {
            System.out.println("Error: ClassNotFoundException at constructor");
        }
        catch(SQLException e) {
            System.out.println("Error: SQLException at constructor");
        }

        frame = new JFrame("DB Interface: Table Employee");
        refreshButton = new JButton("Refresh");
        updateButton = new JButton("Update Table");
        insertEmpID = new JTextField();
        insertFName = new JTextField();
        insertMI = new JTextField();
        insertLName = new JTextField();
        insertButton = new JButton("Insert");
        queryInput = new JTextArea("Enter a query", 3, 30);
        runQueryButton = new JButton("Run query");

        // action for close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    // close connection
                    con.close();
                }
                catch(SQLTimeoutException E) {
                    System.out.println("Error: Connection timed out");
                }
                catch(SQLException E) {
                    System.out.println("Error: Connection failed");
                }
                finally {
                    System.exit(0);
                }
            }
        });

        // action for refresh button
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setLatest();
                refreshTable();
            }
        });

        // action for update button
        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // update the tuples in the DB that were changed in the table
                updateTable();
            }
        });

        // action for insert button
        insertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // insert a new tuple
                insertIntoTable();
            }
        });

        runQueryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // run a query
                runQuery();
            }
        });
    }

    public static void main(String[] args) {
        // create instance
        DbGUI db = new DbGUI();
        // set up GUI
        db.setupGUI();
    }

    public void setupGUI() {
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        //frame.setSize(100, 300);
        frame.setDefaultCloseOperation(3);
        // create the table with data from the employee table
        JLabel tableLabel = new JLabel("View and Update Data");
        tableLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        setLatest();
        table = getLatestTable();
        scroll = new JScrollPane(table);
        frame.getContentPane().add(tableLabel);
        frame.getContentPane().add(scroll);
        // buttons for the table
        JPanel tableButtonGroup = new JPanel();
        tableButtonGroup.add(refreshButton);
        tableButtonGroup.add(updateButton);
        tableButtonGroup.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.getContentPane().add(tableButtonGroup);
        // components for inserting data
        JPanel insertGroup = new JPanel();
        insertGroup.setLayout(new GridLayout(4, 2));
        JLabel insertLabel = new JLabel("Insert Data");
        JLabel empIDLabel = new JLabel(COLUMNS[0]);
        JLabel fNameLabel = new JLabel(COLUMNS[1]);
        JLabel mILabel = new JLabel(COLUMNS[2]);
        JLabel lNameLabel = new JLabel(COLUMNS[3]);
        insertGroup.add(empIDLabel);
        insertGroup.add(insertEmpID);
        insertGroup.add(fNameLabel);
        insertGroup.add(insertFName);
        insertGroup.add(mILabel);
        insertGroup.add(insertMI);
        insertGroup.add(lNameLabel);
        insertGroup.add(insertLName);
        insertLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        insertGroup.setAlignmentX(Component.CENTER_ALIGNMENT);
        insertButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.getContentPane().add(insertLabel);
        frame.getContentPane().add(insertGroup);
        frame.getContentPane().add(insertButton);
        // components for running a query
        JPanel queryGroup = new JPanel();
        JLabel queryLabel = new JLabel("Run a Query");
        queryGroup.add(queryInput);
        queryGroup.add(runQueryButton);
        queryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        queryGroup.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.getContentPane().add(queryLabel);
        frame.getContentPane().add(queryGroup);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Refreshes the displayed table with the current data in the DB
     */
    public void refreshTable() {
        table = getLatestTable();
        ((TModel)table.getModel()).fireTableDataChanged();
        table.repaint();
        scroll.repaint();
        frame.repaint();
    }

    /**
     * Updates/deletes a row in the table in the DB with changes made in the displayed table
     */
    public void updateTable() {
        // find rows that have been changed
        ArrayList<Integer> changed = new ArrayList<>();
        for(int i = 0; i < data.length; i++) {
            // if the tuple in i at dataCopy and data are different, set changed[i] to true
            if(!Arrays.equals(dataCopy[i], data[i])) changed.add(i);
        }
        System.out.println(changed.toString());

        // for each row that has been changed, execute an UPDATE or DELETE statement
        // primary key is employeeID
        int index;
        for(int i = 0; i < changed.size(); i++) {
            index = changed.get(i);
            String command;
        
            // if all of a row's contents were erased, then delete this row
            if(dataCopy[index][0].isEmpty()
                && dataCopy[index][1].isEmpty()
                && dataCopy[index][2].isEmpty()
                && dataCopy[index][3].isEmpty()) {
                command = "DELETE FROM " + TABLENAME + " WHERE " + COLUMNS[0] + "='" + data[index][0] + "'";
            }
            else {  // do an update
                command = "UPDATE " + TABLENAME + " SET " 
                    + COLUMNS[1] + "='" + dataCopy[index][1] + "', "
                    + COLUMNS[2] + "='" + dataCopy[index][2] + "', "
                    + COLUMNS[3] + "='" + dataCopy[index][3] + "' "
                    + "WHERE " + COLUMNS[0] + "='" + data[index][0] + "'";
            }
            try {
                System.out.println(command);
                stmt.executeUpdate(command);
            }
            catch(SQLTimeoutException e) {
                System.out.println("Error: SQLTimeoutException at updateTable");
                e.printStackTrace();
            }
            catch(SQLException e) {
                System.out.println("Error: SQLException at updateTable");
                e.printStackTrace();
            }
        }
        // refresh the table once the DB table is updated
        setLatest();
        refreshTable();
    }

    /**
     * Inserts a new tuple into the table
     */
    public void insertIntoTable() {
        // get text
        String empID = insertEmpID.getText().trim();
        String fName = insertFName.getText().trim();
        String mI = insertMI.getText().trim();
        String lName = insertLName.getText().trim();

        // if empID, fName, and lName aren't blank, insert
        if(mI == null) mI = "";
        if(!empID.equals("") && !fName.equals("") && !lName.equals("")) {
            String command = "INSERT INTO " + TABLENAME + " VALUES ('" + empID + "', '" + fName + "', '" + mI + "', '" + lName + "')";
            System.out.println(command);
            try {
                stmt.executeUpdate(command);
                // clear the text fields
                insertEmpID.setText(null);
                insertFName.setText(null);
                insertMI.setText(null);
                insertLName.setText(null);
            }
            catch(SQLTimeoutException e) {
                System.out.println("Error: SQLTimeoutException at insertIntoTable");
            }
            catch(SQLException e) {
                System.out.println("Error: SQLException at insertIntoTable");
            }
        }

        // refresh the table
        setLatest();
        refreshTable();
    }

    /**
     * Runs the query given by the user and displays the results
     */
    public void runQuery() {
        String command = queryInput.getText().trim();
        System.out.println(command);

        if(!command.isEmpty()) {
            try {
                ResultSet res = stmt.executeQuery(command);
                // update data with this new query
                updateData(res);
            }
            catch(SQLTimeoutException e) {
                System.out.println("Error: SQLTimeoutException at runQuery");
            }
            catch(SQLException e) {
                System.out.println("Error: SQLException at runQuery");
            }
        }

        // refresh the table
        refreshTable();
    }

    /**
     * Gets the current contents in the employee table and updates data.
     */
    public void setLatest() {
        try {
            // get the latest
            ResultSet res = stmt.executeQuery("SELECT * FROM " + TABLENAME + " ORDER BY " + COLUMNS[0] + " ASC");
            // update data
            updateData(res);
        }
        catch(SQLTimeoutException e) {
            System.out.println("Error: SQLTimeoutException at setLatest");
        }
        catch(SQLException e) {
            System.out.println("Error: SQLException at setLatest");
        }
    }

    public void updateData(ResultSet res) {
        try {
            ArrayList<String> latestEmployeeID = new ArrayList<>();
            ArrayList<String> latestFName = new ArrayList<>();
            ArrayList<String> latestMI = new ArrayList<>();
            ArrayList<String> latestLName = new ArrayList<>();
            while(res.next()) {
                latestEmployeeID.add(res.getString(COLUMNS[0]));
                latestFName.add(res.getString(COLUMNS[1]));
                latestMI.add(res.getString(COLUMNS[2]));
                latestLName.add(res.getString(COLUMNS[3]));
            }
        
            data = new String[latestEmployeeID.size()][];
            dataCopy = new String[latestEmployeeID.size()][];

            for(int i = 0; i < latestEmployeeID.size(); i++) {
                Tuple tuple = new Tuple(latestEmployeeID.get(i), 
                                        latestFName.get(i), 
                                        latestMI.get(i), 
                                        latestLName.get(i));
                data[i] = tuple.toArray();
                dataCopy[i] = tuple.toArray();
            }
        }
        catch(SQLException e) {
            System.out.println("Error: SQLException at updateData");
        }
    }

    /**
     * Prints the results from table employee.
     */
    public static void printResultSet(ResultSet rs) {
        if(rs != null) {
            try {
                while(rs.next()) {
                    System.out.println(rs.getString(COLUMNS[0]) + '\t' + rs.getString(COLUMNS[1]) + '\t' + rs.getString(COLUMNS[2]) + '\t' + rs.getString(COLUMNS[3]));
                }
            }
            catch(SQLException e) {
                System.out.println("Error: SQL exception at printResultSet");
            }
        }
        else {
            System.out.println("ResultSet is empty");
        }
    }

    /**
     * Creates a new JTable from the data in the latest arrays.
     */
    public JTable getLatestTable() {        
        return new JTable(new TModel());
    }

    /**
     * An implementation of AbstractTableModel to manage the table.
     */
    class TModel extends AbstractTableModel {
        public int getColumnCount() {
            return COLUMNS.length;
        }
    
        public int getRowCount() {
            return dataCopy.length;
        }
        
        @Override
        public String getColumnName(int col) {
            return COLUMNS[col];
        }

        public Object getValueAt(int row, int col) {
            return dataCopy[row][col];
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            dataCopy[row][col] = (String) value;
            fireTableCellUpdated(row, col);
        }
    
    }
}