package edu.pitt.dbmi.facebase.hd.domain;

import java.io.IOException;
import java.io.File;

/** Defines required methods for all Instructions descendants
 * This application uses ArrayList<Instructions> alot, when looping through such a list, these methods get called. 
 */
public interface Instructions
{
    public long makeFiles() throws IOException;
    public String getRelativePathToFileInArchive();
    public File getFile();
}
