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

package jlite.examples;

import java.util.List;

import jlite.GridSession;
import jlite.GridSessionConfig;
import jlite.GridSessionFactory;
import jlite.MatchedCE;
import jlite.util.PasswordPrompt;
import jlite.util.Util;

import org.glite.jdl.JobAd;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.myproxy.MyProxy;
import org.ietf.jgss.GSSCredential;

/**
 * This example illustrates how to use MyProxy with jLite API 
 * when MyProxy stores plain Globus proxy without VOMS extensions.
 * 
 * @author Oleg Sukhoroslov
 */
public class MyProxyDemo {
	
	public static void main(String[] args) {
		try {
			
			/*
			 * ---------------------------------------------------------------------
			 * Step O. Retrieve proxy (without VOMS extensions!) from MyProxy server
			 * ---------------------------------------------------------------------
			 */
			// set trusted CA certificates directory for Java CoG Kit
			System.setProperty("X509_CERT_DIR", "etc/certs/ca");
	        // invoke MyProxy via Java CoG Kit API
			MyProxy myProxy = new MyProxy("myproxy.ct.infn.it", 7512);
			String user = PasswordPrompt.promptNoMasking("Enter MyProxy user: ");
			String pass = PasswordPrompt.promptNoMasking("Enter MyProxy password: ");
			GSSCredential proxy = myProxy.get(user, pass, 12*3600);
			System.out.println("Retrieved proxy: " + proxy.getName());
			
			
			/*
			 * -----------------------------------------
			 * Step 1. Configure and create Grid session
			 * -----------------------------------------
			 */
			
			GridSessionConfig config = new GridSessionConfig();
			
			// set proxy retrieved from MyProxy as user credentials
			config.setUserCredentials(((GlobusGSSCredentialImpl)proxy).getGlobusCredential());
			
			// path to CA certificates
			config.setCertDir("etc/certs/ca");
			
			// paths to VOMS configuration files and certificates
			config.setVOMSDir("etc/vomses");
			config.setVOMSCertDir("etc/certs/voms");
			
			// path to WMProxy configuration files
			config.setWMSDir("etc/wms");
			
	        // create Grid session
	        GridSession session = GridSessionFactory.create(config);
	        
	        
	        /*
	         * -----------------------------------------
	         * Step 2. Create proxy with VOMS extensions
	         * -----------------------------------------
	         */	        
	        String vo = PasswordPrompt.promptNoMasking("Enter VO name: ");
	        GlobusCredential vomsProxy = session.createProxy(vo, 12*3600);
	        System.out.println("Created user proxy: " + vomsProxy.getSubject());
	        
	        
	        /*
	         * ----------------------------------------
	         * Step 3. Delegate proxy to WMProxy server 
	         * ----------------------------------------
	         */
	        session.delegateProxy("myId");
	        
	        
	        /*
	         * ----------------------------
	         * Step 4. Load job description  
	         * ----------------------------
	         */
	        JobAd jad = new JobAd();
	        jad.fromFile("test/normal/hello/hello.jdl");
	        String jdl = jad.toString();

	        
	        /*
	         * -----------------------------------------
	         * Step 5 (optional). List matched resources
	         * -----------------------------------------
	         */
	        List<MatchedCE> ces = session.listMatchedCE(jdl);
	        System.out.println("Matched Computing Elements: ");
	        for (MatchedCE ce : ces) {
	        	System.out.println("\t" + ce.getId());
	        }

	        
	        /*
	         * --------------------------
	         * Step 6. Submit job to grid
	         * --------------------------
	         */
	        String jobId = session.submitJob(jdl, "test/normal/hello");
	        System.out.println("Started job: " + jobId);				       

	        
	        /*
	         * --------------------------
	         * Step 7. Monitor job status
	         * --------------------------
	         */
	        String jobState = "";
	        do {
	        	Thread.sleep(10000);
	        	jobState = session.getJobState(jobId);
	        	System.out.println("Job status: " + jobState);
	        } while (!jobState.equals("DONE") && !jobState.equals("ABORTED"));


	        /*
	         * ---------------------------
	         * Step 8. Download job output
	         * ---------------------------
	         */
	        if (jobState.equals("DONE")) {
		        String outputDir = "test/normal/hello/" + Util.getShortJobId(jobId);
				session.getJobOutput(jobId, outputDir, true);
				System.out.println("Job output is downloaded to: " + outputDir);
	        }

			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
