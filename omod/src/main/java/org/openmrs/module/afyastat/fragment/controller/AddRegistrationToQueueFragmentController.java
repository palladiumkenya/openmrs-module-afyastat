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
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.api.service.MedicOutgoingRegistrationService;
import org.openmrs.module.afyastat.model.MedicOutgoingRegistration;
import org.openmrs.module.afyastat.util.MedicDataExchange;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppAction;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

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
	
	/**
	 * Gets outgoing registration entry for a given patient
	 * 
	 * @param personId the queue reference
	 * @return the summary
	 */
	@AppAction("kenyaemr.afyastat.home")
	public SimpleObject getOutgoingEntryForPatient(@RequestParam("personId") Integer personId,
	        @SpringBean KenyaUiUtils kenyaUi, UiUtils ui) {
		MedicOutgoingRegistrationService service = Context.getService(MedicOutgoingRegistrationService.class);
		
		MedicOutgoingRegistration entry = service.getRecordByPatientId(personId);
		
		SimpleObject resp = new SimpleObject();
		
		if (entry != null) {
			Person person = Context.getPersonService().getPerson(entry.getPatientId());
			String fullName = person.getGivenName();
			
			if (person.getFamilyName() != null) {
				fullName += " " + person.getFamilyName();
			}
			
			if (person.getMiddleName() != null) {
				fullName += " " + person.getMiddleName();
			}
			
			resp.put("hasEntry", true);
			resp.put("patientName", fullName);
			resp.put("purpose", entry.getPurpose());
			resp.put("dateCreated", entry.getDateCreated());
			resp.put("status", entry.getStatus().equals(1) ? "Sent" : "Pending");
		} else {
			resp.put("hasEntry", false);
		}
		return resp;
	}
	
	/**
	 * Gets outgoing registration entry for a given patient
	 * 
	 * @param personId the queue reference
	 * @return the summary
	 */
	@AppAction("kenyaemr.afyastat.home")
	public SimpleObject addOutgoingEntryForPatient(@RequestParam("personId") Integer personId,
	        @RequestParam("purpose") String purpose, @SpringBean KenyaUiUtils kenyaUi, UiUtils ui) {
		MedicOutgoingRegistrationService service = Context.getService(MedicOutgoingRegistrationService.class);
		
		MedicDataExchange me = new MedicDataExchange();
		me.queueClientForOutgoingRegistration(personId, purpose);
		MedicOutgoingRegistration entry = service.getRecordByPatientId(personId);
		
		SimpleObject resp = new SimpleObject();
		
		if (entry != null) {
			Person person = Context.getPersonService().getPerson(entry.getPatientId());
			String fullName = person.getGivenName();
			
			if (person.getFamilyName() != null) {
				fullName += " " + person.getFamilyName();
			}
			
			if (person.getMiddleName() != null) {
				fullName += " " + person.getMiddleName();
			}
			
			resp.put("patientName", fullName);
			resp.put("purpose", entry.getPurpose());
			resp.put("dateCreated", entry.getDateCreated());
			resp.put("status", "Pending");
		} else {
			resp.put("hasEntry", false);
		}
		return resp;
	}
	
}
