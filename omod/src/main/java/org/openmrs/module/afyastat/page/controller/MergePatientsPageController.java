/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.afyastat.page.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.api.service.InfoService;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.ErrorInfo;
import org.openmrs.module.afyastat.model.ErrorMessagesInfo;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Merge patients page
 */
@AppPage("kenyaemr.afyastat.home")
public class MergePatientsPageController {
	
	public void controller(@RequestParam(value = "queueUuid", required = false) String queueUuid,
	        @RequestParam("returnUrl") String returnUrl, PageModel model) {
		
		ErrorInfo eObj = Context.getService(InfoService.class).getErrorDataByUuid(queueUuid);
		Patient emrPatient = null;
		
		for (ErrorMessagesInfo info : eObj.getErrorMessages()) {
			if (info.getMessage() != null
			        && (info.getMessage().contains("patientId =") || info.getMessage().contains("Identifier Id ="))) {
				String stringParts[] = info.getMessage().split(" ");
				if (stringParts.length > 0) {
					String patIdentifier = stringParts[stringParts.length - 1];
					List<Patient> patients = Context.getPatientService().getPatients(null, patIdentifier.trim(), null, true);
					if (patients.size() > 0) {
						emrPatient = patients.get(0); // just pick the first patient for now
					}
					
				}
			}
		}
		// Patient 2 is existing patient in the EMR
		
		model.addAttribute("patient1", null);
		model.addAttribute("queueUuid", queueUuid);
		model.addAttribute("patient2", emrPatient);
		model.addAttribute("returnUrl", returnUrl);
	}
}
