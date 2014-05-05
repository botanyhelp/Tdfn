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

import edu.pitt.dbmi.facebase.hd.domain.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.*;
import org.apache.commons.io.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import org.apache.log4j.Logger;

/** Controls disparate file-related operations.  
 * Makes csv, text and files Instructions objects from JSON-format fb_queue.instructions column data.
 * Loops through Instructions object list and calls makeFiles() on each.
 * Manages file size and path information.
 * Copies files to mounted TrueCrypt volume.
 * 
 * @author Facebase Java Authors, http://www.facebase.org/
 */
public class FileManager {
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.FileManager.class");
    private long sizeInBytes;
    private String dataNamePrefix = "";
    private String dataName = "";
    private String trueCryptPath;
    private String trueCryptFilename;
    private String trueCryptVolumePath;
    private String trueCryptBasePath;
    private String trueCryptExtension;
    private String trueCryptMountpoint;

    /** Sets various TrueCrypt volume attributes, initializes sizeInBytes to larger than TrueCrypt minimum size (292k) */
    public FileManager(String instructionName, String trueCryptBasePath, String trueCryptExtension, String trueCryptMountpoint){
        this.trueCryptMountpoint = trueCryptMountpoint;
        this.trueCryptExtension = trueCryptExtension;
        this.trueCryptBasePath = trueCryptBasePath;
        dataNamePrefix = instructionName;
        sizeInBytes = 3000000;
        log.debug("FileManager constructed");
    }

    /** Gathers and sums total size of files to be packaged
     * Each Instruction object in the looped-through list calls length() on its File object
     *
     * @return total size of all files...NOT the size of trueCrypt volume where they will be copied
     */
    public long getTotalSize(ArrayList<Instructions> alival){
        log.debug("FileManager.getTotalSize() called.");
        long size = 0;
        for(Instructions i: alival) {
            size += i.getFile().length();
        }
        return size;
    }

    /** getter for base name
     * 
     * @return the String that holds the name of the dataset being packaged
     */
    public String getDataName() {
        return dataName;
    }

    /** setter for base name
     * 
     * @param dataNameVal the name of the dataset being packaged (ie Facebase3Ddata)
     */
    public void setDataName(String dataNameVal) {
        dataName = dataNameVal;
    }

    /** getter for truecrypt filename being created/mounted/populated/dismounted
     * 
     * @return the name of the file housing the truecrypt volume (ie HiddenData.tc)
     */
    public String getTrueCryptFilename() {
        return trueCryptFilename;
    }

    /** getter for path to truecrypt binary program 
     * 
     * @return the full path to truecrypt (ie /usr/bin/truecrypt)
     */
    public String getTrueCryptPath() {
        return trueCryptPath;
    }

    /** getter for path to truecrypt data scratch area, for mounting, copying activityj
     * 
     * @return the path to truecrypt copying activity for this request (ie /var/tmp/Data132151351/)
     */
    public String getTrueCryptVolumePath() {
        log.debug("FileManager.getTrueCryptVolumePath() called.");
        File dir = new File(trueCryptVolumePath);
        dir.mkdirs();
        return trueCryptVolumePath;
    }

    /** copies files to mounted truecrypt volume
     * Assumes all Instruction objects and their associated Files are in order.
     * Assumes suitably large truecrypt volume is mounted and writable and can hold all of the data. 
     * Double-checks that data will fit onto volume (although this fact has also been established elsewhere by now).
     * This method will block for a very long time if large amounts of data are being copied. 
     * 
     * @return true if copy effort succeeded
     */
    public boolean copyFilesToVolume(ArrayList<Instructions> alival){
        log.debug("FileManager.copyFilesToVolume() called.");
        long totalSize = 0;
        HashMap<File,String> hmfs = new HashMap<File,String>();
        for(Instructions i: alival) {
            hmfs.put(i.getFile(), i.getRelativePathToFileInArchive());
            totalSize += i.getFile().length();
        }
        long volSize = 0;
        File trueCryptVolFile = new File(this.getTrueCryptPath());
        volSize = trueCryptVolFile.length();
        if(totalSize > volSize){
            }else{
        }
        try{
            for(File f: hmfs.keySet()) {
                File tcvpPlusRptfia = new File(trueCryptVolumePath+hmfs.get(f));
                File parentDir = new File(tcvpPlusRptfia.getParent());
                if(f.isFile()) {
                    if(parentDir.isDirectory()) {
                        }else {
                        parentDir.mkdirs();
                        if(parentDir.isDirectory()) {
                        }
                    }
                    log.debug("About to copy with FileUtils.copyFileToDirectory() using two args, FILE, DIR:");
                    log.debug(f + " " + parentDir);
                    FileUtils.copyFileToDirectory(f,parentDir);
                }
            }
        } catch(IOException ioe) {
            String errorString = "FileManager caught an ioe in copyFilesToVolume()" + ioe.toString();
            String logString = ioe.getMessage();
            edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
            log.error(errorString, ioe);
            return false;
        }
        //Uncomment these next 2 lines to simulate errors
        //edu.pitt.dbmi.facebase.hd.HumanDataController.addError("BADSTUFFmessage","BADSTUFFlog");
        //log.error("FileManager caught an ioe in copyFilesToVolume()BADSTUFF", new IOException());
        return true;
    }
    
    /** Populates an ArrayList with Instruction objects using the JSON-formatted fb_queue.instructions column data
     * Each Instructions object will be asked to create the (actual) file under its management. 
     * This method will block for a long time if any of the Instructions objects have large SQL resultsets
     *
     * @param alival the list of Instructions objects whose makeFiles() methods will be called
     * @return true if Instructions object creation effort succeeds.
     */
    public boolean makeFiles(ArrayList<Instructions> alival){
        log.debug("FileManager.makeFiles() called.");
        for(Instructions i: alival) {
            try {
                sizeInBytes += i.makeFiles();
            }catch(IOException ioe){
                String errorString = "FileManager caught an ioe in makeFiles()" + ioe.toString();
                String logString = ioe.getMessage();
                edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
                log.error(errorString, ioe);
                //returning false here will let the controller/caller deal with it.
                return false;
            }
        }
        return true;
    }

    /** getter for sizeInBytes attribute--see getTotalSize() method above to see how this attribute is set */
    public long getSize() {
        return sizeInBytes;
    }
    
    /** populates an ArrayList with Instruction objects using the JSON-formatted fb_queue.instructions column data
     * The JSON string (from the database) is keyed by "csv", "text" and "files" depending on the type of data. 
     * Instructions descendants (InstructionsCsv, InstructionsText, InstructionsFiles) are constructed. 
     * File objects are constructed (and stuffed into their container Instructions objects). 
     * As an example, the JSON shown here would result in 6 Instructions objects, 2 of each kind shown:
     * </ b>
     * <pre>
     * {
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
     *     "files": [
     *         {
     *             "content": "/path/on/server/1101.obj",
     *             "path": "/meshes/1101.obj"
     *         },
     *         {
     *             "content": "/path/on/server/1102.obj",
     *             "path": "/meshes/1102.obj"
     *         }
     *     ]
     * }
     * </pre>
     * </ b>
     * 
     * @param instructionsString the JSON from the data request.
     * @param ali the list of Instructions objects that will be populated during execution of this method.
     * @return true if Instructions object creation effort succeeds.
     */
    public boolean makeInstructionsObjects(String instructionsString, ArrayList<Instructions> ali) {
        log.debug("FileManager.makeInstructionsObjects() called.");
        long unixEpicTime = System.currentTimeMillis()/1000L;
        String unixEpicTimeString = (new Long(unixEpicTime)).toString();
        this.setDataName(dataNamePrefix+unixEpicTimeString);
        trueCryptPath = trueCryptBasePath + dataName + trueCryptExtension;
        trueCryptFilename = dataName + trueCryptExtension;
        trueCryptVolumePath = trueCryptBasePath + dataName + trueCryptMountpoint;
        File trueCryptVolumePathDir = new File(trueCryptVolumePath);
        if(!trueCryptVolumePathDir.isDirectory()) {
            trueCryptVolumePathDir.mkdirs();
        }
        JSONObject jsonObj=(JSONObject)JSONValue.parse(instructionsString);
        
        if(jsonObj.containsKey("csv")) {
            log.debug("FileManager.makeInstructionsObjects() has a csv.");
            JSONArray jsonCsvArray = (JSONArray)jsonObj.get("csv");
            if(jsonCsvArray.size() > 0) {
                for(Object jsonObjectCsvItem: jsonCsvArray) {
                    JSONObject jsonObjSql = (JSONObject)jsonObjectCsvItem;
                    String sqlString = (String)jsonObjSql.get("content");
                    JSONObject jsonObjSqlPath = (JSONObject)jsonObjectCsvItem;
                    String sqlPathString = (String)jsonObjSql.get("path");
                    File file = new File(trueCryptBasePath + dataName + sqlPathString);
                    InstructionsCsv i = new InstructionsCsv(sqlString, file, sqlPathString);
                    ali.add(i);
                }
            }
        }
        
        if(jsonObj.containsKey("text")) {
            log.debug("FileManager.makeInstructionsObjects() has a text.");
            JSONArray jsonTextArray = (JSONArray)jsonObj.get("text");
            if(jsonTextArray.size() > 0) {
                for(Object jsonObjectTextItem: jsonTextArray) {
                    JSONObject jsonObjText = (JSONObject)jsonObjectTextItem;
                    String contentString = (String)jsonObjText.get("content");
                    JSONObject jsonObjTextPath = (JSONObject)jsonObjectTextItem;
                    String textPathString = (String)jsonObjText.get("path");
                    File file = new File(trueCryptBasePath + dataName + textPathString);
                    InstructionsText i = new InstructionsText(contentString, file, textPathString);
                    ali.add(i);
                }
            }
        }
        
        if(jsonObj.containsKey("files")) {
            log.debug("FileManager.makeInstructionsObjects() has a files.");
            JSONArray jsonFilesArray = (JSONArray)jsonObj.get("files");
            if(jsonFilesArray.size() > 0) {
                for(Object jsonObjectFilesItem: jsonFilesArray) {
                    JSONObject jsonObjFiles = (JSONObject)jsonObjectFilesItem;
                    String serverPathString = (String)jsonObjFiles.get("content");
                    JSONObject jsonObjFilesPath = (JSONObject)jsonObjectFilesItem;
                    String archivePathString = (String)jsonObjFiles.get("path");
                    File fileSource = new File(serverPathString);
                    File fileDest = new File(trueCryptBasePath + dataName + archivePathString);
                    InstructionsFiles i = new InstructionsFiles(fileSource, fileDest, archivePathString);
                    ali.add(i);
                }
            }
        }
        return true;
    }
}
