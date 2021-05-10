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

import com.jayway.jsonpath.InvalidPathException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.openmrs.*;
import org.openmrs.annotation.Handler;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.api.service.RegistrationInfoService;
import org.openmrs.module.afyastat.exception.StreamProcessorException;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.RegistrationInfo;
import org.openmrs.module.afyastat.model.handler.QueueInfoHandler;
import org.openmrs.module.afyastat.utils.JsonFormatUtils;
import org.openmrs.module.afyastat.utils.PatientLookUpUtils;

import org.springframework.stereotype.Component;

import java.util.*;

import static org.openmrs.module.afyastat.utils.JsonFormatUtils.getElementFromJsonObject;
import static org.openmrs.module.afyastat.utils.PersonCreationsUtils.getPersonAddressFromJsonObject;
import static org.openmrs.module.afyastat.utils.PersonCreationsUtils.getPersonAttributeFromJsonObject;

/**
 */
@Component
@Handler(supports = AfyaStatQueueData.class, order = 6)
public class DemographicsUpdateQueueInfoHandler implements QueueInfoHandler {
	
	private static final String DISCRIMINATOR_VALUE = "json-demographics-update";
	
	private final Log log = LogFactory.getLog(DemographicsUpdateQueueInfoHandler.class);
	
	private Patient unsavedPatient;
	
	private Patient savedPatient;
	
	private String payload;
	
	private StreamProcessorException queueProcessorException;
	
	@Override
	public void process(final AfyaStatQueueData queueData) throws StreamProcessorException {
		log.info("Processing demographics update form data: " + queueData.getUuid());
		try {
			if (validate(queueData)) {
				updatePatientDemographicObs();
				updateSavedPatientDemographics();
				Context.getPatientService().savePatient(savedPatient);
				
				String temporaryUuid = getTemporaryPatientUuidFromPayload();
				if (StringUtils.isNotEmpty(temporaryUuid)) {
					saveRegistrationData(temporaryUuid);
				}
			}
		}
		catch (Exception e) {
			if (!e.getClass().equals(StreamProcessorException.class)) {
				queueProcessorException.addException(e);
			}
		}
		finally {
			if (queueProcessorException.anyExceptions()) {
				throw queueProcessorException;
			}
		}
	}
	
	private String getTemporaryPatientUuidFromPayload() {
		return JsonFormatUtils.readAsString(payload, "$['demographicsupdate']['demographicsupdate.temporal_patient_uuid']");
	}
	
	private void saveRegistrationData(String temporaryUuid) {
		RegistrationInfoService registrationDataService = Context.getService(RegistrationInfoService.class);
		RegistrationInfo registrationData = registrationDataService.getRegistrationDataByTemporaryUuid(temporaryUuid);
		if (registrationData == null) {
			registrationData = new RegistrationInfo();
			registrationData.setTemporaryUuid(temporaryUuid);
			String assignedUuid = savedPatient.getUuid();
			registrationData.setAssignedUuid(assignedUuid);
			registrationDataService.saveRegistrationData(registrationData);
		}
	}
	
	private void updateSavedPatientDemographics() {
		if (unsavedPatient.getIdentifiers() != null) {
			for (final PatientIdentifier identifier : unsavedPatient.getIdentifiers()) {
				boolean identifierExists = false;
				for (PatientIdentifier savedPatientIdentifier : savedPatient.getIdentifiers()) {
					if (savedPatientIdentifier.getIdentifierType().equals(identifier.getIdentifierType())
					        && savedPatientIdentifier.getIdentifier().equalsIgnoreCase(identifier.getIdentifier())) {
						identifierExists = true;
						break;
					}
				}
				if (!identifierExists) {
					savedPatient.addIdentifier(identifier);
				}
			}
		}
		if (unsavedPatient.getPersonName() != null) {
			savedPatient.getPersonName().setFamilyName(unsavedPatient.getPersonName().getFamilyName());
			savedPatient.getPersonName().setGivenName(unsavedPatient.getPersonName().getGivenName());
			savedPatient.getPersonName().setMiddleName(unsavedPatient.getPersonName().getMiddleName());
			// savedPatient.addName(unsavedPatient.getPersonName().);
		}
		if (StringUtils.isNotBlank(unsavedPatient.getGender())) {
			savedPatient.setGender(unsavedPatient.getGender());
		}
		if (unsavedPatient.getBirthdate() != null) {
			savedPatient.setBirthdate(unsavedPatient.getBirthdate());
			savedPatient.setBirthdateEstimated(unsavedPatient.getBirthdateEstimated());
		}
		if (unsavedPatient.getPersonAddress() != null) {
			savedPatient.getPersonAddress().setStateProvince(unsavedPatient.getPersonAddress().getStateProvince());
			savedPatient.getPersonAddress().setCountyDistrict(unsavedPatient.getPersonAddress().getCountyDistrict());
			savedPatient.getPersonAddress().setAddress4(unsavedPatient.getPersonAddress().getAddress4());
			savedPatient.getPersonAddress().setAddress2(unsavedPatient.getPersonAddress().getAddress2());
			savedPatient.getPersonAddress().setAddress1(unsavedPatient.getPersonAddress().getAddress1());
			savedPatient.getPersonAddress().setAddress6(unsavedPatient.getPersonAddress().getAddress6());
			savedPatient.getPersonAddress().setAddress5(unsavedPatient.getPersonAddress().getAddress5());
			savedPatient.getPersonAddress().setCityVillage(unsavedPatient.getPersonAddress().getCityVillage());
			
			//savedPatient.addAddress(unsavedPatient.getPersonAddress());
		}
		if (unsavedPatient.getAttributes() != null) {
			Set<PersonAttribute> attributes = unsavedPatient.getAttributes();
			Iterator<PersonAttribute> iterator = attributes.iterator();
			while (iterator.hasNext()) {
				savedPatient.addAttribute(iterator.next());
			}
		}
		if (unsavedPatient.getChangedBy() != null) {
			savedPatient.setChangedBy(unsavedPatient.getChangedBy());
		}
		
	}
	
	private void updatePatientDemographicObs() {
		Patient p = Context.getPatientService().getPatientByUuid(savedPatient.getUuid());
		ConceptService cs = Context.getConceptService();
		String occupation = JsonFormatUtils.readAsString(payload, "$['observation']['1542^OCCUPATION^99DCT']");
		String civilStatus = JsonFormatUtils.readAsString(payload, "$['observation']['1054^CIVIL STATUS^99DCT']");
		String educationLevel = JsonFormatUtils.readAsString(payload,
		    "$['observation']['1712^HIGHEST EDUCATION LEVEL^99DCT']");
		Integer occupationConAns = handleEditObsValues(occupation.replace("^", "_"));
		Integer civilStatusConAns = handleEditObsValues(civilStatus.replace("^", "_"));
		Integer educationLevelConAns = handleEditObsValues(educationLevel.replace("^", "_"));
		
		if (occupationConAns != null) {
			Obs occupationObs = new Obs();
			occupationObs.setPerson(p);
			occupationObs.setObsDatetime(new Date());
			occupationObs.setConcept(cs.getConcept(1542)); // occupation concept
			occupationObs.setValueCoded(cs.getConcept(occupationConAns));
			Context.getObsService().saveObs(occupationObs, null);
		}
		
		if (civilStatusConAns != null) {
			Obs civilStatusObs = new Obs();
			civilStatusObs.setPerson(p);
			civilStatusObs.setObsDatetime(new Date());
			civilStatusObs.setConcept(cs.getConcept(1054)); // civil status concept
			civilStatusObs.setValueCoded(cs.getConcept(civilStatusConAns));
			Context.getObsService().saveObs(civilStatusObs, null);
		}
		
		if (educationLevelConAns != null) {
			Obs eduLevelObs = new Obs();
			eduLevelObs.setPerson(p);
			eduLevelObs.setObsDatetime(new Date());
			eduLevelObs.setConcept(cs.getConcept(1712)); // education level concept
			eduLevelObs.setValueCoded(cs.getConcept(educationLevelConAns));
			Context.getObsService().saveObs(eduLevelObs, null);
		}
		
	}
	
	@Override
	public boolean validate(AfyaStatQueueData queueData) {
		log.info("Processing demographics Update form data: " + queueData.getUuid());
		queueProcessorException = new StreamProcessorException();
		try {
			payload = queueData.getPayload();
			Patient candidatePatient = getCandidatePatientFromPayload();
			savedPatient = PatientLookUpUtils.findSavedPatient(candidatePatient, true);
			if (savedPatient == null) {
				queueProcessorException.addException(new Exception("Unable to uniquely identify patient for this "
				        + "demographic update form data. "));
			} else {
				unsavedPatient = new Patient();
				populateUnsavedPatientDemographicsFromPayload();
			}
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
	
	private Patient getCandidatePatientFromPayload() {
		Patient candidatePatient = new Patient();
		
		String uuid = getCandidatePatientUuidFromPayload();
		candidatePatient.setUuid(uuid);
		
		PatientIdentifier medicalRecordNumber = getMedicalRecordNumberFromPayload();
		if (medicalRecordNumber != null) {
			medicalRecordNumber.setPreferred(true);
			candidatePatient.addIdentifier(medicalRecordNumber);
		}
		
		PersonName personName = getCandidatePatientPersonNameFromPayload();
		candidatePatient.addName(personName);
		
		String gender = getCandidatePatientGenderFromPayload();
		candidatePatient.setGender(gender);
		
		Date birthDate = getCandidatePatientBirthDateFromPayload();
		candidatePatient.setBirthdate(birthDate);
		
		return candidatePatient;
	}
	
	private String getCandidatePatientUuidFromPayload() {
		return JsonFormatUtils.readAsString(payload, "$['patient']['patient.uuid']");
	}
	
	private PatientIdentifier getMedicalRecordNumberFromPayload() {
		JSONObject medicalRecordNumberObject = (JSONObject) JsonFormatUtils.readAsObject(payload,
		    "$['patient']['patient.medical_record_number']");
		return createPatientIdentifier(medicalRecordNumberObject);
	}
	
	private PersonName getCandidatePatientPersonNameFromPayload() {
		PersonName personName = new PersonName();
		String givenName = JsonFormatUtils.readAsString(payload, "$['patient']['patient.given_name']");
		if (StringUtils.isNotBlank(givenName)) {
			personName.setGivenName(givenName);
		}
		String familyName = JsonFormatUtils.readAsString(payload, "$['patient']['patient.family_name']");
		if (StringUtils.isNotBlank(familyName)) {
			personName.setFamilyName(familyName);
		}
		
		String middleName = JsonFormatUtils.readAsString(payload, "$['patient']['patient.middle_name']");
		if (StringUtils.isNotBlank(middleName)) {
			personName.setMiddleName(middleName);
		}
		
		return personName;
	}
	
	private String getCandidatePatientGenderFromPayload() {
		return JsonFormatUtils.readAsString(payload, "$['patient']['patient.sex']");
	}
	
	private Date getCandidatePatientBirthDateFromPayload() {
		return JsonFormatUtils.readAsDate(payload, "$['patient']['patient.birth_date']");
	}
	
	private void populateUnsavedPatientDemographicsFromPayload() {
		setUnsavedPatientIdentifiersFromPayload();
		setUnsavedPatientBirthDateFromPayload();
		setUnsavedPatientBirthDateEstimatedFromPayload();
		setUnsavedPatientGenderFromPayload();
		setUnsavedPatientNameFromPayload();
		setUnsavedPatientAddressesFromPayload();
		setUnsavedPatientPersonAttributesFromPayload();
		setUnsavedPatientChangedByFromPayload();
	}
	
	private void setUnsavedPatientIdentifiersFromPayload() {
		List<PatientIdentifier> demographicsUpdateIdentifiers = getDemographicsUpdatePatientIdentifiersFromPayload();
		if (!demographicsUpdateIdentifiers.isEmpty()) {
			Set<PatientIdentifier> patientIdentifiers = new HashSet<PatientIdentifier>();
			patientIdentifiers.addAll(demographicsUpdateIdentifiers);
			setIdentifierTypeLocation(patientIdentifiers);
			unsavedPatient.addIdentifiers(patientIdentifiers);
		}
	}
	
	private List<PatientIdentifier> getDemographicsUpdatePatientIdentifiersFromPayload() {
		List<PatientIdentifier> identifiers = new ArrayList<PatientIdentifier>();
		PatientIdentifier demographicsUpdateMedicalRecordNumberIdentifier = getDemographicsUpdateMedicalRecordNumberIdentifierFromPayload();
		if (demographicsUpdateMedicalRecordNumberIdentifier != null) {
			identifiers.add(demographicsUpdateMedicalRecordNumberIdentifier);
		}
		
		identifiers.addAll(getOtherDemographicsUpdatePatientIdentifiersFromPayload());
		identifiers.addAll(getLegacyOtherDemographicsUpdatePatientIdentifiersFromPayload());
		return identifiers;
	}
	
	private PatientIdentifier getDemographicsUpdateMedicalRecordNumberIdentifierFromPayload() {
		PatientIdentifier medicalRecordNumber = null;
		Object medicalRecordNumberObject = JsonFormatUtils.readAsObject(payload,
		    "$['demographicsupdate']['demographicsupdate.medical_record_number']");
		if (medicalRecordNumberObject instanceof JSONObject) {
			medicalRecordNumber = createPatientIdentifier((JSONObject) medicalRecordNumberObject);
		} else if (medicalRecordNumberObject instanceof String) {
			
			//process as legacy demographics update medical record number
			String medicalRecordNumberValueString = (String) medicalRecordNumberObject;
			if (StringUtils.isNotEmpty(medicalRecordNumberValueString)) {
				String identifierTypeName = "AMRS Universal ID";
				PatientIdentifier preferredPatientIdentifier = createPatientIdentifier(identifierTypeName,
				    medicalRecordNumberValueString);
				if (preferredPatientIdentifier != null) {
					preferredPatientIdentifier.setPreferred(true);
					medicalRecordNumber = preferredPatientIdentifier;
				}
			}
		}
		return medicalRecordNumber;
	}
	
	private List<PatientIdentifier> getOtherDemographicsUpdatePatientIdentifiersFromPayload() {
		List<PatientIdentifier> otherIdentifiers = new ArrayList<PatientIdentifier>();
		try {
			Object otheridentifierObject = JsonFormatUtils.readAsObject(payload,
			    "$['demographicsupdate']['demographicsupdate.otheridentifier']");
			if (JsonFormatUtils.isJSONArrayObject(otheridentifierObject)) {
				for (Object otherIdentifier : (JSONArray) otheridentifierObject) {
					PatientIdentifier identifier = createPatientIdentifier((JSONObject) otherIdentifier);
					if (identifier != null) {
						otherIdentifiers.add(identifier);
					}
				}
			} else {
				PatientIdentifier identifier = createPatientIdentifier((JSONObject) otheridentifierObject);
				if (identifier != null) {
					otherIdentifiers.add(identifier);
				}
			}
			
			JSONObject patientObject = (JSONObject) JsonFormatUtils.readAsObject(payload, "$['demographicsupdate']");
			Set keys = patientObject.keySet();
			for (Object key : keys) {
				if (((String) key).startsWith("demographicsupdate.otheridentifier^")) {
					PatientIdentifier identifier = createPatientIdentifier((JSONObject) patientObject.get(key));
					if (identifier != null) {
						otherIdentifiers.add(identifier);
					}
				}
			}
		}
		catch (InvalidPathException e) {
			log.error("Error while parsing other identifiers ", e);
		}
		return otherIdentifiers;
	}
	
	private List<PatientIdentifier> getLegacyOtherDemographicsUpdatePatientIdentifiersFromPayload() {
		List<PatientIdentifier> legacyIdentifiers = new ArrayList<PatientIdentifier>();
		Object identifierTypeNameObject = JsonFormatUtils.readAsObject(payload,
		    "$['demographicsupdate']['demographicsupdate.other_identifier_type']");
		Object identifierValueObject = JsonFormatUtils.readAsObject(payload,
		    "$['demographicsupdate']['demographicsupdate.other_identifier_value']");
		
		if (identifierTypeNameObject instanceof JSONArray) {
			JSONArray identifierTypeName = (JSONArray) identifierTypeNameObject;
			JSONArray identifierValue = (JSONArray) identifierValueObject;
			for (int i = 0; i < identifierTypeName.size(); i++) {
				PatientIdentifier identifier = createPatientIdentifier(identifierTypeName.get(i).toString(), identifierValue
				        .get(i).toString());
				if (identifier != null) {
					legacyIdentifiers.add(identifier);
				}
			}
		} else if (identifierTypeNameObject instanceof String) {
			String identifierTypeName = (String) identifierTypeNameObject;
			String identifierValue = (String) identifierValueObject;
			PatientIdentifier identifier = createPatientIdentifier(identifierTypeName, identifierValue);
			if (identifier != null) {
				legacyIdentifiers.add(identifier);
			}
		}
		
		return legacyIdentifiers;
	}
	
	private PatientIdentifier createPatientIdentifier(JSONObject identifierObject) {
		if (identifierObject == null) {
			return null;
		}
		
		String identifierTypeName = (String) getElementFromJsonObject(identifierObject, "identifier_type_name");
		String identifierUuid = (String) getElementFromJsonObject(identifierObject, "identifier_type_uuid");
		String identifierValue = (String) getElementFromJsonObject(identifierObject, "identifier_value");
		
		return createPatientIdentifier(identifierUuid, identifierTypeName, identifierValue);
	}
	
	private PatientIdentifier createPatientIdentifier(String identifierTypeName, String identifierValue) {
		return createPatientIdentifier(null, identifierTypeName, identifierValue);
	}
	
	private PatientIdentifier createPatientIdentifier(String identifierTypeUuid, String identifierTypeName,
	        String identifierValue) {
		if (StringUtils.isBlank(identifierTypeUuid) && StringUtils.isBlank(identifierTypeName)) {
			queueProcessorException.addException(new Exception(
			        "Cannot create identifier. Identifier type name or uuid must be supplied"));
		}
		
		if (StringUtils.isBlank(identifierValue)) {
			queueProcessorException.addException(new Exception(
			        "Cannot create identifier. Supplied identifier value is blank for identifier type name:'"
			                + identifierTypeName + "', uuid:'" + identifierTypeUuid + "'"));
		}
		
		PatientIdentifierType identifierType = Context.getPatientService()
		        .getPatientIdentifierTypeByUuid(identifierTypeUuid);
		if (identifierType == null) {
			identifierType = Context.getPatientService().getPatientIdentifierTypeByName(identifierTypeName);
		}
		if (identifierType == null) {
			queueProcessorException.addException(new Exception("Unable to find identifier type with name:'"
			        + identifierTypeName + "', uuid:'" + identifierTypeUuid + "'"));
		} else {
			PatientIdentifier patientIdentifier = new PatientIdentifier();
			patientIdentifier.setIdentifierType(identifierType);
			patientIdentifier.setIdentifier(identifierValue);
			return patientIdentifier;
		}
		return null;
	}
	
	private void setIdentifierTypeLocation(final Set<PatientIdentifier> patientIdentifiers) {
		String locationIdString = JsonFormatUtils.readAsString(payload, "$['encounter']['encounter.location_id']");
		Location location = null;
		int locationId;
		
		if (locationIdString != null) {
			locationId = Integer.parseInt(locationIdString);
			location = Context.getLocationService().getLocation(locationId);
		}
		
		if (location == null) {
			queueProcessorException.addException(new Exception("Unable to find encounter location using the id: "
			        + locationIdString));
		} else {
			Iterator<PatientIdentifier> iterator = patientIdentifiers.iterator();
			while (iterator.hasNext()) {
				PatientIdentifier identifier = iterator.next();
				identifier.setLocation(location);
			}
		}
	}
	
	private void setUnsavedPatientBirthDateFromPayload() {
		Date birthDate = JsonFormatUtils.readAsDate(payload, "$['demographicsupdate']['demographicsupdate.birth_date']");
		if (birthDate != null) {
			unsavedPatient.setBirthdate(birthDate);
			
		}
	}
	
	private void setUnsavedPatientBirthDateEstimatedFromPayload() {
		boolean birthdateEstimated = JsonFormatUtils.readAsBoolean(payload,
		    "$['demographicsupdate']['demographicsupdate.birthdate_estimated']");
		unsavedPatient.setBirthdateEstimated(birthdateEstimated);
	}
	
	private void setUnsavedPatientGenderFromPayload() {
		String gender = JsonFormatUtils.readAsString(payload, "$['demographicsupdate']['demographicsupdate.sex']");
		if (StringUtils.isNotBlank(gender)) {
			unsavedPatient.setGender(gender);
		}
	}
	
	private void setUnsavedPatientNameFromPayload() {
		
		PersonName personName = new PersonName();
		String givenName = JsonFormatUtils.readAsString(payload, "$['demographicsupdate']['demographicsupdate.given_name']");
		if (StringUtils.isNotBlank(givenName)) {
			personName.setGivenName(givenName);
		}
		
		String familyName = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.family_name']");
		if (StringUtils.isNotBlank(familyName)) {
			personName.setFamilyName(familyName);
		}
		
		String middleName = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.middle_name']");
		if (StringUtils.isNotBlank(middleName)) {
			personName.setMiddleName(middleName);
		}
		
		if (StringUtils.isNotBlank(personName.getFullName())) {
			unsavedPatient.addName(personName);
		}
	}
	
	private void setUnsavedPatientAddressesFromPayload() {
		Set<PersonAddress> addresses = new TreeSet<PersonAddress>();
		
		try {
			Object patientAddressObject = JsonFormatUtils.readAsObject(payload,
			    "$['demographicsupdate']['demographicsupdate.personaddress']");
			if (JsonFormatUtils.isJSONArrayObject(patientAddressObject)) {
				for (Object personAddressJSONObject : (JSONArray) patientAddressObject) {
					PersonAddress patientAddress = getPersonAddressFromJsonObject((JSONObject) personAddressJSONObject);
					if (patientAddress != null) {
						addresses.add(patientAddress);
					}
				}
			} else {
				PersonAddress patientAddress = getPersonAddressFromJsonObject((JSONObject) patientAddressObject);
				if (patientAddress != null) {
					addresses.add(patientAddress);
				}
			}
			
			JSONObject patientObject = (JSONObject) JsonFormatUtils.readAsObject(payload, "$['demographicsupdate']");
			Set keys = patientObject.keySet();
			for (Object key : keys) {
				if (((String) key).startsWith("demographicsupdate.personaddress^")) {
					PersonAddress patientAddress = getPersonAddressFromJsonObject((JSONObject) patientObject.get(key));
					if (patientAddress != null) {
						addresses.add(patientAddress);
					}
				}
			}
			
			PersonAddress legacyPersonAddress = getLegacyPatientAddressFromPayload();
			if (legacyPersonAddress != null) {
				addresses.add(legacyPersonAddress);
			}
			
		}
		catch (InvalidPathException e) {
			log.error("Error while parsing person address", e);
		}
		
		if (!addresses.isEmpty()) {
			unsavedPatient.setAddresses(addresses);
		}
	}
	
	private PersonAddress getLegacyPatientAddressFromPayload() {
		PersonAddress personAddress = null;
		
		String county = JsonFormatUtils.readAsString(payload, "$['demographicsupdate']['demographicsupdate.county']");
		if (StringUtils.isNotEmpty(county)) {
			if (personAddress == null)
				personAddress = new PersonAddress();
			personAddress.setCountyDistrict(county);
		}
		
		String subCounty = JsonFormatUtils.readAsString(payload, "$['demographicsupdate']['demographicsupdate.sub_county']");
		if (StringUtils.isNotEmpty(subCounty)) {
			if (personAddress == null)
				personAddress = new PersonAddress();
			personAddress.setStateProvince(subCounty);
		}
		
		String ward = JsonFormatUtils.readAsString(payload, "$['demographicsupdate']['demographicsupdate.ward']");
		if (StringUtils.isNotEmpty(ward)) {
			if (personAddress == null)
				personAddress = new PersonAddress();
			personAddress.setAddress4(ward);
		}
		
		String landMark = JsonFormatUtils.readAsString(payload, "$['demographicsupdate']['demographicsupdate.landmark']");
		if (StringUtils.isNotEmpty(landMark)) {
			if (personAddress == null)
				personAddress = new PersonAddress();
			personAddress.setAddress2(landMark);
		}
		
		String postalAddress = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.postal_address']");
		if (StringUtils.isNotEmpty(postalAddress)) {
			if (personAddress == null)
				personAddress = new PersonAddress();
			personAddress.setAddress1(postalAddress);
		}
		
		String location = JsonFormatUtils.readAsString(payload, "$['demographicsupdate']['demographicsupdate.location']");
		if (StringUtils.isNotEmpty(location)) {
			if (personAddress == null)
				personAddress = new PersonAddress();
			personAddress.setAddress6(location);
		}
		
		String subLocation = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.sub_location']");
		if (StringUtils.isNotEmpty(subLocation)) {
			if (personAddress == null)
				personAddress = new PersonAddress();
			personAddress.setAddress5(subLocation);
		}
		
		String village = JsonFormatUtils.readAsString(payload, "$['demographicsupdate']['demographicsupdate.village']");
		if (StringUtils.isNotEmpty(village)) {
			if (personAddress == null)
				personAddress = new PersonAddress();
			personAddress.setCityVillage(village);
		}
		return personAddress;
	}
	
	private void setUnsavedPatientPersonAttributesFromPayload() {
		Set<PersonAttribute> attributes = new TreeSet<PersonAttribute>();
		try {
			Object patientAttributeObject = JsonFormatUtils.readAsObject(payload,
			    "$['demographicsupdate']['demographicsupdate.personattribute']");
			if (JsonFormatUtils.isJSONArrayObject(patientAttributeObject)) {
				for (Object personAdttributeJSONObject : (JSONArray) patientAttributeObject) {
					try {
						PersonAttribute personAttribute = getPersonAttributeFromJsonObject((JSONObject) personAdttributeJSONObject);
						if (personAttribute != null) {
							attributes.add(personAttribute);
						}
					}
					catch (Exception e) {
						queueProcessorException.addException(e);
					}
				}
			} else {
				try {
					PersonAttribute personAttribute = getPersonAttributeFromJsonObject((JSONObject) patientAttributeObject);
					if (personAttribute != null) {
						attributes.add(personAttribute);
					}
				}
				catch (Exception e) {
					queueProcessorException.addException(e);
				}
			}
			
			JSONObject patientObject = (JSONObject) JsonFormatUtils.readAsObject(payload, "$['demographicsupdate']");
			Set keys = patientObject.keySet();
			for (Object key : keys) {
				if (((String) key).startsWith("demographicsupdate.personattribute^")) {
					try {
						PersonAttribute personAttribute = getPersonAttributeFromJsonObject((JSONObject) patientObject
						        .get(key));
						if (personAttribute != null) {
							attributes.add(personAttribute);
						}
					}
					catch (Exception e) {
						queueProcessorException.addException(e);
					}
				}
			}
			
			attributes.addAll(getLegacyPersonAttributes());
		}
		catch (InvalidPathException ex) {
			log.error("Error while parsing person attribute", ex);
		}
		
		if (!attributes.isEmpty()) {
			unsavedPatient.setAttributes(attributes);
		}
	}
	
	private Set<PersonAttribute> getLegacyPersonAttributes() {
		Set<PersonAttribute> attributes = new TreeSet<PersonAttribute>();
		// mothers name currently not implemented in Afyastat
        /*String mothersName = JsonUtils.readAsString(payload, "$['demographicsupdate']['demographicsupdate.mothers_name']");
        if(StringUtils.isNotEmpty(mothersName))
            attributes.add(createPersonAttribute("Mother's Name",null,mothersName));*/
		String phoneNumber = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.phone_number']");
		if (StringUtils.isNotEmpty(phoneNumber))
			attributes.add(createPersonAttribute("Telephone contact", null, phoneNumber));
		
		String nearestHealthFacility = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.nearest_health_center']");
		if (StringUtils.isNotEmpty(nearestHealthFacility))
			attributes.add(createPersonAttribute(null, JsonRegistrationQueueInfoHandler.NEAREST_HEALTH_CENTER,
			    nearestHealthFacility));
		
		String alternativePhoneNumber = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.alternate_phone_contact']");
		if (StringUtils.isNotEmpty(alternativePhoneNumber))
			attributes.add(createPersonAttribute(null, JsonRegistrationQueueInfoHandler.ALTERNATE_PHONE_CONTACT,
			    alternativePhoneNumber));
		
		String emailAddress = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.email_address']");
		if (StringUtils.isNotEmpty(emailAddress))
			attributes.add(createPersonAttribute(null, JsonRegistrationQueueInfoHandler.EMAIL_ADDRESS, emailAddress));
		
		String nxtOfKinName = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.next_of_kin_name']");
		if (StringUtils.isNotEmpty(nxtOfKinName))
			attributes.add(createPersonAttribute(null, JsonRegistrationQueueInfoHandler.NEXT_OF_KIN_NAME, nxtOfKinName));
		
		String nxtOfKinContact = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.next_of_kin_contact']");
		if (StringUtils.isNotEmpty(nxtOfKinContact))
			attributes
			        .add(createPersonAttribute(null, JsonRegistrationQueueInfoHandler.NEXT_OF_KIN_CONTACT, nxtOfKinContact));
		
		String nxtOfKinAddress = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.next_of_kin_address']");
		if (StringUtils.isNotEmpty(nxtOfKinAddress))
			attributes
			        .add(createPersonAttribute(null, JsonRegistrationQueueInfoHandler.NEXT_OF_KIN_ADDRESS, nxtOfKinAddress));
		
		String nxtOfKinRelationship = JsonFormatUtils.readAsString(payload,
		    "$['demographicsupdate']['demographicsupdate.next_of_kin_relationship']");
		if (StringUtils.isNotEmpty(nxtOfKinRelationship))
			attributes.add(createPersonAttribute(null, JsonRegistrationQueueInfoHandler.NEXT_OF_KIN_RELATIONSHIP,
			    nxtOfKinRelationship));
		
		return attributes;
	}
	
	private PersonAttribute createPersonAttribute(String attributeTypeName, String attributeTypeUuid, String attributeValue) {
		PersonService personService = Context.getPersonService();
		PersonAttributeType attributeType = null;
		
		if (StringUtils.isNotEmpty(attributeTypeUuid)) {
			attributeType = personService.getPersonAttributeTypeByUuid(attributeTypeUuid);
		}
		
		if (attributeType == null) {
			attributeType = personService.getPersonAttributeTypeByName(attributeTypeName);
		}
		
		if (attributeType == null) {
			queueProcessorException.addException(new Exception("Unable to find Person Attribute Type by name: '"
			        + attributeTypeName + "' , uuid: '" + attributeTypeUuid + "'"));
		}
		
		PersonAttribute personAttribute = new PersonAttribute();
		personAttribute.setAttributeType(attributeType);
		personAttribute.setValue(attributeValue);
		return personAttribute;
	}
	
	private void setUnsavedPatientChangedByFromPayload() {
		String userString = JsonFormatUtils.readAsString(payload, "$['encounter']['encounter.user_system_id']");
		String providerString = JsonFormatUtils.readAsString(payload, "$['encounter']['encounter.provider_id']");
		
		User user = Context.getUserService().getUserByUsername(userString);
		if (user == null) {
			providerString = JsonFormatUtils.readAsString(payload, "$['encounter']['encounter.provider_id']");
			user = Context.getUserService().getUserByUsername(providerString);
		}
		if (user == null) {
			queueProcessorException.addException(new Exception("Unable to find user using the User Id: " + userString
			        + " or Provider Id: " + providerString));
		} else {
			unsavedPatient.setChangedBy(user);
		}
	}
	
	private boolean isBirthDateChangeValidated() {
		return JsonFormatUtils.readAsBoolean(payload,
		    "$['demographicsupdate']['demographicsupdate.birthdate_change_validated']");
	}
	
	private boolean isGenderChangeValidated() {
		return JsonFormatUtils.readAsBoolean(payload,
		    "$['demographicsupdate']['demographicsupdate.gender_change_validated']");
	}
	
	private Integer handleEditObsValues(String obsValue) {
		ArrayNode arrNodeValues = JsonNodeFactory.instance.arrayNode();
		Integer conceptValue = null;
		if (obsValue != null) {
			for (String s : obsValue.split("_")) {
				arrNodeValues.add(s);
			}
			if (arrNodeValues != null) {
				conceptValue = Integer.parseInt(arrNodeValues.get(0).getTextValue());
			}
		}
		return conceptValue;
	}
	
	@Override
	public boolean accept(final AfyaStatQueueData queueData) {
		return StringUtils.equals(DISCRIMINATOR_VALUE, queueData.getDiscriminator());
	}
}
