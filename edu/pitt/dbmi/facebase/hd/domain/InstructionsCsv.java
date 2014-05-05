package edu.pitt.dbmi.facebase.hd.domain;

import edu.pitt.dbmi.facebase.hd.HumanDataController;
import java.util.ArrayList;
import java.io.*;
import org.apache.commons.io.*;
import org.apache.log4j.Logger;

/** Represents the portion of Queue item requests that need SQL data. 
 * Uses the static HumanDataManager object to query the human data server's mysqlDB. 
 * Makes a text file from CSV data that was made (elsewhere) from JDBC ResultSet. 
 * Gathers data from mysql using queries and CSV output desintation found in fb_queue.instructions column's JSON keyed by "csv", like this:
 * <pre>
 *     "csv": [
 *         {
 *             "content": "SELECT name FROM user",
 *             "path": "/Data/User Names.csv"
 *         },
 *         {
 *             "content": "SELECT uid FROM user",
 *             "path": "/Data/User IDs.csv"
 *         }
 *     ],
 * </pre>
 *
 */
public class InstructionsCsv implements Instructions {
    String userSql;
    String csvContents;
    File destinationFile;
    String relativePathToFileInArchive;
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.domain.InstructionsCsv.class"); 

    /** Sets attributes for provided SQL and file path info 
     *
     * @param userSqlValue the provided SQL to get the data the user wants from HDMysqlDB
     * @param destinationFileValue the name of the file in the user archive
     * @param relativePathToFileInArchiveValue path in archive
     */
    public InstructionsCsv(String userSqlValue, File destinationFileValue, String relativePathToFileInArchiveValue){
        log.debug("InstructionsCsv constructed with:");
        log.debug(userSqlValue + destinationFileValue + relativePathToFileInArchiveValue);
        userSql = userSqlValue;
        destinationFile = destinationFileValue;
        relativePathToFileInArchive = relativePathToFileInArchiveValue;
    }

    /** Uses static HumanDataManager object (in HumanDataController) to talk to the database  
     *
     * @return CSV data as a String
     */
    public String getCsvData() {
        log.debug("InstructionsCsv.getCsvData() called");
        String csvData = HumanDataController.hdm.getCsvData(userSql);
        return csvData;
    }

    /** Makes the actual CSV file.  
     * Uses apache commons FileUtils.writeStringToFile() to turn the String into a file. 
     *
     * @throws IOException FileUtils handles it.
     * @return size of the file.
     */
    public long makeFiles() throws IOException {
        log.debug("InstructionsCsv.makeFiles() called");
        csvContents = this.getCsvData();
        log.debug("InstructionsCsv.makeFiles() is about to write the SQL data to file:");
        log.debug(destinationFile);
        FileUtils.writeStringToFile(destinationFile, getCsvData());
        return destinationFile.length();
    }

    /** getter that returns the File object under managment of this object */
    public File getFile() {
        log.debug("InstructionsCsv.getFile() called");
        return destinationFile;
    }

    /** getter that returns the directory path in the user archive to where the file will be placed. */
    public String getRelativePathToFileInArchive() {
        log.debug("InstructionsCsv.getRelativePathToFileInArchive() called");
        return relativePathToFileInArchive;
    }
}
