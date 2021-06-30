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

package org.openmrs.module.afyastat.fragment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.api.IdentifierNotUniqueException;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.api.service.InfoService;
import org.openmrs.module.afyastat.model.ErrorInfo;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppAction;
import org.openmrs.module.kenyaui.form.ValidatingCommandObject;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.BindParams;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.MethodParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.ui.framework.fragment.action.FailureResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Merge patients form fragment
 */
public class MergePatientsFragmentController {
	
	protected static final Log log = LogFactory.getLog(MergePatientsFragmentController.class);
	
	public void controller(@FragmentParam(value = "patient2", required = false) Patient patient2,
	        @FragmentParam(value = "queueUuid", required = false) String queueUuid,
	        @FragmentParam(value = "returnUrl") String returnUrl, FragmentModel model) {
		
		model.addAttribute("command", new MergePatientsForm(patient2, queueUuid));
		model.addAttribute("queueUuid", queueUuid);
		model.addAttribute("returnUrl", returnUrl);
	}
	
	/**
	 * Handles a merge action request
	 * 
	 * @param form the form
	 * @param ui
	 * @return
	 */
	@AppAction("kenyaemr.afyastat.home")
	public Object merge(@MethodParam("newMergePatientsForm") @BindParams MergePatientsForm form, UiUtils ui,
	        @SpringBean KenyaUiUtils kenyaUi, HttpSession session) {
		ui.validate(form, form, null);
		
		try {
			System.out.println("Patient 2: " + form.getPatient2().getPatientId());
			System.out.println("Record UUID: " + form.getQueueUuid());
			InfoService service = Context.getService(InfoService.class);
			service.mergeDuplicatePatient(form.getQueueUuid(), form.getPatient2().getUuid(),
			    service.getErrorDataByUuid(form.queueUuid).getPayload());
			
			kenyaUi.notifySuccess(session, "Patients merged successfully");
		}
		catch (IdentifierNotUniqueException ex) {
			return new FailureResult(ex.getMessage());
		}
		catch (Exception ex) {
			log.error("Unable to merge patients #" + form.getQueueUuid() + " and #" + form.getPatient2().getId(), ex);
			return new FailureResult("Unable to merge");
		}
		
		return SimpleObject.fromObject(form.getPatient2(), ui, "patientId");
	}
	
	/**
	 * Gets a summary of the specified patient
	 * 
	 * @param patient the patient
	 * @return the summary
	 */
	@AppAction("kenyaemr.afyastat.home")
	public SimpleObject patientSummary(@RequestParam("patientId") Patient patient, @SpringBean KenyaUiUtils kenyaUi) {
		
		List<SimpleObject> infopoints = new ArrayList<SimpleObject>();
		infopoints.add(dataPoint("Patient id", patient.getId()));
		infopoints.add(dataPoint("Gender", patient.getGender().toLowerCase().equals("f") ? "Female" : "Male"));
		infopoints.add(dataPoint("Birthdate", kenyaUi.formatDate(patient.getBirthdate())));
		infopoints.add(dataPoint("Death date", kenyaUi.formatDate(patient.getDeathDate())));
		infopoints.add(dataPoint("Created", kenyaUi.formatDate(patient.getDateCreated())));
		infopoints.add(dataPoint("Modified", kenyaUi.formatDate(patient.getDateChanged())));
		
		List<SimpleObject> names = new ArrayList<SimpleObject>();
		for (PersonName name : patient.getNames()) {
			names.add(dataPoint(null, name.getFullName()));
		}
		
		List<SimpleObject> identifiers = new ArrayList<SimpleObject>();
		for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
			identifiers.add(dataPoint(identifier.getIdentifierType().getName(), identifier.getIdentifier()));
		}
		
		List<SimpleObject> attributes = new ArrayList<SimpleObject>();
		for (PersonAttribute attribute : patient.getActiveAttributes()) {
			attributes.add(dataPoint(attribute.getAttributeType().getName(), attribute.getValue()));
		}
		
		List<SimpleObject> encounters = new ArrayList<SimpleObject>();
		for (Encounter encounter : Context.getEncounterService().getEncountersByPatient(patient)) {
			StringBuilder sb = new StringBuilder(encounter.getEncounterType().getName());
			if (encounter.getLocation() != null) {
				sb.append(" @ " + encounter.getLocation().getName());
			}
			encounters.add(dataPoint(kenyaUi.formatDate(encounter.getEncounterDatetime()), sb.toString()));
		}
		
		SimpleObject summary = new SimpleObject();
		summary.put("infopoints", infopoints);
		summary.put("names", names);
		summary.put("identifiers", identifiers);
		summary.put("attributes", attributes);
		summary.put("encounters", encounters);
		
		return summary;
	}
	
	/**
	 * Gets a summary of the specified error queue record
	 * 
	 * @param uuid the queue reference
	 * @return the summary
	 */
	@AppAction("kenyaemr.afyastat.home")
	public SimpleObject errorQueueRegistrationSummary(@RequestParam("errorQueueUuid") String uuid,
	        @SpringBean KenyaUiUtils kenyaUi) {
		
		List<SimpleObject> infopoints = new ArrayList<SimpleObject>();
		ErrorInfo qObj = Context.getService(InfoService.class).getErrorDataByUuid(uuid);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String clientName = "";
		JsonNode jsonNode = null;
		try {
			jsonNode = objectMapper.readTree(qObj.getPayload());
		}
		catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		ObjectNode patientObj = (ObjectNode) jsonNode.get("patient");
		clientName = patientObj.get("patient.given_name").asText();
		clientName = clientName + " " + patientObj.get("patient.family_name").asText();
		clientName = clientName + " " + patientObj.get("patient.middle_name").asText();
		
		infopoints.add(dataPoint("Queue id", qObj.getId()));
		infopoints.add(dataPoint("Gender", patientObj.get("patient.sex").asText().toLowerCase().equals("f") ? "Female"
		        : "Male"));
		infopoints.add(dataPoint("Birthdate", patientObj.get("patient.birth_date").asText()));
		infopoints.add(dataPoint("Death date", ""));
		infopoints.add(dataPoint("Created", kenyaUi.formatDate(qObj.getDateCreated())));
		infopoints.add(dataPoint("Modified", kenyaUi.formatDate(qObj.getDateChanged())));
		
		List<SimpleObject> names = new ArrayList<SimpleObject>();
		names.add(dataPoint(null, clientName));
		
		List<SimpleObject> identifiers = new ArrayList<SimpleObject>();
		ArrayNode identifierList = (ArrayNode) patientObj.get("patient.otheridentifier");
		if (identifierList.size() > 0) {
			for (int i = 0; i < identifierList.size(); i++) {
				identifiers.add(dataPoint(identifierList.get(i).get("identifier_type_name").asText(), identifierList.get(i)
				        .get("identifier_value").asText()));
			}
		}
		
		List<SimpleObject> attributes = new ArrayList<SimpleObject>();
		String phoneNumber = patientObj.get("patient.phone_number").asText();
		String alternatePhoneNumber = patientObj.get("patient.alternate_phone_contact").asText();
		String nextOfKinName = patientObj.get("patient.next_of_kin_name").asText();
		String nextOfKinRelationship = patientObj.get("patient.next_of_kin_relationship").asText();
		String nextOfKinContact = patientObj.get("patient.next_of_kin_contact").asText();
		String nextOfKinAddress = patientObj.get("patient.next_of_kin_address").asText();
		
		if (StringUtils.isNotBlank(phoneNumber)) {
			attributes.add(dataPoint("Telephone contact", phoneNumber));
		}
		
		if (StringUtils.isNotBlank(alternatePhoneNumber)) {
			attributes.add(dataPoint("Alternate Phone Number", alternatePhoneNumber));
		}
		
		if (StringUtils.isNotBlank(nextOfKinName)) {
			attributes.add(dataPoint("Next of kin name", nextOfKinName));
		}
		
		if (StringUtils.isNotBlank(nextOfKinRelationship)) {
			attributes.add(dataPoint("Next of kin relationship", nextOfKinRelationship));
		}
		
		if (StringUtils.isNotBlank(nextOfKinContact)) {
			attributes.add(dataPoint("Next of kin contact", nextOfKinContact));
		}
		
		if (StringUtils.isNotBlank(nextOfKinAddress)) {
			attributes.add(dataPoint("Next of kin address", nextOfKinAddress));
		}
		
		List<SimpleObject> encounters = new ArrayList<SimpleObject>();
		
		SimpleObject summary = new SimpleObject();
		summary.put("infopoints", infopoints);
		summary.put("names", names);
		summary.put("identifiers", identifiers);
		summary.put("attributes", attributes);
		summary.put("encounters", encounters);
		
		return summary;
	}
	
	/**
	 * Gets message payload
	 * 
	 * @param uuid the queue reference
	 * @return the summary
	 */
	@AppAction("kenyaemr.afyastat.home")
	public SimpleObject getMessagePayload(@RequestParam("queueUuid") String uuid, @SpringBean KenyaUiUtils kenyaUi,
	        UiUtils ui) {
		ErrorInfo qObj = Context.getService(InfoService.class).getErrorDataByUuid(uuid);
		
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = null;
		try {
			jsonNode = objectMapper.readTree(qObj.getPayload());
		}
		catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		SimpleObject summary = new SimpleObject();
		summary.put("payload", qObj.getPayload());
		return summary;
	}
	
	/**
	 * Convenience method to create a simple data point object
	 * 
	 * @param label the label
	 * @param value the value
	 * @return the simple object
	 */
	protected SimpleObject dataPoint(String label, Object value) {
		return SimpleObject.create("label", label, "value", value);
	}
	
	/**
	 * Creates a merge patients form
	 * 
	 * @return the form
	 */
	public MergePatientsForm newMergePatientsForm() {
		return new MergePatientsForm(null, null);
	}
	
	/**
	 * Form command object
	 */
	public class MergePatientsForm extends ValidatingCommandObject {
		
		private Patient patient2;
		
		private String queueUuid;
		
		public MergePatientsForm(Patient patient2, String queueUuid) {
			this.patient2 = patient2;
			this.queueUuid = queueUuid;
		}
		
		@Override
		public void validate(Object o, Errors errors) {
			require(errors, "queueUuid");
			require(errors, "patient2");
			
			/*if (patient1 != null && patient2 != null && patient1.equals(patient2)) {
				errors.reject("Patients must be different");
			}*/
		}
		
		/**
		 * Gets the second patient
		 * 
		 * @return the patient
		 */
		public Patient getPatient2() {
			return patient2;
		}
		
		/**
		 * Sets the second patient
		 * 
		 * @param patient2 the patient
		 */
		public void setPatient2(Patient patient2) {
			this.patient2 = patient2;
		}
		
		public String getQueueUuid() {
			return queueUuid;
		}
		
		public void setQueueUuid(String queueUuid) {
			this.queueUuid = queueUuid;
		}
	}
}
