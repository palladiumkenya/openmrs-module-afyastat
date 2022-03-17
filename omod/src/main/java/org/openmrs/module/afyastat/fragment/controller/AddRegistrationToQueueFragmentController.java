package org.openmrs.module.afyastat.fragment.controller;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

import org.openmrs.Person;
import org.openmrs.ui.framework.page.PageModel;

/**
 * Handler for afyastat registration queue search page
 */
public class AddRegistrationToQueueFragmentController {
	
	public void controller(PageModel model) {
		
		model.addAttribute("command", new AfyastatRegistrationQueueSearchForm());
	}
	
	public class AfyastatRegistrationQueueSearchForm {
		
		private Person person;
		
		public AfyastatRegistrationQueueSearchForm() {
			
		}
		
		public Person getPerson() {
			return person;
		}
		
		public void setPerson(Person person) {
			this.person = person;
		}
		
	}
	
}
