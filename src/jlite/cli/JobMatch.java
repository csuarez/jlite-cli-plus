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

import java.util.List;

import jlite.GridAPIException;
import jlite.GridSession;
import jlite.GridSessionConfig;
import jlite.GridSessionFactory;
import jlite.MatchedCE;
import jlite.util.Util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.glite.jdl.JobAd;

public class JobMatch {
	
	private static final String COMMAND = "job-match [options] <jdl_file>";

	public static void main(String[] args) {
		System.out.println(); // extra line
	    CommandLineParser parser = new GnuParser();
	    Options options = setupOptions();
	    HelpFormatter helpFormatter = new HelpFormatter();
	    helpFormatter.setSyntaxPrefix("Usage: ");
		try {
			CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {                
            	helpFormatter.printHelp(100, COMMAND, "\noptions:", options, "\n"+CLI.FOOTER, false);
        		System.out.println(); // extra line
                System.exit(0);
            } else {
            	String[] remArgs = line.getArgs();
            	if (remArgs.length == 1) {
            		run(remArgs[0], line);
            	} else if (remArgs.length == 0) {
            		throw new MissingArgumentException("Missing required argument: <jdl_file>");
            	} else {
            		throw new UnrecognizedOptionException("Unrecognized extra arguments");
            	}
            }
		} catch (ParseException e) {
			System.err.println(e.getMessage() + "\n");
            helpFormatter.printHelp(100, COMMAND, "\noptions:", options, "\n"+CLI.FOOTER, false);
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
        		.withArgName("id_string")
                .withDescription("delegation id (default is user name)")
                .hasArg()
                .create("d"));
        
        options.addOption(OptionBuilder
                .withDescription("automatic proxy delegation")
                .create("a"));
        
        options.addOption(OptionBuilder
        		.withArgName("service_URL")
                .withDescription("WMProxy service endpoint")
                .hasArg()
                .create("e"));
        
        return options;
	}

	private static void run(String jdlFile, CommandLine line) throws Exception {
		GridSessionConfig conf = new GridSessionConfig();
		GridSession grid;        

		String vo = Util.readVOFromVOMSProxy(conf.getProxyPath());
		System.out.println("Working VO: " + vo);
		String wmProxyURL = conf.getWMProxies().get(vo);
		if (line.hasOption("e")) {
			wmProxyURL = line.getOptionValue("e");
		}
		if (wmProxyURL == null) {
			throw new GridAPIException("Could not find WMProxy server for VO: " + vo);
		}
		System.out.println("Connecting to WMProxy service: " + wmProxyURL + "\n");
	
		String delegationId;
		if (line.hasOption("a")) {
			grid = GridSessionFactory.create(conf);			
			delegationId = line.getOptionValue("d", System.getProperty("user.name"));	
			grid.delegateProxy(wmProxyURL, delegationId);			
			System.out.println("Your proxy has been successfully delegated to WMProxy\n" + 
					"Delegation identifier: " + delegationId + "\n");
		} else {
			delegationId = line.getOptionValue("d", System.getProperty("user.name"));
			conf.setDelegationId(delegationId);
			grid = GridSessionFactory.create(conf);
		}
		
		JobAd jad = new JobAd();
        jad.fromFile(jdlFile);
		
        List<MatchedCE> ces = grid.listMatchedCE(wmProxyURL, jad.toString());
        
        System.out.println("Found " + ces.size() + " CE(s) matching your job requirements: ");
        int longestCeId = 0;
        for (MatchedCE ce : ces) {
        	if (ce.getId().length() > longestCeId) {
        		longestCeId = ce.getId().length();
        	}
        }
        int rankPos = longestCeId + 10;
        for (MatchedCE ce : ces) {
        	System.out.print("\t" + ce.getId());
        	for (int i=0; i < rankPos - ce.getId().length(); i++) {
        		System.out.print(" ");
        	}
        	System.out.print(ce.getRank() + "\n");
        }
	}

}
