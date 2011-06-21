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

import jlite.GridAPIException;
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

public class ProxyDelegate {
	
	private static final String COMMAND = "proxy-delegate [options]";

	public static void main(String[] args) {
		System.out.println(); // extra line
	    CommandLineParser parser = new GnuParser();
	    Options options = setupOptions();
	    HelpFormatter helpFormatter = new HelpFormatter();
	    helpFormatter.setSyntaxPrefix("Usage: ");
	    CommandLine line = null;
		try {
			line = parser.parse(options, args);
            if (line.hasOption("help")) {                
            	helpFormatter.printHelp(100, COMMAND, "\noptions:", options, "\n"+CLI.FOOTER, false);
            	System.out.println(); // extra line
                System.exit(0);
            }
            if (line.hasOption("xml")) {
				System.out.println("<output>");
			}
            run(line);
		} catch (ParseException e) {
			System.err.println(e.getMessage() + "\n");
            helpFormatter.printHelp(100, COMMAND, "\noptions:", options, "\n"+CLI.FOOTER, false);
            System.out.println(); // extra line
            System.exit(-1);
		} catch (Exception e) {
			if ((line != null) && (line.hasOption("xml"))) {
				System.out.println("<error>" + e.getMessage() + "</error>");
			} else {
				System.err.println(e.getMessage());
			}
		} finally {
			if (line.hasOption("xml")) {
				System.out.println("</output>");
			}
		}
		System.out.println(); // extra line
	}

	private static Options setupOptions() {
        Options options = new Options();
                
        options.addOption(OptionBuilder
                .withDescription("displays usage")
                .create("help"));

        options.addOption(OptionBuilder
        		.withArgName("id_string")
                .withDescription("delegation id (default is user name)")
                .hasArg()
                .create("d"));

        options.addOption(OptionBuilder
        		.withArgName("service_URL")
                .withDescription("WMProxy service endpoint")
                .hasArg()
                .create("e"));

        options.addOption(OptionBuilder
                .withArgName("proxyfile")
                .withDescription("non-standard location of proxy cert")
                .hasArg()
                .create("proxypath"));
        
        options.addOption(OptionBuilder
        		.withArgName("xml")
                .withDescription("output as xml")
                .create("xml"));
          
        
        return options;
	}

	private static void run(CommandLine line) throws Exception {
		GridSessionConfig conf = new GridSessionConfig();

		if (line.hasOption("proxypath")) {
            conf.setProxyPath(line.getOptionValue("proxypath"));
        }
		
		GridSession grid = GridSessionFactory.create(conf);

		String vo = Util.readVOFromVOMSProxy(conf.getProxyPath());
		if (line.hasOption("xml")) {
			System.out.println("<vo>" + vo + "</vo>");
		} else {
			System.out.println("Working VO: " + vo);
		}
		String wmProxyURL = conf.getWMProxies().get(vo);
		if (line.hasOption("e")) {
			wmProxyURL = line.getOptionValue("e");
		}
		if (wmProxyURL == null) {
			throw new GridAPIException("Could not find WMProxy server for VO: " + vo);
		}
		if (line.hasOption("xml")) {
			System.out.println("<wmProxyURL>" + wmProxyURL + "</wmProxyURL>");
		} else {
			System.out.println("Connecting to WMProxy service: " + wmProxyURL);
		}

		String delegationId = line.getOptionValue("d", System.getProperty("user.name"));	
		grid.delegateProxy(wmProxyURL, delegationId);

		if (line.hasOption("xml")) {
			System.out.println("<delegationId>" + delegationId + "</delegationId>");
		} else {
			System.out.println("Your proxy has been successfully delegated to WMProxy\n" + 
				"Delegation identifier: " + delegationId);
		}
	}

}
