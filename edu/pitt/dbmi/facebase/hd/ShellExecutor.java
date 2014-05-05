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

import java.util.Map;
import java.io.*;
import org.apache.log4j.Logger;

/** Invokes shell, launches external child process. 
 * Used for /bin/touch and /usr/bin/truecrypt
 * Can block for a long time for large TrueCrypt volume. 
 * SECURITY: Be careful turning on "debug" loglevel because truecrypt commanlines (and passwds) will be in logfile. 
 * SECURITY: Does not sanitize shell passed in--add checking to "bad stuff" comment below. 
 * SECURITY: Consider editing PATH environment variable to only include /bin/touch and /usr/bin/truecrypt, etc before starting java
 * 
 * @author Facebase Java Authors, http://www.facebase.org/
 */
public class ShellExecutor {
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.ShellExecutor.class");
    /** Launches a shell and executes command; uses ProcessBuilder */
    public boolean launchProcess(String command) {
        log.debug("ShellExecutor.launchProcess() called with this command:");
        //Caution HERE: this command line is going to logfile if log.properties says DEBUG loglevel
        log.debug(command);
        ProcessBuilder launcher = new ProcessBuilder();
        //Caution HERE: edit the environment to reduce what can be called, or edit PATH environment variable of user launching java
        //Map<String,String> environment = launcher.environment();
        launcher.redirectErrorStream(true);
        String[] commandLineTokens = command.split(" ");
        for(String s: commandLineTokens) {
            //Caution HERE: sanitize commands like '/bin/rm' 
            //check for bad stuff
        }
        launcher.command(commandLineTokens);
        try {
            log.debug("ShellExecutor.launchProcess() is in about to ProcessBuilder.launcher.start()...could take awhile.");
            Process p = launcher.start();
            BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while((line = output.readLine()) != null) {
                log.debug(line);
                p.waitFor();
            }
        }catch(Exception e) {
            String errorString = "ShellExecutor caught an e in launchProcess: " + e.toString();
            String logString = e.getMessage();
            edu.pitt.dbmi.facebase.hd.HumanDataController.addError(errorString,logString);
            log.error(errorString, e);
            return false;
        }
        return true;
    }
}
