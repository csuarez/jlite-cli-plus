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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobsSelector {

	public static List<String> select(String jobsFile) throws IOException {
		List<String> fileJobIds = new ArrayList<String>();
		File inFile = new File(jobsFile);
		BufferedReader in = new BufferedReader(new FileReader(inFile));
		String jobLine;
		while ((jobLine = in.readLine()) != null) {
			if (jobLine.startsWith("https://")) {
				fileJobIds.add(jobLine);
			}
		}
		in.close();
		
		List<String> selectedJobIds = new ArrayList<String>();
		if (fileJobIds.size() > 1) {
			System.out.println("------------------------------------------------------------------");
			for (int i=0; i<fileJobIds.size(); i++) {
				System.out.println((i+1) + " : " + fileJobIds.get(i));
			}
			System.out.println("a : all");
			System.out.println("q : quit");
			System.out.println("------------------------------------------------------------------");
			while (selectedJobIds.size() == 0) {
				System.out.print("Choose one or more jobId(s) in the list - [1-" + fileJobIds.size() + "]all: ");
				BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
				String choice = stdin.readLine();
				if (choice.equals("a")) {
					selectedJobIds = fileJobIds;
				} else if (choice.equals("q")) {
					System.exit(0);
				} else {
					Pattern jobsSelectPattern = Pattern.compile("[0-9\\,\\-]+");
					Matcher jobsSelectMatcher = jobsSelectPattern.matcher(choice);
					if (jobsSelectMatcher.matches()) {
						String[] parts = choice.split("\\,");
						for (String part : parts) {
							if (part.length() > 0) {									
								if (part.indexOf("-") < 0) {
									int index = Integer.parseInt(part);
									if (index > 0 && index <= fileJobIds.size()) {
										selectedJobIds.add(fileJobIds.get(index-1));
									}
								} else {
									String[] parts2 = part.split("\\-");
									if (parts2.length == 2 
											&& !parts2[0].equals("") && !parts2[1].equals("")) {
										int from = Integer.parseInt(parts2[0]);
										int to = Integer.parseInt(parts2[1]);
										if (from > 0 && from < fileJobIds.size() 
												&& to > from && to <= fileJobIds.size()) {
											for (int i=from; i<=to; i++) {
												selectedJobIds.add(fileJobIds.get(i-1));
											}
										}
									}
								}
							}
						}
					}
				}
			}
			System.out.println("\n******************************************************************\n");
		} else {
			selectedJobIds = fileJobIds;
		}
		return selectedJobIds;
	}
	
}
