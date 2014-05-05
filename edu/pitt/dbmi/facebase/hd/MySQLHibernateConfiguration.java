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

import java.sql.*;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.cfg.*;

import edu.pitt.dbmi.facebase.hd.domain.InstructionQueueItem;

/**
* A class to provide Hibernate services against a MySQL database.
* This class creates a single (static) Hibernate SessionFactory which is used
* to open Hibernate Sessions when needed.
*
* @author SHIREY
*/
public class MySQLHibernateConfiguration
{
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.MySQLHibernateConfiguration.class");
    private static SessionFactory lclSessionFactory = null;
    
    private String connectionURL = null;
    private String username = null;
    private String password = null;
    
    /**
    * Create a configuration connected to a specified database.
    *
    * @param username The MySQL username used to connect to the database.
    * @param password The password corresponding to the supplied username.
    * @param connectionURL The URL used to connect to the database.  It will
    */
    public MySQLHibernateConfiguration(String username, String password, String connectionURL)
    {
        log.debug("MySQLHibernateConfiguration constructed");
        this.connectionURL = connectionURL;
        this.username = username;
        this.password = password;
    }
    
    public synchronized Session openSession()
    {
        log.debug("MySQLHibernateConfiguration.openSession() called ");
        //uncomment this to simulate a bad DB connection report--NOTE-this only exercises the reporting-it does NOT shutdown the dbconn. :
        //edu.pitt.dbmi.facebase.hd.HumanDataController.reportDbConnectionFailure();
        return(getSessionFactory().openSession());
    }
    public static synchronized Connection closeSession(Session session)
    {
        log.debug("MySQLHibernateConfiguration.closeSession() called ");
        return(session.close());
    }
    
    private SessionFactory getSessionFactory()
    {
        log.debug("MySQLHibernateConfiguration.getSessionFactory() called ");
        if(lclSessionFactory != null)
        return(lclSessionFactory);
        
        try
        {
            boolean validConnection = false;
            try
            {
                DriverManager.registerDriver(new com.mysql.jdbc.Driver());
                Connection conn = DriverManager.getConnection(connectionURL, username, password);
                conn.close();
                validConnection = true;
            }
            catch (Exception e)
            {
                String errorString = "Cannot connect to database at " + connectionURL + " with username " + username + e.toString();
                String logString = e.getMessage();
                edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
                log.error(errorString, e);
                edu.pitt.dbmi.facebase.hd.HumanDataController.reportDbConnectionFailure();
                return(null);
            }
            
            
            if(validConnection)
            {
                Configuration hibernateConf = null;
                hibernateConf = new Configuration();
                String className = null;
                try
                {
                    hibernateConf.addAnnotatedClass(InstructionQueueItem.class);
                }
                catch(Exception ex)
                {
                    String errorString = "Unable to load annotated class for hibernate: " + className + ex.toString();
                    String logString = ex.getMessage();
                    edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
                    edu.pitt.dbmi.facebase.hd.HumanDataController.reportDbConnectionFailure();
                    log.error("Unable to load annotated class for hibernate: " + className, ex);
                }
                
                hibernateConf.setProperty("hibernate.connection.driver_class","com.mysql.jdbc.Driver")
                .setProperty("hibernate.connection.url", connectionURL)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .setProperty("hibernate.connection.username", username)
                .setProperty("hibernate.connection.password", password)
                .setProperty("hibernate.show_sql", "false")
                .setProperty("hibernate.current_session_context_class", "thread")
                .setProperty("hibernate.connection.provider_class","org.hibernate.connection.C3P0ConnectionProvider")
                .setProperty("hibernate.c3p0.acquire_increment", "1")
                .setProperty("hibernate.c3p0.idle_test_period","240")
                .setProperty("hibernate.c3p0.timeout","600")
                .setProperty("hibernate.c3p0.max_size","100")
                .setProperty("hibernate.c3p0.min_size","3")
                .setProperty("hibernate.c3p0.validate", "false")  /*this is an expensive property to turn on...*/
                .setProperty("hibernate.c3p0.max_statements", "500")
                .setProperty("hibernate.cache.use_second_level_cache","false")
                .setProperty("hibernate.jdbc.batch.size","20");
                
                lclSessionFactory = hibernateConf.buildSessionFactory();
                
            }
            
        } catch (Throwable ex) {
            String errorString = "Initial SessionFactory creation failed." + ex.toString();
            String logString = ex.getMessage();
            edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
            edu.pitt.dbmi.facebase.hd.HumanDataController.reportDbConnectionFailure();
            log.error("Initial SessionFactory creation failed.", ex);
            throw new ExceptionInInitializerError(ex);
        }
        
        return(lclSessionFactory);
    }
}
