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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.hivtestingservices.api.HTSService;
import org.openmrs.module.hivtestingservices.api.PatientContact;

import org.openmrs.module.afyastat.api.service.RegistrationInfoService;
import org.openmrs.module.afyastat.exception.StreamProcessorException;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.RegistrationInfo;
import org.openmrs.module.afyastat.model.handler.QueueInfoHandler;
import org.openmrs.module.afyastat.utils.JsonFormatUtils;

import java.util.Date;

/**
 * Processes patient contact registration using relationships
 */
@Handler(supports = AfyaStatQueueData.class, order = 12)
public class PatientContactRegistrationQueueDataHandler implements QueueInfoHandler {
	
	private static final String DISCRIMINATOR_VALUE = "json-createpatientcontactusingrelatioship";
	
	private final Log log = LogFactory.getLog(PatientContactRegistrationQueueDataHandler.class);
	
	private PatientContact unsavedPatientContact;
	
	private String payload;
	
	private StreamProcessorException queueProcessorException;
	
	@Override
	public void process(final AfyaStatQueueData queueData) throws StreamProcessorException {
		log.info("Processing patient contact registration using relationship: " + queueData.getUuid());
		queueProcessorException = new StreamProcessorException();
		try {
			if (validate(queueData)) {
				registerUnsavedPatientContact();
			}
		}
		catch (Exception e) {
			if (!e.getClass().equals(StreamProcessorException.class)) {
				queueProcessorException.addException(new Exception("Exception while process payload ", e));
			}
		}
		finally {
			if (queueProcessorException.anyExceptions()) {
				throw queueProcessorException;
			}
		}
	}
	
	@Override
	public boolean validate(AfyaStatQueueData queueData) {
		log.info("Processing patient contact registration using relationship: " + queueData.getUuid());
		queueProcessorException = new StreamProcessorException();
		try {
			payload = queueData.getPayload();
			unsavedPatientContact = new PatientContact();
			populateUnsavedPatientContactFromPayload();
			return true;
		}
		catch (Exception e) {
			queueProcessorException.addException(new Exception("Exception while validating payload ", e));
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
	
	private void populateUnsavedPatientContactFromPayload() {
		setPatientContactFromPayload();
	}
	
	private void setPatientContactFromPayload() {
		String givenName = JsonFormatUtils.readAsString(payload, "$['patient_firstName']");
		String middleName = JsonFormatUtils.readAsString(payload, "$['patient_middleName']");
		String familyName = JsonFormatUtils.readAsString(payload, "$['patient_familyName']");
		Integer relType = relationshipTypeConverter(JsonFormatUtils.readAsString(payload, "$['relation_type']"));
		Date birthDate = JsonFormatUtils.readAsDate(payload, "$['patient_birthDate']",
		    JsonFormatUtils.YYYY_MM_DD_DATE_PATTERN);
		String sex = gender(JsonFormatUtils.readAsString(payload, "$['patient_sex']"));
		String phoneNumber = JsonFormatUtils.readAsString(payload, "$['patient_telephone']");
		Integer maritalStatus = maritalStatusConverter(JsonFormatUtils.readAsString(payload, "$['patient_marital_status']"));
		String physicalAddress = JsonFormatUtils.readAsString(payload, "$['patient_postalAddress']");
		
		Integer patientRelatedTo = null;
		Integer patient = null;
		String afyaStatRelationUuid = JsonFormatUtils.readAsString(payload, "$['relation_uuid']");
		String parentId = JsonFormatUtils.readAsString(payload, "$['parent']['_id']"); // the default patient contact parent's uuid
		String kemrRef = JsonFormatUtils.readAsString(payload, "$['parent']['kemr_uuid']"); // exists if the parent was listed in afyastat, pushed to the emr for full registration, and pushed back to afyastat
		patientRelatedTo = org.apache.commons.lang3.StringUtils.isNotBlank(afyaStatRelationUuid) ? getPatientRelatedToContact(afyaStatRelationUuid)
		        : getPatientRelatedToContact(org.apache.commons.lang3.StringUtils.isNotBlank(kemrRef) ? kemrRef : parentId);
		String uuid = JsonFormatUtils.readAsString(payload, "$['_id']");
		patient = org.apache.commons.lang3.StringUtils.isNotBlank(uuid) ? getPatientAsContact(uuid)
		        : getPatientAsContact(JsonFormatUtils.readAsString(payload, "$['parent']['_id']"));
		
		Boolean voided = false;
		
		unsavedPatientContact.setFirstName(givenName);
		unsavedPatientContact.setMiddleName(middleName);
		unsavedPatientContact.setLastName(familyName);
		unsavedPatientContact.setRelationType(relType);
		unsavedPatientContact.setBirthDate(birthDate);
		unsavedPatientContact.setSex(sex);
		unsavedPatientContact.setPhoneContact(phoneNumber);
		unsavedPatientContact.setMaritalStatus(maritalStatus);
		unsavedPatientContact.setPhysicalAddress(physicalAddress);
		unsavedPatientContact.setPatientRelatedTo(Context.getPatientService().getPatient(patientRelatedTo));
		if (patient != null) {
			unsavedPatientContact.setPatient(Context.getPatientService().getPatient(patient));
		}
		unsavedPatientContact.setUuid(uuid);
		unsavedPatientContact.setVoided(voided);
	}
	
	private void registerUnsavedPatientContact() {
		HTSService htsService = Context.getService(HTSService.class);
		RegistrationInfoService registrationDataService = Context.getService(RegistrationInfoService.class);
		String temporaryUuid = getPatientContactUuidFromPayload();
		RegistrationInfo registrationData = registrationDataService.getRegistrationDataByTemporaryUuid(temporaryUuid);
		
		if (registrationData != null) {
			try {
				htsService.savePatientContact(unsavedPatientContact);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			log.info("Unable to save, same contact already exist for the patient");
		}
	}
	
	private String getPatientContactUuidFromPayload() {
		return JsonFormatUtils.readAsString(payload, "$['_id']");
	}
	
	private Integer getPatientRelatedToContact(String uuid) {
		Integer patientId = null;
		RegistrationInfoService regDataService = Context.getService(RegistrationInfoService.class);
		RegistrationInfo regData = regDataService.getRegistrationDataByTemporaryUuid(uuid);
		if (regData != null) {
			Patient p = Context.getPatientService().getPatientByUuid(regData.getAssignedUuid());
			if (p != null) {
				patientId = p.getPatientId();
			}
		}
		if (patientId == null) {
			// check to see if the uuid is for patient
			Person person = Context.getPersonService().getPersonByUuid(uuid);
			if (person != null) {
				patientId = person.getPersonId();
			}
		}
		return patientId;
	}
	
	private Integer getPatientAsContact(String uuid) {
		Integer patientId = null;
		RegistrationInfoService regDataService = Context.getService(RegistrationInfoService.class);
		RegistrationInfo regData = regDataService.getRegistrationDataByTemporaryUuid(uuid);
		if (regData != null) {
			Patient p = Context.getPatientService().getPatientByUuid(regData.getAssignedUuid());
			if (p != null) {
				patientId = p.getPatientId();
			}
		}
		
		return patientId;
	}
	
	private Integer relationshipTypeConverter(String relType) {
		Integer relTypeConverter = null;
		if (relType.equalsIgnoreCase("partner")) {
			relTypeConverter = 163565;
		} else if (relType.equalsIgnoreCase("parent") || relType.equalsIgnoreCase("guardian")
		        || relType.equalsIgnoreCase("mother") || relType.equalsIgnoreCase("father")) {
			relTypeConverter = 970;
		} else if (relType.equalsIgnoreCase("sibling")) {
			relTypeConverter = 972;
		} else if (relType.equalsIgnoreCase("child")) {
			relTypeConverter = 1528;
		} else if (relType.equalsIgnoreCase("spouse")) {
			relTypeConverter = 5617;
		} else if (relType.equalsIgnoreCase("co-wife")) {
			relTypeConverter = 162221;
		} else if (relType.equalsIgnoreCase("Injectable drug user")) {
			relTypeConverter = 157351;
		}
		return relTypeConverter;
	}
	
	private Integer maritalStatusConverter(String marital_status) {
		Integer civilStatusConverter = null;
		if (marital_status.equalsIgnoreCase("_1057_neverMarried_99DCT")) {
			civilStatusConverter = 1057;
		} else if (marital_status.equalsIgnoreCase("_1058_divorced_99DCT")) {
			civilStatusConverter = 1058;
		} else if (marital_status.equalsIgnoreCase("_1059_widowed_99DCT")) {
			civilStatusConverter = 1059;
		} else if (marital_status.equalsIgnoreCase("_5555_marriedMonogomous_99DCT")) {
			civilStatusConverter = 5555;
		} else if (marital_status.equalsIgnoreCase("_159715_marriedPolygamous_99DCT")) {
			civilStatusConverter = 159715;
		} else if (marital_status.equalsIgnoreCase("_1060_livingWithPartner_99DCT")) {
			civilStatusConverter = 1060;
		}
		return civilStatusConverter;
	}
	
	private Integer livingWithPartnerConverter(String livingWithPatient) {
		Integer livingWithPatientConverter = null;
		if (livingWithPatient.equalsIgnoreCase("no")) {
			livingWithPatientConverter = 1066;
		} else if (livingWithPatient.equalsIgnoreCase("yes")) {
			livingWithPatientConverter = 1065;
		} else if (livingWithPatient.equalsIgnoreCase("Declined to answer")) {
			livingWithPatientConverter = 162570;
		}
		return livingWithPatientConverter;
	}
	
	private Integer pnsApproachConverter(String pns_approach) {
		Integer pnsApproach = null;
		if (pns_approach.equalsIgnoreCase("dual_referral")) {
			pnsApproach = 162284;
		} else if (pns_approach.equalsIgnoreCase("provider_referral")) {
			pnsApproach = 163096;
		} else if (pns_approach.equalsIgnoreCase("contract_referral")) {
			pnsApproach = 161642;
		} else if (pns_approach.equalsIgnoreCase("passive_referral")) {
			pnsApproach = 160551;
		}
		return pnsApproach;
	}
	
	private String gender(String gender) {
		String abbreviateGender = null;
		if (gender.equalsIgnoreCase("male")) {
			abbreviateGender = "M";
		}
		if (gender.equalsIgnoreCase("female")) {
			abbreviateGender = "F";
		}
		return abbreviateGender;
	}
	
	@Override
	public boolean accept(final AfyaStatQueueData queueData) {
		return StringUtils.equals(DISCRIMINATOR_VALUE, queueData.getDiscriminator());
	}
	
}
