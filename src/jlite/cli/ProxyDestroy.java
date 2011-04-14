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

public class ProxyDestroy {

	private static final String COMMAND = "proxy-destroy [options]";
	
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
            }
            run(line);
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
		grid.destroyProxy();
		System.out.println("Proxy is destroyed: " + new File(conf.getProxyPath()).getAbsolutePath());
	}

}
