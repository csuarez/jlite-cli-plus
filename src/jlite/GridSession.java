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

package jlite;

import java.util.List;

import org.glite.wsdl.types.lb.JobStatus;
import org.globus.gsi.GlobusCredential;

/**
 * A central interface of jLite API which represents a grid session 
 * with certain user/proxy credentials.<br>
 * It provides methods for VOMS proxy creation, job submission, 
 * monitoring, output retrieval etc.<br>
 * Grid session is configured by a GridSessionConfig object, 
 * instantiated via a GridSessionFactory and implemented by a GridSessionImpl.
 * 
 * @author Oleg Sukhoroslov
 */
public interface GridSession {

	/**
	 * Creates a VOMS user proxy certificate.<br>
	 * If you want to request specific ACs or create a limited proxy, 
	 * check the advanced version.
	 * 
	 * @param vo VO name
	 * @param lifetime proxy validity time in seconds
	 * @return created proxy certificate
	 * @throws GridAPIException
	 */
	public GlobusCredential createProxy(String vo, int lifetime) throws GridAPIException;
	
	/**
	 * Creates a VOMS user proxy certificate (advanced version).
	 * 
	 * @param vomsArgs a list of VOMS commands in the form <voms>[:<command>] 
	 * identical to "-voms" arguments of glite-voms-proxy-init command
	 * @param lifetime proxy validity time in seconds
	 * @param proxyType version of proxy certificate {2,3,4} (use 2 for default gLite behavior)
	 * @param limited creates a limited proxy (use false for default gLite behavior)
	 * @return created proxy certificate 
	 * @throws GridAPIException
	 */
	public GlobusCredential createProxy(String[] vomsArgs, int lifetime, int proxyType, boolean limited) throws GridAPIException;
	
	/**
	 * Returns a proxy certificate linked to the session. 
	 * 
	 * @return proxy certiciate (null if proxy is not found)
	 */
	public GlobusCredential getProxy();
	
	/**
	 * Destroys a proxy certificate linked to the session.
	 * 
	 * @throws GridAPIException
	 */
	public void destroyProxy() throws GridAPIException;
	
	/**
	 * Delegates a proxy certificate linked to the session 
	 * to WMProxy service.<br> 
	 * The WMProxy service endpoint is determined by the 
	 * default VO of the proxy certificate.
	 * 
	 * @param delegationId delegation identifier
	 * @throws GridAPIException
	 */
	public void delegateProxy(String delegationId) throws GridAPIException;
	
	/**
	 * Delegates a proxy certificate linked to the session 
	 * to the specified WMProxy service.
	 * 
	 * @param wmProxyURL WMProxy service endpoint
	 * @param delegationId delegation identifier
	 * @throws GridAPIException
	 */
	public void delegateProxy(String wmProxyURL, String delegationId) throws GridAPIException;
	
	/**
	 * Matches available grid resources (computing elements, CE) 
	 * to requirements specified in the job description.<br>
	 * The WMProxy service endpoint is determined by the 
	 * default VO of the proxy certificate.<br>
	 * Returns a list of matched CEs sorted in decreasing rank order. 
	 * The MatchedCE structure contains a CE identifier and its rank.
	 * 
	 * @param jdl job description in JDL format
	 * @return list of matched CEs
	 * @throws GridAPIException
	 */
	public List<MatchedCE> listMatchedCE(String jdl) throws GridAPIException;
	
	/**
	 * Matches available grid resources (computing elements) 
	 * to requirements specified in the job description.<br>
	 * Returns a list of matched CEs sorted in decreasing rank order. 
	 * The MatchedCE structure contains a CE identifier and its rank.
	 * 
	 * @param wmProxyURL WMProxy service endpoint
	 * @param jdl job description in JDL format
	 * @return list of matched CEs
	 * @throws GridAPIException
	 */
	public List<MatchedCE> listMatchedCE(String wmProxyURL, String jdl) throws GridAPIException;
	
	/**
	 * Submits a job via WMProxy service.<br>
	 * The WMProxy service endpoint is determined by the 
	 * default VO of the proxy certificate.
	 * 
	 * @param jdl job description in JDL format
	 * @return job identifier
	 * @throws GridAPIException
	 */
	public String submitJob(String jdl) throws GridAPIException;
	
	/**
	 * Submits a job via WMProxy service.<br>
	 * The WMProxy service endpoint is determined by the 
	 * default VO of the proxy certificate.
	 * 
	 * @param jdl job description in JDL format
	 * @param inputDir all input files with relative paths 
	 * will be searched in the specified directory
	 * @return job identifier
	 * @throws GridAPIException
	 */
	public String submitJob(String jdl, String inputDir) throws GridAPIException;
	
	/**
	 * Submits a job via WMProxy service.
	 * 
	 * @param wmProxyURL WMProxy service endpoint
	 * @param jdl job description in JDL format
	 * @param inputDir search input files in the specified directory
	 * @return job identifier
	 * @throws GridAPIException
	 */
	public String submitJob(String wmProxyURL, String jdl, String inputDir) throws GridAPIException;
	
	/**
	 * Retrieves the status of a job.
	 * 
	 * @param jobId job identifier
	 * @return job status structure (as returned from LB WS endpoint)
	 * @throws GridAPIException
	 */
	public JobStatus getJobStatus(String jobId) throws GridAPIException;
	
	/**
	 * Retrieves current job state of a job.<br>
	 * Possible job states are: SUBMITTED, WAITING, READY, SCHEDULED, RUNNING, DONE, ABORTED, CANCELED, CLEARED 
	 * (refer to gLite documentation).
	 * 
	 * @param jobId job identifier
	 * @return job state 
	 * @throws GridAPIException
	 */
	public String getJobState(String jobId) throws GridAPIException;
	
	/**
	 * Retrieves the output of a job via GridFTP.<br>
	 * The WMProxy service endpoint is determined by the 
	 * default VO of the proxy certificate.<br>
	 * Throws GridAPIException if the job is not in DONE state.
	 * 
	 * @param jobId job identifier
	 * @param outputDir directory to store retrieved files
	 * @param purge purge job output from the server after retrieval 
	 * (use true for default gLite behavior) 
	 * @throws GridAPIException
	 */
	public void getJobOutput(String jobId, String outputDir, boolean purge) throws GridAPIException;
	
	/**
	 * Retrieves the output of a job via GridFTP.<br>
	 * Throws GridAPIException if the job is not in DONE state.
	 * 
	 * @param wmProxyURL WMProxy service endpoint
	 * @param jobId job identifier
	 * @param outputDir directory to store retrieved files
	 * @param purge purge job output from the server after retrieval 
	 * (use true for default gLite behavior) 
	 * @throws GridAPIException
	 */
	public void getJobOutput(String wmProxyURL, String jobId, String outputDir, boolean purge) throws GridAPIException;
	
	/**
	 * Returns URIs of the job output files on the server.<br>
	 * The WMProxy service endpoint is determined by the 
	 * default VO of the proxy certificate.<br>
	 * Throws GridAPIException if the job is not in DONE state.
	 * 
	 * @param jobId job identifier
	 * @return list of file URIs
	 * @throws GridAPIException
	 */
	public List<String> listJobOutput(String jobId) throws GridAPIException;
	
	/**
	 * Returns URIs of the job output files on the server.<br>
	 * Throws GridAPIException if the job is not in DONE state.
	 * 
	 * @param wmProxyURL WMProxy service endpoint
	 * @param jobId job identifier
	 * @return list of file URIs
	 * @throws GridAPIException
	 */
	public List<String> listJobOutput(String wmProxyURL, String jobId) throws GridAPIException;

	/**
	 * Requests cancellation of a job.<br>
	 * The WMProxy service endpoint is determined by the 
	 * default VO of the proxy certificate.
	 *  
	 * @param jobId job identifier
	 * @throws GridAPIException
	 */
	public void cancelJob(String jobId) throws GridAPIException;

	/**
	 * Requests cancellation of a job.
	 * 
	 * @param wmProxyURL WMProxy service endpoint
	 * @param jobId job identifier
	 * @throws GridAPIException
	 */
	public void cancelJob(String wmProxyURL, String jobId) throws GridAPIException;	
	
}
