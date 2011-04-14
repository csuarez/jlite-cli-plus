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
import java.io.IOException;
import java.io.InputStreamReader;

public class PasswordPrompt {

	public static String prompt(String name) throws IOException {
		MaskingThread mt = new MaskingThread();
		System.out.print(name);
		BufferedReader stdin = new BufferedReader(
				new InputStreamReader(System.in));
		mt.start();
		String pass = stdin.readLine();
		mt.stopMasking();
		return pass;
	}
	
	public static String promptNoMasking(String name) throws IOException {
		System.out.print(name);
		BufferedReader stdin = new BufferedReader(
				new InputStreamReader(System.in));
		String pass = stdin.readLine();
		return pass;
	}
}

class MaskingThread extends Thread {
	private volatile boolean running = true;

	public void run() {
		int priority = Thread.currentThread().getPriority();
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		try {
			while (running) {
				System.out.print("\010 ");
	           try {
	              Thread.sleep(1);
	           }catch (InterruptedException iex) {
	              Thread.currentThread().interrupt();
	              return;
	           }
			}
		} finally {
			System.out.print("\010");
			Thread.currentThread().setPriority(priority);
		}
	}

	public synchronized void stopMasking() {
		running = false;
	}
}