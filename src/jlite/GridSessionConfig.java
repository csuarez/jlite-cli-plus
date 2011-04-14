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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jlite.util.VOMSESFileParser;
import jlite.util.WMSDirParser;

import org.apache.axis.AxisProperties;
import org.glite.voms.contact.VOMSServerInfo;
import org.glite.voms.contact.VOMSServerMap;
import org.globus.gsi.GlobusCredential;

/**
 * Custom configuration of the grid session.<br>
 * Use it to override the default configuration locations and options 
 * (refer to the user manual).<br>
 * When instantiated it applies the default configuration, so you 
 * can also use getter methods to check the default values. 
 * 
 * @author Oleg Sukhoroslov
 */
public class GridSessionConfig {
	
	private String userCertPath;
	private String userKeyPath;
	private String userKeyPass;	
	private GlobusCredential userCred;
	
	private String proxyPath;
	private GlobusCredential proxy;

	private String certDir;
	private String vomsesDir;
	private String vomsCertDir;
	private VOMSServerMap vomsServers;
	private String wmsDir;
	private Map<String,String> wmProxies;
	private String delegationId;
	
	/**
	 * Applies the default configuration locations and options 
	 * (refer to the user manual).
	 */
	public GridSessionConfig() {
		vomsServers = new VOMSServerMap();
		wmProxies = new HashMap<String,String>();
		
	    // default path to user certificate
		if (System.getenv("X509_USER_CERT") != null) {
			setUserCertPath(System.getenv("X509_USER_CERT"));
		} else {
			setUserCertPath(System.getProperty("user.home")
				+ "/.globus/usercert.pem");			
		}
		
		// default path to user key
		if (System.getenv("X509_USER_KEY") != null) {
			setUserKeyPath(System.getenv("X509_USER_KEY"));
		} else {
			setUserKeyPath(System.getProperty("user.home")
				+ "/.globus/userkey.pem");			
		}
		
    	// default path to VOMS user proxy file
		if (System.getenv("X509_USER_PROXY") != null) {
			setProxyPath(System.getenv("X509_USER_PROXY"));
		} else {
			setProxyPath(System.getProperty("java.io.tmpdir")
					+ File.separator + "x509up_u_"
					+ System.getProperty("user.name"));
		}
		
		// default path to CA certificates
		if (System.getenv("JLITE_HOME") != null && 
				new File(System.getenv("JLITE_HOME") + "/etc/certs/ca").exists()) {
			setCertDir(System.getenv("JLITE_HOME") + "/etc/certs/ca");
		} else if (System.getProperty("JLITE_HOME") != null && 
					new File(System.getProperty("JLITE_HOME") + "/etc/certs/ca").exists()) {
				setCertDir(System.getProperty("JLITE_HOME") + "/etc/certs/ca");			
		} else if (System.getenv("X509_CERT_DIR") != null) {
			setCertDir(System.getenv("X509_CERT_DIR"));
		} else if (new File("/etc/grid-security/certificates").exists()) {
			setCertDir("/etc/grid-security/certificates");
		}
		
		// default path to VOMS configuration files
		if (System.getenv("JLITE_HOME") != null && 
				new File(System.getenv("JLITE_HOME") + "/etc/vomses").exists()) {
			setVOMSDir(System.getenv("JLITE_HOME") + "/etc/vomses");
		} else if (System.getProperty("JLITE_HOME") != null && 
				new File(System.getProperty("JLITE_HOME") + "/etc/vomses").exists()) {
			setVOMSDir(System.getProperty("JLITE_HOME") + "/etc/vomses");
		} else if (System.getenv("VOMS_USERCONF") != null) {
			setVOMSDir(System.getenv("VOMS_USERCONF"));
		} else if (new File(System.getProperty("user.home") + "/.glite/vomses").exists()) {
			setVOMSDir(System.getProperty("user.home") + "/.glite/vomses");
		} else if (System.getenv("GLITE_LOCATION") != null && 
			new File(System.getenv("GLITE_LOCATION") + "/etc/vomses").exists()) {		
			setVOMSDir(System.getenv("GLITE_LOCATION") + "/etc/vomses");
		} else if (new File("/opt/glite/etc/vomses").exists()) {
			setVOMSDir("/opt/glite/etc/vomses");
		}
		
		// default path to VOMS certificates
		if (System.getenv("JLITE_HOME") != null && 
				new File(System.getenv("JLITE_HOME") + "/etc/certs/voms").exists()) {
			setVOMSCertDir(System.getenv("JLITE_HOME") + "/etc/certs/voms");
		} else if (System.getProperty("JLITE_HOME") != null && 
				new File(System.getProperty("JLITE_HOME") + "/etc/certs/voms").exists()) {
			setVOMSCertDir(System.getProperty("JLITE_HOME") + "/etc/certs/voms");
		} else if (System.getenv("X509_VOMS_DIR") != null) {
			setVOMSCertDir(System.getenv("X509_VOMS_DIR"));
		} else if (new File("/etc/grid-security/vomsdir").exists()) {
			setVOMSCertDir("/etc/grid-security/vomsdir");
		}
		
		// WMProxy client configuration files
		if (new File("/opt/glite/etc").exists()) {
			wmProxies.putAll(WMSDirParser.readWMProxies("/opt/glite/etc")); 
		}		
		if (System.getenv("GLITE_LOCATION") != null &&
				new File(System.getenv("GLITE_LOCATION") + "/etc").exists()) {
			wmProxies.putAll(WMSDirParser.readWMProxies(System.getenv("GLITE_LOCATION") + "/etc")); 
		}		
		if (System.getenv("GLITE_WMS_LOCATION") != null &&
				new File(System.getenv("GLITE_WMS_LOCATION") + "/etc").exists()) {
			wmProxies.putAll(WMSDirParser.readWMProxies(System.getenv("GLITE_WMS_LOCATION") + "/etc")); 
		}		
		if (new File(System.getProperty("user.home") + "/.glite").exists()) {
			wmProxies.putAll(WMSDirParser.readWMProxies(System.getProperty("user.home") + "/.glite")); 
		}		
		if (System.getenv("JLITE_HOME") != null && 
				new File(System.getenv("JLITE_HOME") + "/etc/wms").exists()) {
			wmProxies.putAll(WMSDirParser.readWMProxies(System.getenv("JLITE_HOME") + "/etc/wms")); 
		}
		if (System.getProperty("JLITE_HOME") != null && 
				new File(System.getProperty("JLITE_HOME") + "/etc/wms").exists()) {
			wmProxies.putAll(WMSDirParser.readWMProxies(System.getProperty("JLITE_HOME") + "/etc/wms")); 
		}
				

		// important properties
		System.setProperty( "sslProtocol", "SSLv3" );
        AxisProperties.setProperty( "axis.socketSecureFactory",
                            "org.glite.security.trustmanager.axis.AXISSocketFactory" );

	}
		
	/**
	 * Returns the location of trusted certificates directory.
	 * 
	 * @return trusted certificates directory
	 */
	public String getCertDir() {
		return certDir;
	}

	/**
	 * Sets the location of trusted certificates directory.
	 * 
	 * @param certDir trusted certificates directory
	 */
	public void setCertDir(String certDir) {
		this.certDir = certDir;
		System.setProperty("X509_CERT_DIR", certDir);
        System.setProperty("CADIR", certDir);	  
        System.setProperty("crlFiles", 
        		certDir + File.separator + "*.r0");
        System.setProperty(org.glite.security.trustmanager.ContextWrapper.CA_FILES, certDir 
        		+ File.separator + "*.0");
	}
	
	/**
	 * Returns the location of VOMS configuration files.
	 * 
	 * @return location of VOMS configuration files
	 */
	public String getVOMSDir() {
		return vomsesDir;
	}

	/**
	 * Sets the location of VOMS configuration files.
	 * 
	 * @param vomsDir location of VOMS configuration files
	 */
	public void setVOMSDir(String vomsDir) {
		this.vomsesDir = vomsDir;
    	String vomsParentDir = vomsDir.substring(0, vomsDir.lastIndexOf("/"));    	
		System.setProperty("VOMSES_LOCATION", vomsParentDir);
    	try {
    		vomsServers.merge(
    				VOMSESFileParser.parse(new File(vomsDir)));
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
	}
	
	/**
	 * Returns the location of the VOMS certificates directory.
	 * 
	 * @return location of the VOMS certificates directory
	 */
	public String getVOMSCertDir() {
		return vomsCertDir;
	}

	/**
	 * Sets the location of the VOMS certificates directory.
	 * 
	 * @param vomsCertDir location of the VOMS certificates directory
	 */
	public void setVOMSCertDir(String vomsCertDir) {
		this.vomsCertDir = vomsCertDir;
		System.setProperty("VOMSDIR", vomsCertDir);
	}

	/**
	 * Returns all known VOMS servers. 
	 * 
	 * @return map <VO-name, VOMS-server-config>
	 */
	public VOMSServerMap getVOMSServers() {
		return vomsServers;
	}

	/**
	 * Adds VOMS server configuration. 
	 * 
	 * @param voms VOMS server configuration
	 */
	public void addVOMSServer(VOMSServerInfo voms) {
		vomsServers.add(voms);
	}

	/**
	 * Returns the location of the user certificate file.
	 * 
	 * @return location of the user certificate file
	 */
	public String getUserCertPath() {
		return userCertPath;
	}

	/**
	 * Sets the location of the user certificate file. 
	 * 
	 * @param userCertPath location of the user certificate file
	 */
	public void setUserCertPath(String userCertPath) {
		this.userCertPath = userCertPath;
		System.setProperty("X509_USER_CERT", userCertPath);
	}

	/**
	 * Returns the location of the user private key file.
	 * 
	 * @return location of the user private key file
	 */
	public String getUserKeyPath() {
		return userKeyPath;
	}

	/**
	 * Sets the location of the user private key file.
	 * 
	 * @param userKeyPath location of the user private key file
	 */
	public void setUserKeyPath(String userKeyPath) {
		this.userKeyPath = userKeyPath;
		System.setProperty("X509_USER_KEY", userKeyPath);
	}

	/**
	 * Returns the passphrase for the user private key.
	 * 
	 * @return passphrase for the user private key
	 */
	public String getUserKeyPass() {
		return userKeyPass;
	}

	/**
	 * Sets the passphrase for the user private key.
	 * 
	 * @param userKeyPass passphrase for the user private key
	 */
	public void setUserKeyPass(String userKeyPass) {
		this.userKeyPass = userKeyPass;
	}

	/**
	 * Returns the location of the VOMS user proxy certificate.
	 * 
	 * @return location of the VOMS user proxy certificate
	 */
	public String getProxyPath() {
		return proxyPath;
	}

	/**
	 * Sets the location of the VOMS user proxy certificate.
	 * 
	 * @param proxyPath location of the VOMS user proxy certificate
	 */
	public void setProxyPath(String proxyPath) {
		this.proxyPath = proxyPath;
		System.setProperty("X509_USER_PROXY", proxyPath);
	}
	
	/**
	 * Returns the location of WMProxy client configuration files 
	 * (only if specified via setWMSDir()).
	 * 
	 * @return location of WMProxy client configuration files
	 */
	public String getWMSDir() {
		return wmsDir;
	}

	/**
	 * Sets the location of WMProxy client configuration files 
	 * (the parent directory with <VO-name> subdirectories inside).
	 * 
	 * @param wmsDir location of WMProxy client configuration files
	 */
	public void setWMSDir(String wmsDir) {
		this.wmsDir = wmsDir;
		Map<String,String> wmProxiesFromDir = WMSDirParser.readWMProxies(wmsDir);
		wmProxies.putAll(wmProxiesFromDir);
	}
	
	/**
	 * Returns all known WMProxy service endpoints.
	 * 
	 * @return map <VO-name, WMProxy-endpoint>
	 */
	public Map<String,String> getWMProxies() {
		return wmProxies;
	}

	/**
	 * Adds the WMProxy service endpoint.
	 * 
	 * @param vo VO name
	 * @param wmProxyURL WMProxy service URI
	 */
	public void addWMProxy(String vo, String wmProxyURL) {
		wmProxies.put(vo, wmProxyURL);
	}

	/**
	 * Returns the delegation identifier for already delegated proxy.
	 * 
	 * @return delegation identifier
	 */
	public String getDelegationId() {
		return delegationId;
	}

	/**
	 * Sets the delegation identifier for already delegated proxy.
	 * 
	 * @param delegationId delegation identifier
	 */
	public void setDelegationId(String delegationId) {
		this.delegationId = delegationId;
	}
	
	/**
	 * Returns user credentials.
	 * 
	 * @return user credentials
	 */
	public GlobusCredential getUserCredentials() {
		return userCred;
	}

	/**
	 * Sets user credentials.
	 * This method provides an alternative to setUserCertPath/setUserKeyPath methods 
	 * for cases when credentials are already available as a GlobusCredential instance.
	 * 
	 * @param cred user credentials
	 */
	public void setUserCredentials(GlobusCredential cred) {
		this.userCred = cred;
		// unset default paths
		this.userCertPath = null;
		this.userKeyPath = null;
	}

	/**
	 * Returns the VOMS user proxy certificate.
	 * 
	 * @return VOMS user proxy certificate
	 */
	public GlobusCredential getProxy() {
		return proxy;
	}

	/**
	 * Sets the VOMS user proxy certificate.
	 * This method provides an alternative to setProxyPath method 
	 * for cases when proxy is already available as a GlobusCredential instance. 
	 * The proxy must have VOMS extensions.
	 * 
	 * @param proxy VOMS user proxy certificate
	 */
	public void setProxy(GlobusCredential proxy) {
		this.proxy = proxy;
		// unset default proxy path
		this.proxyPath = null;
	}
	
}