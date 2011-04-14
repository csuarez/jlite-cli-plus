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
import java.io.FileWriter;

import jlite.GridAPIException;
import jlite.GridSession;
import jlite.GridSessionConfig;
import jlite.GridSessionFactory;
import jlite.GridSessionImpl;
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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.glite.jdl.JobAd;

import condor.classad.Constant;
import condor.classad.Expr;
import condor.classad.Op;

public class JobSubmit {

	private static final String COMMAND = "job-submit [options] <jdl_file>";
	
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
                .withDescription("enables extra debug output")
                .create("debug"));

        options.addOption(OptionBuilder
        		.withArgName("dir_path")
                .withDescription("search input files in the specified directory (default is current directory)")
                .hasArg()
                .create("in"));
        
        options.addOption(OptionBuilder
        		.withArgName("ce_id")
                .withDescription("send job to specified computing element")
                .hasArg()
                .create("r"));
        
        options.addOption(OptionBuilder
        		.withArgName("file_path")
                .withDescription("write job id to specified file")
                .hasArg()
                .create("o"));
        
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
        
//        options.addOption(OptionBuilder
//        		.withArgName("protocol")
//                .withDescription("protocol to be used for file tranfer {gsiftp,https} (default is gsiftp)")
//                .hasArg()
//                .create("proto"));
        
        return options;
	}

	private static void run(String jdlFile, CommandLine line) throws Exception {
		if (line.hasOption("debug")) {
			Logger logger = Logger.getLogger(GridSessionImpl.class);
			logger.setLevel(Level.DEBUG);
		}
		
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
        if (line.hasOption("r")) {        	
        	Op expr = new Op(
        			Expr.EQUAL,
        			Constant.getInstance("other.GlueCEUniqueID"),
        			Constant.getInstance(line.getOptionValue("r")));
        	jad.delAttribute("Requirements");
        	jad.setAttribute("Requirements", expr);
        }
        
        String inputDir = line.getOptionValue("in", System.getProperty("user.dir"));        
        
        String jobId = grid.submitJob(wmProxyURL, jad.toString(), inputDir);
        
		System.out.println("The job has been successfully submitted to the WMProxy");
		System.out.println("Your job identifier is: \n\n\t" + jobId);
		
		if (line.hasOption("o")) {
			File outFile = new File(line.getOptionValue("o"));
			FileWriter out = new FileWriter(outFile, true);
			out.write(jobId + "\n");
			out.close();
			System.out.println("\nThe job identifier has been saved in the following file:\n" + 
					outFile.getAbsolutePath());
		}
	}
	
}
