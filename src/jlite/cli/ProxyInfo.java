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

package jlite.cli;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import jlite.GridSession;
import jlite.GridSessionConfig;
import jlite.GridSessionFactory;
import jlite.util.Util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.glite.voms.VOMSAttribute;
import org.glite.voms.VOMSValidator;
import org.globus.gsi.CertUtil;
import org.globus.gsi.GlobusCredential;

public class ProxyInfo {

	private static final String COMMAND = "proxy-info [options]";
	
	public static void main(String[] args) {
		System.out.println(); // extra line
	    CommandLineParser parser = new GnuParser();
	    Options options = setupOptions();
	    HelpFormatter helpFormatter = new HelpFormatter();
	    helpFormatter.setSyntaxPrefix("Usage: ");
		try {
			CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
            	helpFormatter.printHelp(100, COMMAND, "\noptions:", options, "\n"+CLI.FOOTER+"\n", false);
            	System.out.println(); // extra line
                System.exit(0);
            }
            run(line);
		} catch (ParseException e) {
			System.err.println(e.getMessage() + "\n");
            helpFormatter.printHelp(100, COMMAND, "\noptions:", options, "\n"+CLI.FOOTER+"\n", false);
            System.out.println(); // extra line
            System.exit(-1);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		System.out.println(); // extra line
	}

	private static Options setupOptions() {
        Options options = new Options();
                
        options.addOption(OptionBuilder
        		.withDescription("displays usage")
                .create("help"));

        options.addOption(OptionBuilder
        		.withArgName("proxyfile")
                .withDescription("non-standard location of proxy")
                .hasArg()
                .create("file"));
        
        return options;
	}

	private static void run(CommandLine line) throws Exception {		
		GridSessionConfig conf = new GridSessionConfig();
		if (line.hasOption("file")) {
			conf.setProxyPath(line.getOptionValue("file"));
		}			
		GridSession grid = GridSessionFactory.create(conf);
		GlobusCredential proxy = grid.getProxy();
		
		if (proxy != null) {
			System.out.println("Subject: " + proxy.getSubject());
			System.out.println("Issuer: " + proxy.getIssuer());
			System.out.println("Identity: " + proxy.getIdentity());
			System.out.println("Type: " + CertUtil.getProxyTypeAsString(proxy.getProxyType()));			
			System.out.println("Strength: " + proxy.getStrength() + " bits");
			System.out.println("Path: " + new File(conf.getProxyPath()).getAbsolutePath());
			long expiryTime = System.currentTimeMillis() + proxy.getTimeLeft()*1000;
			SimpleDateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy z", Locale.ENGLISH);				
			System.out.println("Valid until: " + df.format(new Date(expiryTime)));
			System.out.println("Time left: " + Util.secondsToHHMMSS(proxy.getTimeLeft()));
			
			// VOMS Attributes
			Vector<VOMSAttribute> atts = VOMSValidator.parse(proxy.getCertificateChain());
			for (VOMSAttribute att : atts) {
				System.out.println("\n=== VO " + att.getVO() + " extension information ===");
				System.out.println("VO: " + att.getVO());
				System.out.println("Holder: " + att.getHolder());
				System.out.println("Issuer: " + att.getIssuer());
				for (Object fqan : att.getListOfFQAN()) {
					System.out.println("Attribute: " + fqan);
				}
				System.out.println("Valid until: " + df.format(att.getNotAfter()));
				System.out.println("Time left: " + Util.secondsToHHMMSS(
						(att.getNotAfter().getTime() - System.currentTimeMillis())/1000));				
			}
			
		} else {
			System.out.println("Proxy not found: " + conf.getProxyPath());
		}
	}

}
