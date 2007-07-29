package no.printf.slumber;

/**
 * JDBC Bridge for the Sleep programming language
 * Copyright (C) 2006 Andreas Ravnestad
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import java.util.*;

import sleep.interfaces.*;
import sleep.runtime.*;
import sleep.bridges.*;
import java.sql.*;


/**
 * JDBC bridge (beta) for the Sleep scripting language
 * --------------------------------------------------------------------
 * This bridge provides functions to connect to and manipulate
 * databases. Note that proper JDBC drivers must be available for this
 * bridge to work.
 * 
 * @author Andreas Ravnestad
 * @since 1.0
 */
public class JDBC implements Loadable
{

    // Number of fetched rows in the previous SQL query
    private static int fetchedRows;


    // Cache for result rows so they don't have to be created
    // for every row fetch
    // private static HashMap rowCache = new HashMap(20);
    // private static HashMap arrayRowCache = new HashMap(20);
    private static HashMap<Connection, Statement> statementCache = new HashMap<Connection, Statement>(20);

    public boolean scriptUnloaded(ScriptInstance s)
    {
        return true;
    }


    public boolean scriptLoaded(ScriptInstance s)
    {

        Hashtable<String, Function> env = (Hashtable<String, Function>)(s.getScriptEnvironment().getEnvironment());

        // Connection
        env.put("&dbConnect",               new JDBC.dbConnect());
        env.put("&dbClose",                 new JDBC.dbClose());

        // Operations
        env.put("&dbQuery",                 new JDBC.dbQuery());
        env.put("&dbUpdate",                new JDBC.dbUpdate());

        // Retrieval
        env.put("&dbFetch",                 new JDBC.dbFetch());
        env.put("&dbAssign",                new JDBC.dbAssign());
        env.put("&dbFetchArray",            new JDBC.dbFetchArray());
        env.put("&dbAssignArray",           new JDBC.dbAssignArray());
        env.put("&dbFetchBuffered",         new JDBC.dbFetchBuffered());
        env.put("&dbFetchBufferedArray",    new JDBC.dbFetchBufferedArray());

        // Prepared statements
        env.put("&dbPrepare",               new JDBC.dbPrepare());
        env.put("&dbSet",                   new JDBC.dbSet());
        env.put("&dbExec",                  new JDBC.dbExecute());
        env.put("&dbExecute",               new JDBC.dbExecute());

        // Utilities
        env.put("&dbFetchedRows",           new JDBC.dbFetchedRows());

        return true;
    }


    /**
     * This function connects to a database and returns a handle to that connection
     * This handle must be used for further operations on that connection.
     */
    private static class dbConnect implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = -1973360117804535146L;

        public Scalar evaluate(String n, ScriptInstance i, Stack args)
        {

            // Declarations
            Connection con = null;

            // Fetch parameters
            String driver = BridgeUtilities.getScalar(args).stringValue();
            String url = BridgeUtilities.getScalar(args).stringValue();
            String username = BridgeUtilities.getScalar(args).stringValue();
            String password = BridgeUtilities.getScalar(args).stringValue();

            // Load the driver
            try {
                Class.forName(driver);
            }
            catch(ClassNotFoundException e) {
                i.getScriptEnvironment().flagError(e.getMessage());
            }

            // Connect to the database
            try {
                con = DriverManager.getConnection(url, username, password);
            }
            catch(SQLException e) {
                i.getScriptEnvironment().flagError(e.getMessage());
            }

            // Return the connection handle
            return SleepUtils.getScalar(con);
        }
    }

    /**
     * This function will attempt to close the connection to a database
     */
    private static class dbClose implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = -2282906875493940983L;

        public Scalar evaluate(String n, ScriptInstance i, Stack args)
        {

            Connection con = (Connection)BridgeUtilities.getObject(args);

            try {
                con.close();
            }

            catch(SQLException e) {
                i.getScriptEnvironment().flagError(e.getMessage());
            }

            return SleepUtils.getEmptyScalar();

        }
    }


    /**
     * This function will execute a query on a database and return a
     * resultset, if any.
     */
    private static class dbQuery implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = 9045071276866110205L;

        public Scalar evaluate(String name, ScriptInstance inst, Stack args)
        {
            // Get the connection handle
            Connection connection = (Connection)BridgeUtilities.getObject(args);

            // Get query string
            String query = BridgeUtilities.getScalar(args).stringValue();

            try {

                // Execute the query and store the resultset
                return SleepUtils.getScalar(JDBC.query(connection, query));
            }
            catch(SQLException e) {
                inst.getScriptEnvironment().flagError(e.getMessage());
            }

            // Return the resultset
            return SleepUtils.getEmptyScalar();
        }
    }


    /**
     * Executes an update on the database
     */
    private static class dbUpdate implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = -3641225601120176753L;

        public Scalar evaluate(String n, ScriptInstance i, Stack args)
        {

            // Reset the number of fetched rows; this field
            // will not be affected by this operation.
            JDBC.fetchedRows = 0;

            // Fetch connection argument
            Connection connection = (Connection)BridgeUtilities.getObject(args);

            // Fetch update string argument
            String update = BridgeUtilities.getScalar(args).stringValue();

            try {

                // Perform the update on the database
                return SleepUtils.getScalar(JDBC.update(connection, update));
            }
            catch(SQLException e) {
                i.getScriptEnvironment().flagError(e.getMessage());
            }

            // Return default value
            return SleepUtils.getScalar(0);
        }
    }


    /**
     * Fetches the next row in a ResultSet and returns it to the
     * calling script as a ScalarHash.
     */
    private static class dbFetch implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = 7786205027362380251L;

        public Scalar evaluate(String name, ScriptInstance inst, Stack args)
        {

            JDBC.fetchedRows = 0;

            // Declarations
            ResultSet result = (ResultSet)BridgeUtilities.getObject(args);


            // Fetch next row
            RowScalarHash row = JDBC.getNextRow(result);

            if (row != null) {
                return SleepUtils.getHashScalar(row);
            }
            else {
                return SleepUtils.getEmptyScalar();
            }
        }
    }


    /**
     * Fetches an entire ResultSet, buffers it in an array, and
     * returns it to the calling script as a ScalarArray containing
     * a bunch of hashes representing rows.
     */
    private static class dbFetchBuffered implements Function
    {

        /**
         * 
         */
        private static final long serialVersionUID = 6447737885474811855L;

        public Scalar evaluate(String name, ScriptInstance inst, Stack args)
        {
            // Declarations
            RowScalarHash row;
            ResultSet result = (ResultSet)BridgeUtilities.getObject(args);
            ScalarArray bufferedResult = SleepUtils.getArrayScalar().getArray();
            JDBC.fetchedRows = 0;

            // Fetch all results and push it onto array
            while ((row = JDBC.getNextRow(result)) != null) {
                bufferedResult.push(SleepUtils.getHashScalar((RowScalarHash)row.clone()));

                JDBC.fetchedRows++;
            }

            // Return the array to the calling script
            return SleepUtils.getArrayScalar(bufferedResult);
        }

    }


    /**
     * Fetches the next row in a ResultSet and returns it to the
     * calling script as a ScalarHash.
     */
    private static class dbFetchArray implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = -142632406553795689L;

        public Scalar evaluate(String name, ScriptInstance inst, Stack args)
        {
            JDBC.fetchedRows = 0;

            // Declarations
            ResultSet result = (ResultSet)BridgeUtilities.getObject(args);


            // Fetch next row
            ScalarArray row = JDBC.getNextRowAsArray(result);

            if (row != null) {
                return SleepUtils.getArrayScalar(row);
            }
            else {
                return SleepUtils.getEmptyScalar();
            }
        }
    }

    /**
     * Fetches an entire ResultSet, buffers it in an array, and
     * returns it to the calling script as a ScalarArray containing
     * a bunch of hashes representing rows.
     */
    private static class dbFetchBufferedArray implements Function
    {

        /**
         * 
         */
        private static final long serialVersionUID = -8954205487071612742L;

        public Scalar evaluate(String name, ScriptInstance inst, Stack args)
        {
            // Declarations
            ScalarArray row;
            ResultSet result = (ResultSet)BridgeUtilities.getObject(args);
            ScalarArray bufferedResult = SleepUtils.getArrayScalar().getArray();
            JDBC.fetchedRows = 0;

            // Fetch all results and push it onto array
            while ((row = JDBC.getNextRowAsArray(result)) != null) {
                bufferedResult.push(SleepUtils.getArrayScalar(row));
                JDBC.fetchedRows++;
            }

            // Return the array to the calling script
            return SleepUtils.getArrayScalar(bufferedResult);
        }

    }


    /**
     * Fetches the next row in a ResultSet and puts it into the
     * second argument. This function also returns the row.
     */
    private static class dbAssign implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = 5042274872632142933L;

        public Scalar evaluate(String name, ScriptInstance inst, Stack args)
        {
            JDBC.fetchedRows = 0;

            // Declarations
            ResultSet result = (ResultSet)BridgeUtilities.getObject(args);
            Scalar opt = BridgeUtilities.getScalar(args);

            // Fetch next row
            RowScalarHash row = JDBC.getNextRow(result);

            if (row != null) {
                opt.setValue(SleepUtils.getHashScalar(row));
                return SleepUtils.getHashScalar(row);
            }
            else {
                opt.setValue(SleepUtils.getEmptyScalar());
                return SleepUtils.getEmptyScalar();
            }
        }
    }


    /**
     * Fetches the next row in a ResultSet and returns it to the
     * calling script as a ScalarHash.
     */
    private static class dbAssignArray implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = 666751977825529741L;

        public Scalar evaluate(String name, ScriptInstance inst, Stack args)
        {
            JDBC.fetchedRows = 0;

            // Declarations
            ResultSet result = (ResultSet)BridgeUtilities.getObject(args);
            Scalar opt = BridgeUtilities.getScalar(args);


            // Fetch next row
            ScalarArray row = JDBC.getNextRowAsArray(result);

            if (row != null) {
                opt.setValue(SleepUtils.getArrayScalar(row));
                return SleepUtils.getArrayScalar(row);
            }
            else {
                opt.setValue(SleepUtils.getEmptyScalar());
                return SleepUtils.getEmptyScalar();
            }
        }
    }


    /**
     * Returns the number of fetched rows in the previous query
     */
    private static class dbFetchedRows implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = -3923959881422396953L;

        public Scalar evaluate(String name, ScriptInstance inst, Stack args)
        {
            return SleepUtils.getScalar(JDBC.getFetchedRows());
        }
    }


    /**
     * Creates a prepared statement
     * 
     * @author Andreas
     *
     */
    private static class dbPrepare implements Function
    {

        /**
         * 
         */
        private static final long serialVersionUID = 1687493511177917567L;

        /* (non-Javadoc)
         * @see sleep.interfaces.Function#evaluate(java.lang.String, sleep.runtime.ScriptInstance, java.util.Stack)
         */
        public Scalar evaluate(String name, ScriptInstance instance, Stack args) {

            /* Check number of arguments */
            if (args.size() < 2) {
                instance.getScriptEnvironment().flagError("Not enough arguments");
                return null;
            }
            
            /* Declare prepared statement */
            PreparedStatement statement = null;
            
            /* Get the connection handle */
            Connection connection = (Connection)BridgeUtilities.getObject(args);
            
            /* Get the query */
            String query = BridgeUtilities.getScalar(args).stringValue();
            
            /* Create a prepared statement */
            try {
                statement = connection.prepareStatement(query);
            
                if (args.size() > 0) {
                    /* User decided to set values at once */
                    
                    int size = args.size();
                    
                    for(int i = 0; i < size; i++) {
                        
                        /* Get argument from stack */
                        Object arg = BridgeUtilities.getObject(args);
                        
                        /* Set value in prepared statement */
                        statement.setObject(i + 1, arg);
                    }
                }
            } 

            catch(Exception e) {
                instance.getScriptEnvironment().flagError(e.getMessage());
                return null;
            }
            
            return SleepUtils.getScalar(statement);
        }
        
    }
    
    
    /**
     * Set a value in a prepared statement
     * 
     * @author Andreas
     *
     */
    private static class dbSet implements Function
    {
        /**
         * 
         */
        private static final long serialVersionUID = 7883375884590149183L;

        /* (non-Javadoc)
         * @see sleep.interfaces.Function#evaluate(java.lang.String, sleep.runtime.ScriptInstance, java.util.Stack)
         */
        public Scalar evaluate(String name, ScriptInstance instance, Stack args) {

            /* Check number of arguments */
            if (args.size() < 3) {
                instance.getScriptEnvironment().flagError("Not enough arguments");
                return null;
            }
            
            /* Get arguments */
            PreparedStatement stmt = (PreparedStatement)BridgeUtilities.getObject(args);
            int index = BridgeUtilities.getInt(args);
            Object value = BridgeUtilities.getObject(args);
            
            /* Set value */
            try {
                stmt.setObject(index, value);
            } 
            catch(Exception e) {
                instance.getScriptEnvironment().flagError(e.getMessage());
                return SleepUtils.getScalar(false);
            }
            
            return SleepUtils.getScalar(true);
        }
        
    }
    
    /**
     * Set a value in a prepared statement
     * 
     * @author Andreas
     *
     */
    private static class dbExecute implements Function
    {

        /**
         * 
         */
        private static final long serialVersionUID = 8088754343975056604L;

        /* (non-Javadoc)
         * @see sleep.interfaces.Function#evaluate(java.lang.String, sleep.runtime.ScriptInstance, java.util.Stack)
         */
        public Scalar evaluate(String name, ScriptInstance instance, Stack args) {
            
            /* Check number of arguments */
            if (args.size() < 1) {
                instance.getScriptEnvironment().flagError("Not enough arguments");
                return null;
            }
            
            /* Get arguments */
            PreparedStatement stmt = (PreparedStatement)BridgeUtilities.getObject(args);
            
            try {
                return SleepUtils.getScalar(stmt.execute());
            } 
            catch(Exception e) {
                instance.getScriptEnvironment().flagError(e.getMessage());
                return SleepUtils.getScalar(false);                
            }
        }
    } 
    
    
    /**
     * This is a generic helper method that fetches the next result from
     * a resultset, stores it in a RowScalarHash, and returns it. This
     * is probably a good place to look for bottlenecks.
     * TODO: Profiling and optimization
      */
    private static RowScalarHash getNextRow(ResultSet result) {

        // Create new row hash
        RowScalarHash row = new RowScalarHash();

        try {

            // Check if there are any more rows available in the given result set
            if (result.next()) {

                // Get the meta data
                ResultSetMetaData resultmd = result.getMetaData();

                // Populate the hash with columns and data
                int t = resultmd.getColumnCount();

                // This kinda sucks, but it's the best I could think of.
                // This code populates the row map with values mapped to
                // keys (columns)
                for (int i = 1; i <= t; i++) {

                    // Put column/value into row hash
                    row.put(resultmd.getColumnLabel(i), SleepUtils.getScalar(result.getString(i)));
                }

                return row;

            }
            else {
                return null;
            }
        }
        catch(SQLException e) {
            throw new RuntimeException("row fetch failed (" + e.getMessage() + ")");
        }
    }


    /**
     * This is a generic helper method that fetches the next row from the result set
     * and returns it in a ScalarArray.
     */
    private static ScalarArray getNextRowAsArray(ResultSet result) {

        ScalarArray row = SleepUtils.getArrayScalar().getArray();  // Create new row array

        try {

            // Check if there are any more rows available in the given result set
            if (result.next()) {

                int t = result.getMetaData().getColumnCount();

                for (int i = 1; i <= t; i++) {

                    // Push onto array
                    row.push(SleepUtils.getScalar(result.getString(i)));
                }

                return row;

            }
            else {
                return null;
            }
        }
        catch(SQLException e) {
            throw new RuntimeException("row fetch failed (" + e.getMessage() + ")");
        }
    }

    

    /**
     * This is a custom ScalarHash implementation. Using this instead of the standard
     * ScalarHash returned from SleepUtils cuts down on time drastically by using the
     * HashMap.put() method instead of the ScalarHash.getAT() method.
     */
    private static class RowScalarHash extends sleep.engine.types.HashContainer {

        /**
         * 
         */
        private static final long serialVersionUID = -962941671697749347L;

        public RowScalarHash() {
            super();
        }

        public RowScalarHash(HashMap _values) {
            this.values = _values;
        }

        public Scalar put(String key, Scalar value) {
            return (Scalar) (((HashMap<String,Scalar>)this.values).put(key, value));
        }

        public Scalar get(String key) {
            return (Scalar) this.values.get(key);
        }

        public Object clone() {
            return (Object)(new RowScalarHash((HashMap)this.values.clone()));
        }

        public String toString() {
            return this.values.toString();
        }
    }

    // Returns the number of fetched rows in the last query,
    // or 0 if no rows were fetched.
    public static int getFetchedRows() {
        return JDBC.fetchedRows;
    }

    /**
     * Returns a new statement object, either an existing one
     * from cache, or a brand new one.
     */
    private static Statement getStatement(Connection c) {

        // Try to fetch from cache first
        Statement stmt = JDBC.statementCache.get(c);

        // Check if it existed in cache
        if (stmt == null) {
            try {

                // Create new statement
                stmt = c.createStatement();
            }
            catch(SQLException e) {

                // This would be a pretty bad error, so we throw an exception
                throw new RuntimeException(e.getMessage());
            }

            // Check if cache is too big yet
            if (JDBC.statementCache.size() >= 20) {

                // Flush the cache
                JDBC.statementCache = new HashMap<Connection, Statement>(20);
            }

            // Put the new statement object into the cache
            JDBC.statementCache.put(c, stmt);
        }

        // Return statement object
        return stmt;
    }


    /**
     * Performs a query on the database and returns the resultset
     */
    private static ResultSet query(Connection c, String query) throws SQLException {

        ResultSet result;

        // Fetch statement
        Statement stmt = JDBC.getStatement(c);

        try {
            // Execute the query and store the resultset
            result = stmt.executeQuery(query);
        }
        catch(SQLException e) {
            throw e;
        }

        return result;
    }

    /**
     * Performs an update on the database and returns the number of affected rows
     */
    private static int update(Connection connection, String update) throws SQLException {

        int affectedRows;

        // Fetch statement
        Statement stmt = JDBC.getStatement(connection);

        try {
            // Execute the query and store the resultset
            affectedRows = stmt.executeUpdate(update);
        }
        catch(SQLException e) {
            throw e;
        }

        return affectedRows;
    }
}
