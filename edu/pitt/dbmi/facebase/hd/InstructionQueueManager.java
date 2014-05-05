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
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.LinkedHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import org.json.simple.*;
import org.json.simple.parser.*;

import edu.pitt.dbmi.facebase.hd.domain.InstructionQueueItem;
import edu.pitt.dbmi.facebase.hd.MySQLHibernateConfiguration;

import org.apache.log4j.Logger;


/** Communicates with Hub's mysql database fb_queue--gathers new queue items and updates queue record with progress information. 
 * Uses Hibernate to interact with Hub's mysql fb_queue table of queue items (user requests for human data)
 * 
 * @author Facebase Java Authors, http://www.facebase.org/
 */
public class InstructionQueueManager
{
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.InstructionQueueManager.class");
    private static MySQLHibernateConfiguration conf = null;

    /** Sets up Hibernate connection to Hub server's mysqlDB   */
    public InstructionQueueManager(String fbDbUser, String fbPasswd, String fbJdbcUrl) {
        conf = new MySQLHibernateConfiguration(fbDbUser, fbPasswd, fbJdbcUrl);
    }
    
    /** Retrieves new queue items from Hub's DB
     * Calls getNewQueueItems()
     *
     * @return list of queue items found with status='request'
     */
    List<InstructionQueueItem> queryInstructions()
    {
        log.debug("InstructionQueueManager.queryInstructions() called.");
        Session session = null;
        Transaction transaction = null;
        List<InstructionQueueItem> items = new ArrayList<InstructionQueueItem>();
        try
        {
            session = conf.openSession();
            items = getNewQueueItems(session);
            session.flush();
            session.close();
        }
        catch(Throwable t){
            String errorString = "InstructionQueueManager caught a t in queryInstructions() " + t.toString();
            String logString = t.getMessage();
            edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
            log.error(errorString, t);
            handleThrowable(t, session, transaction);
        }
        return(items);
    }
    
    /** Retrieves new queue items from Hub's DB
     * Makes a Criteria to get only rows where status="request"
     * Sets status="pending" for the most recent record, the one about to be processes by this application.
     *
     * @param session the Hibernate session object
     * @return list of queue items found with status='request'
     */
    List<InstructionQueueItem> getNewQueueItems(Session session)
    {
        
        log.debug("InstructionQueueManager.getNewQueueItems() called.");
        Transaction transaction = null;
        transaction = session.beginTransaction();
        Criteria c = session.createCriteria(InstructionQueueItem.class).add(Restrictions.like("status","request")).addOrder(Order.asc("created"));
        List<InstructionQueueItem> results = c.list();
        if(results.size() > 0) {
            InstructionQueueItem iqitemp = results.get(0);
            iqitemp.setStatus("pending");
            long unixEpicTime = System.currentTimeMillis()/1000L;
            iqitemp.setReceived(unixEpicTime);
            session.save(iqitemp);
            transaction.commit();
        }
        return(results);
    }
    
    /** Retrieves queue items from Hub's DB by queue id
     * Makes a Criteria to get only rows where qid=CURRENT-qid (ie. one row)
     *
     * @param session the Hibernate session object
     * @param qid the id of the QueueItem (and the row in the table) being processed
     * @return list of queue items found with qid=CURRENT-qid' (list should have exactly one member).
     */
    List<InstructionQueueItem> getPendingQueueItems(Session session, long qid)
    {
        log.debug("InstructionQueueManager.getPendingQueueItems() called");
        Criteria c = session.createCriteria(InstructionQueueItem.class).add(Restrictions.like("qid",qid));
        List<InstructionQueueItem> results = c.list();
        return(results);
    }
    
    /** Tell's Hub DB how much data is being packaged
     * Writes the value (in bytes) of the size of the files requests to the "results" column in the database in JSON format. 
     * Tells the Hub DB how big (and therefore how long to process) the requested data is. 
     *
     * @param session the Hibernate session object
     * @param qid the id of the QueueItem (and the row in the table) being processed
     * @return true if successful
     */
    boolean updateInstructionSize(long size, long qid) {
        log.debug("InstructionQueueManager.updateInstructionSize() called.");
        Session session = null;
        Transaction transaction = null;
        try
        {
            session = conf.openSession();
            transaction = session.beginTransaction();
            List<InstructionQueueItem> items = getPendingQueueItems(session, qid);
            String sizeString = (new Long(size)).toString();
            Map resultsJSON = new LinkedHashMap();
            resultsJSON.put("size", sizeString);
            String jsonResultsString = JSONValue.toJSONString(resultsJSON); 
            if(items != null && items.size() >= 1)
            {
                InstructionQueueItem item = items.get(0);
                item.setResults(jsonResultsString);
                session.update(item);
                transaction.commit();
            }
            session.close();
            return true;
        }
        catch(Throwable t){
            String errorString = "InstructionQueueManager caught a t in updateInstructionSize(): " + t.toString();
            String logString = t.getMessage();
            edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
            log.error(errorString, t);
            handleThrowable(t, session, transaction);
        }
        return false;
    }

    /** Tell's Hub DB about the results of the queue processing effort
     * Called after processing of the queue item and construction of the TrueCrypt volume is complete. 
     * Tells Hub where TrueCrypt volume is located, when processing finished, and any error reporting; sets status
     * Sets status='complete' or status='error' in fb_queue row for this queue item
     * Sets Completed=unixEpicTime in fb_queue row for this queue item
     * Sets results=ComplexJSONstring holding path, logs, messages.  That string would look like this:
     * <pre>
     * {
     *    "path": "/path/to/file/3DData1-2-11-35username.tc",
     *    "log": "",
     *    "messages": [
     *        "File 1102.obj was not found and not included in your zip file.",
     *        "Blah blah blah"
     *    ]
     * }
     * <pre>
     *
     * @param trueCryptFilename the full path name of the trueCrypt file (ie. /var/downloads/FBdata.tc)
     * @param size total size of TrueCrypt file
     * @param qid the queue id of the item that was processed
     * @param errors list of human-readable errors that were encountered (if nonzero, the status will be set to "error", otherwise status="complete".
     * @param logs list of detailed error information that was gathered (usually with Exception.getMessage()). 
     * @return true if successful
     */
    boolean updateInstructionToCompleted(String trueCryptFilename, long size, long qid, ArrayList<String> errors, ArrayList<String> logs) {
        log.debug("InstructionQueueManager.updateInstructionToCompleted() called.");
        Session session = null;
        Transaction transaction = null;
        long unixEpicTime = System.currentTimeMillis()/1000L;
        try
        {
            session = conf.openSession();
            transaction = session.beginTransaction();
            List<InstructionQueueItem> items = getPendingQueueItems(session, qid);
            String sizeString = (new Long(size)).toString();
            //LIKE THIS: jsonResultsString = "{\"path\":\""+trueCryptFilename+"\",\"log\":\"\",\"messages\":[\""+errorsString+"\"],\"size\":\""+sizeString+"\"}";
            Map resultsJSON = new LinkedHashMap();
            resultsJSON.put("path", trueCryptFilename);
            resultsJSON.put("size", sizeString);
            JSONArray messagesJSON = new JSONArray();
            JSONArray logsJSON = new JSONArray();
            for (String message : errors) { 
                messagesJSON.add(message); 
            }
            resultsJSON.put("messages", messagesJSON);
            for (String log : logs) { 
                logsJSON.add(log); 
            }
            resultsJSON.put("log", logsJSON);
            //LIKE THIS: jsonResultsString = resultsJSON.toString();...but toString() won't work...need toJSONString():
            String jsonResultsString = JSONValue.toJSONString(resultsJSON); 
            if(items != null && items.size() >= 1)
            {
                InstructionQueueItem item = items.get(0);
                if(errors.isEmpty()) {
                    item.setStatus("complete");
                } else {
                    item.setStatus("error");
                }
                item.setResults(jsonResultsString);
                item.setCompleted(unixEpicTime);
                session.update(item);
                transaction.commit();
            }
            session.close();
            return true;
        }
        catch(Throwable t){
            String errorString = "InstructionQueueManager caught a t in updateInstructionsToCompleted()" + t.toString();
            String logString = t.getMessage();
            edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
            log.error(errorString, t);
            handleThrowable(t, session, transaction);
        }
        return false;
    }

    /** Deals with exceptions gracefully 
     * Called from catch clause
     *
     * @param t the exception
     * @param sessionHd Hibernate session
     * @param transactionHd Hibernate's Transaction object, for rolling back unfinished transactions
     */
    static void handleThrowable(Throwable t, Session session, Transaction transaction) {
        log.error("An unexpected error occured while talking to a database.", t);
        if(transaction != null)
        {
            try{transaction.rollback();}
            catch(Throwable t1){log.error("Unable to rollback a transaction.", t1);}
        }
        if(session != null)
        {
            try{session.close();}
            catch(Throwable t2){log.error("Unable to close a session.", t2);}
        }
    }
}
