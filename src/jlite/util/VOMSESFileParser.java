/*   
 * Copyright 2008-2010 Oleg Sukhoroslov
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jlite.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.glite.voms.contact.VOMSServerInfo;
import org.glite.voms.contact.VOMSServerMap;

public class VOMSESFileParser {

	private static final Pattern vomsesPattern = Pattern.compile(
			"^\"([^\"]+)\"[^\"]+\"([^\"]+)\"[^\"]+\"([^\"]+)\"[^\"]+\"([^\"]+)\"[^\"]+\"([^\"]+)\".*$");
	private static final Logger log = Logger.getLogger(VOMSESFileParser.class);
	
    public static VOMSServerMap parse(File vomsesFile) throws IOException {
        if (vomsesFile.isDirectory()) {
            return parseDir(vomsesFile);
        } else {
	        VOMSServerMap vomses = new VOMSServerMap();
	        BufferedReader reader = null;
	        try {
	            reader = new BufferedReader(new InputStreamReader(
	            		new FileInputStream(vomsesFile)));			        
		        log.debug("Parsing vomses file: " + vomsesFile.getAbsolutePath());
		        String line;
		        while ((line = reader.readLine()) != null) {
		        	Matcher vomsesMatcher = vomsesPattern.matcher(line);
		        	if (vomsesMatcher.find()) {
		                VOMSServerInfo info = new VOMSServerInfo();                
		                info.setVoName(vomsesMatcher.group(1));
		                info.setHostName(vomsesMatcher.group(2));
		                info.setPort(Integer.parseInt(vomsesMatcher.group(3)));
		                info.setHostDn(vomsesMatcher.group(4));
		                vomses.add(info);
		            } else {
		            	log.debug("No vomses found!");
		            }        	
		        }
		    } catch (FileNotFoundException e) {
		    	log.error("Error opening vomses file '"
		    			+ vomsesFile.getAbsolutePath() + "': " + e.getMessage());
		    	throw e;	        
	        } finally {
	        	if (reader != null) {
	        		reader.close();
	        	}
	        }
	        return vomses;
        }
    }

    
    private static VOMSServerMap parseDir(File vomsesDir) throws IOException {
        VOMSServerMap vomses = new VOMSServerMap();
        log.debug("Parsing vomses dir: " + vomsesDir);
        for (File file : vomsesDir.listFiles()) {
            vomses.merge(parse(file));
        }
        return vomses;
    }

}
