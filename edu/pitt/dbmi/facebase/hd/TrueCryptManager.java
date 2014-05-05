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

import org.apache.log4j.Logger;
import org.apache.commons.io.*;
import java.io.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Performs TrueCrypt and file and shell operations.  
 * Calls ShellExecutor.launchProcess alot
 * Touches, creates, mounts, dismounts and copies-to-final-location TrueCrypt volume.
 * Has a pair of methods for each call to the shell--one to build command-line strings, and the other passes that String to the Shell for execution.
 * See /usr/bin/truecrypt -h for truecrypt operation. 
 * 
 * @author Facebase Java Authors, http://www.facebase.org/
 */
public class TrueCryptManager
{
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.TrueCryptManager.class");
    private String trueCryptBin = "";
    private String scpBin = "";
    private String sshServerUrl= "";
    private String finalLocation = "";
    private String sshServerUrlFinalLocation = "";
    private String touchBin = "";
    private String algorithm = "";
    private String hash = "";
    private String filesystem = "";
    private String volumeType = "";
    private String randomSource = "";
    private String protectHidden = "";
    private String extraArgs = "";

    private String volumeMountPoint = "";
    private String key;
    private long size;
    private String volume;
    private ShellExecutor shellExecutor = new ShellExecutor();

    /** Construct with all the details needed to manipulate truecrypt
     * 
     * @param keyval the encryption key.
     * @param sizeInBytes how much data will need to be copied to the volume.
     * @param volumeLocation filesystem path where TrueCrypt file processing occurs.
     * @param volumeMountLocation non-full-path directory under which truecrypt volume will be mounted.
     * @param trueCryptParams truecrypt options for  creating, mounting, dismounting truecrypt file--set in and gathered from hd.properties file
     */ 
    public TrueCryptManager(String keyval, long sizeInBytes, String volumeLocation, String volumeMountLocation, HashMap<String,String> trueCryptParams) {
        log.debug("TrueCryptManager constructed");
        size = sizeInBytes + (1024 - (sizeInBytes % 1024)) + (10 * 1024);
        key = keyval;
        volume = volumeLocation;
        volumeMountPoint = volumeMountLocation;

        trueCryptBin = trueCryptParams.get("trueCryptBin");
        scpBin = trueCryptParams.get("scpBin");
        sshServerUrl = trueCryptParams.get("sshServerUrl");
        finalLocation = trueCryptParams.get("finalLocation");
        sshServerUrlFinalLocation = sshServerUrl+finalLocation;
        touchBin = trueCryptParams.get("touchBin");
        algorithm = trueCryptParams.get("algorithm");
        hash = trueCryptParams.get("hash");
        filesystem = trueCryptParams.get("filesystem");
        volumeType = trueCryptParams.get("volumeType");
        randomSource = trueCryptParams.get("randomSource");
        protectHidden = trueCryptParams.get("protectHidden");
        extraArgs = trueCryptParams.get("extraArgs");

    }

    /** getter for full path to file where user will retrieve it. */
    public String getFinalLocation() {
        log.debug("TrueCryptManager.getFinalLocation() called");
        return finalLocation;
    }

    /** Build a Create-truecrypt-volume command string to send to the shell 
     *
     * @return the command-line shell that will create the truecrypt volume 
     */
    public String buildMakeVolumeCommandString() {
        log.debug("TrueCryptManager.buildMakeVolumeCommandString() called");
        String cmdMakeVolume = trueCryptBin;
        cmdMakeVolume += " -t -c --encryption=";
        cmdMakeVolume += algorithm;
        cmdMakeVolume += " --hash=";
        cmdMakeVolume += hash;
        cmdMakeVolume += " --filesystem=";
        cmdMakeVolume += filesystem;
        cmdMakeVolume += " --size=";
        cmdMakeVolume += size;
        cmdMakeVolume += " --password=";
        cmdMakeVolume += key;
        cmdMakeVolume += " --volume-type=";
        cmdMakeVolume += volumeType;
        cmdMakeVolume += " --random-source=";
        cmdMakeVolume += randomSource;
        cmdMakeVolume += " --protect-hidden=";
        cmdMakeVolume += protectHidden;
        cmdMakeVolume += " ";
        cmdMakeVolume += extraArgs;
        cmdMakeVolume += " ";
        cmdMakeVolume += volume;
        return cmdMakeVolume;
    }

    /** Ask shell to create TrueCrypt volume with command string
     *
     * @return true if successful TrueCrypt volume creation.
     */
    public boolean makeVolume() {
        log.debug("TrueCryptManager.makeVolume() called");
        String cmdMakeVolume = this.buildMakeVolumeCommandString();
        if(shellExecutor.launchProcess(cmdMakeVolume)) {
            return true;
            }else{
            return false;
        }
    }

    /** Build a mount-truecrypt-volume string to send to the shell 
     *
     * @return the command-line shell that will mount the truecrypt volume 
     */
    public String buildMountVolumeCommandString() {
        log.debug("TrueCryptManager.buildMountVolumeCommandString() called");
        String cmdMountVolume = trueCryptBin;
        cmdMountVolume += " -t";
        cmdMountVolume += " --password=";
        cmdMountVolume += key;
        cmdMountVolume += " ";
        cmdMountVolume += extraArgs;
        cmdMountVolume += " ";
        cmdMountVolume += volume;
        cmdMountVolume += " ";
        cmdMountVolume += volumeMountPoint;
        return cmdMountVolume;
    }

    /** Ask shell to mount TrueCrypt volume
     *
     * @return true if successful mounting
     */
    public boolean mountVolume() {
        log.debug("TrueCryptManager.mountVolume() called");
        String cmdMountVolume = this.buildMountVolumeCommandString();
        if(shellExecutor.launchProcess(cmdMountVolume)) {
            return true;
            }else{
            return false;
        }
    }
    
    /** Build a send-truecrypt-volume-elsewhere string to send to the shell 
     *
     * @return the command-line shell that will send the truecrypt volume to its final destination. 
     */
    public String buildSendVolumeToFinalLocationString() {
        log.debug("TrueCryptManager.buildSendVolumeToFinalLocationString() called");
        String cmdSendVolumeToFinalLocation = scpBin;
        cmdSendVolumeToFinalLocation += " ";
        cmdSendVolumeToFinalLocation += volume;
        cmdSendVolumeToFinalLocation += " ";
        cmdSendVolumeToFinalLocation += sshServerUrlFinalLocation;
        cmdSendVolumeToFinalLocation += " ";
        return cmdSendVolumeToFinalLocation;
    }

    /** Ask shell to send TrueCrypt volume
     *
     * @return true if successful sending truecrypt volume to final location 
     */
    public boolean sendVolumeToFinalLocation() {
        log.debug("TrueCryptManager.sendVolumeToFinalLocation() called");
        String cmdSendVolumeToFinalLocation= this.buildSendVolumeToFinalLocationString();
        if(shellExecutor.launchProcess(cmdSendVolumeToFinalLocation)) {
            return true;
            }else{
            return false;
        }
    }
    
    /** Build a dismount-truecrypt-volume command string to send to the shell 
     *
     * @return the command-line shell that will dismount the truecrypt volume 
     */
    public String buildDisMountVolumeCommandString() {
        log.debug("TrueCryptManager.buildDisMountVolumeCommandString() called");
        String cmdDisMountVolume = trueCryptBin;
        cmdDisMountVolume += " -t";
        cmdDisMountVolume += " -d";
        return cmdDisMountVolume;
    }

    /** Ask shell to dismount TrueCrypt volume
     *
     * @return true if successful dismounting truecrypt volume.
     */
    public boolean disMountVolume() {
        log.debug("TrueCryptManager.disMountVolume() called");
        String cmdDisMountVolume = this.buildDisMountVolumeCommandString();
        if(shellExecutor.launchProcess(cmdDisMountVolume)) {
            return true;
            }else{
            return false;
        }
    }

    /** "/bin/touch" TrueCrypt volume file using not the shell but FileUtils.touch()
     * Creates zero-length file
     * truecrypt binary requires that the file holding the truecrypt volume exists before it will create a truecrypt volume
     *
     * @return true if successful creating the file housing the truecrypt volume.
     */
    public boolean touchVolume() {
        log.debug("TrueCryptManager.touchVolume() called");
        try {
            File volumeFile = new File(volume);
            FileUtils.touch(volumeFile);
        } catch(IOException ioe) {
            String errorString = "TrueCryptManager caught an ioe in touchVolume()" + ioe.toString();
            String logString = ioe.getMessage();
            edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
            log.error(errorString, ioe);
            return false;
        }
        return true;
    }
}
