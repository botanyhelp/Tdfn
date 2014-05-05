/**Copyright (C) 2011 the University of Pittsburgh
 * Author: Thomas Maher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package edu.pitt.dbmi.facebase.hd;

import java.sql.ResultSet;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import au.com.bytecode.opencsv.CSVWriter;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.math.BigInteger;
import org.apache.log4j.Logger; 
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import edu.pitt.dbmi.facebase.hd.domain.FbKey;
import edu.pitt.dbmi.facebase.hd.MySQLHibernateConfigurationHd;

/** Performs human data related operations.  
 * Manages access to encryption keys.
 * Creates encryption keys.
 * Gathers user-requested SQL data from human data server's mysql database via Hibernate, extracts data to CSV
 * 
 * @author Facebase Java Authors, http://www.facebase.org/
 */
public class HumanDataManager
{
    private static MySQLHibernateConfigurationHd hdconf = null;
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.HumanDataManager.class");

    /** Sets up Hibernate connection to human data server's mysqlDB  */
    public HumanDataManager(String hdDb, String hdPasswd, String hdJdbcUrl) {
        hdconf = new MySQLHibernateConfigurationHd(hdDb, hdPasswd, hdJdbcUrl);
    }

    /**  Sanitize SQL string by checking it for bad words
     * Does not repair SQL--only causes it to not be executed. 
     *
     * @param suspectUserSql the provided SQL query string (ie SELECT * FROM table)
     * @return value "OK" if ok, otherwise it tosses a status="error" condition and returns a bad string. 
     */
    public String sanitizeSql(String suspectUserSql) {
        String[] disallowedSqlTokensArray = {"waitfor", "delay", "encode(", "char(", "character(", "benchmark(", "load_file(", "outfile", "infile", "if(", "into", "cast(", "version", "/*", "*/", "%0", "insert", "update", "union", "drop", "truncate", "create", "change", "declare", "describe", "exit", "exists", "grant", "loop", "leave", "modifies", "purge", "rename", "replace", "restrict", "return", "undo", "information_schema", "database", "databases", "schema", "schemas", "users", "roles", "role_permission", "fb_keychain", "sessions", "field_data_", "field_revision_", "cache", "watchdog", "variables", "system"};
        ArrayList<String> disallowedSqlTokens = new ArrayList<String>();
        for(String badToken : disallowedSqlTokensArray) {
            disallowedSqlTokens.add(badToken.toLowerCase());
        }
        String[] userSqlArray = suspectUserSql.split(" ");
        ArrayList<String> tokenizedUserSql = new ArrayList<String>();
        for(String token : userSqlArray) {
            tokenizedUserSql.add(token.toLowerCase());
        }
        for (String badToken : disallowedSqlTokens){
            if (tokenizedUserSql.contains(badToken)) {
                String errorString = "BAD SQL was provided! Query NOT permitted because it contains: " + badToken;
                String logString = "";
                //bad sql won't be executed and so does not need to cause status="error" condition for this request; other files can go out okay
                //However, if the following line is uncommented, then a bad sql query string will cause status="error":
                edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
                log.error(errorString);
                return errorString;
            }
        }
        //The suspectUserSql is okay, and the caller will expect sanitizeSql() to return "OK" to indicate all is well:
        return new String("OK");
    }

    /** Use provided SQL query string to query database and build CSV-formatted String
     * Sanitizes provided SQL query strings. 
     * Converts JDBC ResultSet object to CSV String data--two mechanisms are contained in this source file, one is commented out.
     * <ol>
     * <li>Manually parsing each row in the ResultSet object and printing a line to an accumulating String of CSV data</li>
     * <li>Using au.com.bytecode.opencsv.CSVWriter to convert the ResultSet to String--this STILL requires manually parsing to obtain the header row column names</li>
     * </ol>
     * <b>Both schemes can produce usable CSV data...as of Jan2012, CSVWriter mechanism is being used. </b>
     * <b>Both schemes populate a String in memory.  Thus large ResultSets could crash the program by overconsuming memory--if large ResultSets are required, this program should be edited to instead buffer the data to a file</b>
     *
     * @param userSql the provided SQL query string (ie SELECT * FROM table)
     * @return double-quote delimited CSV data as a String where the first line has column-name header row, all subsequent rows are data
     */
    public String getCsvData(String userSql) {
        log.debug("HumanDataManager.getCsvData() called");
        Session session = null;
        Transaction transaction = null;
        String metaData = "";
        String csvData = "";

        //Here we'll do some basic sanitization of the supplied sql:
        String isSqlOk = sanitizeSql(userSql);
        if(isSqlOk.equals("OK")) {
            log.debug("SQL provided passed sanitization tests:");
            log.debug(userSql);
        } else {
            log.debug("SQL provided FAILED sanitization tests:");
            return isSqlOk;
        }

        try
        {
            session = hdconf.openSession();
            log.debug("HumanDataManager.getCsvData() about to execute SQL:");
            log.debug(userSql);
            ResultSet results = session.connection().createStatement().executeQuery(userSql);
/*
 *          We can do get the csv from the ResultSet manually, as shown over the next dozen 
 *          or so lines, OR, we can use CSVWriter from the imported jar, as shown in the few 
 *          lines following.  Don't use both--use manual OR CSVWriter.
 *          ResultSetMetaData rsMeta = results.getMetaData();
 *          int columns = rsMeta.getColumnCount();
 *          while(results.next()) {
 *              String rowAsString = "";
 *              for (int i = 1; i <= columns; i++) {
 *                  Object value = results.getObject(i);
 *                  if(value == null) {
 *                      rowAsString += "\",\"";
 *                  }else{
 *                      rowAsString += "\"";
 *                      rowAsString += value.toString();
 *                      rowAsString += "\"";
 *                  }
 *                  if(i != columns) {
 *              	rowAsString += ",";
 *                  }
 *              }
 *              csvData += rowAsString;
 *              csvData += "\n";
 *          }
 *
 *
 *          Here we use CSVWriter to get a csv String  from the ResultSet, using the imported 
 *          jar.
 */
            ResultSetMetaData rsmd = results.getMetaData();
            int columnCount = rsmd.getColumnCount();
            String rowAsString = "";
            for (int i = 1; i < columnCount + 1; i++ ) {
                String value = rsmd.getColumnName(i);
                if(value == null) {
                    rowAsString += "\",\"";
                }else{
                    rowAsString += "\"";
                    rowAsString += value.toString();
                    rowAsString += "\"";
                }
                if(i != columnCount) {
                    rowAsString += ",";
                }
            }
            csvData += rowAsString;
            csvData += "\n";
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            csvWriter.writeAll(results, false);
            csvWriter.close();
            csvData += stringWriter.toString();
            return csvData;
        }catch(Throwable t){
            String errorString = "getCsvData threw this: " + t.toString();
            String logString = t.getMessage();
            edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
            log.error(errorString, t);
            handleThrowable(t, session, transaction);
        }	
        return "BADDATA";
    }
    
    /** Gets a key for the userid
     * calls getFbKey()
     *
     * @param uid the user id whose key we want
     * @return the FbKey object with the user's key
     */
    FbKey queryKey(Integer uid) {
        log.debug("HumanDataManager.queryKey() called");
        Session sessionHd = null;
        FbKey key = new FbKey();
        Transaction transactionHd = null;
        try
        {
            sessionHd = hdconf.openSession();
            transactionHd = sessionHd.beginTransaction();
            key = getFbKey(sessionHd, uid);
            sessionHd.flush();
            transactionHd.commit();
            sessionHd.close();
        } catch(Throwable t){
            String errorString = "queryKey() threw this: " + t.toString();
            String logString = t.getMessage();
            edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
            log.error(errorString, t);
            handleThrowable(t, sessionHd, transactionHd);
        }
        
        return(key);
    }
    
    /** Gets a key for the userid from fb_keychain via Hibernate
     * Tries to get a key if one exists; otherwise it creates and stores a new key
     * Calls makeKey() if a new key is needed
     * 
     * @param sessionHd for Hibernate to access table
     * @param uid the user id whose key we want
     * @return the FbKey object with the user's key
     */
    FbKey getFbKey(Session sessionHd, Integer uid) {
        log.debug("HumanDataManager.getFbKey() called");
        FbKey key;
        Criteria cHd = sessionHd.createCriteria(FbKey.class).add(Restrictions.like("uid",uid));
        List<FbKey> results = cHd.list();
        if(results.size() == 0) {
            key = new FbKey();
            long unixEpicTime = System.currentTimeMillis()/1000L;
            key.setCreated(unixEpicTime);
            key.setUid(uid);
            key.setEncryption_key(this.makeKey());
            sessionHd.save(key);
        }else{
            key = results.get(0);
        }
        return(key);
    }
    
    /** Makes a new 20-character random key 
     * 
     * @param uid the user id whose key we want
     * @return the FbKey object with the user's key
     */
    String makeKey() {
        log.debug("HumanDataManager.makeKey() called");
        //Here we need to make a real key-random
        SecureRandom random = new SecureRandom();
        String key = new BigInteger(130, random).toString(32).substring(0,20);
        return key;
    }

    /** Deals with exceptions gracefully 
     * Called from catch clause
     *
     * @param t the exception
     * @param sessionHd Hibernate session
     * @param transactionHd Hibernate's Transaction object, for rolling back unfinished transactions
     */
    static void handleThrowable(Throwable t, Session sessionHd, Transaction transactionHd)
    {
        log.error("An unexpected error occured while talking to a database.", t);
        if(transactionHd != null)
        {
            try{transactionHd.rollback();}
            catch(Throwable t1){log.error("Unable to rollback a transactionHd.", t1);}
        }
        if(sessionHd != null)
        {
            try{sessionHd.close();}
            catch(Throwable t2){log.error("Unable to close a sessionHd.", t2);}
        }
    }
    
}
