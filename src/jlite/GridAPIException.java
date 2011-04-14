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

/**
 * A default exception for all errors thrown by jLite API.<br>
 * All underlying exceptions from Globus/gLite APIs 
 * are wrapped in GridAPIException.
 * 
 * @author Oleg Sukhoroslov
 */
public class GridAPIException extends Exception {

	public GridAPIException(String message) {
		super(message);
	}

	public GridAPIException(Throwable throwable) {
		super(throwable);
	}
}
