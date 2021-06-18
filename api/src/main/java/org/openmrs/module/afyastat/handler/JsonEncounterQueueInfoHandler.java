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
package org.openmrs.module.afyastat.handler;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.annotation.Handler;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.api.service.RegistrationInfoService;
import org.openmrs.module.afyastat.exception.StreamProcessorException;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.RegistrationInfo;
import org.openmrs.module.afyastat.model.handler.QueueInfoHandler;
import org.openmrs.module.afyastat.utils.JsonFormatUtils;
import org.openmrs.module.afyastat.utils.PatientLookUpUtils;

import org.openmrs.util.OpenmrsUtil;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Processes encounter payload data
 * Adapted from Adapted from openmrs-module-muzimacore.
 * See https://github.com/muzima/openmrs-module-muzimacore/blob/master/api/src/main/java/org/openmrs/module/muzima/handler/JsonEncounterQueueDataHandler.java
 */
@Component
@Handler(supports = AfyaStatQueueData.class, order = 5)
public class JsonEncounterQueueInfoHandler implements QueueInfoHandler {
	
	private static final String DISCRIMINATOR_VALUE = "json-encounter";
	
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final DateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
	
	private final Log log = LogFactory.getLog(JsonEncounterQueueInfoHandler.class);
	
	private static final String DEFAULT_ENCOUNTER_ROLE_UUID = "a0b03050-c99b-11e0-9572-0800200c9a66";
	
	private StreamProcessorException queueProcessorException;
	
	private Encounter encounter;
	
	/**
	 * @param queueData
	 * @return
	 */
	@Override
	public boolean validate(AfyaStatQueueData queueData) {
		
		try {
			queueProcessorException = new StreamProcessorException();
			log.info("Processing encounter form data: " + queueData.getUuid());
			encounter = new Encounter();
			String payload = queueData.getPayload();
			
			//Object encounterObject = JsonUtils.readAsObject(queueData.getPayload(), "$['encounter']");
			processEncounter(encounter, payload);
			
			//Object patientObject = JsonUtils.readAsObject(queueData.getPayload(), "$['patient']");
			processPatient(encounter, payload);
			
			Object obsObject = JsonFormatUtils.readAsObject(queueData.getPayload(), "$['observation']");
			processObs(encounter, null, obsObject);
			
			return true;
			
		}
		catch (Exception e) {
			queueProcessorException.addException(e);
			return false;
		}
		finally {
			if (queueProcessorException.anyExceptions()) {
				throw queueProcessorException;
			}
		}
	}
	
	@Override
	public String getDiscriminator() {
		return DISCRIMINATOR_VALUE;
	}
	
	@Override
	public void process(final AfyaStatQueueData queueData) throws StreamProcessorException {
		
		try {
			if (validate(queueData)) {
				assignToVisit(encounter);
				Context.getEncounterService().saveEncounter(encounter);
			}
		}
		catch (Exception e) {
			if (!e.getClass().equals(StreamProcessorException.class))
				queueProcessorException.addException(e);
		}
		finally {
			if (queueProcessorException.anyExceptions()) {
				throw queueProcessorException;
			}
		}
	}
	
	/**
	 * @param encounter
	 * @param patientObject
	 */
	private void processPatient(final Encounter encounter, final Object patientObject) {
		Patient unsavedPatient = new Patient();
		String patientPayload = patientObject.toString();
		
		String uuid = JsonFormatUtils.readAsString(patientPayload, "$['patient']['patient.uuid']");
		unsavedPatient.setUuid(uuid);
		
		Patient candidatePatient;
		if (StringUtils.isNotEmpty(unsavedPatient.getUuid())) {
			candidatePatient = Context.getPatientService().getPatientByUuid(unsavedPatient.getUuid());
			if (candidatePatient == null) {
				String temporaryUuid = unsavedPatient.getUuid();
				RegistrationInfoService dataService = Context.getService(RegistrationInfoService.class);
				RegistrationInfo registrationData = dataService.getRegistrationDataByTemporaryUuid(temporaryUuid);
				if (registrationData != null) {
					candidatePatient = Context.getPatientService().getPatientByUuid(registrationData.getAssignedUuid());
				} else {
					candidatePatient = Context.getPatientService().getPatientByUuid(uuid);
					
				}
			}
		}
		
		else {
			List<Patient> patients = Context.getPatientService().getPatients(unsavedPatient.getPersonName().getFullName());
			candidatePatient = PatientLookUpUtils.findSimilarPatientByNameAndGender(patients, unsavedPatient);
		}
		
		if (candidatePatient == null) {
			queueProcessorException.addException(new Exception(
			        "Unable to uniquely identify patient for this encounter form data. "));
		} else {
			encounter.setPatient(candidatePatient);
		}
	}
	
	/**
	 * @param encounter - Encounter
	 * @param parentObs - Obs
	 * @param obsObject - Object
	 */
	private void processObs(final Encounter encounter, final Obs parentObs, final Object obsObject) {
		if (obsObject instanceof JSONObject) {
			JSONObject obsJsonObject = (JSONObject) obsObject;
			for (String conceptQuestion : obsJsonObject.keySet()) {
				String[] conceptElements = StringUtils.split(conceptQuestion, "\\^");
				if (conceptElements.length < 3)
					continue;
				
				if (!StringUtils.isNumeric(conceptElements[0])) // skip if the element is not a concept id
					continue;
				
				int conceptId = Integer.parseInt(conceptElements[0]);
				Concept concept = Context.getConceptService().getConcept(conceptId);
				if (concept == null) {
					queueProcessorException.addException(new Exception("Unable to find Concept for Question with ID: "
					        + conceptId));
				} else {
					if (concept.isSet()) {
						Obs obsGroup = new Obs();
						obsGroup.setConcept(concept);
						Object childObsObject = obsJsonObject.get(conceptQuestion);
						processObsObject(encounter, obsGroup, childObsObject);
						if (parentObs != null) {
							parentObs.addGroupMember(obsGroup);
						}
					} else {
						Object valueObject = obsJsonObject.get(conceptQuestion);
						if (valueObject instanceof JSONArray) {
							JSONArray jsonArray = (JSONArray) valueObject;
							for (Object arrayElement : jsonArray) {
								createObs(encounter, parentObs, concept, arrayElement);
							}
						} else {
							createObs(encounter, parentObs, concept, valueObject);
						}
					}
				}
			}
		} else if (obsObject instanceof LinkedHashMap) {
			Object obsAsJsonObject = new JSONObject((Map<String, ?>) obsObject);
			processObs(encounter, parentObs, obsAsJsonObject);
		}
	}
	
	/**
	 * @param encounter - Encounter
	 * @param parentObs - Obs
	 * @param concept - Concept
	 * @param o - java.lang.Object
	 */
	private void createObs(final Encounter encounter, final Obs parentObs, final Concept concept, final Object o) {
		String value = null;
		Obs obs = new Obs();
		obs.setConcept(concept);
		
		//check and parse if obs_value / obs_datetime object
		if (o instanceof LinkedHashMap) {
			LinkedHashMap obj = (LinkedHashMap) o;
			if (obj.containsKey("obs_value")) {
				value = (String) obj.get("obs_value");
			}
			if (obj.containsKey("obs_datetime")) {
				String dateString = (String) obj.get("obs_datetime");
				Date obsDateTime = parseDate(dateString);
				obs.setObsDatetime(obsDateTime);
			}
		} else {
			value = o.toString();
		}
		
		// only process if value is not null/empty
		
		if (org.apache.commons.lang3.StringUtils.isNotBlank(value)) {
			// find the obs value :)
			if (concept.getDatatype().isNumeric()) {
				obs.setValueNumeric(Double.parseDouble(value));
			} else if (concept.getDatatype().isDate() || concept.getDatatype().isTime()
			        || concept.getDatatype().isDateTime()) {
				obs.setValueDatetime(parseDate(value));
			} else if (concept.getDatatype().isCoded() || concept.getDatatype().isBoolean()) {
				String[] valueCodedElements = StringUtils.split(value, "\\^");
				int valueCodedId = Integer.parseInt(valueCodedElements[0]);
				Concept valueCoded = Context.getConceptService().getConcept(valueCodedId);
				if (valueCoded == null) {
					queueProcessorException.addException(new Exception("Unable to find concept for value coded with id: "
					        + valueCodedId));
				} else {
					obs.setValueCoded(valueCoded);
				}
			} else if (concept.getDatatype().isText()) {
				obs.setValueText(value);
			}
			// only add if the value is not empty :)
			encounter.addObs(obs);
			if (parentObs != null) {
				parentObs.addGroupMember(obs);
			}
		}
	}
	
	/**
	 * @param encounter - Encounter
	 * @param parentObs Obs
	 * @param childObsObject - java.lang.Object
	 */
	private void processObsObject(final Encounter encounter, final Obs parentObs, final Object childObsObject) {
		//Object o = JsonUtils.readAsObject(childObsObject.toString(), "$");
		if (childObsObject instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) childObsObject;
			for (Object arrayElement : jsonArray) {
				Obs obsGroup = new Obs();
				obsGroup.setConcept(parentObs.getConcept());
				processObs(encounter, obsGroup, arrayElement);
				encounter.addObs(obsGroup);
			}
		} else if (childObsObject instanceof JSONObject) {
			processObs(encounter, parentObs, childObsObject);
			encounter.addObs(parentObs);
		} else if (childObsObject instanceof LinkedHashMap) {
			Object childObsAsJsonObject = new JSONObject((Map<String, ?>) childObsObject);
			processObs(encounter, parentObs, childObsAsJsonObject);
			encounter.addObs(parentObs);
		}
	}
	
	/**
	 * @param encounter - Encounter
	 * @param encounterObject - java.lang.Object
	 * @throws StreamProcessorException
	 */
	private void processEncounter(final Encounter encounter, final Object encounterObject) throws StreamProcessorException {
		String encounterPayload = encounterObject.toString();
		
		String formUuid = JsonFormatUtils.readAsString(encounterPayload, "$['encounter']['encounter.form_uuid']");
		Form form = Context.getFormService().getFormByUuid(formUuid);
		if (form == null) {
			log.info("Unable to find form using the uuid: " + formUuid + ". Setting the form field to null!");
			String encounterTypeString = JsonFormatUtils.readAsString(encounterPayload,
			    "$['encounter']['encounter.type_id']");
			int encounterTypeId = NumberUtils.toInt(encounterTypeString, -999);
			EncounterType encounterType = Context.getEncounterService().getEncounterType(encounterTypeId);
			if (encounterType == null) {
				queueProcessorException.addException(new Exception("Unable to find encounter type using the id: "
				        + encounterTypeString));
			} else {
				encounter.setEncounterType(encounterType);
			}
			
		} else {
			encounter.setForm(form);
			encounter.setEncounterType(form.getEncounterType());
		}
		//String encounterRoleString = JsonUtils.readAsString(encounterPayload, "$['encounter']['encounter.provider_role_uuid']");
		String encounterRoleString = null; // Not currently implemented in Afyastat
		EncounterRole encounterRole = null;
		
		if (StringUtils.isBlank(encounterRoleString)) {
			encounterRole = Context.getEncounterService().getEncounterRoleByUuid(DEFAULT_ENCOUNTER_ROLE_UUID);
		} else {
			encounterRole = Context.getEncounterService().getEncounterRoleByUuid(encounterRoleString);
		}
		
		if (encounterRole == null) {
			queueProcessorException.addException(new Exception("Unable to find encounter role using the uuid: ["
			        + encounterRoleString + "] or the default role [" + DEFAULT_ENCOUNTER_ROLE_UUID + "]"));
		}
		
		String providerString = JsonFormatUtils.readAsString(encounterPayload, "$['encounter']['encounter.provider_id']");
		Provider provider = Context.getProviderService().getProviderByIdentifier(providerString);
		if (provider == null) {
			queueProcessorException.addException(new Exception("Unable to find provider using the id: " + providerString));
		} else {
			encounter.setProvider(encounterRole, provider);
		}
		
		String userString = JsonFormatUtils.readAsString(encounterPayload, "$['encounter']['encounter.user_system_id']");
		User user = Context.getUserService().getUserByUsername(userString);
		
		if (user == null) {
			user = Context.getUserService().getUserByUsername(providerString);
		}
		if (user == null) {
			queueProcessorException.addException(new Exception("Unable to find user using the User Id: " + userString
			        + " or Provider Id: " + providerString));
		} else {
			encounter.setCreator(user);
		}
		
		String locationString = JsonFormatUtils.readAsString(encounterPayload, "$['encounter']['encounter.location_id']");
		int locationId = NumberUtils.toInt(locationString, -999);
		Location location = Context.getLocationService().getLocation(locationId);
		if (location == null) {
			queueProcessorException.addException(new Exception("Unable to find encounter location using the id: "
			        + locationString));
		} else {
			encounter.setLocation(location);
		}
		
		//String jsonPayloadTimezone = JsonUtils.readAsString(encounterPayload, "$['encounter']['encounter.device_time_zone']"); // still not used
		Date encounterDatetime = JsonFormatUtils.readAsDate(encounterPayload,
		    "$['encounter']['encounter.encounter_datetime']");
		encounter.setEncounterDatetime(encounterDatetime);
	}
	
	/**
	 * @param dateValue - String representation of the date value.
	 * @return java.util.Date Object
	 */
	private Date parseDate(final String dateValue) {
		Date date = null;
		try {
			date = dateFormat.parse(dateValue);
		}
		catch (ParseException e) {
			log.error("Unable to parse date data for encounter!", e);
		}
		return date;
	}
	
	@Override
	public boolean accept(final AfyaStatQueueData queueData) {
		return StringUtils.equals(DISCRIMINATOR_VALUE, queueData.getDiscriminator());
	}
	
	protected static void useNewVisit(Encounter encounter) {
		String VISIT_SOURCE_FORM = "8bfab185-6947-4958-b7ab-dfafae1a3e3d";
		Visit visit = new Visit();
		visit.setStartDatetime(OpenmrsUtil.firstSecondOfDay(encounter.getEncounterDatetime()));
		visit.setStopDatetime(getLastMomentOfDay(encounter.getEncounterDatetime()));
		visit.setLocation(encounter.getLocation());
		visit.setPatient(encounter.getPatient());
		visit.setVisitType(Context.getVisitService().getVisitTypeByUuid("3371a4d4-f66f-4454-a86d-92c7b3da990c"));
		
		VisitAttribute sourceAttr = new VisitAttribute();
		sourceAttr.setAttributeType(Context.getVisitService().getVisitAttributeTypeByUuid(VISIT_SOURCE_FORM));
		sourceAttr.setOwner(visit);
		sourceAttr.setValue(encounter.getForm());
		visit.addAttribute(sourceAttr);
		
		Context.getVisitService().saveVisit(visit);
		
		setVisitOfEncounter(visit, encounter);
	}
	
	protected static void setVisitOfEncounter(Visit visit, Encounter encounter) {
		// Remove from old visit
		if (encounter.getVisit() != null) {
			encounter.getVisit().getEncounters().remove(encounter);
		}
		
		// Set to new visit
		encounter.setVisit(visit);
		
		if (visit != null) {
			visit.addEncounter(encounter);
		}
	}
	
	/**
	 * Checks whether a date has any time value
	 * 
	 * @param date the date
	 * @return true if the date has time
	 * @should return true only if date has time
	 */
	protected boolean dateHasTime(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.HOUR) != 0 || cal.get(Calendar.MINUTE) != 0 || cal.get(Calendar.SECOND) != 0
		        || cal.get(Calendar.MILLISECOND) != 0;
	}
	
	/**
	 * Uses an existing visit for the given encounter
	 * 
	 * @param encounter the encounter
	 * @return true if a suitable visit was found
	 */
	protected boolean useExistingVisit(Encounter encounter) {
		// If encounter has time, then we need an exact fit for an existing visit
		List<Visit> visits = Context.getVisitService().getVisits(null, Collections.singletonList(encounter.getPatient()),
		    null, null, null, encounter.getEncounterDatetime(), null, null, null, true, false);
		
		for (Visit visit : visits) {
			// Skip visits which ended before the encounter dategit
			if (visit.getStopDatetime() != null && visit.getStopDatetime().before(encounter.getEncounterDatetime())) {
				continue;
			}
			
			if (checkLocations(visit, encounter)) {
				setVisitOfEncounter(visit, encounter);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Convenience method to check whether the location of a visit and an encounter are compatible
	 * 
	 * @param visit the visit
	 * @param encounter the encounter
	 * @return true if locations won't conflict
	 */
	protected boolean checkLocations(Visit visit, Encounter encounter) {
		return visit.getLocation() == null || Location.isInHierarchy(encounter.getLocation(), visit.getLocation());
	}
	
	/**
	 * Does the actual assignment of the encounter to a visit
	 * 
	 * @param encounter the encounter
	 */
	protected void assignToVisit(Encounter encounter) {
		// Do nothing if the encounter already belongs to a visit and can't be moved
		if (encounter.getVisit() != null) {
			return;
		}
		
		// Try using an existing visit
		if (!useExistingVisit(encounter)) {
			useNewVisit(encounter);
			
		}
	}
	
	/**
	 * Adapted from openmrs core. the method return start of next day Gets the date having the last
	 * millisecond of a given day. Meaning that the hours, seconds, and milliseconds are the latest
	 * possible for that day.
	 * 
	 * @param day the day.
	 * @return the date with the last millisecond of the day.
	 */
	public static Date getLastMomentOfDay(Date day) {
		Calendar calender = Calendar.getInstance();
		calender.setTime(day);
		calender.set(Calendar.HOUR_OF_DAY, 23);
		calender.set(Calendar.MINUTE, 59);
		calender.set(Calendar.SECOND, 59);
		//calender.set(Calendar.MILLISECOND, 999);
		
		return calender.getTime();
	}
}
