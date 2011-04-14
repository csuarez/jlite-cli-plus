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
 * Mother of all grid sessions.
 * 
 * @author Oleg Sukhoroslov
 */
public class GridSessionFactory {
	
	/**
	 * Creates a grid session with default configuration 
	 * (refer to the user manual). 
	 * 
	 * @return new grid session
	 */
	public static GridSession create() {
        return create(new GridSessionConfig());
	}

	/**
	 * Creates a grid session with custom configuration. 
	 *
	 * @param config
	 * @return new grid session
	 */
	public static GridSession create(GridSessionConfig config) {
        return new GridSessionImpl(config);
	}

}
