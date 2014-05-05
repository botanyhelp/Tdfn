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
