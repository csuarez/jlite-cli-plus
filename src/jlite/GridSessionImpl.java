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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jlite.util.LBServiceFix;
import jlite.util.Util;

import org.apache.log4j.Logger;
import org.glite.jdl.Ad;
import org.glite.jdl.Jdl;
import org.glite.jdl.JobAd;
import org.glite.voms.VOMSAttribute;
import org.glite.voms.VOMSValidator;
import org.glite.voms.contact.VOMSProxyBuilder;
import org.glite.voms.contact.VOMSProxyInit;
import org.glite.voms.contact.VOMSRequestOptions;
import org.glite.voms.contact.VOMSServerInfo;
import org.glite.wms.wmproxy.DestURIStructType;
import org.glite.wms.wmproxy.JobIdStructType;
import org.glite.wms.wmproxy.StringAndLongType;
import org.glite.wms.wmproxy.WMProxyAPI;
import org.glite.wsdl.services.lb.LoggingAndBookkeepingLocator;
import org.glite.wsdl.services.lb.LoggingAndBookkeepingPortType;
import org.glite.wsdl.types.lb.JobFlags;
import org.glite.wsdl.types.lb.JobFlagsValue;
import org.glite.wsdl.types.lb.JobStatus;
import org.glite.wsdl.types.lb.StatName;
import org.glite.wsdl.types.lb.StateEnterTimesItem;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.io.urlcopy.UrlCopy;
import org.globus.io.urlcopy.UrlCopyException;
import org.globus.util.GlobusURL;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import condor.classad.Constant;
import condor.classad.Expr;
import condor.classad.ListExpr;

/**
 * Default implementation of the {@link GridSession} interface.
 * 
 * @author Oleg Sukhoroslov
 * @see GridSession
 */
public class GridSessionImpl implements GridSession {
	
    private static final Logger logger = Logger.getLogger(GridSessionImpl.class);
	
	private GridSessionConfig config;	
	private GlobusCredential vomsProxy = null;
	private Map<String,String> delegationIds;
	private Map<String,WMProxyAPI> wmProxyClients;
	
	protected GridSessionImpl(GridSessionConfig config) {
		this.config = config;
        
		if (config.getProxy() != null) {
			vomsProxy = config.getProxy();
		} else {
			if ((new File(config.getProxyPath()).exists())) {
				try {
					vomsProxy = new GlobusCredential(config.getProxyPath());
				} catch (GlobusCredentialException e) {
					e.printStackTrace();
				}
			}
		}
		
		delegationIds = new HashMap<String,String>();			
		wmProxyClients = new HashMap<String,WMProxyAPI>();		        
	}

	public void cancelJob(String jobId) throws GridAPIException {
		String vo = readVOFromVOMSProxy();
		if (config.getWMProxies().containsKey(vo)) {
			String wmProxyURL = config.getWMProxies().get(vo);
			cancelJob(wmProxyURL, jobId);
		} else {
			throw new GridAPIException("Could not find WMProxy server for VO: " + vo);
		}
	}

	public void cancelJob(String wmProxyURL, String jobId) throws GridAPIException {
		try {			
			WMProxyAPI client = getWMProxyClient(wmProxyURL);
			client.jobCancel(jobId);
		} catch (Exception e) {
			if (e instanceof GridAPIException) {
				throw (GridAPIException)e;
			} else {
				throw new GridAPIException(e);
			}
		}
	}

	public GlobusCredential createProxy(String vo, int lifetime) throws GridAPIException {
		return createProxy(new String[]{vo}, lifetime, VOMSProxyBuilder.DEFAULT_PROXY_TYPE, false);
	}
	
	public GlobusCredential createProxy(String[] vomsArgs, int lifetime, int proxyType, boolean limited) throws GridAPIException {
		try {
			VOMSProxyInit vomsProxyInit;
			if (config.getUserCredentials() == null) {
				vomsProxyInit = VOMSProxyInit.instance(config.getUserKeyPass());
			} else {
				vomsProxyInit = VOMSProxyInit.instance(config.getUserCredentials());
			}
			if (config.getProxyPath() != null) {
				vomsProxyInit.setProxyOutputFile(config.getProxyPath());
			}
	        vomsProxyInit.setProxyType(proxyType);
	        if (limited) {
	        	vomsProxyInit.setDelegationType(GSIConstants.DELEGATION_LIMITED);
	        } else {
	        	vomsProxyInit.setDelegationType(GSIConstants.DELEGATION_FULL);
	        }
	        vomsProxyInit.setProxyLifetime(lifetime);
	        
	        List<VOMSRequestOptions> optionsList = new ArrayList<VOMSRequestOptions>();
	        Map<String,Integer> vomsMap = new HashMap<String,Integer>();
	        for (String vomsArg : vomsArgs) {
	        	String voms = vomsArg;
	        	String fqan = null;
	        	if (vomsArg.indexOf(":") > 0) {
	    			String[] vomsParts = vomsArg.split(":");
	    			voms = vomsParts[0];
	    			fqan = vomsParts[1];
	        	}
    			if (config.getVOMSServers().get(voms) != null) {
    		        VOMSServerInfo vomsInfo = (VOMSServerInfo)config.getVOMSServers().get(voms).iterator().next();
    		        logger.debug("VOMS for VO " + voms + ": " + vomsInfo.getHostName() + ":" + vomsInfo.getPort());
    		        vomsProxyInit.addVomsServer(vomsInfo);
    		        VOMSRequestOptions options;
    		        if (vomsMap.containsKey(voms)) {
    		        	options = optionsList.get(vomsMap.get(voms));    		        	
    		        } else {
    		        	options = new VOMSRequestOptions(); 
    		        }
    		        options.setVoName(voms);
    		        options.setLifetime(lifetime);
    		        if (fqan != null) {
    		        	options.addFQAN(fqan);
    		        	logger.debug("Attribute for VO " + voms + ": " + fqan);
    		        }
    		        if (vomsMap.containsKey(voms)) {
    		        	optionsList.set(vomsMap.get(voms), options);    		        	
    		        } else {
    		        	optionsList.add(options);
    		        	vomsMap.put(voms, optionsList.size() - 1); 
    		        }
    			} else {
    				throw new GridAPIException("Could not find VOMS server info for VO: " + voms);
    			}
	        }	        
	        
	        vomsProxy = vomsProxyInit.getVomsProxy(optionsList);
	        return vomsProxy;
		} catch (Exception e) {
			if (e instanceof GridAPIException) {
				throw (GridAPIException)e;
			} else {
				throw new GridAPIException(e);
			}
		}
	}

	public void delegateProxy(String delegationId) throws GridAPIException {
		String vo = readVOFromVOMSProxy();
		if (config.getWMProxies().containsKey(vo)) {
			String wmProxyURL = config.getWMProxies().get(vo);
			delegateProxy(wmProxyURL, delegationId);
		} else {
			throw new GridAPIException("Could not find WMProxy server for VO: " + vo);
		}
	}
	
	public void delegateProxy(String wmProxyURL, String delegationId) throws GridAPIException {
		try {
			WMProxyAPI client = getWMProxyClient(wmProxyURL);
			String proxy = client.grstGetProxyReq(delegationId);
			client.grstPutProxy(delegationId, proxy);
			delegationIds.put(wmProxyURL, delegationId);
		} catch (Exception e) {
			if (e instanceof GridAPIException) {
				throw (GridAPIException)e;
			} else {
				throw new GridAPIException(e);
			}
		}
	}
	
	public void destroyProxy() throws GridAPIException {
		File proxyFile = new File(config.getProxyPath());
		if (proxyFile.exists()) {
			boolean deleted = proxyFile.delete();
			if (!deleted) {
				throw new GridAPIException("Could not delete proxy file: " + config.getProxyPath());
			}
		} else {
			throw new GridAPIException("Proxy file not found: " + config.getProxyPath());
		}
	}

	public void getJobOutput(String jobId, String outputDir, boolean purge) throws GridAPIException {
		String vo = readVOFromVOMSProxy();
		if (config.getWMProxies().containsKey(vo)) {
			String wmProxyURL = config.getWMProxies().get(vo);
			getJobOutput(wmProxyURL, jobId, outputDir, purge);
		} else {
			throw new GridAPIException("Could not find WMProxy server for VO: " + vo);
		}
	}
	
	public void getJobOutput(String wmProxyURL, String jobId, String outputDir, boolean purge) throws GridAPIException {
		try {			
            JobStatus status = getJobStatus(jobId);
            StatName currState = status.getState();
            if (currState.equals(StatName.DONE)) {
            	
                File dir = new File(outputDir);
                if (!dir.exists()) {
                	dir.mkdirs();
                }
            
				WMProxyAPI client = getWMProxyClient(wmProxyURL);
	            String[] children = status.getChildren();            
	            
	            if (children == null || children.length == 0) { // no children
	            	
	    			List<String> files = new ArrayList<String>();
	    	        StringAndLongType[] fileInfo = client.getOutputFileList(jobId, "gsiftp").getFile();
	    	        for (StringAndLongType file : fileInfo) {
	    	        	files.add(file.getName());    
	    	        }
	    	        downloadFilesFromGridFTP(files, outputDir);
	    	        logger.debug("Downloaded " + files.size() + " output files");
	            	
	            } else { // download children outputs
	            	
	            	for (int i=0; i<children.length; i++) {
	            		String child = children[i];
	        			List<String> files = new ArrayList<String>();
	        	        StringAndLongType[] fileInfo = client.getOutputFileList(child, "gsiftp").getFile();
	        	        for (StringAndLongType file : fileInfo) {
	        	        	files.add(file.getName());		            
	        	        }
	        	        
	        	        String jobName = "job" + (i+1); // by default
	    	    		JobAd jdl = new JobAd(status.getChildrenStates(i).getJdl());
	    	    		if (jdl.hasAttribute("NodeName")) {
	    	    			jobName = jdl.getString("NodeName");
	    	    		}
	    	    		
	        	        File childDir = new File(outputDir + "/" + jobName);
	        	        childDir.mkdirs();
	        	        downloadFilesFromGridFTP(files, childDir.getAbsolutePath());
	        	        logger.debug("Downloaded " + files.size() + 
	        	        		" output files for child job " + (i+1) + ": " + child);
	            	}
	            	
	            }
	
	            if (purge) {
	            	client.jobPurge(jobId);
	            }
	            
            } else {
            	throw new GridAPIException("Job output is not available. Current job status is: " + currState);
            }
		} catch (Exception e) {
			if (e instanceof GridAPIException) {
				throw (GridAPIException)e;
			} else {
				throw new GridAPIException(e);
			}
		}
	}

	public String getJobState(String jobId) throws GridAPIException {
		try {
			return getJobStatus(jobId).getState().getValue();
		} catch (Exception e) {
			if (e instanceof GridAPIException) {
				throw (GridAPIException)e;
			} else {
				throw new GridAPIException(e);
			}
		}
	}
	
	public JobStatus getJobStatus(String jobId) throws GridAPIException {
		try {
			URL jobUrl = new URL(jobId);
			URL lbServiceURL = new URL(jobUrl.getProtocol(), jobUrl.getHost(), 9003, "");
			LoggingAndBookkeepingLocator locator = new LoggingAndBookkeepingLocator();
			LoggingAndBookkeepingPortType lbService = locator.getLoggingAndBookkeeping(lbServiceURL);
			String lbServiceVersion = lbService.getVersion(null);
			
			if (logger.isDebugEnabled()) {
				logger.debug("LB Service URL: " + lbServiceURL);
				logger.debug("LB Service Version: " + lbServiceVersion);
			}
	        
			JobFlags flags = new JobFlags();    
	        flags.setFlag( new JobFlagsValue[] { JobFlagsValue.CLASSADS, JobFlagsValue.CHILDREN, JobFlagsValue.CHILDSTAT } );
			JobStatus status = lbService.jobStatus(jobId, flags);
			
			// if LB version < 1.7.1 fix the status names
			String[] versionParts = lbServiceVersion.split("\\.");
			if (Integer.parseInt(versionParts[0]) == 1 && (Integer.parseInt(versionParts[1]) < 7 ||
					(Integer.parseInt(versionParts[1]) == 7 && Integer.parseInt(versionParts[2]) < 1))) {
				logger.debug("Using fix for LB job status history");
				status = LBServiceFix.fixJobStatus(status); 
			}
			
			// remove state enter times with wrong chronology (a bug in LB WS?)
			List<StateEnterTimesItem> newItems = new ArrayList<StateEnterTimesItem>();
			long prevTime = 0;
			for (StateEnterTimesItem item : status.getStateEnterTimes()) {
				long currTime = item.getTime().getTimeInMillis();
				if (prevTime == 0 || currTime > prevTime) {
					newItems.add(item);
					prevTime = currTime;
				}
			}
			status.setStateEnterTimes(newItems.toArray(new StateEnterTimesItem[]{}));
			

			return status;
		} catch (Exception e) {
			if (e instanceof GridAPIException) {
				throw (GridAPIException)e;
			} else {
				throw new GridAPIException(e);
			}
		}
	}

	public GlobusCredential getProxy() {
		return vomsProxy;
	}
	
	public List<String> listJobOutput(String jobId) throws GridAPIException {
		String vo = readVOFromVOMSProxy();
		if (config.getWMProxies().containsKey(vo)) {
			String wmProxyURL = config.getWMProxies().get(vo);
			return listJobOutput(wmProxyURL, jobId);
		} else {
			throw new GridAPIException("Could not find WMProxy server for VO: " + vo);
		}
	}

	public List<String> listJobOutput(String wmProxyURL, String jobId)
			throws GridAPIException {
		try {
			List<String> files = new ArrayList<String>();
            JobStatus status = getJobStatus(jobId);
            StatName currState = status.getState();
            if (currState.equals(StatName.DONE)) {
            
				WMProxyAPI client = getWMProxyClient(wmProxyURL);
	            String[] children = status.getChildren();            
	            
	            if (children == null || children.length == 0) { // no children
	            	
	    	        StringAndLongType[] fileInfo = client.getOutputFileList(jobId, "gsiftp").getFile();
	    	        for (StringAndLongType file : fileInfo) {
	    	        	files.add(file.getName());    
	    	        }
	            	
	            } else { // children outputs
	            	
	            	for (int i=0; i<children.length; i++) {
	            		String child = children[i];
	        	        StringAndLongType[] fileInfo = client.getOutputFileList(child, "gsiftp").getFile();
	        	        for (StringAndLongType file : fileInfo) {
	        	        	files.add(file.getName());		            
	        	        }
	            	}
	            	
	            }
	            
	            return files;	            
            } else {
            	throw new GridAPIException("Job output is not available. Current job status is: " + currState);
            }
		} catch (Exception e) {
			if (e instanceof GridAPIException) {
				throw (GridAPIException)e;
			} else {
				throw new GridAPIException(e);
			}
		}		
	}

	public List<MatchedCE> listMatchedCE(String jdl) throws GridAPIException {
		String vo = readVOFromVOMSProxy();
		if (config.getWMProxies().containsKey(vo)) {
			String wmProxyURL = config.getWMProxies().get(vo);
			return listMatchedCE(wmProxyURL, jdl);
		} else {
			throw new GridAPIException("Could not find WMProxy server for VO: " + vo);
		}
	}
	
	public List<MatchedCE> listMatchedCE(String wmProxyURL, String jdl) throws GridAPIException {
		if (delegationIds.containsKey(wmProxyURL) || config.getDelegationId() != null) {
			try {
				List<MatchedCE> ces = new ArrayList<MatchedCE>();
				WMProxyAPI client = getWMProxyClient(wmProxyURL);
				String delegationId = delegationIds.containsKey(wmProxyURL)?
						delegationIds.get(wmProxyURL):config.getDelegationId();
		        StringAndLongType[] matchedCEs = client.jobListMatch(jdl, delegationId).getFile();
		        for (StringAndLongType ce : matchedCEs) {		        	
		            ces.add(new MatchedCE(ce.getName(), ce.getSize()));
		        }
		        return ces;
			} catch (Exception e) {
				throw new GridAPIException(e);
			}
		} else {
			throw new GridAPIException("Could not find delegationId for WMProxy server: " + wmProxyURL);
		}
	}

	public String submitJob(String jdl) throws GridAPIException {
		return submitJob(jdl, null);
	}	

	public String submitJob(String jdl, String inputDir) throws GridAPIException {
		String vo = readVOFromVOMSProxy();
		if (config.getWMProxies().containsKey(vo)) {
			String wmProxyURL = config.getWMProxies().get(vo);
			return submitJob(wmProxyURL, jdl, inputDir);
		} else {
			throw new GridAPIException("Could not find WMProxy server for VO: " + vo);
		}	
	}
	
	public String submitJob(String wmProxyURL, String jdl, String inputDir) throws GridAPIException {
		if (delegationIds.containsKey(wmProxyURL) || config.getDelegationId() != null) {
			try {
				WMProxyAPI client = getWMProxyClient(wmProxyURL);				
				String delegationId = delegationIds.containsKey(wmProxyURL)?
						delegationIds.get(wmProxyURL):config.getDelegationId();
				JobIdStructType job = null;
				String jobId = null;
				
				JobAd jobAd = new JobAd(jdl);
				logger.debug("Sumbitting JDL: " + jdl);
				
				// determine request type (default is "Job")
				String requestType = Jdl.TYPE_JOB;
				if (jobAd.hasAttribute(Jdl.TYPE)) {
					requestType = jobAd.getString(Jdl.TYPE);
				}
				logger.debug("Request type: " + requestType);
				
				if (requestType.equalsIgnoreCase(Jdl.TYPE_JOB)) { // JOB

					// determine job type (default is "Normal")
					String jobType = Jdl.JOBTYPE_NORMAL;
					if (jobAd.hasAttribute(Jdl.JOBTYPE)) {
						jobType = (String)jobAd.getStringValue(Jdl.JOBTYPE).get(0);
					}
					logger.debug("Job type: " + jobType);
					
					List<File> filesToUpload = new ArrayList<File>();
					
					if (jobType.equalsIgnoreCase(Jdl.JOBTYPE_NORMAL)) { // Normal
						
						filesToUpload = Util.getLocalInputSandboxFiles(jobAd, inputDir);
						
					} else if (jobType.equalsIgnoreCase(Jdl.JOBTYPE_INTERACTIVE)) { // Interactive
						
						throw new GridAPIException("Unsupported JDL job type: " + jobType);
						
					} else if (jobType.equalsIgnoreCase(Jdl.JOBTYPE_MPICH)) { // MPICH
						
						throw new GridAPIException("Unsupported JDL job type: " + jobType);
						
					} else if (jobType.equalsIgnoreCase(Jdl.JOBTYPE_PARAMETRIC)) { // Parametric
						
						if (jobAd.hasAttribute(Jdl.PARAMETRIC_PARAMS)) {
							
							// Create parameters list
							List<String> params = new ArrayList<String>();							
							Expr paramsExpr = jobAd.lookup(Jdl.PARAMETRIC_PARAMS);							
							if (paramsExpr instanceof Constant) { // single integer value
								int paramsAtt = ((Constant)paramsExpr).intValue();
								int paramsStartAtt = 0; // default value
								if (jobAd.hasAttribute(Jdl.PARAMETRIC_PARAMS_START)) {
									paramsStartAtt = jobAd.getInt(Jdl.PARAMETRIC_PARAMS_START);
								}
								int paramsStepAtt = 1; // default value
								if (jobAd.hasAttribute(Jdl.PARAMETRIC_PARAMS_STEP)) {
									paramsStepAtt = jobAd.getInt(Jdl.PARAMETRIC_PARAMS_STEP);
								}
								for (int i=paramsStartAtt; i<paramsAtt; i+=paramsStepAtt) {
									params.add(String.valueOf(i));
								}
							} else if (paramsExpr instanceof ListExpr) { // parameters list
								System.out.println(jobAd.lookup(Jdl.PARAMETRIC_PARAMS).getClass().getName());
								Iterator<Expr> iParams = ((ListExpr)jobAd.lookup(Jdl.PARAMETRIC_PARAMS)).iterator();
								while (iParams.hasNext()) {
									params.add(iParams.next().toString());
								}
							}

							// Create local input files list
							List<File> isbFiles = Util.getLocalInputSandboxFiles(jobAd, inputDir);
							for (File file : isbFiles) {
								if (file.getAbsolutePath().indexOf("_PARAM_") >= 0) {
									for (String param : params) {
										File paramFile = new File(
												file.getAbsolutePath().replaceAll("_PARAM_", param));
										filesToUpload.add(paramFile);
									}
								} else {
									filesToUpload.add(file);
								}
							}
							
						} else {
							throw new GridAPIException("Parametric job description must contain Parameters attribute");
						}												
					} else {
						throw new GridAPIException("Unknown JDL job type: " + jobType);
					}
					
					if (filesToUpload.size() == 0) { // no files to upload, just submit job
						
						job = client.jobSubmit(jdl, delegationId);
						jobId = job.getId();
						logger.debug("Submitted job: " + jobId);
						
					} else { // register job, upload files to input sandbox, start job
						
						job = client.jobRegister(jdl, delegationId);
						jobId = job.getId();
						logger.debug("Registered job: " + jobId);
						
						String[] destURIs = client.getSandboxDestURI(jobId, "gsiftp").getItem();
						String gridFTPDir = Util.sandboxDestURIToGridFTPDir(destURIs[0]);
						uploadFilesToGridFTP(filesToUpload, gridFTPDir);
						logger.debug("Uploaded " + filesToUpload.size() + " job input file(s)");
						
				        client.jobStart(jobId);
				        logger.debug("Started job: " + jobId);
					}

				} else if (requestType.equalsIgnoreCase(Jdl.TYPE_COLLECTION)) { // COLLECTION

					List<List<File>> filesToUpload = new ArrayList<List<File>>();
					int filesToUploadCount = 0;
					
					// root input sandbox
					filesToUpload.add(Util.getLocalInputSandboxFiles(jobAd, inputDir));
					
					// children input sandboxes
					Vector<Ad> nodes = jobAd.getAdValue("Nodes");
					for (Ad node : nodes) {
						filesToUpload.add(Util.getLocalInputSandboxFiles(node, inputDir));
					}
					logger.debug("Collection has " + (filesToUpload.size()-1)  + " children");
					
					if (filesToUploadCount == 0) { // no files to upload, just submit job
						
						job = client.jobSubmit(jdl, delegationId);
						jobId = job.getId();
						logger.debug("Submitted job: " + jobId);
						
					} else { // register job, upload files to input sandboxes, start job
						
						job = client.jobRegister(jdl, delegationId);
						jobId = job.getId();
						logger.debug("Registered job: " + jobId);
						
						DestURIStructType[] sboxes = client.getSandboxBulkDestURI(jobId, "gsiftp").getItem();
						// root input sandbox
						if (filesToUpload.get(0).size() > 0) {
							String rootSbox = sboxes[0].getItem()[0];
							uploadFilesToGridFTP(filesToUpload.get(0), 
									Util.sandboxDestURIToGridFTPDir(rootSbox));
							logger.debug("Uploaded " + filesToUpload.get(0).size() 
									+ " input file(s) for root job");
						} else {
							logger.debug("No input files to upload for root job");
						}
						// children input sandboxes
						for (int i=1; i<sboxes.length; i++) {
							if (filesToUpload.get(i).size() > 0) {
								String sbox = sboxes[i].getItem()[0];
								uploadFilesToGridFTP(filesToUpload.get(i), 
										Util.sandboxDestURIToGridFTPDir(sbox));
								logger.debug("Uploaded " + filesToUpload.get(i).size() + 
										" input file(s) for child job: " + sboxes[i].getId());
							} else {
								logger.debug("No input files to upload for child job: " + sboxes[i].getId());
							}
						}
						
				        client.jobStart(jobId);
				        logger.debug("Started job: " + jobId);
					}	
					
				} else if (requestType.equalsIgnoreCase(Jdl.TYPE_DAG)) { // DAG
				
					throw new GridAPIException("Unsupported JDL request type: " + requestType);
					
				} else {
					throw new GridAPIException("Unknown JDL request type: " + requestType);
				}
								
		        return jobId;
			} catch (Exception e) {
				if (e instanceof GridAPIException) {
					throw (GridAPIException)e;
				} else {
					throw new GridAPIException(e);
				}
			}
		} else {
			throw new GridAPIException("Could not find delegationId for WMProxy server: " + wmProxyURL);
		}
	}


	
	private String readVOFromVOMSProxy() throws GridAPIException {
		if (vomsProxy != null) {
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
				throw new GridAPIException("VOMS attributes not found in proxy");
			}
		} else {
			throw new GridAPIException("VOMS proxy not found");
		}
	}
	
	private WMProxyAPI getWMProxyClient(String wmProxyURL) throws Exception {
		if (wmProxyClients.containsKey(wmProxyURL)) {
			return wmProxyClients.get(wmProxyURL);
		} else {
			WMProxyAPI client;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			vomsProxy.save(out);
			out.close();
			InputStream in = new ByteArrayInputStream(out.toByteArray());
			if (config.getCertDir() == null) {
				client = new WMProxyAPI(wmProxyURL, in);
				//client = new WMProxyAPI(wmProxyURL, config.getProxyPath());
			} else {
				client = new WMProxyAPI(wmProxyURL, in, config.getCertDir());
				//client = new WMProxyAPI(wmProxyURL, config.getProxyPath(), config.getCertDir());
			}
			in.close();
			wmProxyClients.put(wmProxyURL, client);
			return client;
		}
	}
	
	private void uploadFilesToGridFTP(List<File> files, String destDir) throws MalformedURLException, UrlCopyException, GSSException {
		for (File file : files) {
            String sourceURI = "file:///" + file.getAbsolutePath();                
            String destURI = destDir + "/" + file.getName();	            
            
            GlobusURL from = new GlobusURL(sourceURI);
            GlobusURL to = new GlobusURL(destURI);
                    
            UrlCopy uCopy = new UrlCopy();
            uCopy.setCredentials(new GlobusGSSCredentialImpl(vomsProxy, GSSCredential.DEFAULT_LIFETIME));
            uCopy.setDestinationUrl(to);
            uCopy.setSourceUrl(from);
            
            logger.debug("Start uploading file: " + sourceURI + " >> " + destURI);
            uCopy.copy();            
            logger.debug("Uploaded file: " + sourceURI + " >> " + destURI);
		}
	}
	
	private void downloadFilesFromGridFTP(List<String> files, String destDir) throws MalformedURLException, UrlCopyException, GSSException {
		for (String file : files) {
            int pos = file.indexOf("2811");
            int length = file.length();                        
            String front = file.substring(0 , pos);
            String rear = file.substring(pos + 4 , length);                        	            
            String sourceURI = front + "2811/" + rear;
			
            String destURI = "file:///" + destDir + "/" + file.substring(file.lastIndexOf("/")+1);
                            
            GlobusURL from = new GlobusURL(sourceURI);
            GlobusURL to = new GlobusURL(destURI);
                    
            UrlCopy uCopy = new UrlCopy();
            uCopy.setCredentials(new GlobusGSSCredentialImpl(vomsProxy, GSSCredential.DEFAULT_LIFETIME));
            uCopy.setDestinationUrl(to);
            uCopy.setSourceUrl(from);
            uCopy.setUseThirdPartyCopy(true);
            
            logger.debug("Start downloading file: " + sourceURI + " >> " + destURI);
            uCopy.copy();            
            logger.debug("Downloaded file: " + sourceURI + " >> " + destURI);
		}
	}
	
}
