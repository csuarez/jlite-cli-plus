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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jlite.GridSession;
import jlite.GridSessionConfig;
import jlite.GridSessionFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.glite.jdl.JobAd;
import org.glite.wsdl.types.lb.StatName;
import org.glite.wsdl.types.lb.StateEnterTimesItem;

public class JobStatus {

	private static final String COMMAND = "job-status [options] <jobId> ...";
	
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
            	run(line.getArgs(), line);
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
        		.withArgName("file_path")
                .withDescription("select JobId(s) from the specified file")
                .hasArg()
                .create("i"));

        options.addOption(OptionBuilder
                .withArgName("proxyfile")
                .withDescription("non-standard location of proxy cert")
                .hasArg()
                .create("proxypath"));

//        options.addOption(OptionBuilder
//        		.withArgName("level")
//                .withDescription("sets verbosity level of displayed information")
//                .hasArg()
//                .create("v"));
        
        return options;
	}

	private static void run(String[] jobIdArgs, CommandLine line) throws Exception {
		GridSessionConfig conf = new GridSessionConfig();

		if (line.hasOption("proxypath")) {
            conf.setProxyPath(line.getOptionValue("proxypath"));
        }
		
		GridSession grid = GridSessionFactory.create(conf);

		List<String> jobIds = new ArrayList<String>();;
		if (jobIdArgs.length > 0) {
			for (String jobId : jobIdArgs) {
				jobIds.add(jobId);
			}
		}
		
		if (line.hasOption("i")) {
			for (String jobId : JobsSelector.select(line.getOptionValue("i"))) {
				jobIds.add(jobId);
			}
		}
		
		if (jobIds.size() == 0){
			throw new Exception("JobId(s) not found");
		}
		
		for (String jobId : jobIds) {
		
			org.glite.wsdl.types.lb.JobStatus status = grid.getJobStatus(jobId);
	        
			System.out.println("Status info for the job\n" + jobId + "\n");
			System.out.print("Current status: " + status.getState().getValue());
			if (status.getState().equals(StatName.DONE)) {
				System.out.println(" (" + status.getDoneCode().getValue() + ")");
			} else {
				System.out.print("\n");
			}
			System.out.println("Status reason: " + status.getReason());
			
	        System.out.println("\nJob state history:");	        
	        StateEnterTimesItem[] states = status.getStateEnterTimes();
	        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
	        for (StateEnterTimesItem state : states) {
	        	if (state.getTime().getTimeInMillis() != 0) {
	        		System.out.println("\t" + df.format(state.getTime().getTime()) + "\t" + state.getState().getValue());
	        	}
	        }
	        
			System.out.println("\nDestination: " + status.getDestination());
	        System.out.println("CPU Time: " + status.getCpuTime());
	        System.out.println("Exit code: " + status.getExitCode());
	        
	        
	        org.glite.wsdl.types.lb.JobStatus[] children = status.getChildrenStates();
	        if (children != null && children.length > 0) {
	        	System.out.println("\nJob has " + children.length + " children **************************************** ");
	        	
		        for (org.glite.wsdl.types.lb.JobStatus child : children) {
		    		System.out.println("\nStatus info for the job\n" + child.getJobId() + "\n");
		    		JobAd jdl = new JobAd(child.getJdl());
		    		if (jdl.hasAttribute("NodeName")) {
		    			System.out.println("Node name: " + jdl.getString("NodeName"));
		    		}
		    		System.out.print("Current status: " + child.getState().getValue());
		    		if (child.getState().equals(StatName.DONE)) {
		    			System.out.println(" (" + child.getDoneCode().getValue() + ")");
		    		} else {
		    			System.out.print("\n");
		    		}
		    		System.out.println("Status reason: " + child.getReason());
		    		
		            System.out.println("\nJob state history:");	        
		            states = child.getStateEnterTimes();            
		            for (StateEnterTimesItem state : states) {
		            	if (state.getTime().getTimeInMillis() != 0) {
		            		System.out.println("\t" + df.format(state.getTime().getTime()) + "\t" + state.getState().getValue());
		            	}
		            }
		            
		    		System.out.println("\nDestination: " + child.getDestination());
		            System.out.println("CPU Time: " + child.getCpuTime());
		            System.out.println("Exit code: " + child.getExitCode());
		            
			        System.out.println("\n-----------------------------------------------------------");
		        }
		        
	        }
	        
	        if (jobIds.size() > 1) {
	        	System.out.println("\n******************************************************************\n");
	        }
		}

	}
	
}
