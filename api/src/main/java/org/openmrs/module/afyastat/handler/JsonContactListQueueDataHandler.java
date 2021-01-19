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
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.api.service.RegistrationInfoService;
import org.openmrs.module.afyastat.exception.StreamProcessorException;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.RegistrationInfo;
import org.openmrs.module.afyastat.model.handler.QueueInfoHandler;
import org.openmrs.module.afyastat.utils.JsonFormatUtils;
import org.openmrs.module.hivtestingservices.api.HTSService;
import org.openmrs.module.hivtestingservices.api.PatientContact;

import java.util.Date;

/**
 * Processes contact list from CHT
 */
@Handler(supports = AfyaStatQueueData.class, order = 11)
public class JsonContactListQueueDataHandler implements QueueInfoHandler {
	
	private static final String DISCRIMINATOR_VALUE = "json-patientcontact";
	
	private final Log log = LogFactory.getLog(JsonContactListQueueDataHandler.class);
	
	private PatientContact unsavedPatientContact;
	
	private String payload;
	
	private StreamProcessorException queueProcessorException;
	
	@Override
	public void process(final AfyaStatQueueData queueData) throws StreamProcessorException {
		log.info("Processing patient contact form data: " + queueData.getUuid());
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
		log.info("Processing contact list form data: " + queueData.getUuid());
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
		String givenName = JsonFormatUtils.readAsString(payload, "$['f_name']");
		String middleName = JsonFormatUtils.readAsString(payload, "$['o_name']");
		String familyName = JsonFormatUtils.readAsString(payload, "$['s_name']");
		Integer relType = relationshipTypeConverter(JsonFormatUtils.readAsString(payload, "$['contact_relationship']"));
		String baselineStatus = JsonFormatUtils.readAsString(payload, "$['baseline_hiv_status']");
		Date nextTestDate = JsonFormatUtils.readAsDate(payload, "$['booking_date']", JsonFormatUtils.DATE_PATTERN_MEDIC);
		Date birthDate = JsonFormatUtils.readAsDate(payload, "$['date_of_birth']", JsonFormatUtils.DATE_PATTERN_MEDIC);
		String sex = gender(JsonFormatUtils.readAsString(payload, "$['sex']"));
		String phoneNumber = JsonFormatUtils.readAsString(payload, "$['phone']");
		Integer maritalStatus = maritalStatusConverter(JsonFormatUtils.readAsString(payload, "$['marital_status']"));
		Integer livingWithPatient = livingWithPartnerConverter(JsonFormatUtils.readAsString(payload,
		    "$['living_with_client']"));
		Integer pnsApproach = pnsApproachConverter(JsonFormatUtils.readAsString(payload, "$['pns_approach']"));
		String physicalAddress = JsonFormatUtils.readAsString(payload, "$['physical_address']");
		
		Integer patientRelatedTo = null;
		String indexKemrUuid = JsonFormatUtils.readAsString(payload, "$['parent']['kemr_uuid']");
		patientRelatedTo = org.apache.commons.lang3.StringUtils.isNotBlank(indexKemrUuid) ? getPatientRelatedToContact(indexKemrUuid)
		        : getPatientRelatedToContact(JsonFormatUtils.readAsString(payload, "$['parent']['_id']"));
		String uuid = JsonFormatUtils.readAsString(payload, "$['_id']");
		Boolean voided = false;
		
		if (org.apache.commons.lang3.StringUtils.isNotBlank(baselineStatus)) {
			
			if (baselineStatus.equals("unknown")) {
				baselineStatus = "Unknown";
			} else if (baselineStatus.equals("positive")) {
				baselineStatus = "Positive";
			} else if (baselineStatus.equals("negative")) {
				baselineStatus = "Negative";
			} else if (baselineStatus.equals("exposed_infant")) {
				baselineStatus = "Exposed Infant";
			}
		}
		
		unsavedPatientContact.setFirstName(givenName);
		unsavedPatientContact.setMiddleName(middleName);
		unsavedPatientContact.setLastName(familyName);
		unsavedPatientContact.setRelationType(relType);
		unsavedPatientContact.setBaselineHivStatus(baselineStatus);
		unsavedPatientContact.setAppointmentDate(nextTestDate);
		unsavedPatientContact.setBirthDate(birthDate);
		unsavedPatientContact.setSex(sex);
		unsavedPatientContact.setPhoneContact(phoneNumber);
		unsavedPatientContact.setMaritalStatus(maritalStatus);
		unsavedPatientContact.setLivingWithPatient(livingWithPatient);
		unsavedPatientContact.setPnsApproach(pnsApproach);
		unsavedPatientContact.setContactListingDeclineReason("CHT");// using this to identify contact pushed from CHT
		unsavedPatientContact.setPhysicalAddress(physicalAddress);
		unsavedPatientContact.setPatientRelatedTo(Context.getPatientService().getPatient(patientRelatedTo));
		unsavedPatientContact.setUuid(uuid);
		unsavedPatientContact.setVoided(voided);
	}
	
	private void registerUnsavedPatientContact() {
		HTSService htsService = Context.getService(HTSService.class);
		RegistrationInfoService registrationDataService = Context.getService(RegistrationInfoService.class);
		String temporaryUuid = getPatientContactUuidFromPayload();
		RegistrationInfo registrationData = registrationDataService.getRegistrationDataByTemporaryUuid(temporaryUuid);
		if (registrationData == null) {
			registrationData = new RegistrationInfo();
			registrationData.setTemporaryUuid(temporaryUuid);
			try {
				htsService.savePatientContact(unsavedPatientContact);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			String assignedUuid = unsavedPatientContact.getUuid();
			registrationData.setAssignedUuid(assignedUuid);
			registrationDataService.saveRegistrationData(registrationData);
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
		if (marital_status.equalsIgnoreCase("Single")) {
			civilStatusConverter = 1057;
		} else if (marital_status.equalsIgnoreCase("Divorced")) {
			civilStatusConverter = 1058;
		} else if (marital_status.equalsIgnoreCase("Widowed")) {
			civilStatusConverter = 1059;
		} else if (marital_status.equalsIgnoreCase("Married Monogamous")) {
			civilStatusConverter = 5555;
		} else if (marital_status.equalsIgnoreCase("Married Polygamous")) {
			civilStatusConverter = 159715;
		} else if (marital_status.equalsIgnoreCase("cohabiting")) {
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
		String abbriviateGender = null;
		if (gender.equalsIgnoreCase("male")) {
			abbriviateGender = "M";
		}
		if (gender.equalsIgnoreCase("female")) {
			abbriviateGender = "F";
		}
		return abbriviateGender;
	}
	
	@Override
	public boolean accept(final AfyaStatQueueData queueData) {
		return StringUtils.equals(DISCRIMINATOR_VALUE, queueData.getDiscriminator());
	}
	
}
