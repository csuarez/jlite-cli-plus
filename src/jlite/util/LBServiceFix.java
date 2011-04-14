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

import org.glite.wsdl.types.lb.JobStatus;
import org.glite.wsdl.types.lb.StatName;
import org.glite.wsdl.types.lb.StateEnterTimesItem;

/**
 * Client-side fix for https://savannah.cern.ch/bugs/?29165
 * Corrects status names in job history obtained via LB WS interface 
 * for LB versions < 1.7.1
 */
public class LBServiceFix {

	public static JobStatus fixJobStatus(JobStatus status) {
		StateEnterTimesItem[] states = status.getStateEnterTimes();
		StatName prevState = StatName.SUBMITTED;
		StatName temp = null;
		for (StateEnterTimesItem state : states) {
			temp = state.getState();
			state.setState(prevState);
			prevState = temp;
		}
		JobStatus[] children = status.getChildrenStates();
		if (children != null && children.length > 0) {
			JobStatus[] fixedChildren = new JobStatus[children.length];
			for (int i=0; i<children.length; i++) {
				fixedChildren[i] = fixJobStatus(children[i]);
			}
			status.setChildrenStates(fixedChildren);
		}
		return status;
	}
	
}
