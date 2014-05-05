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
import java.util.Date;
import java.io.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.exception.*;
import org.apache.log4j.Logger;

/** Controller class to gather and encrypt requested human data. 
* See the <b>README.txt</b> to learn about the required complex operating environment including:
* <ol>
* <li>hd.properties file on CLASSPATH containing dozens of parameters </li>
* <li>log4j.properties file on CLASSPATH </li>
* <li>Access to Drupal-managed fb_queue module database tables </li>
* <li>Access to Drupal-managed fb_keychain module database tables </li>
* <li>Locally installed TrueCrypt binary </li>
* <li>Runs as root (because this program needs to make filesystem on TrueCrypt) </li>
* </ol>
*
* @author Facebase Java Authors, http://www.facebase.org/
*/
public class HumanDataController {
    /** errors - a list of human-readable errors populated by all classes in the application 
     * errors is checked after processing a queue item--if there is even one error then the 
     * queue item (data request) is assumed to have failed and "status" will be set to "error"
     */
    public static ArrayList<String> errors = new ArrayList<String>();
    /** logs - a list of detailed info/error data populated by all classes in the application */
    public static ArrayList<String> logs = new ArrayList<String>();

    /** accessor method to get errors list; called after queue processing to learn if status="error" 
     *
     * @return list of errors (happenings bad enough to cause status="error" for this request)
     */
    public static ArrayList<String> getErrors() {
        return errors;
    }

    /** accessor method to get logs list; called after queue processing */
    public static ArrayList<String> getLogs() {
        return logs;
    }

    /** Populates errors and logs lists, can be called from any application object 
     * 
     * @param errorString human readable error description, set when item processing should fail
     * @param logString detailed error information, stack-trace-like 
     */
    public static void addError(String errorString, String logString) {
        errors.add(errorString);
        logs.add(logString);
    }

    /** Reports DB connection failure, can be called from any application object;
     * triggers an http request to Hub to alert the problem.  All communication happens via 
     * the database and this is the mechanism to communicate when database connection fails. 
     */
    public static void reportDbConnectionFailure() {
        httpGetter("event", "1");
    }

    /** HumanDataManager is a class object as it needs to be called to get data from database */
    public static HumanDataManager hdm = null;
    /** log4j logging--configure in log4j.properties file on CLASSPATH, call with log.debug(), log.error() etc. */
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.HumanDataController.class");
    /** base URL where Hub application module awaits http-based communication--used by httpGetter() */
    static String hubURL = "";
    /** URL path where Hub application module awaits http-based communication--used by httpGetter() */
    static String responseTrigger = "";

    /** Entry point to application.  Firstly it gathers properties from hd.properties file on CLASSPATH 
     * Most properties are localisms to do with file path information, database connection strings, 
     * and TrueCrypt program operation.  Hopefully the application can run by only editing entries in 
     * hd.properties file.  hd.properties has normal java properties fragility but also app-specific 
     * fragility--for example, the sshServerUrl must end with a colon, as in root@server:
     * Notice that program properties can also be set below--by filling in the empty strings following 
     * declaration AND commenting-out the try-catch clause that follows which gathers these properties 
     * from hd.properties.  
     */
    public static void main(String[] args) {
        /** holds human-readable error data to be passed to addError() */
        String errorString = "";

        log.info( "HumanDataController Started" );
        /** length of time to sleep in between each polling loop, 5secs is responsive, 5mins is alot */
	String sleepFor = "";
        /** Prefix path where TrueCrypt write operations will be performed (i.e. /tmp or /var/tmp), no trailing-slash */
        String trueCryptBasePath = "";
        /** TrueCrypt volume file extension (probably .tc or .zip) */
        String trueCryptExtension = "";
        /** Middle-of-path directory name to be created where TrueCrypt volume will be mounted */
        String trueCryptMountpoint = "";
        /** Human Data Server database credentials */
        String hdDbUser = "";
        String hdPasswd = "";
        String hdJdbcUrl = "";
        /** Hub database credentials */
        String fbDbUser = "";
        String fbPasswd = "";
        String fbJdbcUrl = "";
        /** Full path to truecrypt binary, (ie /usr/bin/truecrypt) */
        String trueCryptBin = "";
        /** Full path to scp binary, (ie /usr/bin/scp) */
        String scpBin = "";
        /** user@host portion of scp destination argument (ie. root@www.server.com:) */
        String sshServerUrl= "";
        /** file full path portion of scp destination argument (ie. /usr/local/downloads/) */
        String finalLocation = "";
        /** Full path to touch binary, (ie /bin/touch) */
        String touchBin = "";
        /** hardcoded truecrypt parameters; run "truecrypt -h" to learn about these */
        String algorithm = "";
        String hash = "";
        String filesystem = "";
        String volumeType = "";
        String randomSource = "";
        String protectHidden = "";
        String extraArgs = "";

        /** truecrypt parameters are packed into a map so we only pass one arg (this map) to method invoking truecrypt */
        HashMap<String,String> trueCryptParams = new HashMap<String,String>();
        trueCryptParams.put("trueCryptBin", "");
        trueCryptParams.put("scpBin", "");
        trueCryptParams.put("sshServerUrl", "");
        trueCryptParams.put("finalLocation", "");
        trueCryptParams.put("touchBin", "");
        trueCryptParams.put("algorithm", "");
        trueCryptParams.put("hash", "");
        trueCryptParams.put("filesystem", "");
        trueCryptParams.put("volumeType", "");
        trueCryptParams.put("randomSource", "");
        trueCryptParams.put("protectHidden", "");
        trueCryptParams.put("extraArgs", "");

        try {
            /** The properties file name is hardcoded to hd.properties--cannot be changed--this file must be on or at root of classpath */
            final Configuration config = new PropertiesConfiguration("hd.properties");
            sleepFor = config.getString("sleepFor");
            hubURL = config.getString("hubURL");
            responseTrigger = config.getString("responseTrigger");
            trueCryptBasePath = config.getString("trueCryptBasePath");
            trueCryptExtension = config.getString("trueCryptExtension");
            trueCryptMountpoint = config.getString("trueCryptMountpoint");
            hdDbUser = config.getString("hdDbUser");
            hdPasswd = config.getString("hdPasswd");
            hdJdbcUrl = config.getString("hdJdbcUrl");
            fbDbUser = config.getString("fbDbUser");
            fbPasswd = config.getString("fbPasswd");
            fbJdbcUrl = config.getString("fbJdbcUrl");
            trueCryptBin = config.getString("trueCryptBin");
            scpBin = config.getString("scpBin");
            sshServerUrl = config.getString("sshServerUrl");
            finalLocation = config.getString("finalLocation");
            touchBin = config.getString("touchBin");
            algorithm = config.getString("algorithm");
            hash = config.getString("hash");
            filesystem = config.getString("filesystem");
            volumeType = config.getString("volumeType");
            randomSource = config.getString("randomSource");
            protectHidden = config.getString("protectHidden");
            extraArgs = config.getString("extraArgs");

            trueCryptParams.put("trueCryptBin", trueCryptBin);
            trueCryptParams.put("scpBin", scpBin);
            trueCryptParams.put("sshServerUrl", sshServerUrl);
            trueCryptParams.put("finalLocation", finalLocation);
            trueCryptParams.put("touchBin", touchBin);
            trueCryptParams.put("algorithm", algorithm);
            trueCryptParams.put("hash", hash);
            trueCryptParams.put("filesystem", filesystem);
            trueCryptParams.put("volumeType", volumeType);
            trueCryptParams.put("randomSource", randomSource);
            trueCryptParams.put("protectHidden", protectHidden);
            trueCryptParams.put("extraArgs", extraArgs);
            log.debug("properties file loaded successfully");
        }catch (final ConfigurationException e) {
            errorString = "Properties file problem";
            String logString = e.getMessage();
            addError(errorString, logString);
            log.error(errorString);
        }
        log.debug("initialize static class variable HumanDataManager declared earlier");
        hdm = new HumanDataManager(hdDbUser, hdPasswd, hdJdbcUrl);
        log.debug("declare and initialize InstructionQueueManager"); 
        InstructionQueueManager iqm = new InstructionQueueManager(fbDbUser, fbPasswd, fbJdbcUrl);
        log.debug("pass to the logfile/console all startup parameters for troubleshooting");
        log.info("HumanDataController started with these settings from hd.properties: " + "hubURL="+hubURL+" "+"responseTrigger="+responseTrigger+" "+"trueCryptBasePath="+trueCryptBasePath+" "+"trueCryptExtension="+trueCryptExtension+" "+"trueCryptMountpoint="+trueCryptMountpoint+" "+"hdDbUser="+hdDbUser+" "+"hdPasswd="+hdPasswd+" "+"hdJdbcUrl="+hdJdbcUrl+" "+"fbDbUser="+fbDbUser+" "+"fbPasswd="+fbPasswd+" "+"fbJdbcUrl="+fbJdbcUrl+" "+"trueCryptBin="+trueCryptBin+" "+"scpBin="+scpBin+" "+"sshServerUrl="+sshServerUrl+" "+"finalLocation="+finalLocation+" "+"touchBin="+touchBin+" "+"algorithm="+algorithm+" "+"hash="+hash+" "+"filesystem="+filesystem+" "+"volumeType="+volumeType+" "+"randomSource="+randomSource+" "+"protectHidden="+protectHidden+" "+"extraArgs="+extraArgs);
        log.debug("Enter infinite loop where program will continuously poll Hub server database for new requests");
        while(true) {
            log.debug("LOOP START");
            try {
                Thread.sleep(Integer.parseInt(sleepFor) * 1000);
            } catch (InterruptedException ie) {
                errorString = "Failed to sleep, got interrupted.";
                log.error(errorString, ie);
                addError(errorString, ie.getMessage());
            }
            log.debug("About to invoke InstructionQueueManager.queryInstructions()--Hibernate to fb_queue starts NOW");
            List<InstructionQueueItem> aiqi = iqm.queryInstructions();
            log.debug("Currently there are " + aiqi.size() + " items in the queue");
            InstructionQueueItem iqi;
            String instructionName = "";
            log.debug("About to send http request -status- telling Hub we are alive:");
            httpGetter("status", "0");
            if(aiqi.size() > 0) {
                log.debug("There is at least one request, status=pending, queue item; commence processing of most recent item");
                iqi = aiqi.get(0);
                log.debug("About to get existing user key, or create a new one, via fb_keychain Hibernate");
                FbKey key = hdm.queryKey(iqi.getUid());
                log.debug("About to pull the JSON Instructions string, and other items, from the InstructionQueueItem");
                String instructionsString = iqi.getInstructions();
                instructionName = iqi.getName();
                log.debug("About to create a new FileManager object with:");
                log.debug(instructionName + trueCryptBasePath + trueCryptExtension +  trueCryptMountpoint);
                FileManager fm = new FileManager(instructionName, trueCryptBasePath, trueCryptExtension, trueCryptMountpoint);
                ArrayList<Instructions> ali = new ArrayList<Instructions>();
                log.debug("FileManager.makeInstructionsObjects() creates multiple Instruction objects from the InstructionQueueItem.getInstructions() value");
                if(fm.makeInstructionsObjects(instructionsString, ali)) {
                    log.debug("FileManager.makeInstructionsObjects() returned true");
                } else {
                    errorString = "FileManager.makeInstructionsObjects() returned false";
                    log.error(errorString); 
                    addError(errorString, "");
                }
                log.debug("FileManager.makeFiles() uses its list of Instruction objects and calls its makeFiles() method to make/get requested data files");
                if(fm.makeFiles(ali)) {
                    log.debug("FileManager.makeFiles() returned true");
                } else {
                    errorString = "FileManager.makeFiles() returned false";
                    log.error(errorString); 
                    addError(errorString, "");
                }
                //sends the size/100000 as seconds(100k/sec)...needs to be real seconds.");
                long bytesPerSecond = 100000;
                Long timeToMake = new Long(fm.getSize()/bytesPerSecond);
                String timeToMakeString = timeToMake.toString();
                log.debug("Send http request -status- to Hub with total creation time estimate:");
                log.debug(timeToMakeString);
                httpGetter("status", timeToMakeString);
                log.debug("Update the queue_item row with the total size of the data being packaged with InstructionQueueManager.updateInstructionSize()");
                if(iqm.updateInstructionSize(fm.getSize(),iqi.getQid())) {
                    log.debug("InstructionQueueManager.updateInstructionSize() returned true");
                } else {
                    errorString = "InstructionQueueManager.updateInstructionSize() returned false";
                    log.error(errorString); 
                    addError(errorString, "");
                }
                log.debug("About to make new TrueCryptManager with these args:");
                log.debug(key.getEncryption_key() + fm.getSize() + fm.getTrueCryptPath() + fm.getTrueCryptVolumePath() + trueCryptParams);
                TrueCryptManager tcm = new TrueCryptManager(key.getEncryption_key(), fm.getSize(), fm. getTrueCryptPath(), fm.getTrueCryptVolumePath(), trueCryptParams);
                if(tcm.touchVolume()) {
                    log.debug("TrueCryptManager.touchVolume() returned true, touched file");
                } else {
                    errorString = "TrueCryptManager.touchVolume() returned false";
                    log.error(errorString); 
                    addError(errorString, "");
                }
                if(tcm.makeVolume()) {
                    log.debug("TrueCryptManager.makeVolume() returned true, created TrueCrypt volume");
                } else {
                    errorString = "TrueCryptManager.makeVolume() returned false";
                    log.error(errorString); 
                    addError(errorString, "");
                }
                if(tcm.mountVolume()) {
                    log.debug("TrueCryptManager.mountVolume() returned true, mounted TrueCrypt volume");
                } else {
                    errorString = "TrueCryptManager.mountVolume() returned false";
                    log.error(errorString); 
                    addError(errorString, "");
                }
                if(fm.copyFilesToVolume(ali)) {
                    log.debug("TrueCryptManager.copyFilesToVolume() returned true, copied requested files to mounted volume");
                } else {
                    errorString = "TrueCryptManager.copyFilesToVolume() returned false";
                    log.error(errorString); 
                    addError(errorString, "");
                }
                if(tcm.disMountVolume()) {
                    log.debug("TrueCryptManager.disMountVolume() returned true, umounted TrueCrypt volume");
                } else {
                    errorString = "TrueCryptManager.disMountVolume() returned false";
                    log.error(errorString); 
                    addError(errorString, "");
                }
                if(tcm.sendVolumeToFinalLocation()) {
                    log.debug("TrueCryptManager.sendVolumeToFinalLocation() returned true, copied TrueCrypt volume to retreivable, final location");
                } else {
                    errorString = "TrueCryptManager.sendVolumeToFinalLocation() returned false";
                    log.error(errorString); 
                    addError(errorString, "");
                }
                if(iqm.updateInstructionToCompleted(tcm.getFinalLocation()+fm.getTrueCryptFilename(),fm.getSize(),iqi.getQid(), getErrors(), getLogs())) {
                    log.debug("InstructionQueueManager.updateInstructionToCompleted() returned true");
                    log.debug("Processing of queue item is almost finished, updated fb_queue item row with location, size, status, errors, logs:");
                    log.debug(tcm.getFinalLocation() + fm.getTrueCryptFilename() + fm.getSize() + iqi.getQid() + getErrors() + getLogs());
                }else{
                    errorString = "InstructionQueueManager.updateInstructionToCompleted() returned false";
                    log.error(errorString); 
                    addError(errorString, "");
                }
                log.debug("About to send http request -update- telling Hub which item is finished.");
                httpGetter("update",iqi.getHash());
                log.debug("Finished processing pending queue item, status should now be complete or error");
            } else {
                log.debug("Zero queue items");
            }
            log.debug("LOOP END");
        }
    }

    /** http web client class used for communicating with Hub 
     * @param action String is one of status, update, event
     * @param signal String is a timeInSeconds or QueueID or eventCode
     * for action=status, signal is "0", number-of-seconds-until-back, 
     * for action=update, signal is qid.hash
     * for action=event, signal "1" means mysql server is unavailable.
     * uses getOTP() method for URL construction
     */
    public static void httpGetter(String action, String signal) {
        String oneTimePassword = (new Long(getOTP())).toString();
        String url = hubURL+responseTrigger+oneTimePassword+"/";
        if(action == "update") {
            url += action+"/"+signal;
        } else if (action == "status") {
            long time = System.currentTimeMillis()/1000L;
            time += Long.parseLong(signal);                
            Long timeLong;
            timeLong = new Long(time);
            url += action+"/"+timeLong.toString();
        } else if (action == "event") {
            url += action+"/"+signal;
        } else { 
        }
        HttpClient webClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        try {
            webClient.executeMethod(httpMethod);
            String response = httpMethod.getResponseBodyAsString();
            } catch( HttpException httpe ) {
            } catch( IOException ioe ) {
            } finally {
            httpMethod.releaseConnection();
            httpMethod.recycle();
        }
    }

    /** generates and returns one-time-password 
     * OTP is a long sent to Hub demonstrating that http request is legit, Hub checks it against 
     * This should thwart any bogus/malicious but similar HTTP requests from Internet, since bogus 
     * requesters won't be able to generate a hash that passes muster...Hub ignores requests with bad OTP
     * @return code, based on current time
     */
    public static long getOTP() {
        long w = 100;
        long t = System.currentTimeMillis() / 1000;
        long r = (long) Math.floor(t / w) * w;
        long s = 0;
        Long rLong = new Long(r);
        String sString = rLong.toString();
        for (int i = 0; i < sString.length(); i++) {
            char aChar = sString.charAt(i);
            String oneCharString = Character.toString(aChar);
            s +=  Integer.parseInt(oneCharString);
        }
        long p = (long) Math.pow(s, 5);
        return (r % p);
    }
}
