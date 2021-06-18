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
import org.openmrs.*;
import org.openmrs.annotation.Handler;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;

import org.openmrs.module.afyastat.api.service.RegistrationInfoService;
import org.openmrs.module.afyastat.exception.StreamProcessorException;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.RegistrationInfo;
import org.openmrs.module.afyastat.model.handler.QueueInfoHandler;
import org.openmrs.module.afyastat.utils.JsonFormatUtils;

import static org.openmrs.module.afyastat.utils.PersonCreationsUtils.getPersonAddressFromJsonObject;
import static org.openmrs.module.afyastat.utils.PersonCreationsUtils.getPersonAttributeFromJsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

/**
 * This handler has been adopted from openmrs-module-muzimacore
 * This Handler processes relationships received from
 * {@link org.openmrs.module.afyastat.model.AfyaDataSource} = "mobile" The handler will: <b>Create a
 * new relationship between two persons based on their uuid</b> <b>Update a relationship between to
 * persons based on their uuid</b> <b>Delete a relationship between two persons based on their
 * uuid</b> <b>Throw an error to the {@link org.openmrs.module.afyastat.model.ErrorInfo} where any
 * of this fails</b>
 * 
 * @author sthaiya
 *
 * Please see https://github.com/muzima/openmrs-module-muzimacore/blob/master/api/src/main/java/org/openmrs/module/muzima/handler/RelationshipQueueDataHandler.java
 */

@Handler(supports = AfyaStatQueueData.class, order = 8)
public class RelationshipQueueInfoHandler implements QueueInfoHandler {
	
	public static final String DISCRIMINATOR_VALUE = "json-relationship";
	
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private final Log log = LogFactory.getLog(RelationshipQueueInfoHandler.class);
	
	private String payload;
	
	private StreamProcessorException queueProcessorException;
	
	private PersonService personService;
	
	@Override
	public void process(final AfyaStatQueueData queueData) throws StreamProcessorException {
		log.info("Processing relationship data: " + queueData.getUuid());
		queueProcessorException = new StreamProcessorException();
		personService = Context.getPersonService();
		payload = queueData.getPayload();
		createRelationship();
	}
	
	@Override
	public boolean validate(AfyaStatQueueData queueData) {
		log.info("Processing relationship form data: " + queueData.getUuid());
		queueProcessorException = new StreamProcessorException();
		try {
			personService = Context.getPersonService();
			payload = queueData.getPayload();
			String temporaryUuid = queueData.getPatientUuid();
			Person patient = personService.getPersonByUuid(temporaryUuid);
			if (patient == null) {
				RegistrationInfoService dataService = Context.getService(RegistrationInfoService.class);
				RegistrationInfo registrationData = dataService.getRegistrationDataByTemporaryUuid(temporaryUuid);
				if (registrationData != null) {
					patient = Context.getPatientService().getPatientByUuid(registrationData.getAssignedUuid());
				}
			}
			
			if (patient == null)
				queueProcessorException.addException(new Exception("Unable to validate a relationship patient"));
			
			if (personService.getRelationshipTypeByUuid(getRelationshipTypeUuidFromPayload()) == null)
				queueProcessorException.addException(new Exception(
				        "Unable to validate a relationship type used in a relationship"));
			
			return true;
		}
		catch (Exception e) {
			log.error("Exception while validating payload ", e);
			queueProcessorException.addException(new Exception("Exception while validating payload ", e));
			return false;
		}
		finally {
			if (queueProcessorException.anyExceptions())
				throw queueProcessorException;
		}
	}
	
	private void createRelationship() {
		String personATemporaryUuid = JsonFormatUtils.readAsString(payload, "['uuid']");
		String personBTemporaryUuid = JsonFormatUtils.readAsString(payload, "['personBUuid']");
		
		Person p = personService.getPersonByUuid(personATemporaryUuid);
		Person pb = personService.getPersonByUuid(personBTemporaryUuid);
		
		//get person A details
		if (p == null) {
			RegistrationInfoService registrationDataService = Context.getService(RegistrationInfoService.class);
			RegistrationInfo registrationData = registrationDataService
			        .getRegistrationDataByTemporaryUuid(personATemporaryUuid);
			if (registrationData != null) {
				p = personService.getPersonByUuid(registrationData.getAssignedUuid());
			}
		}
		
		//Get person B details
		if (pb == null) {
			RegistrationInfoService registrationDataService = Context.getService(RegistrationInfoService.class);
			RegistrationInfo registrationData = registrationDataService
			        .getRegistrationDataByTemporaryUuid(personBTemporaryUuid);
			if (registrationData != null) {
				pb = personService.getPersonByUuid(registrationData.getAssignedUuid());
			}
		}
		
		Person personA = p; //validateOrCreate(getPersonUuidFromPayload("personA"), "personA");
		Person personB = pb; //validateOrCreate(getPersonUuidFromPayload("personB"), "personB");
		try {
			if (personA != null && personB != null) {
				RelationshipType relationshipType = personService
				        .getRelationshipTypeByUuid(getRelationshipTypeUuidFromPayload());
				Relationship relationship = new Relationship(personA, personB, relationshipType);
				
				// We reuse the uuid created on the mobile device
				relationship.setUuid(getRelationshipUuidFromPayload());
				
				personService.saveRelationship(relationship);
			}
		}
		catch (Exception e) {
			log.error(e);
		}
	}
	
	private Person validateOrCreate(String personUuid, String root) {
		Person p = personService.getPersonByUuid(personUuid);
		if (p == null) {
			RegistrationInfoService registrationDataService = Context.getService(RegistrationInfoService.class);
			RegistrationInfo registrationData = registrationDataService.getRegistrationDataByTemporaryUuid(personUuid);
			if (registrationData != null) {
				p = personService.getPersonByUuid(registrationData.getAssignedUuid());
			} else {
				Person person = new Person();
				try {
					person.addName(getPersonNameFromPayload(root));
					person.setBirthdate(getPersonBirthDateFromPayload(root));
					person.setBirthdateEstimated(getPersonBirthDateEstimatedFromPayload(root));
					person.setGender(getPersonGenderFromPayload(root));
					person.setCreator(getCreatorFromPayload());
					person.setAddresses(getPersonAddressesFromPayload(root));
					person.setAttributes(getPersonAttributesFromPayload(root));
					
					// We reuse the person uuid created on the mobile device
					person.setUuid(personUuid);
					
					p = personService.savePerson(person);
				}
				catch (Exception e) {
					log.error(e);
				}
			}
		}
		return p;
	}
	
	private String getPersonUuidFromPayload(String root) {
		return JsonFormatUtils.readAsString(payload, root + "['uuid']");
	}
	
	private String getRelationshipUuidFromPayload() {
		// add key relation uuid for testing
		return JsonFormatUtils.readAsString(payload, "$['relationUuid']['uuid']");
	}
	
	private String getRelationshipTypeUuidFromPayload() {
		return JsonFormatUtils.readAsString(payload, "$['relationshipType']['uuid']");
	}
	
	private PersonName getPersonNameFromPayload(String root) {
		String givenName = JsonFormatUtils.readAsString(payload, root + "['given_name']");
		String familyName = JsonFormatUtils.readAsString(payload, root + "['family_name']");
		String middleName = "";
		try {
			middleName = JsonFormatUtils.readAsString(payload, root + "['middle_name']");
		}
		catch (Exception e) {
			log.error(e);
		}
		
		PersonName personName = new PersonName();
		personName.setGivenName(givenName);
		personName.setMiddleName(middleName);
		personName.setFamilyName(familyName);
		return personName;
	}
	
	private Date getPersonBirthDateFromPayload(String root) {
		return JsonFormatUtils.readAsDate(payload, root + "['birth_date']");
	}
	
	private Boolean getPersonBirthDateEstimatedFromPayload(String root) {
		boolean birthdateEstimated = false;
		
		try {
			birthdateEstimated = JsonFormatUtils.readAsBoolean(payload, root + "['birthdate_estimated']");
		}
		catch (Exception e) {
			log.error(e);
		}
		
		return birthdateEstimated;
	}
	
	private String getPersonGenderFromPayload(String root) {
		return JsonFormatUtils.readAsString(payload, root + "['sex']");
	}
	
	private User getCreatorFromPayload() {
		String providerString = JsonFormatUtils.readAsString(payload, "$['encounter']['encounter.provider_id']");
		
		if (StringUtils.isEmpty(providerString))
			providerString = JsonFormatUtils.readAsString(payload, "$['encounter']['encounter.provider_id_select']");
		
		User user = Context.getUserService().getUserByUsername(providerString);
		if (user == null) {
			queueProcessorException.addException(new Exception("Unable to find user using the User Id: " + providerString));
			return null;
		} else {
			return user;
		}
	}
	
	private Set<PersonAddress> getPersonAddressesFromPayload(String root) {
		Set<PersonAddress> addresses = new TreeSet<PersonAddress>();
		try {
			Object patientAddressObject = JsonFormatUtils.readAsObject(payload, root + "['addresses']");
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
		}
		catch (InvalidPathException e) {
			log.error("Error while parsing person address", e);
		}
		return addresses;
	}
	
	private Set<PersonAttribute> getPersonAttributesFromPayload(String root) {
		Set<PersonAttribute> attributes = new TreeSet<PersonAttribute>();
		try {
			Object patientAttributeObject = JsonFormatUtils.readAsObject(payload, root + "['attributes']");
			if (JsonFormatUtils.isJSONArrayObject(patientAttributeObject)) {
				for (Object personAttributeJSONObject : (JSONArray) patientAttributeObject) {
					try {
						PersonAttribute personAttribute = getPersonAttributeFromJsonObject((JSONObject) personAttributeJSONObject);
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
		}
		catch (InvalidPathException ex) {
			log.error("Error while parsing person attribute", ex);
		}
		return attributes;
	}
	
	@Override
	public String getDiscriminator() {
		return DISCRIMINATOR_VALUE;
	}
	
	@Override
	public boolean accept(final AfyaStatQueueData queueData) {
		return StringUtils.equals(DISCRIMINATOR_VALUE, queueData.getDiscriminator());
	}
}
