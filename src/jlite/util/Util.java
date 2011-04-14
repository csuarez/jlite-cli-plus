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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import jlite.GridAPIException;

import org.glite.jdl.Ad;
import org.glite.jdl.Jdl;
import org.glite.voms.VOMSAttribute;
import org.glite.voms.VOMSValidator;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;

import condor.classad.Constant;
import condor.classad.Expr;
import condor.classad.ListExpr;

public class Util {
	
	public static String getShortJobId(String jobId) {
		return jobId.substring(jobId.lastIndexOf("/") + 1);
	}
	
	public static String sandboxDestURIToGridFTPDir(String destURI) {
        int pos = destURI.indexOf("2811");
        int length = destURI.length();                        
        String front = destURI.substring(0 , pos);
        String rear = destURI.substring(pos + 4 , length);                        	            
        return front + "2811/" + rear;
	}	
	
	public static List<File> getLocalInputSandboxFiles(Ad jdl, String pathPrefix) {
		List<File> files = new ArrayList<File>();
		if (jdl.hasAttribute(Jdl.INPUTSB) && !jdl.hasAttribute(Jdl.ISBBASEURI)) {

			// create list of ISB files
			List<String> isbFiles = new ArrayList<String>();
			Expr isbExpr = jdl.lookup(Jdl.INPUTSB);
			if (isbExpr instanceof Constant) { // single value
				isbFiles.add(((Constant)isbExpr).stringValue().replaceAll("\"", ""));
			} else if (isbExpr instanceof ListExpr) { // list
				Iterator<Expr> iFiles = ((ListExpr)isbExpr).iterator();
				while (iFiles.hasNext()) {
					isbFiles.add(iFiles.next().toString().replaceAll("\"", ""));
				}
			}
	
			// filter local files and add path prefix
			for (String file : isbFiles) { 
				if (!file.startsWith("gsiftp://") && !file.startsWith("root.")) { // local file
					String path;
					if (file.startsWith("file")) {
						path = file.replace("file://","");
					} else {
						path = pathPrefix!=null ? pathPrefix+"/"+file : file;
					}
					files.add(new File(path));
				}
			}
		}
		return files;
	}
	
	public static String readVOFromVOMSProxy(String proxyPath) throws GridAPIException {
		if ((new File(proxyPath).exists())) {
			try {
				GlobusCredential vomsProxy = new GlobusCredential(proxyPath);
				String vo = null;
				Vector<VOMSAttribute> atts = VOMSValidator.parse(vomsProxy.getCertificateChain());
				for (VOMSAttribute att : atts) {
					vo = att.getVO();
					if (vo != null) {
						break;
					}
				}
				if (vo != null) {
					return vo;
				} else {
					throw new GridAPIException("VOMS attributes not found in proxy file: " + proxyPath);
				}
			} catch (GlobusCredentialException e) {
				throw new GridAPIException(e);
			}
		} else {
			throw new GridAPIException("VOMS proxy file not found: " + proxyPath);
		}
	}
	
	public static String secondsToHHMMSS(long secs) {

		int hours = (int)(secs / 3600);
		int remainder = (int)(secs % 3600);
		int minutes = remainder / 60;
		int seconds = remainder % 60;
	
		return ( (hours < 10 ? "0" : "") + hours
		+ ":" + (minutes < 10 ? "0" : "") + minutes
		+ ":" + (seconds < 10 ? "0" : "") + seconds );

	}

}
