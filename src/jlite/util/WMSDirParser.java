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

package jlite.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.glite.jdl.Ad;
import org.glite.jdl.JobAd;

import condor.classad.Constant;
import condor.classad.Expr;
import condor.classad.ListExpr;

public class WMSDirParser {

	public static Map<String,String> readWMProxies(String dir) {
		Map<String,String> wmProxies = new HashMap<String,String>();
		File parent = new File(dir);
		if (parent.exists() && parent.isDirectory()) {
			File[] children = parent.listFiles();
			BufferedReader reader = null;
			for (File child : children) {
				try {
					if (child.isDirectory()) {
						File[] files = child.listFiles();
						for (File file : files) {
							if (file.isFile() && (
									file.getName().equals("glite_wmsclient.conf") || 
									file.getName().equals("glite_wms.conf"))) {
								Ad ad = new JobAd();
								ad.fromFile(file.getAbsolutePath());
								Expr expr = ad.lookup("WMProxyEndPoints");
								if (expr == null) {
									ad = ad.getAd("WmsClient");
									expr = ad.lookup("WMProxyEndPoints");
								}
								if (expr instanceof Constant) { // single value
									wmProxies.put(child.getName(), ((Constant)expr).stringValue());
								} else if (expr instanceof ListExpr) { // list
									wmProxies.put(child.getName(),
											((ListExpr)expr).iterator().next().toString().replaceAll("\"", ""));
								}						
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
	                if (reader != null) {
	                    try {
	                        reader.close();
	                    } catch (IOException e) {
	                        e.printStackTrace();
	                    }
	                }
	            }
			}
		}
		return wmProxies;
	}
	
//	public static void main(String[] args) {
//		Map<String,String> wmProxies = WMSDirParser.readWMProxies("etc/wms");
//		for (Entry<String,String> entry : wmProxies.entrySet()) {
//			System.out.println(entry.getKey() + " : " + entry.getValue());
//		}
//	}
	
}
