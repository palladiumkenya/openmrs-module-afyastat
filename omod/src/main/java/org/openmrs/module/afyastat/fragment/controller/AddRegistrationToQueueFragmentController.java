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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

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
	public List<SimpleObject> getOutgoingEntryForPatient(@RequestParam("personId") Integer personId,
	        @SpringBean KenyaUiUtils kenyaUi, UiUtils ui) {
		MedicOutgoingRegistrationService medicOutgoingRegistrationService = Context
		        .getService(MedicOutgoingRegistrationService.class);
		
		List<MedicOutgoingRegistration> outgoingQueueList = medicOutgoingRegistrationService.getRecordsByPatientId(personId);
		
		List<SimpleObject> queueList = new ArrayList<SimpleObject>();
		
		for (MedicOutgoingRegistration entry : outgoingQueueList) {
			SimpleObject resp = new SimpleObject();
			
			if (entry != null) {
				Person person = Context.getPersonService().getPerson(entry.getPatientId());
				
				String clientName = person.getGivenName();
				clientName = (clientName == null) ? "" : clientName;
				
				String familyName = person.getFamilyName();
				clientName += (familyName == null) ? "" : (" " + familyName);
				
				String middleName = person.getMiddleName();
				clientName += (middleName == null) ? "" : (" " + middleName);
				
				clientName = (clientName == null) ? "" : clientName;
				
				resp.put("hasEntry", true);
				resp.put("clientName", clientName);
				resp.put("purpose", entry.getPurpose());
				resp.put("dateCreated", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(entry.getDateCreated()));
				resp.put("status", entry.getStatus().equals(1) ? "Sent" : "Pending");
			} else {
				resp.put("hasEntry", false);
			}
			queueList.add(resp);
		}
		return (queueList);
	}
	
	/**
	 * Gets outgoing registration entry for a given patient
	 * 
	 * @param personId the queue reference
	 * @return the summary
	 */
	@AppAction("kenyaemr.afyastat.home")
	public List<SimpleObject> addOutgoingEntryForPatient(@RequestParam("personId") Integer personId,
	        @RequestParam("purpose") String purpose, @SpringBean KenyaUiUtils kenyaUi, UiUtils ui) {
		MedicOutgoingRegistrationService medicOutgoingRegistrationService = Context
		        .getService(MedicOutgoingRegistrationService.class);
		
		MedicDataExchange medicDataExchange = new MedicDataExchange();
		boolean saved = medicDataExchange.queueClientForOutgoingRegistration(personId, purpose);
		
		if (saved) {
			System.out.println("Afyastat outgoing queue. Successfully Queued a patient into outgoing queue");
		} else {
			System.err.println("Afyastat outgoing queue. Failed to Queue a patient into outgoing queue");
		}
		
		List<MedicOutgoingRegistration> outgoingQueueList = medicOutgoingRegistrationService.getRecordsByPatientId(personId);
		
		List<SimpleObject> queueList = new ArrayList<SimpleObject>();
		
		for (MedicOutgoingRegistration entry : outgoingQueueList) {
			SimpleObject resp = new SimpleObject();
			
			if (entry != null) {
				Person person = Context.getPersonService().getPerson(entry.getPatientId());
				
				String clientName = person.getGivenName();
				clientName = (clientName == null) ? "" : clientName;
				
				String familyName = person.getFamilyName();
				clientName += (familyName == null) ? "" : (" " + familyName);
				
				String middleName = person.getMiddleName();
				clientName += (middleName == null) ? "" : (" " + middleName);
				
				clientName = (clientName == null) ? "" : clientName;
				
				resp.put("hasEntry", true);
				resp.put("clientName", clientName);
				resp.put("purpose", entry.getPurpose());
				resp.put("dateCreated", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(entry.getDateCreated()));
				resp.put("status", (entry.getStatus() == 0) ? "Pending" : "Sent");
			} else {
				resp.put("hasEntry", false);
			}
			queueList.add(resp);
		}
		return (queueList);
	}
	
}
