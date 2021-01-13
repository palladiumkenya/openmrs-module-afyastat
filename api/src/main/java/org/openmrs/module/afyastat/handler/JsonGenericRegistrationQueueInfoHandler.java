/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.afyastat.handler;

import com.jayway.jsonpath.InvalidPathException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.api.service.AfyaStatDataService;
import org.openmrs.module.afyastat.api.service.RegistrationInfoService;
import org.openmrs.module.afyastat.exception.StreamProcessorException;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.RegistrationInfo;
import org.openmrs.module.afyastat.model.handler.QueueInfoHandler;
import org.openmrs.module.afyastat.utils.JsonFormatUtils;
import org.openmrs.module.afyastat.utils.PatientLookUpUtils;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.service.IdentifierSourceService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.openmrs.module.afyastat.utils.JsonFormatUtils.getElementFromJsonObject;
import static org.openmrs.module.afyastat.utils.PersonCreationsUtils.getPersonAddressFromJsonObject;
import static org.openmrs.module.afyastat.utils.PersonCreationsUtils.getPersonAttributeFromJsonObject;

/**
 * TODO: Write brief description about the class here.
 */
@Handler(supports = AfyaStatQueueData.class, order = 1)
public class JsonGenericRegistrationQueueInfoHandler implements QueueInfoHandler {
	
	private static final String TAG = "JsonGenericRegistrationQueueDataHandler";
	
	private static final String DISCRIMINATOR_VALUE = "json-generic-registration";
	
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private final Log log = LogFactory.getLog(JsonGenericRegistrationQueueInfoHandler.class);
	
	private Patient unsavedPatient;
	
	private String payload;
	
	Set<PersonAttribute> personAttributes;
	
	private StreamProcessorException queueProcessorException;
	
	@Override
	public void process(final AfyaStatQueueData queueData) throws StreamProcessorException {
		log.info("Processing registration form data: " + queueData.getUuid());
		queueProcessorException = new StreamProcessorException();
		try {
			if (validate(queueData)) {
				registerUnsavedPatient();
				
				Object obsObject = JsonFormatUtils.readAsObject(queueData.getPayload(), "$['observation']");
				if (obsObject != null) {
					AfyaStatQueueData encounterQueueData = new AfyaStatQueueData();
					encounterQueueData.setDiscriminator("json-encounter");
					encounterQueueData.setDataSource(queueData.getDataSource());
					encounterQueueData.setPayload(queueData.getPayload());
					encounterQueueData.setCreator(queueData.getCreator());
					encounterQueueData.setDateCreated(queueData.getDateCreated());
					encounterQueueData.setUuid(UUID.randomUUID().toString());
					encounterQueueData.setFormName(queueData.getFormName());
					encounterQueueData.setLocation(queueData.getLocation());
					encounterQueueData.setProvider(queueData.getProvider());
					encounterQueueData.setPatientUuid(queueData.getPatientUuid());
					encounterQueueData.setFormDataUuid(queueData.getFormDataUuid());
					Context.getService(AfyaStatDataService.class).saveQueueData(encounterQueueData);
				}
			}
		}
		catch (Exception e) {
			/*Custom exception thrown by the validate function should not be added again into @queueProcessorException.
			 It should add the runtime dao Exception while saving the data into @queueProcessorException collection */
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
	
	@Override
	public boolean validate(AfyaStatQueueData queueData) {
		log.info("Processing registration form data: " + queueData.getUuid());
		queueProcessorException = new StreamProcessorException();
		try {
			payload = queueData.getPayload();
			unsavedPatient = new Patient();
			populateUnsavedPatientFromPayload();
			validateUnsavedPatient();
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
	
	private void validateUnsavedPatient() {
		if (!JsonFormatUtils.readAsBoolean(payload, "$['skipPatientMatching']")) {
			Patient savedPatient = findSimilarSavedPatient();
			if (savedPatient != null) {
				queueProcessorException.addException(new Exception(
				        "Found a patient with similar characteristic :  patientId = " + savedPatient.getPatientId()
				                + " Identifier Id = " + savedPatient.getPatientIdentifier().getIdentifier()));
			}
		}
	}
	
	private void populateUnsavedPatientFromPayload() {
		setPatientIdentifiersFromPayload();
		setPatientBirthDateFromPayload();
		setPatientBirthDateEstimatedFromPayload();
		setPatientGenderFromPayload();
		setPatientDeadFromPayload();
		setPatientNameFromPayload();
		setPatientAddressesFromPayload();
		setPersonAttributesFromPayload();
		setUnsavedPatientCreatorFromPayload();
	}
	
	/*private MuzimaSetting getIdentifierAutogenerationSetting(){
	    MuzimaSettingService settingService = Context.getService(MuzimaSettingService.class);
	    MuzimaSetting autogenerationSetting = null;
	    String activeSetupConfigUuid = JsonUtils.readAsString(payload, "$['encounter']['encounter.setup_config_uuid']");
	    if(StringUtils.isNotBlank(activeSetupConfigUuid)){
	        MuzimaConfigService configService = Context.getService(MuzimaConfigService.class);
	        MuzimaConfig config = configService.getConfigByUuid(activeSetupConfigUuid);
	        if(config != null){
	            autogenerationSetting = config.getConfigMuzimaSettingByProperty(PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING_PROPERTY);
	            if(autogenerationSetting == null){
	                autogenerationSetting = settingService.getMuzimaSettingByProperty(PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING_PROPERTY);
	            }
	        } else {
	            autogenerationSetting = settingService.getMuzimaSettingByProperty(PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING_PROPERTY);
	        }
	    }else{
	        autogenerationSetting = settingService.getMuzimaSettingByProperty(PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING_PROPERTY);
	    }
	    return autogenerationSetting;
	}*/
	
	private void setPatientIdentifiersFromPayload() {
		Set<PatientIdentifier> patientIdentifiers = new TreeSet<PatientIdentifier>();
		//  MuzimaSetting autogenerationSetting = getIdentifierAutogenerationSetting();
		boolean shouldAutogenerateIdentifier = false;
		/*if(autogenerationSetting != null) {
		    shouldAutogenerateIdentifier = autogenerationSetting.getValueBoolean() || Constants.MuzimaSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING_DEFAULT_VALUE;
		}*/
		PatientIdentifier preferredIdentifier;
		
		//preferredIdentifier = getAutogeneratedIdentifier();
		PatientIdentifier medicalRecordNumberFromPayload = getMedicalRecordNumberFromPayload();
		if (medicalRecordNumberFromPayload != null) {
			patientIdentifiers.add(medicalRecordNumberFromPayload);
		}
		preferredIdentifier = getMedicalRecordNumberFromPayload();
		if (preferredIdentifier == null) {
			queueProcessorException.addException(new Exception("Could not retrieve medical record number from payload"));
		}
		
		if (preferredIdentifier != null) {
			preferredIdentifier.setPreferred(true);
			patientIdentifiers.add(preferredIdentifier);
		}
		
		List<PatientIdentifier> otherIdentifiers = getOtherPatientIdentifiersFromPayload();
		if (!otherIdentifiers.isEmpty()) {
			patientIdentifiers.addAll(otherIdentifiers);
		}
		setIdentifierTypeLocation(patientIdentifiers);
		unsavedPatient.setIdentifiers(patientIdentifiers);
	}
	
	/*private PatientIdentifier getAutogeneratedIdentifier(){
	    MuzimaSettingService settingService = Context.getService(MuzimaSettingService.class);
	    MuzimaSetting autoGenerationSourceSetting = settingService.getMuzimaSettingByProperty(
	            Constants.MuzimaSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SOURCE_NAME);
	    if(autoGenerationSourceSetting == null){
	        queueProcessorException.addException(
	            new Exception("Could not auto-generate patient identifier. mUzima setting for idgen source is not defined.")
	        );
	        return null;
	    }

	    try {
	        IdentifierSourceService sourceService = Context.getService(IdentifierSourceService.class);
	        if (sourceService != null) {
	            String autoGenerationSource = autoGenerationSourceSetting.getValueString();
	            List<IdentifierSource> sources = sourceService.getAllIdentifierSources(false);
	            IdentifierSource source = null;
	            for (IdentifierSource s : sources) {
	                if (StringUtils.equals(s.getName(), autoGenerationSource)) {
	                    source = s;
	                }
	            }
	            if (source == null) {
	                queueProcessorException.addException(
	                        new Exception("Could not auto-generate patient identifier." +
	                                " Could not find idgen source with name: "+ autoGenerationSource)
	                );
	                return null;
	            }
	            PatientIdentifierType identifierType = source.getIdentifierType();
	            String identifierValue = sourceService.generateIdentifier(source, "mUzima registration");
	            PatientIdentifier identifier = new PatientIdentifier();
	            identifier.setIdentifierType(identifierType);
	            identifier.setIdentifier(identifierValue);
	            return identifier;
	        }
	    }catch (NoClassDefFoundError e){
	        queueProcessorException.addException(
	                new Exception("Could not auto-generate patient identifier. Idgen module is not running.")
	        );
	    }
	    return null;
	}*/
	
	private PatientIdentifier getMedicalRecordNumberFromPayload() {
		JSONObject medicalRecordNumberObject = (JSONObject) JsonFormatUtils.readAsObject(payload,
		    "$['patient']['patient.medical_record_number']");
		return createPatientIdentifier(medicalRecordNumberObject);
	}
	
	private List<PatientIdentifier> getOtherPatientIdentifiersFromPayload() {
		List<PatientIdentifier> otherIdentifiers = new ArrayList<PatientIdentifier>();
		try {
			Object otheridentifierObject = JsonFormatUtils.readAsObject(payload, "$['patient']['patient.otheridentifier']");
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
			
			JSONObject patientObject = (JSONObject) JsonFormatUtils.readAsObject(payload, "$['patient']");
			Set keys = patientObject.keySet();
			for (Object key : keys) {
				if (((String) key).startsWith("patient.otheridentifier^")) {
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
	
	private PatientIdentifier createPatientIdentifier(JSONObject identifierObject) {
		if (identifierObject == null) {
			return null;
		}
		
		String identifierTypeName = (String) getElementFromJsonObject(identifierObject, "identifier_type_name");
		String identifierUuid = (String) getElementFromJsonObject(identifierObject, "identifier_type_uuid");
		String identifierValue = (String) getElementFromJsonObject(identifierObject, "identifier_value");
		
		return createPatientIdentifier(identifierUuid, identifierTypeName, identifierValue);
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
		PatientIdentifierType identifierType = null;
		if (StringUtils.isNotBlank(identifierTypeUuid)) {
			identifierType = Context.getPatientService().getPatientIdentifierTypeByUuid(identifierTypeUuid);
		}
		if (identifierType == null && StringUtils.isNotBlank(identifierTypeName)) {
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
	
	private void setPatientBirthDateFromPayload() {
		Date birthDate = JsonFormatUtils.readAsDate(payload, "$['patient']['patient.birth_date']");
		unsavedPatient.setBirthdate(birthDate);
	}
	
	private void setPatientBirthDateEstimatedFromPayload() {
		boolean birthdateEstimated = JsonFormatUtils.readAsBoolean(payload, "$['patient']['patient.birthdate_estimated']");
		unsavedPatient.setBirthdateEstimated(birthdateEstimated);
	}
	
	private void setPatientGenderFromPayload() {
		String gender = JsonFormatUtils.readAsString(payload, "$['patient']['patient.sex']");
		unsavedPatient.setGender(gender);
	}
	
	private void setPatientDeadFromPayload() {
		Boolean isDead = JsonFormatUtils.readAsBoolean(payload, "$['patient']['patient.persondead']");
		unsavedPatient.setDead(isDead);
	}
	
	private void setPatientNameFromPayload() {
		String givenName = JsonFormatUtils.readAsString(payload, "$['patient']['patient.given_name']");
		String familyName = JsonFormatUtils.readAsString(payload, "$['patient']['patient.family_name']");
		String middleName = "";
		try {
			middleName = JsonFormatUtils.readAsString(payload, "$['patient']['patient.middle_name']");
		}
		catch (Exception e) {
			log.error(e);
		}
		PersonName personName = new PersonName();
		personName.setGivenName(givenName);
		personName.setMiddleName(middleName);
		personName.setFamilyName(familyName);
		unsavedPatient.addName(personName);
	}
	
	private void registerUnsavedPatient() {
		RegistrationInfoService registrationDataService = Context.getService(RegistrationInfoService.class);
		String temporaryUuid = getPatientUuidFromPayload();
		RegistrationInfo registrationData = registrationDataService.getRegistrationDataByTemporaryUuid(temporaryUuid);
		if (registrationData == null) {
			registrationData = new RegistrationInfo();
			registrationData.setTemporaryUuid(temporaryUuid);
			Context.getPatientService().savePatient(unsavedPatient);
			String assignedUuid = unsavedPatient.getUuid();
			registrationData.setAssignedUuid(assignedUuid);
			registrationDataService.saveRegistrationData(registrationData);
		}
	}
	
	private String getPatientUuidFromPayload() {
		return JsonFormatUtils.readAsString(payload, "$['patient']['patient.uuid']");
	}
	
	private void setPatientAddressesFromPayload() {
		Set<PersonAddress> addresses = new TreeSet<PersonAddress>();
		
		try {
			Object patientAddressObject = JsonFormatUtils.readAsObject(payload, "$['patient']['patient.personaddress']");
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
			
			JSONObject patientObject = (JSONObject) JsonFormatUtils.readAsObject(payload, "$['patient']");
			Set keys = patientObject.keySet();
			for (Object key : keys) {
				if (((String) key).startsWith("patient.personaddress^")) {
					PersonAddress patientAddress = getPersonAddressFromJsonObject((JSONObject) patientObject.get(key));
					if (patientAddress != null) {
						addresses.add(patientAddress);
					}
				}
			}
		}
		catch (InvalidPathException e) {
			log.error("Error while parsing person address", e);
		}
		
		if (!addresses.isEmpty()) {
			unsavedPatient.setAddresses(addresses);
		}
		
	}
	
	private void setPersonAttributesFromPayload() {
		Set<PersonAttribute> attributes = new TreeSet<PersonAttribute>();
		try {
			Object patientAttributeObject = JsonFormatUtils.readAsObject(payload, "$['patient']['patient.personattribute']");
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
						log.error(e);
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
					log.error(e);
				}
			}
			
			JSONObject patientObject = (JSONObject) JsonFormatUtils.readAsObject(payload, "$['patient']");
			Set keys = patientObject.keySet();
			for (Object key : keys) {
				if (((String) key).startsWith("patient.personattribute^")) {
					try {
						PersonAttribute personAttribute = getPersonAttributeFromJsonObject((JSONObject) patientObject
						        .get(key));
						if (personAttribute != null) {
							attributes.add(personAttribute);
						}
					}
					catch (Exception e) {
						queueProcessorException.addException(e);
						log.error(e);
					}
				}
			}
		}
		catch (InvalidPathException ex) {
			log.error("Error while parsing person attribute", ex);
		}
		
		if (!attributes.isEmpty()) {
			unsavedPatient.setAttributes(attributes);
		}
	}
	
	private void setUnsavedPatientCreatorFromPayload() {
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
			unsavedPatient.setCreator(user);
		}
	}
	
	private Patient findSimilarSavedPatient() {
		Patient savedPatient = null;
		if (unsavedPatient.getNames().isEmpty()) {
			PatientIdentifier identifier = unsavedPatient.getPatientIdentifier();
			if (identifier != null) {
				List<Patient> patients = Context.getPatientService().getPatients(identifier.getIdentifier());
				savedPatient = PatientLookUpUtils.findSimilarPatientByNameAndGender(patients, unsavedPatient);
			}
		} else {
			PersonName personName = unsavedPatient.getPersonName();
			List<Patient> patients = Context.getPatientService().getPatients(personName.getFullName());
			savedPatient = PatientLookUpUtils.findSimilarPatientByNameAndGender(patients, unsavedPatient);
		}
		return savedPatient;
	}
	
	@Override
	public boolean accept(final AfyaStatQueueData queueData) {
		return StringUtils.equals(DISCRIMINATOR_VALUE, queueData.getDiscriminator());
	}
}
