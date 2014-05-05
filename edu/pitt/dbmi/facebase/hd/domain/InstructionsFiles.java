package edu.pitt.dbmi.facebase.hd.domain;

import java.util.ArrayList;
import java.io.*;
import org.apache.commons.io.*;
import org.apache.log4j.Logger;

public class InstructionsFiles implements Instructions
{
    File sourceFile;
    File destinationFile;
    String relativePathToFileInArchive;
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.domain.InstructionsFiles.class");

    public InstructionsFiles(File sourceFileValue, File destinationFileValue, String relativePathToFileInArchiveValue){
        log.debug("InstructionsFiles constructed with:");
        log.debug(sourceFileValue.toString() + destinationFileValue.toString() + relativePathToFileInArchiveValue);
        sourceFile = sourceFileValue;
        destinationFile = destinationFileValue;
        relativePathToFileInArchive = relativePathToFileInArchiveValue;
    }

    /** Returns size of the file--since files (ie. facemesh.obj) requested already exist, no need to actually make them, but need to implement interface. .
     * 
     * @throws IOException doesn't actually ever throw it, just an interface-ism
     * @return size of the file.
     */
    public long makeFiles() throws IOException
    {
        log.debug("InstructionsFiles.makeFiles() called");
        return sourceFile.length();
    }

    /** getter that returns the File object under managment of this object */
    public File getFile() {
        log.debug("InstructionsFiles.getFile() called");
        return sourceFile;
    }

    /** getter that returns the directory path in the user archive to where the file will be placed. */
    public String getRelativePathToFileInArchive() {
        log.debug("InstructionsFiles.getRelativePathToFileInArchive() called");
        return relativePathToFileInArchive;
    }
}
