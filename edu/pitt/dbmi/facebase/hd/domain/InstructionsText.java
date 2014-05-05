package edu.pitt.dbmi.facebase.hd.domain;

import java.util.ArrayList;
import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import org.apache.log4j.Logger;

/** Represents the portion of Queue item requests that needs to turn a string into a file. 
 * Manages application text (like a user Facebase user agreement) that will be written to a file in the TrueCrypt archive.
 * Makes a text file from string/character data. 
 * Uses data found in fb_queue.instructions column's JSON keyed by "text", like this:
 * <pre>
 *     "text": [
 *         {
 *             "content": "Your data was retrieved on 11-02-2011 and has 28 missing values...",
 *             "path": "/Data/Summary.txt"
 *         },
 *         {
 *             "content": "The Facebase Data User Agreement specifies...",
 *             "path": "/FaceBase Agreement.txt"
 *         }
 *     ],
 *  </pre>
 *
 */
public class InstructionsText implements Instructions {
    String fileContents;
    File destinationFile;
    String relativePathToFileInArchive;
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.domain.InstructionsText.class"); 

    /** Sets attributes for provided SQL and file path info 
     *
     * @param fileContentsValue the character data that will be written to a file
     * @param destinationFileValue the name of the file in the user archive
     * @param relativePathToFileInArchiveValue path in archive
     */
    public InstructionsText(String fileContentsValue, File destinationFileValue, String relativePathToFileInArchiveValue) {
        log.debug("InstructionsText constructed with:");
        log.debug( fileContentsValue + destinationFileValue + relativePathToFileInArchiveValue);
        fileContents = fileContentsValue;
        destinationFile = destinationFileValue;
        relativePathToFileInArchive = relativePathToFileInArchiveValue;
    }

    /** Makes the actual text file (ie. FBuserAgreement.txt).  
     * Uses apache commons FileUtils.writeStringToFile() to turn the String into a file. 
     *
     * @throws IOException
     * @return size of the file.
     */
    public long makeFiles() throws IOException
    {
        log.debug("InstructionsText.makeFiles() called, about to write to file:");
        log.debug(destinationFile);
        FileUtils.writeStringToFile(destinationFile, fileContents);
        return destinationFile.length();
    }

    /** getter that returns the File object under managment of this object */
    public File getFile()
    {
        log.debug("InstructionsText.getFile() called");
        return destinationFile;
    }

    /** getter that returns the directory path in the user archive to where the file will be placed. */
    public String getRelativePathToFileInArchive() {
        log.debug("InstructionsText.getRelativePathToFileInArchive() called");
        return relativePathToFileInArchive;
    }
}
