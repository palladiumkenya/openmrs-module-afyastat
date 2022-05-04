package org.openmrs.module.afyastat.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.Provider;
import org.openmrs.Relationship;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.api.AfyastatService;
import org.openmrs.module.afyastat.api.service.InfoService;
import org.openmrs.module.afyastat.api.service.MedicOutgoingRegistrationService;
import org.openmrs.module.afyastat.metadata.AfyaStatMetadata;
import org.openmrs.module.afyastat.model.AfyaDataSource;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.MedicOutgoingRegistration;
import org.openmrs.module.hivtestingservices.api.HTSService;
import org.openmrs.module.hivtestingservices.api.PatientContact;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

;

public class MedicDataExchange {
	
	AfyastatService afyastatService = Context.getService(AfyastatService.class);
	
	InfoService dataService = Context.getService(InfoService.class);
	
	HTSService htsService = Context.getService(HTSService.class);
	
	PersonService personService = Context.getPersonService();
	
	EncounterService encService = Context.getEncounterService();
	
	FormService formService = Context.getFormService();
	
	EncounterType et = encService.getEncounterTypeByUuid(Utils.HTS);
	
	Form initial = formService.getFormByUuid(Utils.HTS_INITIAL_TEST);
	
	Form retest = formService.getFormByUuid(Utils.HTS_CONFIRMATORY_TEST);
	
	PersonAttributeType phoneNumberAttrType = personService.getPersonAttributeTypeByUuid(AfyaStatMetadata.TELEPHONE_CONTACT);
	
	static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	
	private Integer locationId = Context.getService(KenyaEmrService.class).getDefaultLocation().getLocationId();
	
	private final Log log = LogFactory.getLog(MedicDataExchange.class);
	
	/**
	 * processes results from cht *
	 * 
	 * @param resultPayload this should be an object
	 * @return
	 */
	public String processIncomingFormData(String resultPayload) {
		Integer statusCode;
		String statusMsg;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonNode = null;
		try {
			jsonNode = (ObjectNode) mapper.readTree(resultPayload);
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (jsonNode != null) {
			
			ObjectNode formNode = processFormPayload(jsonNode);
			String documentUUID = formNode.get("documentUUID").getTextValue();
			Long dateFormFilled = formNode.get("dateFormFilled").getLongValue();
			String formDataUuid = formNode.path("encounter").path("encounter.form_uuid").getTextValue();
			String patientUuid = formNode.path("patient").path("patient.uuid").getTextValue();
			
			if (Utils.afyastatFormAlreadyExists(documentUUID, formDataUuid, dateFormFilled, patientUuid)) {
				System.out.println("Afyastat attempted to send a duplicate record with uuid = " + documentUUID
				        + ". The payload will be ignored");
				return "Afyastat sent a duplicate form to KenyaEMR. This has been ignored";
			}
			String payload = formNode.toString();
			String discriminator = formNode.path("discriminator").path("discriminator").getTextValue();
			String clientName = getClientName(formNode);
			Integer locationId = Integer.parseInt(formNode.path("encounter").path("encounter.location_id").getTextValue());
			String providerString = formNode.path("encounter").path("encounter.provider_id").getTextValue();
			String userName = formNode.path("encounter").path("encounter.user_system_id").getTextValue();
			saveMedicDataQueue(payload, locationId, providerString, patientUuid, discriminator, formDataUuid, userName,
			    documentUUID, dateFormFilled, clientName);
		}
		return "Data queue form created successfully";
	}
	
	/**
	 * Process registration payload from Afyastat - handles universal_client contact type
	 * 
	 * @param resultPayload
	 * @return
	 */
	
	public String processIncomingRegistration(String resultPayload) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonNode = null;
		try {
			jsonNode = (ObjectNode) mapper.readTree(resultPayload);
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		// Afyastat has a tendency of pushing multiple registrations.
		// first check that a document _id is not in the queue data before processing
		
		if (jsonNode != null) {
			ObjectNode regNode = (ObjectNode) jsonNode.get("registration");
			String documentId = regNode.get("_id").getTextValue();
			Long dateFormFilled = regNode.get("reported_date").getLongValue();
			
			if (Utils.afyastatFormAlreadyExists(documentId, "", dateFormFilled, "")) {
				System.out.println("Afyastat attempted to send a duplicate record with uuid = " + documentId
				        + ". The payload will be ignored");
				return "Afyastat sent a duplicate registration to KenyaEMR. This has been ignored";
			} else {
				
				ObjectNode outputNode = processRegistrationPayload(jsonNode, resultPayload);
				
				ObjectNode registrationNode = (ObjectNode) outputNode.get("clientRegistration");
				ObjectNode relationshipNode = outputNode.has("relationship") ? (ObjectNode) outputNode.get("relationship")
				        : null;
				ObjectNode patientContactNode = outputNode.has("patientContact") ? (ObjectNode) outputNode
				        .get("patientContact") : null;
				
				String payload = registrationNode.toString();
				String discriminator = registrationNode.path("discriminator").path("discriminator").getTextValue();
				String formDataUuid = registrationNode.path("encounter").path("encounter.form_uuid").getTextValue();
				String patientUuid = registrationNode.path("patient").path("patient.uuid").getTextValue();
				String clientName = getClientName(registrationNode);
				Integer locationId = Integer.parseInt(registrationNode.path("encounter").path("encounter.location_id")
				        .getTextValue());
				String providerString = registrationNode.path("encounter").path("encounter.provider_id").getTextValue();
				String userName = registrationNode.path("encounter").path("encounter.user_system_id").getTextValue();
				
				// add registration queue data
				saveMedicDataQueue(payload, locationId, providerString, patientUuid, discriminator, formDataUuid, userName,
				    patientUuid, dateFormFilled, clientName);
				if (relationshipNode != null) {
					String relationshipDiscriminator = "json-relationship";
					saveMedicDataQueue(relationshipNode.toString(), locationId, providerString, patientUuid,
					    relationshipDiscriminator, "", userName);
				}
				
				if (patientContactNode != null) {
					String patientContactDiscriminator = "json-createpatientcontactusingrelatioship";
					saveMedicDataQueue(patientContactNode.toString(), locationId, providerString, patientUuid,
					    patientContactDiscriminator, "", userName);
					
				}
			}
		}
		return "Data queue registration created successfully";
	}
	
	/**
	 * Gets the client name from sent data
	 * 
	 * @param registrationNode the registration node
	 * @param payload the payload data
	 * @return the client name
	 */
	private String getClientName(ObjectNode registrationNode) {
		String clientName = "";
		String givenName = registrationNode.path("patient").path("patient.given_name").getTextValue();
		clientName += (givenName == null) ? "" : givenName;
		String familyName = registrationNode.path("patient").path("patient.family_name").getTextValue();
		clientName += (familyName == null) ? "" : (" " + familyName);
		String middleName = "";
		try {
			middleName = registrationNode.path("patient").path("patient.middle_name").getTextValue();
			clientName += (middleName == null) ? "" : (" " + middleName);
		}
		catch (Exception e) {
			log.error(e);
		}
		clientName = clientName.trim();
		return (clientName);
	}
	
	public String processDemographicsUpdate(String resultPayload) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonNode = null;
		try {
			jsonNode = (ObjectNode) mapper.readTree(resultPayload);
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (jsonNode != null) {
			ObjectNode demographicUpdateNode = processDemographicUpdatePayload(jsonNode);
			String payload = demographicUpdateNode.toString();
			String discriminator = demographicUpdateNode.path("discriminator").path("discriminator").getTextValue();
			String formDataUuid = demographicUpdateNode.path("encounter").path("encounter.form_uuid").getTextValue();
			String patientUuid = demographicUpdateNode.path("patient").path("patient.uuid").getTextValue();
			String clientName = getClientName(demographicUpdateNode);
			Integer locationId = Integer.parseInt(demographicUpdateNode.path("encounter").path("encounter.location_id")
			        .getTextValue());
			String providerString = demographicUpdateNode.path("encounter").path("encounter.provider_id").getTextValue();
			String userName = demographicUpdateNode.path("encounter").path("encounter.user_system_id").getTextValue();
			Long dateFormFilled = demographicUpdateNode.path("demographicsupdate").path("dateFormFilled").getLongValue();
			
			saveMedicDataQueue(payload, locationId, providerString, patientUuid, discriminator, formDataUuid, userName,
			    patientUuid, dateFormFilled, clientName);
		}
		return "Data queue demographics updates created successfully";
	}
	
	public String processPeerCalenderFormData(String resultPayload) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonNode = null;
		try {
			jsonNode = (ObjectNode) mapper.readTree(resultPayload);
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (jsonNode != null) {
			
			ObjectNode formNode = processPeerCalenderPayload(jsonNode);
			String payload = formNode.toString();
			String discriminator = formNode.path("discriminator").path("discriminator").getTextValue();
			String formDataUuid = formNode.path("encounter").path("encounter.form_uuid").getTextValue();
			String patientUuid = formNode.path("patient").path("patient.uuid").getTextValue();
			Integer locationId = Integer.parseInt(formNode.path("encounter").path("encounter.location_id").getTextValue());
			String providerString = formNode.path("encounter").path("encounter.provider_id").getTextValue();
			String userName = formNode.path("encounter").path("encounter.user_system_id").getTextValue();
			saveMedicDataQueue(payload, locationId, providerString, patientUuid, discriminator, formDataUuid, userName);
		}
		return "Data queue form created successfully";
	}
	
	private void saveMedicDataQueue(String payload, Integer locationId, String providerString, String patientUuid,
	        String discriminator, String formUuid, String userString) {
		AfyaDataSource dataSource = dataService.getDataSource(1);
		Provider provider = Context.getProviderService().getProviderByIdentifier(providerString);
		User user = Context.getUserService().getUserByUsername(userString);
		Location location = Context.getLocationService().getLocation(locationId);
		Form form = Context.getFormService().getFormByUuid(formUuid);
		
		AfyaStatQueueData afyaStatQueueData = new AfyaStatQueueData();
		if (form != null && form.getName() != null) {
			afyaStatQueueData.setFormName(form.getName());
		} else {
			afyaStatQueueData.setFormName("Unknown name");
		}
		afyaStatQueueData.setPayload(payload);
		afyaStatQueueData.setDiscriminator(discriminator);
		afyaStatQueueData.setPatientUuid(patientUuid);
		afyaStatQueueData.setFormDataUuid(formUuid);
		afyaStatQueueData.setProvider(provider);
		afyaStatQueueData.setLocation(location);
		afyaStatQueueData.setDataSource(dataSource);
		afyaStatQueueData.setCreator(user);
		afyastatService.saveQueData(afyaStatQueueData);
	}
	
	private void saveMedicDataQueue(String payload, Integer locationId, String providerString, String patientUuid,
	        String discriminator, String formUuid, String userString, String clientName) {
		AfyaDataSource dataSource = dataService.getDataSource(1);
		Provider provider = Context.getProviderService().getProviderByIdentifier(providerString);
		User user = Context.getUserService().getUserByUsername(userString);
		Location location = Context.getLocationService().getLocation(locationId);
		Form form = Context.getFormService().getFormByUuid(formUuid);
		
		AfyaStatQueueData afyaStatQueueData = new AfyaStatQueueData();
		if (form != null && form.getName() != null) {
			afyaStatQueueData.setFormName(form.getName());
		} else {
			afyaStatQueueData.setFormName("Unknown name");
		}
		afyaStatQueueData.setPayload(payload);
		afyaStatQueueData.setDiscriminator(discriminator);
		afyaStatQueueData.setPatientUuid(patientUuid);
		afyaStatQueueData.setFormDataUuid(formUuid);
		afyaStatQueueData.setClientName(clientName);
		afyaStatQueueData.setProvider(provider);
		afyaStatQueueData.setLocation(location);
		afyaStatQueueData.setDataSource(dataSource);
		afyaStatQueueData.setCreator(user);
		afyastatService.saveQueData(afyaStatQueueData);
	}
	
	private void saveMedicDataQueue(String payload, Integer locationId, String providerString, String patientUuid,
	        String discriminator, String formUuid, String userString, String queueUUID, Long dateFormFilled,
	        String clientName) {
		AfyaDataSource dataSource = dataService.getDataSource(1);
		Provider provider = Context.getProviderService().getProviderByIdentifier(providerString);
		User user = Context.getUserService().getUserByUsername(userString);
		Location location = Context.getLocationService().getLocation(locationId);
		Form form = Context.getFormService().getFormByUuid(formUuid);
		
		if (Utils.afyastatFormAlreadyExists(queueUUID, formUuid, dateFormFilled, patientUuid)
		        && !discriminator.equalsIgnoreCase("json-demographics-update")) {
			System.out.println("Afyastat attempted to send a duplicate record with uuid = " + queueUUID
			        + ". The payload will be ignored");
			return;
		}
		AfyaStatQueueData afyaStatQueueData = new AfyaStatQueueData();
		if (form != null && form.getName() != null) {
			afyaStatQueueData.setFormName(form.getName());
		} else {
			afyaStatQueueData.setFormName("Unknown name");
		}
		afyaStatQueueData.setUuid(queueUUID);
		afyaStatQueueData.setDateFormFilled(dateFormFilled);
		afyaStatQueueData.setPayload(payload);
		afyaStatQueueData.setDiscriminator(discriminator);
		afyaStatQueueData.setPatientUuid(patientUuid);
		afyaStatQueueData.setClientName(clientName);
		afyaStatQueueData.setFormDataUuid(formUuid);
		afyaStatQueueData.setProvider(provider);
		afyaStatQueueData.setLocation(location);
		afyaStatQueueData.setDataSource(dataSource);
		afyaStatQueueData.setCreator(user);
		afyastatService.saveQueData(afyaStatQueueData);
	}
	
	/**
	 * Extracts registration data from Afyastat payload into a registration json and queues it for
	 * processing
	 * 
	 * @param jNode
	 * @param regPayload
	 * @return
	 */
	
	private ObjectNode processRegistrationPayload(ObjectNode jNode, String regPayload) {
		
		ObjectNode jsonNode = (ObjectNode) jNode.get("registration");
		ObjectNode outputNode = getJsonNodeFactory().objectNode();
		ObjectNode patientNode = getJsonNodeFactory().objectNode();
		ObjectNode obs = getJsonNodeFactory().objectNode();
		ObjectNode tmp = getJsonNodeFactory().objectNode();
		ObjectNode discriminator = getJsonNodeFactory().objectNode();
		ObjectNode encounter = getJsonNodeFactory().objectNode();
		ObjectNode identifier = getJsonNodeFactory().objectNode();
		ObjectNode registrationWrapper = getJsonNodeFactory().objectNode();
		Date date = new Date();
		
		String patientDobKnown = jsonNode.get("patient_dobKnown") != null ? jsonNode.get("patient_dobKnown").getTextValue()
		        : "";
		String dateOfBirth = null;
		if (patientDobKnown != null && patientDobKnown.equalsIgnoreCase("_1066_No_99DCT")
		        && jsonNode.get("patient_birthDate") != null) {
			dateOfBirth = jsonNode.get("patient_birthDate").getTextValue();
			patientNode.put("patient.birthdate_estimated", "true");
			patientNode.put("patient.birth_date", formatStringDate(dateOfBirth));
			
		}
		
		if (patientDobKnown != null && patientDobKnown.equalsIgnoreCase("_1065_Yes_99DCT")
		        && jsonNode.get("patient_dateOfBirth") != null) {
			dateOfBirth = jsonNode.get("patient_dateOfBirth").getTextValue();
			patientNode.put("patient.birth_date", formatStringDate(dateOfBirth));
		}
		String patientGender = jsonNode.get("patient_sex") != null ? jsonNode.get("patient_sex").getTextValue() : "";
		
		patientNode.put("patient.uuid", jsonNode.get("_id") != null ? jsonNode.get("_id").getTextValue() : "");
		patientNode.put("patient.family_name",
		    jsonNode.get("patient_familyName") != null ? jsonNode.get("patient_familyName").getTextValue() : "");
		patientNode.put("patient.given_name", jsonNode.get("patient_firstName") != null ? jsonNode.get("patient_firstName")
		        .getTextValue() : "");
		patientNode.put("patient.middle_name",
		    jsonNode.get("patient_middleName") != null ? jsonNode.get("patient_middleName").getTextValue() : "");
		patientNode.put("patient.sex", gender(patientGender));
		patientNode.put("patient.county", jsonNode.get("patient_county") != null ? jsonNode.get("patient_county")
		        .getTextValue() : "");
		patientNode.put("patient.sub_county", jsonNode.get("patient_subcounty") != null ? jsonNode.get("patient_subcounty")
		        .getTextValue() : "");
		patientNode.put("patient.ward", jsonNode.get("patient_ward") != null ? jsonNode.get("patient_ward").getTextValue()
		        : "");
		patientNode.put("patient.sub_location",
		    jsonNode.get("patient_sublocation") != null ? jsonNode.get("patient_sublocation").getTextValue() : "");
		patientNode.put("patient.location", jsonNode.get("patient_location") != null ? jsonNode.get("patient_location")
		        .getTextValue() : "");
		patientNode.put("patient.village", jsonNode.get("patient_village") != null ? jsonNode.get("patient_village")
		        .getTextValue() : "");
		patientNode.put("patient.landmark", jsonNode.get("patient_landmark") != null ? jsonNode.get("patient_landmark")
		        .getTextValue() : "");
		patientNode.put("patient.phone_number", jsonNode.get("patient_telephone") != null ? jsonNode
		        .get("patient_telephone").getTextValue() : "");
		patientNode.put("patient.alternate_phone_contact",
		    jsonNode.get("patient_alternatePhone") != null ? jsonNode.get("patient_alternatePhone").getTextValue() : "");
		patientNode.put("patient.postal_address",
		    jsonNode.get("patient_postalAddress") != null ? jsonNode.get("patient_postalAddress").getTextValue() : "");
		patientNode.put("patient.email_address",
		    jsonNode.get("patient_emailAddress") != null ? jsonNode.get("patient_emailAddress").getTextValue() : "");
		patientNode.put("patient.nearest_health_center",
		    jsonNode.get("patient_nearesthealthcentre") != null ? jsonNode.get("patient_nearesthealthcentre").getTextValue()
		            : "");
		patientNode.put("patient.next_of_kin_name",
		    jsonNode.get("patient_nextofkin") != null ? jsonNode.get("patient_nextofkin").getTextValue() : "");
		patientNode.put("patient.next_of_kin_relationship", jsonNode.get("patient_nextofkinRelationship") != null ? jsonNode
		        .get("patient_nextofkinRelationship").getTextValue() : "");
		patientNode.put("patient.next_of_kin_contact",
		    jsonNode.get("patient_nextOfKinPhonenumber") != null ? jsonNode.get("patient_nextOfKinPhonenumber")
		            .getTextValue() : "");
		patientNode.put("patient.next_of_kin_address", jsonNode.get("patient_nextOfKinPostaladdress") != null ? jsonNode
		        .get("patient_nextOfKinPostaladdress").getTextValue() : "");
		patientNode.put("patient.otheridentifier", getIdentifierTypes(jsonNode));
		
		obs.put(
		    "1054^CIVIL STATUS^99DCT",
		    jsonNode.get("patient_marital_status") != null
		            && !jsonNode.get("patient_marital_status").getTextValue().equalsIgnoreCase("") ? jsonNode
		            .get("patient_marital_status").getTextValue().replace("_", "^").substring(1) : "");
		obs.put(
		    "1542^OCCUPATION^99DCT",
		    jsonNode.get("patient_occupation") != null
		            && !jsonNode.get("patient_occupation").getTextValue().equalsIgnoreCase("") ? jsonNode
		            .get("patient_occupation").getTextValue().replace("_", "^").substring(1) : "");
		obs.put(
		    "1712^HIGHEST EDUCATION LEVEL^99DCT",
		    jsonNode.get("patient_education_level") != null
		            && !jsonNode.get("patient_education_level").getTextValue().equalsIgnoreCase("") ? jsonNode
		            .get("patient_education_level").getTextValue().replace("_", "^").substring(1) : "");
		
		tmp.put("tmp.birthdate_type", "age");
		tmp.put("tmp.age_in_years", jsonNode.get("patient_ageYears") != null ? jsonNode.get("patient_ageYears")
		        .getTextValue() : "");
		discriminator.put("discriminator", "json-registration");
		String creator = jsonNode.path("meta").path("created_by") != null ? jsonNode.path("meta").path("created_by")
		        .getTextValue() : "";
		String providerId = checkProviderNameExists(creator);
		String systemId = confirmUserNameExists(creator);
		Long longDate = jsonNode.get("reported_date") != null ? jsonNode.get("reported_date").getLongValue() : date
		        .getTime();
		
		encounter.put("encounter.location_id", locationId != null ? locationId.toString() : null);
		encounter.put("encounter.provider_id_select", providerId != null ? providerId : " ");
		encounter.put("encounter.provider_id", providerId != null ? providerId : " ");
		encounter.put("encounter.encounter_datetime", convertTime(longDate));
		encounter.put("encounter.form_uuid", "8898c6e1-5df1-409f-b8ed-c88e6e0f24e9");
		encounter.put("encounter.user_system_id", systemId);
		encounter.put("encounter.device_time_zone", "Africa\\/Nairobi");
		encounter.put("encounter.setup_config_uuid", "2107eab5-5b3a-4de8-9e02-9d97bce635d2");
		
		registrationWrapper.put("patient", patientNode);
		registrationWrapper.put("observation", obs);
		registrationWrapper.put("tmp", tmp);
		registrationWrapper.put("discriminator", discriminator);
		registrationWrapper.put("encounter", encounter);
		
		outputNode.put("clientRegistration", registrationWrapper);
		
		//TODO: investigate why this block is duplicating rows.
		// The duplicates are due to duplicate data pushed from afyastat
		if (jsonNode.get("relation_uuid") != null && !jsonNode.get("relation_uuid").getTextValue().equalsIgnoreCase("")) {
			// we want to establish relationship
			String patientRelatedTo = jsonNode.get("relation_uuid").getTextValue();
			String relationshipTypeName = jsonNode.get("relation_type") != null ? jsonNode.get("relation_type")
			        .getTextValue() : "";
			String relationshipUuid = UUID.randomUUID().toString();
			String patientUuid = jsonNode.get("_id") != null ? jsonNode.get("_id").getTextValue() : "";
			ObjectNode relationshipPayload = generateRelationshipPayload(patientRelatedTo, relationshipTypeName,
			    relationshipUuid, providerId, systemId, patientUuid);
			outputNode.put("relationship", relationshipPayload);
			outputNode.put("patientContact", jsonNode);
		}
		return outputNode;
	}
	
	/**
	 * Process encounter data payload
	 * 
	 * @param jNode
	 * @return
	 */
	private ObjectNode processFormPayload(ObjectNode jNode) {
		ObjectNode jsonNode = (ObjectNode) jNode.get("encData");
		ObjectNode formsNode = JsonNodeFactory.instance.objectNode();
		ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
		ObjectNode encounter = JsonNodeFactory.instance.objectNode();
		ObjectNode patientNode = JsonNodeFactory.instance.objectNode();
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obsNodes = null;
		ObjectNode jsonNodes = null;
		String json = null;
		try {
			jsonNodes = (ObjectNode) mapper.readTree(jsonNode.path("fields").path("observation").toString());
			json = new ObjectMapper().writeValueAsString(jsonNodes);
			if (json != null) {
				obsNodes = (ObjectNode) mapper.readTree(json.replace("_", "^"));
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		String documentUUID = jsonNode.get("_id") != null ? jsonNode.get("_id").getTextValue() : "";
		Long dateFormFilled = (Long) (jsonNode.get("reported_date") != null ? jsonNode.get("reported_date").getLongValue()
		        : "");
		String encounterDate = jsonNode.path("fields").path("encounter_date").getTextValue() != null
		        && !jsonNode.path("fields").path("encounter_date").getTextValue().equalsIgnoreCase("") ? formatStringDate(jsonNode
		        .path("fields").path("encounter_date").getTextValue())
		        : convertTime(jsonNode.get("reported_date").getLongValue());
		String creator = jsonNode.path("fields").path("audit_trail").path("created_by") != null
		        && jsonNode.path("fields").path("audit_trail").path("created_by").getTextValue() != null ? jsonNode
		        .path("fields").path("audit_trail").path("created_by").getTextValue() : "";
		String providerIdentifier = checkProviderNameExists(creator);
		String systemId = confirmUserNameExists(creator);
		discriminator.put("discriminator", "json-encounter");
		encounter.put("encounter.location_id", locationId != null ? locationId.toString() : null);
		encounter.put("encounter.provider_id_select", providerIdentifier != null ? providerIdentifier : " ");
		encounter.put("encounter.provider_id", providerIdentifier != null ? providerIdentifier : " ");
		encounter.put("encounter.encounter_datetime", encounterDate);
		encounter.put("encounter.form_uuid", jsonNode.path("fields").path("form_uuid").getTextValue());
		encounter.put("encounter.user_system_id", systemId);
		encounter.put("encounter.device_time_zone", "Africa\\/Nairobi");
		encounter.put("encounter.setup_config_uuid", jsonNode.path("fields").path("encounter_type_uuid").getTextValue());
		String kemrUuid = jsonNode.path("fields").path("inputs").path("contact").path("kemr_uuid").getTextValue();
		
		patientNode.put("patient.uuid", StringUtils.isNotBlank(kemrUuid) ? kemrUuid : jsonNode.path("fields").path("inputs")
		        .path("contact").path("_id").getTextValue());
		
		// Get the patient name data from payload
		patientNode.put("patient.given_name", jsonNode.path("place").path("patient_firstName").getTextValue());
		patientNode.put("patient.middle_name", jsonNode.path("place").path("patient_middleName").getTextValue());
		patientNode.put("patient.family_name", jsonNode.path("place").path("patient_familyName").getTextValue());
		patientNode.put("patient.name", jsonNode.path("place").path("patient_name").getTextValue());
		
		List<String> keysToRemove = new ArrayList<String>();
		ObjectNode jsonObsNodes = null;
		ObjectNode obsGroupNode = null;
		String jsonObsGroup = null;
		if (obsNodes != null) {
			Iterator<Map.Entry<String, JsonNode>> iterator = obsNodes.getFields();
			while (iterator.hasNext()) {
				Map.Entry<String, JsonNode> entry = iterator.next();
				
				if (entry.getValue() == null || "".equals(entry.getValue().toString())) {
					keysToRemove.add(entry.getKey());
				}
				
				if (entry.getKey().contains("MULTISELECT")) {
					if (entry.getValue() != null && !"".equals(entry.getValue().toString())
					        && !"".equals(entry.getValue().toString())) {
						obsNodes.put(entry.getKey(), handleMultiSelectFields(entry.getValue().toString().replace(" ", ",")));
					} else {
						keysToRemove.add(entry.getKey());
					}
				}
				
				String[] conceptElements = org.apache.commons.lang.StringUtils.split(entry.getKey(), "\\^");
				if (!StringUtils.isNumeric(conceptElements[0])) // skip if the element is not a concept id
					continue;
				int conceptId = Integer.parseInt(conceptElements[0]);
				Concept concept = Context.getConceptService().getConcept(conceptId);
				
				if (concept == null) {
					log.info("Unable to find Concept for Question with ID:: " + conceptId);
					
				} else {
					
					if (concept.isSet()) {
						
						try {
							if (entry.getValue().isObject()) {
								jsonObsNodes = (ObjectNode) mapper.readTree(entry.getValue().toString());
								jsonObsGroup = new ObjectMapper().writeValueAsString(jsonObsNodes);
								if (jsonObsGroup != null) {
									obsGroupNode = (ObjectNode) mapper.readTree(jsonObsGroup);
								}
							}
						}
						catch (IOException e) {
							e.printStackTrace();
						}
						
						if (obsGroupNode != null) {
							Iterator<Map.Entry<String, JsonNode>> obsGroupIterator = obsGroupNode.getFields();
							while (obsGroupIterator.hasNext()) {
								Map.Entry<String, JsonNode> obsGroupEntry = obsGroupIterator.next();
								
								if (obsGroupEntry.getKey().contains("MULTISELECT")) {
									if (obsGroupEntry.getValue() != null && !"".equals(obsGroupEntry.getValue().toString())
									        && !"".equals(obsGroupEntry.getValue().toString())) {
										obsGroupNode.put(obsGroupEntry.getKey(), handleMultiSelectFields(obsGroupEntry
										        .getValue().toString().replace(" ", ",")));
										obsNodes.put(entry.getKey(), obsGroupNode);
									}
								}
							}
						}
						
					}
					
				}
				
			}
		}
		
		if (keysToRemove.size() > 0) {
			for (String key : keysToRemove) {
				obsNodes.remove(key);
			}
		}
		formsNode.put("patient", patientNode);
		formsNode.put("observation", obsNodes);
		formsNode.put("discriminator", discriminator);
		formsNode.put("encounter", encounter);
		formsNode.put("documentUUID", documentUUID);
		formsNode.put("dateFormFilled", dateFormFilled);
		
		return formsNode;
	}
	
	/**
	 * Process peer calendar form payload
	 * 
	 * @param jNode
	 * @return
	 */
	private ObjectNode processPeerCalenderPayload(ObjectNode jNode) {
		ObjectNode jsonNode = (ObjectNode) jNode.get("peerCalendarData");
		ObjectNode formsNode = JsonNodeFactory.instance.objectNode();
		ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
		ObjectNode encounter = JsonNodeFactory.instance.objectNode();
		ObjectNode patientNode = JsonNodeFactory.instance.objectNode();
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obsNodes = null;
		ObjectNode jsonNodes = null;
		String json = null;
		try {
			jsonNodes = (ObjectNode) mapper.readTree(jsonNode.path("fields").path("observation").toString());
			json = new ObjectMapper().writeValueAsString(jsonNodes);
			if (json != null) {
				obsNodes = (ObjectNode) mapper.readTree(json.replace("_", "^"));
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		String encounterDate = jsonNode.path("fields").path("encounter_date").getTextValue() != null
		        && !jsonNode.path("fields").path("encounter_date").getTextValue().equalsIgnoreCase("") ? formatStringDate(jsonNode
		        .path("fields").path("encounter_date").getTextValue())
		        : convertTime(jsonNode.get("reported_date").getLongValue());
		String creator = jsonNode.path("fields").path("audit_trail").path("created_by") != null
		        && jsonNode.path("fields").path("audit_trail").path("created_by").getTextValue() != null ? jsonNode
		        .path("fields").path("audit_trail").path("created_by").getTextValue() : "";
		String providerIdentifier = checkProviderNameExists(creator);
		String systemId = confirmUserNameExists(creator);
		discriminator.put("discriminator", "json-peerCalendar");
		encounter.put("encounter.location_id", locationId != null ? locationId.toString() : null);
		encounter.put("encounter.provider_id_select", providerIdentifier != null ? providerIdentifier : " ");
		encounter.put("encounter.provider_id", providerIdentifier != null ? providerIdentifier : " ");
		encounter.put("encounter.encounter_datetime", encounterDate);
		encounter.put("encounter.form_uuid", jsonNode.path("fields").path("form_uuid").getTextValue());
		encounter.put("encounter.user_system_id", systemId);
		encounter.put("encounter.device_time_zone", "Africa\\/Nairobi");
		encounter.put("encounter.setup_config_uuid", jsonNode.path("fields").path("encounter_type_uuid").getTextValue());
		String kemrUuid = jsonNode.path("fields").path("inputs").path("contact").path("kemr_uuid").getTextValue();
		
		patientNode.put("patient.uuid", StringUtils.isNotBlank(kemrUuid) ? kemrUuid : jsonNode.path("fields").path("inputs")
		        .path("contact").path("_id").getTextValue());
		
		List<String> keysToRemove = new ArrayList<String>();
		ObjectNode jsonObsNodes = null;
		ObjectNode obsGroupNode = null;
		String jsonObsGroup = null;
		if (obsNodes != null) {
			
			Iterator<Map.Entry<String, JsonNode>> iterator = obsNodes.getFields();
			while (iterator.hasNext()) {
				Map.Entry<String, JsonNode> entry = iterator.next();
				
				if (entry.getValue() == null || "".equals(entry.getValue().toString())) {
					keysToRemove.add(entry.getKey());
				}
				
				if (entry.getKey().contains("MULTISELECT")) {
					if (entry.getValue() != null && !"".equals(entry.getValue().toString())
					        && !"".equals(entry.getValue().toString())) {
						obsNodes.put(entry.getKey(), handleMultiSelectFields(entry.getValue().toString().replace(" ", ",")));
					} else {
						keysToRemove.add(entry.getKey());
					}
				}
				
				String[] conceptElements = org.apache.commons.lang.StringUtils.split(entry.getKey(), "\\^");
				if (!StringUtils.isNumeric(conceptElements[0])) // skip if the element is not a concept id
					continue;
				int conceptId = Integer.parseInt(conceptElements[0]);
				Concept concept = Context.getConceptService().getConcept(conceptId);
				
				if (concept == null) {
					log.info("Unable to find Concept for Question with ID:: " + conceptId);
					
				} else {
					
					if (concept.isSet()) {
						
						try {
							if (entry.getValue().isObject()) {
								jsonObsNodes = (ObjectNode) mapper.readTree(entry.getValue().toString());
								jsonObsGroup = new ObjectMapper().writeValueAsString(jsonObsNodes);
								if (jsonObsGroup != null) {
									obsGroupNode = (ObjectNode) mapper.readTree(jsonObsGroup);
								}
							}
						}
						catch (IOException e) {
							e.printStackTrace();
						}
						
						if (obsGroupNode != null) {
							Iterator<Map.Entry<String, JsonNode>> obsGroupIterator = obsGroupNode.getFields();
							while (obsGroupIterator.hasNext()) {
								Map.Entry<String, JsonNode> obsGroupEntry = obsGroupIterator.next();
								
								if (obsGroupEntry.getKey().contains("MULTISELECT")) {
									if (obsGroupEntry.getValue() != null && !"".equals(obsGroupEntry.getValue().toString())
									        && !"".equals(obsGroupEntry.getValue().toString())) {
										obsGroupNode.put(obsGroupEntry.getKey(), handleMultiSelectFields(obsGroupEntry
										        .getValue().toString().replace(" ", ",")));
										obsNodes.put(entry.getKey(), obsGroupNode);
									}
								}
							}
						}
						
					}
					
				}
				
			}
		}
		
		if (keysToRemove.size() > 0) {
			for (String key : keysToRemove) {
				obsNodes.remove(key);
			}
		}
		formsNode.put("patient", patientNode);
		formsNode.put("observation", obsNodes);
		formsNode.put("discriminator", discriminator);
		formsNode.put("encounter", encounter);
		return formsNode;
	}
	
	/**
	 * Add contact list to queue data
	 * 
	 * @param resultPayload
	 * @return
	 */
	public String addContactListToDataqueue(String resultPayload) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonNode = null;
		ObjectNode parentPatient = null;
		try {
			jsonNode = (ObjectNode) mapper.readTree(resultPayload);
			jsonNode = (ObjectNode) jsonNode.get("formData");
			parentPatient = (ObjectNode) jsonNode.get("parent");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (jsonNode != null) {
			String payload = jsonNode.toString();
			String discriminator = "json-patientcontact";
			String patientContactUuid = jsonNode.get("_id") != null ? jsonNode.get("_id").getTextValue() : "";
			Integer locationId = Context.getService(KenyaEmrService.class).getDefaultLocation().getLocationId();
			String creator = jsonNode.path("meta").path("created_by") != null ? jsonNode.path("meta").path("created_by")
			        .getTextValue() : "";
			String providerString = checkProviderNameExists(creator);
			String userName = confirmUserNameExists(creator);
			
			// Get the patient name from the payload
			String firstName = jsonNode.get("f_name") != null ? jsonNode.get("f_name").getTextValue() : "";
			String middleName = jsonNode.get("o_name") != null ? jsonNode.get("o_name").getTextValue() : "";
			String lastName = jsonNode.get("s_name") != null ? jsonNode.get("s_name").getTextValue() : "";
			String name = jsonNode.get("patient_name") != null ? jsonNode.get("patient_name").getTextValue() : "";
			String parentPatientFirstName = jsonNode.get("patient_firstName") != null ? jsonNode.get("patient_firstName")
			        .getTextValue() : "";
			String parentPatientFamilyName = jsonNode.get("patient_familyName") != null ? jsonNode.get("patient_familyName")
			        .getTextValue() : "";
			String parentPatientMiddleName = jsonNode.get("patient_middleName") != null ? jsonNode.get("patient_middleName")
			        .getTextValue() : "";
			String parentPatientName = jsonNode.get("patient_name") != null ? jsonNode.get("patient_name").getTextValue()
			        : "";
			
			saveMedicDataQueue(payload, locationId, providerString, patientContactUuid, discriminator, "", userName, name);
		}
		return "Queue data for contact created successfully";
	}
	
	// add relationship
	public ObjectNode generateRelationshipPayload(String patientRelatedTo, String relationshipTypeName,
	        String relationshipUuid, String providerId, String systemId, String patientUuid) {
		
		ObjectNode encounter = JsonNodeFactory.instance.objectNode();
		ObjectNode relationshipType = JsonNodeFactory.instance.objectNode();
		ObjectNode relationUuid = JsonNodeFactory.instance.objectNode();
		ObjectNode formNode = JsonNodeFactory.instance.objectNode();
		
		relationshipType.put("uuid", relationshipTypeConverter(relationshipTypeName));
		relationUuid.put("uuid", relationshipUuid);
		encounter.put("encounter.provider_id_select", providerId != null ? providerId : " ");
		encounter.put("encounter.provider_id", providerId != null ? providerId : " ");
		formNode.put("encounter", encounter);
		formNode.put("relationshipType", relationshipType);
		formNode.put("relationUuid", relationUuid);
		formNode.put("uuid", patientRelatedTo);
		formNode.put("personBUuid", patientUuid);
		
		return formNode;
		/*String discriminator = "json-relationship";
		Integer locationId = Context.getService(KenyaEmrService.class).getDefaultLocation().getLocationId();
		saveMedicDataQueue(payload,locationId,providerId,patientRelatedTo,discriminator,"", systemId);

		return "Queue data for relationship created successfully";*/
	}
	
	/**
	 * Creates patientContact from registration feature in Afyastat registration
	 * 
	 * @param payload
	 * @param providerId
	 * @param systemId
	 * @param patientUuid
	 * @return
	 */
	public String createPatientContactFromRelationship(String payload, String providerId, String systemId, String patientUuid) {
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonNode = null;
		try {
			jsonNode = (ObjectNode) mapper.readTree(payload);
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		if (jsonNode != null) {
			ObjectNode registrationNode = (ObjectNode) jsonNode.get("registration");
			return registrationNode.toString();
			/*String discriminator = "json-createpatientcontactusingrelatioship";
			Integer locationId = Context.getService(KenyaEmrService.class).getDefaultLocation().getLocationId();
			saveMedicDataQueue(registrationNode.toString(),locationId,providerId,patientUuid,discriminator,"", systemId);
			return "Queue data for creating contact from relationship created successfully";*/
		}
		return "Could process contact from registration relationship";
		
	}
	
	/**
	 * Adds contact trace to the queue data for processing
	 * 
	 * @param resultPayload
	 * @return
	 */
	
	public String addContactTraceToDataqueue(String resultPayload) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonNode = null;
		try {
			jsonNode = (ObjectNode) mapper.readTree(resultPayload);
			jsonNode = (ObjectNode) jsonNode.get("traceData");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (jsonNode != null) {
			String discriminator = "json-contacttrace";
			String payload = jsonNode.toString();
			String patientContactUuid = jsonNode.get("_id") != null ? jsonNode.get("_id").getTextValue() : "";
			Integer locationId = Context.getService(KenyaEmrService.class).getDefaultLocation().getLocationId();
			String creator = jsonNode.path("fields").path("audit_trail").path("created_by") != null
			        && jsonNode.path("fields").path("audit_trail").path("created_by").getTextValue() != null ? jsonNode
			        .path("fields").path("audit_trail").path("created_by").getTextValue() : "";
			
			// Get the patient name
			String patientFirstName = jsonNode.path("place").path("s_name").getTextValue();
			String patientFamilyName = jsonNode.path("place").path("f_name").getTextValue();
			String patientMiddleName = jsonNode.path("place").path("o_name").getTextValue();
			String patientName = jsonNode.path("place").path("patient_name").getTextValue();
			String parentPatientFirstName = jsonNode.path("place").path("parent").path("patient_firstName").getTextValue();
			String parentPatientFamilyName = jsonNode.path("place").path("parent").path("patient_familyName").getTextValue();
			String parentPatientMiddleName = jsonNode.path("place").path("parent").path("patient_middleName").getTextValue();
			String parentPatientName = jsonNode.path("place").path("parent").path("patient_name").getTextValue();
			
			String providerString = checkProviderNameExists(creator);
			String userName = confirmUserNameExists(creator);
			saveMedicDataQueue(payload, locationId, providerString, patientContactUuid, discriminator, "", userName,
			    patientName);
		}
		return "Queue data for contact trace created successfully";
	}
	
	private ArrayNode handleMultiSelectFields(String listOfItems) {
		ArrayNode arrNode = JsonNodeFactory.instance.arrayNode();
		if (listOfItems != null && StringUtils.isNotBlank(listOfItems)) {
			for (String s : listOfItems.split(",")) {
				arrNode.add(s.substring(1, s.length() - 1));
			}
		}
		return arrNode;
	}
	
	private ArrayNode getIdentifierTypes(ObjectNode jsonNode) {
		ArrayNode identifierTypes = JsonNodeFactory.instance.arrayNode();
		
		String chtContactUuid = jsonNode.get("_id") != null ? jsonNode.get("_id").getTextValue() : "";
		ObjectNode assignedCHTReference = assignCHTReferenceUUID(chtContactUuid);
		identifierTypes.add(assignedCHTReference);
		Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.getFields();
		ObjectNode iden = null;
		while (iterator.hasNext()) {
			Map.Entry<String, JsonNode> entry = iterator.next();
			if (entry.getKey().contains("patient_identifierType")) {
				iden = handleMultipleIdentifiers(entry.getKey(), entry.getValue().getTextValue());
				if (iden != null && iden.size() != 0) {
					identifierTypes.add(iden);
				}
			}
		}
		return identifierTypes;
		
	}
	
	private ObjectNode handleMultipleIdentifiers(String identifierName, String identifierValue) {
		ArrayNode arrNodeName = JsonNodeFactory.instance.arrayNode();
		ObjectNode identifiers = JsonNodeFactory.instance.objectNode();
		if (identifierName != null) {
			
			for (String s : identifierName.split("_")) {
				arrNodeName.add(s);
			}
			PatientIdentifierType identifierTypeName = null;
			if (arrNodeName.get(arrNodeName.size() - 1) != null) {
				identifierTypeName = Context.getPatientService().getPatientIdentifierTypeByUuid(
				    arrNodeName.get(arrNodeName.size() - 1).getTextValue());
			}
			if (identifierTypeName != null && !identifierTypeName.getName().equalsIgnoreCase("")
			        && !identifierValue.equalsIgnoreCase("")) {
				identifiers.put("identifier_type_uuid", arrNodeName.get(arrNodeName.size() - 1));
				identifiers.put("identifier_value", identifierValue);
				identifiers.put("identifier_type_name", identifierTypeName.getName());
			}
		}
		return identifiers;
	}
	
	private ObjectNode assignCHTReferenceUUID(String chtContactUuid) {
		ObjectNode identifiers = getJsonNodeFactory().objectNode();
		identifiers.put("identifier_type_uuid", AfyaStatMetadata._PatientIdentifierType.CHT_RECORD_UUID);
		identifiers.put("identifier_value", chtContactUuid);
		identifiers.put("identifier_type_name", "CHT Record Reference UUID");
		return identifiers;
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
	
	private Integer sexConverter(String sex) {
		Integer sexConcept = null;
		if (sex.equalsIgnoreCase("male")) {
			sexConcept = 1534;
		}
		if (sex.equalsIgnoreCase("female")) {
			sexConcept = 1535;
		}
		return sexConcept;
	}
	
	private String formatStringDate(String dob) {
		String date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
			date = sdf.format(sdf2.parse(dob));
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}
	
	private String convertTime(long time) {
		Date date = new Date(time);
		return DATE_FORMAT.format(date);
	}
	
	private String checkProviderNameExists(String username) {
		String providerIdentifier = null;
		User user = null;
		if (username != null && !username.equalsIgnoreCase("")) {
			user = Context.getUserService().getUserByUsername(username);
		}
		
		Provider unknownProvider = Context.getProviderService().getAllProviders().get(0);
		User superUser = Context.getUserService().getUser(1);
		if (user != null) {
			Provider s = EmrUtils.getProvider(user);
			// check if the user is a provider
			if (s != null) {
				providerIdentifier = s.getIdentifier();
			} else {
				providerIdentifier = unknownProvider.getIdentifier();
			}
		} else {
			Provider p = EmrUtils.getProvider(superUser);
			if (p != null) {
				providerIdentifier = p.getIdentifier();
				
			} else {
				providerIdentifier = unknownProvider.getIdentifier();
			}
		}
		
		return providerIdentifier;
	}
	
	private String confirmUserNameExists(String username) {
		String systemUserName = null;
		User user = null;
		User superUser = Context.getUserService().getUser(1);
		if (username != null && !username.equalsIgnoreCase("")) {
			user = Context.getUserService().getUserByUsername(username);
		}
		if (user != null) {
			systemUserName = user.getUsername();
			
		} else {
			systemUserName = superUser.getUsername();
		}
		return systemUserName;
	}
	
	/**
	 * Get a list of contacts for tracing
	 * 
	 * @return
	 */
	public ObjectNode getContacts() {
		
		JsonNodeFactory factory = getJsonNodeFactory();
		ArrayNode patientContactNode = getJsonNodeFactory().arrayNode();
		ObjectNode responseWrapper = factory.objectNode();
		
		//AfyastatService afyastatService = Context.getService(AfyastatService.class);
		//Set<Integer> listedContacts = getListedContacts(lastContactEntry, lastContactId);
		Map<Integer, ArrayNode> contactMap = new HashMap<Integer, ArrayNode>();
		
		/**
		 * commenting this block for now 26th Aug 2020. AO
		 */
		/*if (listedContacts != null && listedContacts.size() > 0) {

		    for (Integer pc : listedContacts) {
		        PatientContact c = afyastatService.getPatientContactByID(pc);
		        Patient indexClient = c.getPatientRelatedTo();
		        ArrayNode contacts = null;

		        ObjectNode contact = factory.objectNode();

		        String sex = "";
		        String dateFormat = "yyyy-MM-dd";

		        String fullName = "";

		        if (c.getFirstName() != null) {
		            fullName += c.getFirstName();
		        }

		        if (c.getMiddleName() != null) {
		            fullName += " " + c.getMiddleName();
		        }

		        if (c.getLastName() != null) {
		            fullName += " " + c.getLastName();
		        }


		        if (c.getSex() != null) {
		            if (c.getSex().equals("M")) {
		                sex = "male";
		            } else {
		                sex = "female";
		            }
		        }
		        contact.put("role", "person");
		        contact.put("_id", c.getUuid());
		        contact.put("index_client_uuid", indexClient.getUuid());
		        contact.put("s_name",c.getLastName() != null ? c.getLastName() : "");
		        contact.put("f_name",c.getFirstName() != null ? c.getFirstName() : "");
		        contact.put("o_name",c.getMiddleName() != null ? c.getMiddleName() : "");
		        contact.put("name", fullName);
		        contact.put("sex",sex);
		        contact.put("date_of_birth", c.getBirthDate() != null ? MedicDataExchange.getSimpleDateFormat(dateFormat).format(c.getBirthDate()) : "");
		        contact.put("marital_status", c.getMaritalStatus() != null ? getMaritalStatusOptions(c.getMaritalStatus()) : "");
		        contact.put("phone", c.getPhoneContact() != null ? c.getPhoneContact() : "");
		        contact.put("physical_address", c.getPhysicalAddress() != null ? c.getPhysicalAddress() : "");
		        contact.put("contact_relationship", c.getRelationType() != null ? getContactRelation(c.getRelationType()) : "");
		        contact.put("living_with_client", c.getLivingWithPatient() != null ? getLivingWithPatientOptions(c.getLivingWithPatient()) : "");
		        contact.put("ipv_outcome", c.getIpvOutcome());
		        contact.put("baseline_hiv_status", c.getBaselineHivStatus());
		        contact.put("booking_date", c.getAppointmentDate() != null ? MedicDataExchange.getSimpleDateFormat(dateFormat).format(c.getAppointmentDate()) : "");
		        contact.put("pns_approach", c.getPnsApproach() != null ? getPreferredPNSApproachOptions(c.getPnsApproach()) : "");
		        contact.put("reported_date", c.getDateCreated().getTime());

		        if (contactMap.keySet().contains(indexClient.getPatientId())) {
		            contacts = contactMap.get(indexClient.getPatientId());
		            contacts.add(contact);
		        } else {
		            contacts = factory.arrayNode();
		            contacts.add(contact);
		            contactMap.put(indexClient.getPatientId(), contacts);
		        }
		    }

		    for (Map.Entry<Integer, ArrayNode> entry : contactMap.entrySet()) {
		        Integer clientId = entry.getKey();
		        ArrayNode contacts = entry.getValue();

		        Patient patient = Context.getPatientService().getPatient(clientId);
		        ObjectNode contactWrapper = buildPatientNode(patient);
		        contactWrapper.put("contacts", contacts);
		        patientContactNode.add(contactWrapper);

		    }
		}*/
		
		// Get registered contacts in the EMR. These will potentially have no contacts
		ArrayNode emptyContactNode = factory.arrayNode();
		DataResponseObject responseObject = getRegisteredCHTContacts();
		Set<Integer> patientList = responseObject.getPatientList();
		if (patientList.size() > 0) {
			for (Integer ptId : patientList) {
				if (!contactMap.keySet().contains(ptId)) {
					Patient patient = Context.getPatientService().getPatient(ptId);
					ObjectNode contactWrapper = buildPatientNode(patient, true, "");
					contactWrapper.put("contacts", emptyContactNode);
					patientContactNode.add(contactWrapper);
				}
			}
		}
		
		// add peers list
		patientContactNode.addAll(getKpPeerPeerEductorList());
		responseWrapper.put("docs", patientContactNode);
		responseWrapper.put("timestamp", responseObject.getTimestamp());
		return responseWrapper;
	}
	
	/**
	 * Queue contacts into the outgoing queue
	 * 
	 * @return boolean true if successful, false if unsuccessful
	 */
	public boolean queueContacts() {
		
		GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject(
		    AfyaStatMetadata.AFYASTAT_CONTACT_LIST_LAST_FETCH_TIMESTAMP);
		if (globalPropertyObject == null) {
			System.out.println("Missing required global property: "
			        + AfyaStatMetadata.AFYASTAT_CONTACT_LIST_LAST_FETCH_TIMESTAMP);
			return (false);
		}
		
		// Get registered contacts in the EMR. These will potentially have no contacts
		DataResponseObject responseObject = getRegisteredCHTContacts();
		if (responseObject == null) {
			return (false); // just notify the calling task
		}
		Set<Integer> patientList = responseObject.getPatientList();
		if (patientList.size() > 0) {
			System.out.println("AfyaStat Outgoing Registration Queueing These Contacts: " + patientList.size());
			for (Integer ptId : patientList) {
				JsonNodeFactory factory = getJsonNodeFactory();
				ArrayNode patientContactNode = getJsonNodeFactory().arrayNode();
				ArrayNode emptyContactNode = factory.arrayNode();
				ObjectNode responseWrapper = factory.objectNode();
				
				Patient patient = Context.getPatientService().getPatient(ptId);
				PatientContactListData returnData = buildPatientNodeForQueue(patient, true, "");
				ObjectNode contactWrapper = returnData.getContactWrapper();
				contactWrapper.put("contacts", emptyContactNode);
				patientContactNode.add(contactWrapper);
				
				responseWrapper.put("docs", patientContactNode);
				responseWrapper.put("timestamp", responseObject.getTimestamp());
				
				MedicOutgoingRegistrationService medicOutgoingRegistrationService = Context
				        .getService(MedicOutgoingRegistrationService.class);
				if (medicOutgoingRegistrationService.getRecordByPatientAndPurpose(ptId, returnData.getPurpose()) == null) {
					MedicOutgoingRegistration record = new MedicOutgoingRegistration();
					record.setPatientId(ptId);
					record.setChtRef(returnData.getChtRef());
					record.setKemrRef(returnData.getKemrRef());
					record.setPurpose(returnData.getPurpose());
					record.setPayload(responseWrapper.toString());
					record.setStatus(0);
					
					medicOutgoingRegistrationService.saveOrUpdate(record);
				}
			}
		} else {
			System.out.println("AfyaStat Outgoing Registration No Contacts to queue");
			return (false);
		}
		
		// save this at the end just so that we take care of instances when db is down
		globalPropertyObject.setPropertyValue(responseObject.getTimestamp());
		Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
		
		return (true);
	}
	
	/**
	 * Get a list of payloads for sending to afyastat from the queue
	 * 
	 * @return List<MedicOutgoingRegistration> the list of payloads
	 */
	public List<MedicOutgoingRegistration> getQueuedPayloads(Integer limit) {
		MedicOutgoingRegistrationService medicOutgoingRegistrationService = Context
		        .getService(MedicOutgoingRegistrationService.class);
		List<MedicOutgoingRegistration> list = medicOutgoingRegistrationService.getRecordsByStatus(0, limit);
		if (list != null) {
			return (list);
		}
		
		return (null);
	}
	
	/**
	 * After sending the contact payload to afyastat, change the status
	 * 
	 * @param recordId the queue id
	 * @param status the status to set
	 */
	public void setContactQueuePayloadStatus(Integer recordId, Integer status) {
		MedicOutgoingRegistrationService medicOutgoingRegistrationService = Context
		        .getService(MedicOutgoingRegistrationService.class);
		medicOutgoingRegistrationService.recordSetStatus(recordId, status);
	}
	
	/**
	 * Get a list of contacts for tracing
	 * 
	 * @return
	 */
	public ObjectNode getLinkageList() {
		
		JsonNodeFactory factory = getJsonNodeFactory();
		ArrayNode patientContactNode = getJsonNodeFactory().arrayNode();
		ObjectNode responseWrapper = factory.objectNode();
		
		Map<Integer, ArrayNode> contactMap = new HashMap<Integer, ArrayNode>();
		
		// Get registered contacts in the EMR. These will potentially have no contacts
		ArrayNode emptyContactNode = factory.arrayNode();
		DataResponseObject dataResponseObject = getClientsTestedPositiveNotLinked();
		Set<Integer> patientList = dataResponseObject.getPatientList();
		if (patientList.size() > 0) {
			for (Integer ptId : patientList) {
				if (!contactMap.keySet().contains(ptId)) {
					Patient patient = Context.getPatientService().getPatient(ptId);
					ObjectNode contactWrapper = buildPatientNode(patient, false, "");
					patientContactNode.add(contactWrapper);
				}
			}
		}
		
		responseWrapper.put("docs", patientContactNode);
		responseWrapper.put("timestamp", dataResponseObject.getTimestamp());
		return responseWrapper;
	}
	
	/**
	 * Queue linkage contacts into the outgoing queue
	 * 
	 * @return
	 */
	public boolean queueLinkageList() {
		
		GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject(
		    AfyaStatMetadata.AFYASTAT_LINKAGE_LIST_LAST_FETCH_TIMESTAMP);
		if (globalPropertyObject == null) {
			System.out.println("Missing required global property: "
			        + AfyaStatMetadata.AFYASTAT_LINKAGE_LIST_LAST_FETCH_TIMESTAMP);
			return (false);
		}
		
		// Get registered contacts in the EMR. These will potentially have no contacts
		DataResponseObject dataResponseObject = getClientsTestedPositiveNotLinked();
		if (dataResponseObject == null) {
			return (false); // just notify the calling task
		}
		Set<Integer> patientList = dataResponseObject.getPatientList();
		if (patientList.size() > 0) {
			System.out.println("AfyaStat Outgoing Registration Queueing Linkage Contacts: " + patientList.size());
			for (Integer ptId : patientList) {
				JsonNodeFactory factory = getJsonNodeFactory();
				ArrayNode patientContactNode = getJsonNodeFactory().arrayNode();
				ObjectNode responseWrapper = factory.objectNode();
				
				Patient patient = Context.getPatientService().getPatient(ptId);
				PatientContactListData returnData = buildPatientNodeForQueue(patient, false, "");
				ObjectNode contactWrapper = returnData.getContactWrapper();
				patientContactNode.add(contactWrapper);
				
				responseWrapper.put("docs", patientContactNode);
				responseWrapper.put("timestamp", dataResponseObject.getTimestamp());
				
				MedicOutgoingRegistrationService medicOutgoingRegistrationService = Context
				        .getService(MedicOutgoingRegistrationService.class);
				if (medicOutgoingRegistrationService.getRecordByPatientAndPurpose(ptId, returnData.getPurpose()) == null) {
					MedicOutgoingRegistration record = new MedicOutgoingRegistration();
					record.setPatientId(ptId);
					record.setChtRef(returnData.getChtRef());
					record.setKemrRef(returnData.getKemrRef());
					record.setPurpose(returnData.getPurpose());
					record.setPayload(responseWrapper.toString());
					record.setStatus(0);
					
					medicOutgoingRegistrationService.saveOrUpdate(record);
				}
			}
		} else {
			System.out.println("AfyaStat Outgoing Registration No Linkage Contacts to queue");
			return (false);
		}
		
		// save this at the end just so that we take care of instances when db is down
		globalPropertyObject.setPropertyValue(dataResponseObject.getTimestamp());
		Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
		
		return (true);
	}
	
	public ArrayNode getKpPeerPeerEductorList() {
		String PREP_PROGRAM_UUID = "214cad1c-bb62-4d8e-b927-810a046daf62";
		String KP_PROGRAM_UUID = "7447305a-18a7-11e9-ab14-d663bd873d93";
		String CHTUSERNAME_ATTRIBUTETYPE_UUID = "1aaead2d-0e88-40b2-abcd-6bc3d20fa43c";
		Program prepProgram = MetadataUtils.existing(Program.class, PREP_PROGRAM_UUID);
		Program kpProgram = MetadataUtils.existing(Program.class, KP_PROGRAM_UUID);
		Program hivProgram = MetadataUtils.existing(Program.class, HivMetadata._Program.HIV);
		ProgramWorkflowService programWorkflowService = Context.getProgramWorkflowService();
		PersonAttributeType chtPersonAttributeType = personService
		        .getPersonAttributeTypeByUuid(CHTUSERNAME_ATTRIBUTETYPE_UUID);
		
		JsonNodeFactory factory = getJsonNodeFactory();
		ArrayNode peersNode = getJsonNodeFactory().arrayNode();
		ObjectNode responseWrapper = factory.objectNode();
		
		Set<Integer> peerEducatorList = getAllPeerEducatorsForKPProgram();
		if (peerEducatorList.size() > 0) {
			
			for (Integer ptId : peerEducatorList) {
				
				ObjectNode peer = factory.objectNode();
				ObjectNode peerEducator = factory.objectNode();
				;
				
				List<PatientProgram> peerEducatorPrepPrograms = programWorkflowService.getPatientPrograms(Context
				        .getPatientService().getPatient(ptId), prepProgram, null, null, null, null, true);
				
				List<PatientProgram> peerEducatorHivPrograms = programWorkflowService.getPatientPrograms(Context
				        .getPatientService().getPatient(ptId), hivProgram, null, null, null, null, true);
				List<PatientProgram> kpPrograms = programWorkflowService.getPatientPrograms(Context.getPatientService()
				        .getPatient(ptId), kpProgram, null, null, null, null, true);
				String peerEducatorHivStatus = getClientHIVStatusCapturedOnKpClinicalEnrollment(ptId);
				Patient patient = Context.getPatientService().getPatient(ptId);
				Person p = personService.getPerson(ptId);
				
				String fullPeerEducatorName = "";
				String peerEducatorAssignee = "";
				if (p.getGivenName() != null) {
					fullPeerEducatorName += p.getGivenName();
				}
				
				if (p.getMiddleName() != null) {
					fullPeerEducatorName += " " + p.getMiddleName();
				}
				
				if (p.getFamilyName() != null) {
					fullPeerEducatorName += " " + p.getFamilyName();
				}
				
				if (p.getAttribute(chtPersonAttributeType) != null
				        && p.getAttribute(chtPersonAttributeType).getValue() != null) {
					peerEducatorAssignee = p.getAttribute(chtPersonAttributeType).getValue();
					
				}
				if (!peerEducatorAssignee.equalsIgnoreCase("")) {
					peerEducator = buildPatientNode(patient, false, peerEducatorAssignee);
					
					if (peerEducatorHivPrograms.isEmpty() && peerEducatorPrepPrograms.isEmpty()
					        && peerEducatorHivStatus.equalsIgnoreCase("NEGATIVE") && kpPrograms.size() > 0) {
						peerEducator.put("record_purpose", "prep_verification");
						peersNode.add(peerEducator);
						
					} else if (peerEducatorHivPrograms.isEmpty() && peerEducatorHivStatus.equalsIgnoreCase("POSITIVE")
					        && kpPrograms.size() > 0) {
						peerEducator.put("record_purpose", "treatment_verification");
						peersNode.add(peerEducator);
						
					} else {
						if (kpPrograms.size() > 0) {
							peerEducator.put("record_purpose", "kp_followup");
							
							if (peerEducator.size() > 0) {
								peersNode.add(peerEducator);
							}
						}
					}
				}
				
				for (Relationship relationship : Context.getPersonService().getRelationshipsByPerson(
				    Context.getPatientService().getPatient(ptId))) {
					
					if (relationship.getRelationshipType().getbIsToA().equals("Peer") && relationship.getEndDate() == null) {
						List<PatientProgram> peerPrepPrograms = programWorkflowService.getPatientPrograms(Context
						        .getPatientService().getPatient(relationship.getPersonB().getId()), prepProgram, null, null,
						    null, null, true);
						
						List<PatientProgram> peerHivPrograms = programWorkflowService.getPatientPrograms(Context
						        .getPatientService().getPatient(relationship.getPersonB().getId()), hivProgram, null, null,
						    null, null, true);
						List<PatientProgram> kpPeerPrograms = programWorkflowService.getPatientPrograms(Context
						        .getPatientService().getPatient(relationship.getPersonB().getId()), kpProgram, null, null,
						    null, null, true);
						String peerHivStatus = getClientHIVStatusCapturedOnKpClinicalEnrollment(relationship.getPersonB()
						        .getId());
						Patient peerPatient = Context.getPatientService().getPatient(relationship.getPersonB().getId());
						
						String fullName = "";
						String assignee = "";
						if (relationship.getPersonA().getGivenName() != null) {
							fullName += relationship.getPersonA().getGivenName();
						}
						
						if (relationship.getPersonA().getMiddleName() != null) {
							fullName += " " + relationship.getPersonA().getMiddleName();
						}
						
						if (relationship.getPersonA().getFamilyName() != null) {
							fullName += " " + relationship.getPersonA().getFamilyName();
						}
						if (relationship.getPersonA().getAttribute(chtPersonAttributeType) != null
						        && relationship.getPersonA().getAttribute(chtPersonAttributeType).getValue() != null) {
							assignee = relationship.getPersonA().getAttribute(chtPersonAttributeType).getValue();
							
						}
						
						if (peerPrepPrograms.isEmpty() && peerHivPrograms.isEmpty()
						        && peerHivStatus.equalsIgnoreCase("NEGATIVE") && kpPeerPrograms.size() > 0
						        && !assignee.equalsIgnoreCase("")) {
							peer = buildPatientNode(peerPatient, false, assignee);
							peer.put("record_purpose", "prep_verification");
						} else if (peerHivPrograms.isEmpty() && peerHivStatus.equalsIgnoreCase("POSITIVE")
						        && kpPeerPrograms.size() > 0 && !assignee.equalsIgnoreCase("")) {
							peer = buildPatientNode(peerPatient, false, assignee);
							peer.put("record_purpose", "treatment_verification");
						} else {
							if (kpPeerPrograms.size() > 0 && !assignee.equalsIgnoreCase("")) {
								peer = buildPatientNode(peerPatient, false, assignee);
								peer.put("record_purpose", "kp_followup");
							}
						}
						
					}
					
				}
				
				if (peer.size() > 0) {
					peersNode.add(peer);
				}
				
			}
			
		}
		
		return peersNode;
	}
	
	/**
	 * Inserts the list of peers and peer educators into the outgoing queue
	 */
	public void queueKpPeerPeerEductorList() {
		String PREP_PROGRAM_UUID = "214cad1c-bb62-4d8e-b927-810a046daf62";
		String KP_PROGRAM_UUID = "7447305a-18a7-11e9-ab14-d663bd873d93";
		String CHTUSERNAME_ATTRIBUTETYPE_UUID = "1aaead2d-0e88-40b2-abcd-6bc3d20fa43c"; //cht username
		Program prepProgram = MetadataUtils.existing(Program.class, PREP_PROGRAM_UUID);
		Program kpProgram = MetadataUtils.existing(Program.class, KP_PROGRAM_UUID);
		Program hivProgram = MetadataUtils.existing(Program.class, HivMetadata._Program.HIV);
		ProgramWorkflowService programWorkflowService = Context.getProgramWorkflowService();
		PersonAttributeType chtPersonAttributeType = personService
		        .getPersonAttributeTypeByUuid(CHTUSERNAME_ATTRIBUTETYPE_UUID);
		
		JsonNodeFactory factory = getJsonNodeFactory();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		
		Set<Integer> peerEducatorList = getAllPeerEducatorsForKPProgram();
		if (peerEducatorList.size() > 0) {
			for (Integer ptId : peerEducatorList) {
				
				List<PatientProgram> peerEducatorPrepPrograms = programWorkflowService.getPatientPrograms(Context
				        .getPatientService().getPatient(ptId), prepProgram, null, null, null, null, true);
				
				List<PatientProgram> peerEducatorHivPrograms = programWorkflowService.getPatientPrograms(Context
				        .getPatientService().getPatient(ptId), hivProgram, null, null, null, null, true);
				List<PatientProgram> kpPrograms = programWorkflowService.getPatientPrograms(Context.getPatientService()
				        .getPatient(ptId), kpProgram, null, null, null, null, true);
				String peerEducatorHivStatus = getClientHIVStatusCapturedOnKpClinicalEnrollment(ptId);
				Patient patient = Context.getPatientService().getPatient(ptId);
				Person p = personService.getPerson(ptId);
				
				String peerEducatorAssignee = "";
				
				if (p.getAttribute(chtPersonAttributeType) != null
				        && p.getAttribute(chtPersonAttributeType).getValue() != null) {
					peerEducatorAssignee = p.getAttribute(chtPersonAttributeType).getValue();
					
				}
				if (!peerEducatorAssignee.equalsIgnoreCase("")) {
					ArrayNode peerEducatorContactNode = getJsonNodeFactory().arrayNode();
					ObjectNode responseWrapper = factory.objectNode();
					
					PatientContactListData returnData = buildPatientNodeForQueue(patient, false, peerEducatorAssignee);
					ObjectNode peerEducator = returnData.getContactWrapper();
					
					if (peerEducatorHivPrograms.isEmpty() && peerEducatorPrepPrograms.isEmpty()
					        && peerEducatorHivStatus.equalsIgnoreCase("NEGATIVE") && kpPrograms.size() > 0) {
						peerEducator.put("record_purpose", "prep_verification");
						returnData.setPurpose("prep_verification");
					} else if (peerEducatorHivPrograms.isEmpty() && peerEducatorHivStatus.equalsIgnoreCase("POSITIVE")
					        && kpPrograms.size() > 0) {
						peerEducator.put("record_purpose", "treatment_verification");
						returnData.setPurpose("treatment_verification");
					} else {
						if (kpPrograms.size() > 0) {
							peerEducator.put("record_purpose", "kp_followup");
							returnData.setPurpose("kp_followup");
						}
					}
					peerEducatorContactNode.add(peerEducator);
					
					responseWrapper.put("docs", peerEducatorContactNode);
					responseWrapper.put("timestamp", formatter.format(new Date()));
					
					MedicOutgoingRegistrationService medicOutgoingRegistrationService = Context
					        .getService(MedicOutgoingRegistrationService.class);
					if (medicOutgoingRegistrationService.getRecordByPatientAndPurpose(ptId, returnData.getPurpose()) == null) {
						MedicOutgoingRegistration record = new MedicOutgoingRegistration();
						record.setPatientId(ptId);
						record.setChtRef(returnData.getChtRef());
						record.setKemrRef(returnData.getKemrRef());
						record.setPurpose(returnData.getPurpose());
						record.setPayload(responseWrapper.toString());
						record.setStatus(0);
						
						medicOutgoingRegistrationService.saveOrUpdate(record);
					}
					
				}
				
				for (Relationship relationship : Context.getPersonService().getRelationshipsByPerson(
				    Context.getPatientService().getPatient(ptId))) {
					
					if (relationship.getRelationshipType().getbIsToA().equals("Peer") && relationship.getEndDate() == null) {
						ArrayNode peerContactNode = getJsonNodeFactory().arrayNode();
						ObjectNode responseWrapper = factory.objectNode();
						
						List<PatientProgram> peerPrepPrograms = programWorkflowService.getPatientPrograms(Context
						        .getPatientService().getPatient(relationship.getPersonB().getId()), prepProgram, null, null,
						    null, null, true);
						
						List<PatientProgram> peerHivPrograms = programWorkflowService.getPatientPrograms(Context
						        .getPatientService().getPatient(relationship.getPersonB().getId()), hivProgram, null, null,
						    null, null, true);
						List<PatientProgram> kpPeerPrograms = programWorkflowService.getPatientPrograms(Context
						        .getPatientService().getPatient(relationship.getPersonB().getId()), kpProgram, null, null,
						    null, null, true);
						String peerHivStatus = getClientHIVStatusCapturedOnKpClinicalEnrollment(relationship.getPersonB()
						        .getId());
						Patient peerPatient = Context.getPatientService().getPatient(relationship.getPersonB().getId());
						
						String assignee = "";
						
						if (relationship.getPersonA().getAttribute(chtPersonAttributeType) != null
						        && relationship.getPersonA().getAttribute(chtPersonAttributeType).getValue() != null) {
							assignee = relationship.getPersonA().getAttribute(chtPersonAttributeType).getValue();
							
						}
						
						PatientContactListData returnData = buildPatientNodeForQueue(peerPatient, false, assignee);
						ObjectNode peer = returnData.getContactWrapper();
						
						if (peerPrepPrograms.isEmpty() && peerHivPrograms.isEmpty()
						        && peerHivStatus.equalsIgnoreCase("NEGATIVE") && kpPeerPrograms.size() > 0
						        && !assignee.equalsIgnoreCase("")) {
							peer.put("record_purpose", "prep_verification");
							returnData.setPurpose("prep_verification");
						} else if (peerHivPrograms.isEmpty() && peerHivStatus.equalsIgnoreCase("POSITIVE")
						        && kpPeerPrograms.size() > 0 && !assignee.equalsIgnoreCase("")) {
							peer.put("record_purpose", "treatment_verification");
							returnData.setPurpose("treatment_verification");
						} else {
							if (kpPeerPrograms.size() > 0 && !assignee.equalsIgnoreCase("")) {
								peer.put("record_purpose", "kp_followup");
								returnData.setPurpose("kp_followup");
							}
						}
						
						peerContactNode.add(peer);
						
						responseWrapper.put("docs", peerContactNode);
						responseWrapper.put("timestamp", formatter.format(new Date()));
						
						MedicOutgoingRegistrationService medicOutgoingRegistrationService = Context
						        .getService(MedicOutgoingRegistrationService.class);
						if (medicOutgoingRegistrationService.getRecordByPatientAndPurpose(ptId, returnData.getPurpose()) == null) {
							MedicOutgoingRegistration record = new MedicOutgoingRegistration();
							record.setPatientId(ptId);
							record.setChtRef(returnData.getChtRef());
							record.setKemrRef(returnData.getKemrRef());
							record.setPurpose(returnData.getPurpose());
							record.setPayload(responseWrapper.toString());
							record.setStatus(0);
							
							medicOutgoingRegistrationService.saveOrUpdate(record);
						}
					}
				}
			}
		}
	}
	
	/**
	 * @param patientId
	 * @return
	 */
	private String getClientHIVStatusCapturedOnKpClinicalEnrollment(Integer patientId) {
		EncounterService encounterService = Context.getEncounterService();
		FormService formService = Context.getFormService();
		PatientService patientService = Context.getPatientService();
		Patient patient = patientService.getPatient(patientId);
		
		String kpClinicalEnrolmentEncounterTypeUuid = "c7f47a56-207b-11e9-ab14-d663bd873d93";
		String kpClinicalEnrolmentFormUuid = "c7f47cea-207b-11e9-ab14-d663bd873d93";
		Encounter lastClinicalEnrolmentEncForPeers = EmrUtils.lastEncounter(patient,
		    encounterService.getEncounterTypeByUuid(kpClinicalEnrolmentEncounterTypeUuid),
		    formService.getFormByUuid(kpClinicalEnrolmentFormUuid));
		
		String hivStatus = "";
		int positiveConcept = 703;
		int negativeConcept = 664;
		int hivStatusQuestionConcept = 165153;
		if (lastClinicalEnrolmentEncForPeers != null) {
			for (Obs obs : lastClinicalEnrolmentEncForPeers.getObs()) {
				if (obs.getConcept().getConceptId() == hivStatusQuestionConcept
				        && obs.getValueCoded().getConceptId() == positiveConcept) {
					hivStatus = "POSITIVE";
				}
				
				if (obs.getConcept().getConceptId() == hivStatusQuestionConcept
				        && obs.getValueCoded().getConceptId() == negativeConcept) {
					hivStatus = "NEGATIVE";
				}
				
			}
		}
		
		return hivStatus;
		
	}
	
	private ObjectNode buildPatientNode(Patient patient, boolean newRegistration, String assignee) {
		JsonNodeFactory factory = getJsonNodeFactory();
		ObjectNode objectWrapper = factory.objectNode();
		ObjectNode fields = factory.objectNode();
		
		String sex = "";
		String dateFormat = "yyyy-MM-dd";
		
		String fullName = "";
		
		if (patient.getGivenName() != null) {
			fullName += patient.getGivenName();
		}
		
		if (patient.getMiddleName() != null) {
			fullName += " " + patient.getMiddleName();
		}
		
		if (patient.getFamilyName() != null) {
			fullName += " " + patient.getFamilyName();
		}
		
		if (patient.getGender() != null) {
			if (patient.getGender().equals("M")) {
				sex = "male";
			} else {
				sex = "female";
			}
		}
		
		objectWrapper.put("_id", patient.getUuid());
		objectWrapper.put("type", "data_record");
		objectWrapper.put("form", "case_information");
		if (newRegistration) {
			objectWrapper.put("record_purpose", "testing");
		} else {
			objectWrapper.put("record_purpose", "linkage");
		}
		objectWrapper.put("content_type", "xml");
		objectWrapper.put("reported_date", patient.getDateCreated().getTime());
		
		PatientIdentifierType chtRefType = Context.getPatientService().getPatientIdentifierTypeByUuid(
		    AfyaStatMetadata._PatientIdentifierType.CHT_RECORD_UUID);
		//PatientIdentifier nationalId = patient.getPatientIdentifier(Utils.NATIONAL_ID);
		PatientIdentifier chtReference = patient.getPatientIdentifier(chtRefType);
		
		PatientIdentifier parentChtRef = null;
		String relType = null;
		String parentName = "";
		
		// get address
		
		ObjectNode address = getPatientAddress(patient);
		String nationality = address.get("NATIONALITY").getTextValue();
		
		String postalAddress = address.get("POSTAL_ADDRESS").getTextValue();
		String county = address.get("COUNTY").getTextValue();
		String subCounty = address.get("SUB_COUNTY").getTextValue();
		String ward = address.get("WARD").getTextValue();
		String landMark = address.get("NEAREST_LANDMARK").getTextValue();
		
		fields.put("needs_sign_off", false);
		fields.put("case_id", patient.getUuid());
		fields.put("cht_ref_uuid", chtReference != null ? chtReference.getIdentifier() : "");
		
		// add information about the client this contact was listed under
		PatientContact originalContactRecord = htsService.getPatientContactEntryForPatient(patient);
		if (originalContactRecord != null) {
			Patient relatedPatient = originalContactRecord.getPatientRelatedTo(); // we want the cht ref for this client for use in cht/afyastat
			parentChtRef = relatedPatient.getPatientIdentifier(chtRefType);
			if (originalContactRecord.getRelationType() != null) {
				relType = getContactRelation(originalContactRecord.getRelationType());
			}
			
			if (relatedPatient.getGivenName() != null) {
				parentName += relatedPatient.getGivenName();
			}
			
			if (relatedPatient.getMiddleName() != null) {
				parentName += " " + relatedPatient.getMiddleName();
			}
			
			if (relatedPatient.getFamilyName() != null) {
				parentName += " " + relatedPatient.getFamilyName();
			}
		}
		
		fields.put("relation_uuid", parentChtRef != null ? parentChtRef.getIdentifier() : "");
		fields.put("relation_type", relType != null ? relType : "");
		fields.put("relation_name", StringUtils.isNotBlank(parentName) ? parentName : "");
		
		fields.put("patient_familyName", patient.getFamilyName() != null ? patient.getFamilyName() : "");
		fields.put("patient_firstName", patient.getGivenName() != null ? patient.getGivenName() : "");
		fields.put("patient_middleName", patient.getMiddleName() != null ? patient.getMiddleName() : "");
		fields.put("name", fullName);
		fields.put("patient_name", fullName);
		fields.put("patient_sex", sex);
		fields.put("patient_birthDate",
		    patient.getBirthdate() != null ? getSimpleDateFormat(dateFormat).format(patient.getBirthdate()) : "");
		fields.put("patient_dobKnown", "_1066_No_99DCT");
		fields.put("patient_telephone", getPersonAttributeByType(patient, phoneNumberAttrType));
		fields.put("patient_nationality", nationality);
		fields.put("patient_county", county);
		fields.put("patient_subcounty", subCounty);
		fields.put("patient_ward", ward);
		fields.put("location", "");
		fields.put("sub_location", "");
		fields.put("patient_village", "");
		fields.put("patient_landmark", landMark);
		fields.put("patient_residence", postalAddress);
		fields.put("patient_nearesthealthcentre", "");
		
		if (!newRegistration) {
			Encounter encounter = Utils.lastEncounter(patient, et, Arrays.asList(initial, retest));
			if (encounter != null) {
				fields.put("date_tested_positive", getSimpleDateFormat(dateFormat).format(encounter.getEncounterDatetime()));
			}
		}
		fields.put("assignee", assignee);
		objectWrapper.put("fields", fields);
		return objectWrapper;
	}
	
	/**
	 * Builds the patient node for the outgoing queue
	 * 
	 * @param patient the patient
	 * @param newRegistration whether its a new registration or not
	 * @param assignee who is assigned
	 * @return PatientContactListData object with required fields
	 */
	private PatientContactListData buildPatientNodeForQueue(Patient patient, boolean newRegistration, String assignee) {
		JsonNodeFactory factory = getJsonNodeFactory();
		ObjectNode objectWrapper = factory.objectNode();
		ObjectNode fields = factory.objectNode();
		PatientContactListData returnData = new PatientContactListData();
		
		String sex = "";
		String dateFormat = "yyyy-MM-dd";
		
		String fullName = "";
		
		if (patient.getGivenName() != null) {
			fullName += patient.getGivenName();
		}
		
		if (patient.getMiddleName() != null) {
			fullName += " " + patient.getMiddleName();
		}
		
		if (patient.getFamilyName() != null) {
			fullName += " " + patient.getFamilyName();
		}
		
		if (patient.getGender() != null) {
			if (patient.getGender().equals("M")) {
				sex = "male";
			} else {
				sex = "female";
			}
		}
		
		objectWrapper.put("_id", patient.getUuid());
		objectWrapper.put("type", "data_record");
		objectWrapper.put("form", "case_information");
		if (newRegistration) {
			objectWrapper.put("record_purpose", "testing");
			returnData.setPurpose("testing");
		} else {
			objectWrapper.put("record_purpose", "linkage");
			returnData.setPurpose("linkage");
		}
		objectWrapper.put("content_type", "xml");
		objectWrapper.put("reported_date", patient.getDateCreated().getTime());
		
		PatientIdentifierType chtRefType = Context.getPatientService().getPatientIdentifierTypeByUuid(
		    AfyaStatMetadata._PatientIdentifierType.CHT_RECORD_UUID);
		//PatientIdentifier nationalId = patient.getPatientIdentifier(Utils.NATIONAL_ID);
		PatientIdentifier chtReference = patient.getPatientIdentifier(chtRefType);
		
		PatientIdentifier parentChtRef = null;
		String relType = null;
		String parentName = "";
		
		// get address
		
		ObjectNode address = getPatientAddress(patient);
		String nationality = address.get("NATIONALITY").getTextValue();
		
		String postalAddress = address.get("POSTAL_ADDRESS").getTextValue();
		String county = address.get("COUNTY").getTextValue();
		String subCounty = address.get("SUB_COUNTY").getTextValue();
		String ward = address.get("WARD").getTextValue();
		String landMark = address.get("NEAREST_LANDMARK").getTextValue();
		
		fields.put("needs_sign_off", false);
		fields.put("case_id", patient.getUuid());
		returnData.setKemrRef(patient.getUuid());
		fields.put("cht_ref_uuid", chtReference != null ? chtReference.getIdentifier() : "");
		returnData.setChtRef(chtReference != null ? chtReference.getIdentifier() : "");
		
		// add information about the client this contact was listed under
		PatientContact originalContactRecord = htsService.getPatientContactEntryForPatient(patient);
		if (originalContactRecord != null) {
			Patient relatedPatient = originalContactRecord.getPatientRelatedTo(); // we want the cht ref for this client for use in cht/afyastat
			parentChtRef = relatedPatient.getPatientIdentifier(chtRefType);
			if (originalContactRecord.getRelationType() != null) {
				relType = getContactRelation(originalContactRecord.getRelationType());
			}
			
			if (relatedPatient.getGivenName() != null) {
				parentName += relatedPatient.getGivenName();
			}
			
			if (relatedPatient.getMiddleName() != null) {
				parentName += " " + relatedPatient.getMiddleName();
			}
			
			if (relatedPatient.getFamilyName() != null) {
				parentName += " " + relatedPatient.getFamilyName();
			}
		}
		
		fields.put("relation_uuid", parentChtRef != null ? parentChtRef.getIdentifier() : "");
		fields.put("relation_type", relType != null ? relType : "");
		fields.put("relation_name", StringUtils.isNotBlank(parentName) ? parentName : "");
		
		fields.put("patient_familyName", patient.getFamilyName() != null ? patient.getFamilyName() : "");
		fields.put("patient_firstName", patient.getGivenName() != null ? patient.getGivenName() : "");
		fields.put("patient_middleName", patient.getMiddleName() != null ? patient.getMiddleName() : "");
		fields.put("name", fullName);
		fields.put("patient_name", fullName);
		fields.put("patient_sex", sex);
		fields.put("patient_birthDate",
		    patient.getBirthdate() != null ? getSimpleDateFormat(dateFormat).format(patient.getBirthdate()) : "");
		fields.put("patient_dobKnown", "_1066_No_99DCT");
		fields.put("patient_telephone", getPersonAttributeByType(patient, phoneNumberAttrType));
		fields.put("patient_nationality", nationality);
		fields.put("patient_county", county);
		fields.put("patient_subcounty", subCounty);
		fields.put("patient_ward", ward);
		fields.put("location", "");
		fields.put("sub_location", "");
		fields.put("patient_village", "");
		fields.put("patient_landmark", landMark);
		fields.put("patient_residence", postalAddress);
		fields.put("patient_nearesthealthcentre", "");
		
		if (!newRegistration) {
			Encounter encounter = Utils.lastEncounter(patient, et, Arrays.asList(initial, retest));
			if (encounter != null) {
				fields.put("date_tested_positive", getSimpleDateFormat(dateFormat).format(encounter.getEncounterDatetime()));
			}
		}
		fields.put("assignee", assignee);
		objectWrapper.put("fields", fields);
		
		returnData.setContactWrapper(objectWrapper);
		
		return returnData;
	}
	
	private String getContactRelation(Integer key) {
		if (key == null) {
			return "";
		}
		Map<Integer, String> options = new HashMap<Integer, String>();
		options.put(157351, "Injectable drug user");
		options.put(970, "Parent");
		options.put(972, "Sibling");
		options.put(1528, "Child");
		options.put(5617, "Spouse");
		options.put(163565, "Sexual partner");
		options.put(162221, "Co-wife");
		
		if (options.keySet().contains(key)) {
			return options.get(key);
		}
		return "";
	}
	
	private String getLivingWithPatientOptions(Integer key) {
		if (key == null) {
			return "";
		}
		Map<Integer, String> options = new HashMap<Integer, String>();
		options.put(1065, "Yes");
		options.put(1066, "No");
		options.put(162570, "Declined to Answer");
		
		if (options.keySet().contains(key)) {
			return options.get(key);
		}
		return "";
	}
	
	private String getPreferredPNSApproachOptions(Integer key) {
		if (key == null) {
			return "";
		}
		Map<Integer, String> options = new HashMap<Integer, String>();
		options.put(162284, "Dual referral");
		options.put(160551, "Passive referral");
		options.put(161642, "Contract referral");
		options.put(163096, "Provider referral");
		if (options.keySet().contains(key)) {
			return options.get(key);
		}
		return "";
	}
	
	private String getMaritalStatusOptions(Integer key) {
		if (key == null) {
			return "";
		}
		Map<Integer, String> options = new HashMap<Integer, String>();
		options.put(1057, "Single");
		options.put(5555, "Married Monogamous");
		options.put(159715, "Married Polygamous");
		options.put(1058, "Divorced");
		options.put(1059, "Widowed");
		if (options.keySet().contains(key)) {
			return options.get(key);
		}
		return "";
	}
	
	/**
	 * Retrieves contacts listed under a case and needs follow up Filters out contacts who have been
	 * registered in the system as person/patient
	 * 
	 * @return
	 */
	protected Set<Integer> getListedContacts(Integer lastContactEntry, Integer lastId) {
		
		Set<Integer> eligibleList = new HashSet<Integer>();
		String sql = "";
		if (lastContactEntry != null && lastContactEntry > 0) {
			sql = "select id from kenyaemr_hiv_testing_patient_contact where id >" + lastContactEntry
			        + " and patient_id is null and voided=0 and appointment_date is null;"; // get contacts not registered
		} else {
			sql = "select id from kenyaemr_hiv_testing_patient_contact where id <= " + lastId
			        + " and patient_id is null and voided=0 and appointment_date is null;";
			
		}
		
		List<List<Object>> activeList = Context.getAdministrationService().executeSQL(sql, true);
		if (!activeList.isEmpty()) {
			for (List<Object> res : activeList) {
				Integer patientId = (Integer) res.get(0);
				eligibleList.add(patientId);
			}
		}
		return eligibleList;
	}
	
	/**
	 * prepares a list of contacts from Afyastat registered in KenyaEMR
	 * 
	 * @return
	 */
	protected DataResponseObject getRegisteredCHTContacts() {
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		
		Date fetchDate = null;
		String effectiveDate = null;
		GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject(
		    AfyaStatMetadata.AFYASTAT_CONTACT_LIST_LAST_FETCH_TIMESTAMP);
		if (globalPropertyObject == null) {
			System.out.println("Missing required global property: "
			        + AfyaStatMetadata.AFYASTAT_CONTACT_LIST_LAST_FETCH_TIMESTAMP);
			return null;
		}
		
		if (globalPropertyObject.getValue() != null) {
			try {
				String ts = globalPropertyObject.getValue().toString();
				if (StringUtils.isNotBlank(ts)) {
					fetchDate = formatter.parse(ts);
				}
				effectiveDate = sd.format(fetchDate);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}
		}
		
		Set<Integer> eligibleList = new HashSet<Integer>();
		StringBuilder q = new StringBuilder();
		
		q.append("select p.patient_id from patient p inner join kenyaemr_hiv_testing_patient_contact pc on pc.patient_id = p.patient_id and pc.voided = 0"); // we want to cover all contacts
		//q.append(" where pc.voided = 0 and pc.contact_listing_decline_reason='CHT'"); // we temporarily use CHT to mark contacts from Afystat
		
		if (effectiveDate != null) {
			q.append(" where p.date_created >= '" + effectiveDate + "'");
		}
		
		List<List<Object>> activeList = Context.getAdministrationService().executeSQL(q.toString(), true);
		Date nextFetchDate = new Date();
		
		if (!activeList.isEmpty()) {
			for (List<Object> res : activeList) {
				Integer patientId = (Integer) res.get(0);
				eligibleList.add(patientId);
			}
		}
		DataResponseObject responseObject = new DataResponseObject(eligibleList, formatter.format(nextFetchDate));
		return responseObject;
	}
	
	/**
	 * List of peer educators enrolled in Key population program
	 * 
	 * @return
	 */
	protected Set<Integer> getAllPeerEducatorsForKPProgram() {
		
		Set<Integer> kpList = new HashSet<Integer>();
		String sql = "select patient_id from patient_program pp\n" + "join program pr on pr.program_id = pp.program_id\n"
		        + "join relationship r on r.person_a = pp.patient_id\n"
		        + "where pr.uuid ='7447305a-18a7-11e9-ab14-d663bd873d93'";
		
		List<List<Object>> activeList = Context.getAdministrationService().executeSQL(sql, true);
		if (!activeList.isEmpty()) {
			for (List<Object> res : activeList) {
				Integer patientId = (Integer) res.get(0);
				kpList.add(patientId);
			}
		}
		return kpList;
	}
	
	/**
	 * Gets clients who tested positive and have not been linked to care
	 * 
	 * @return
	 */
	protected DataResponseObject getClientsTestedPositiveNotLinked() {
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		
		Date fetchDate = null;
		String effectiveDate = null;
		GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject(
		    AfyaStatMetadata.AFYASTAT_LINKAGE_LIST_LAST_FETCH_TIMESTAMP);
		if (globalPropertyObject == null) {
			System.out.println("Missing required global property: "
			        + AfyaStatMetadata.AFYASTAT_LINKAGE_LIST_LAST_FETCH_TIMESTAMP);
			return null;
		}
		
		if (globalPropertyObject.getValue() != null) {
			try {
				String ts = globalPropertyObject.getValue().toString();
				if (StringUtils.isNotBlank(ts)) {
					fetchDate = formatter.parse(ts);
				}
				effectiveDate = sd.format(fetchDate);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}
		}
		
		Set<Integer> eligibleList = new HashSet<Integer>();
		
		StringBuilder q = new StringBuilder();
		
		String sql = "select\n"
		        + "e.patient_id\n"
		        + "from encounter e\n"
		        + "inner join person p on p.person_id=e.patient_id and p.voided=0\n"
		        + "inner join form f on f.form_id=e.form_id and f.uuid in ('402dc5d7-46da-42d4-b2be-f43ea4ad87b0','b08471f6-0892-4bf7-ab2b-bf79797b8ea4')\n"
		        + "inner join obs o on o.encounter_id = e.encounter_id and o.voided=0 and o.concept_id in (159427) and o.value_coded = 703\n"
		        + "left join patient_identifier pi on pi.patient_id = e.patient_id and pi.identifier_type in (select patient_identifier_type_id from patient_identifier_type where uuid='c6552b22-f191-4557-a432-1f4df872d473')\n"
		        + "left join kenyaemr_hiv_testing_patient_contact c on c.patient_id = e.patient_id and c.voided = 0 and c.contact_listing_decline_reason='CHT'\n"
		        + "left join (\n"
		        + "(\n"
		        + "select\n"
		        + "e.patient_id\n"
		        + "from encounter e\n"
		        + "inner join person p on p.person_id=e.patient_id and p.voided=0\n"
		        + "inner join form f on f.form_id = e.form_id and f.uuid = '050a7f12-5c52-4cad-8834-863695af335d'\n"
		        + "inner join obs o on o.encounter_id = e.encounter_id and o.concept_id in (162053) and o.voided=0 and o.value_numeric is not null\n"
		        + ")\n"
		        + "union \n"
		        + "(\n"
		        + "select\n"
		        + "e.patient_id\n"
		        + "from encounter e\n"
		        + "inner join person p on p.person_id=e.patient_id and p.voided=0\n"
		        + "inner join form f on f.form_id=e.form_id and f.uuid in ('402dc5d7-46da-42d4-b2be-f43ea4ad87b0','b08471f6-0892-4bf7-ab2b-bf79797b8ea4')\n"
		        + "inner join obs o on o.encounter_id = e.encounter_id and o.voided=0 and o.concept_id in (159427) and o.value_coded = 703\n"
		        + "inner join patient_identifier pi on pi.patient_id = e.patient_id and pi.identifier_type in (select patient_identifier_type_id from patient_identifier_type where uuid='05ee9cf4-7242-4a17-b4d4-00f707265c8a')\n"
		        + ")\n" + ") l on l.patient_id = e.patient_id\n"
		        + "where l.patient_id is null and c.patient_id is null and pi.patient_id is null";
		
		q.append(sql);
		
		if (effectiveDate != null) {
			q.append(" and e.date_created >= '" + effectiveDate + "'");
		}
		
		List<List<Object>> activeList = Context.getAdministrationService().executeSQL(q.toString(), true);
		
		Date nextFetchDate = new Date();
		globalPropertyObject.setPropertyValue(formatter.format(nextFetchDate));
		Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
		
		if (!activeList.isEmpty()) {
			for (List<Object> res : activeList) {
				Integer patientId = (Integer) res.get(0);
				eligibleList.add(patientId);
			}
		}
		DataResponseObject responseObject = new DataResponseObject(eligibleList, formatter.format(nextFetchDate));
		return responseObject;
	}
	
	private JsonNodeFactory getJsonNodeFactory() {
		final JsonNodeFactory factory = JsonNodeFactory.instance;
		return factory;
	}
	
	/**
	 * Processes demographics data from registration
	 * 
	 * @param jNode
	 * @return
	 */
	private ObjectNode processDemographicUpdatePayload(ObjectNode jNode) {
		
		ObjectNode jsonNode = (ObjectNode) jNode.get("demographicUpdate");
		ObjectNode patientNode = getJsonNodeFactory().objectNode();
		ObjectNode demographicsUpdateNode = getJsonNodeFactory().objectNode();
		ObjectNode obs = getJsonNodeFactory().objectNode();
		ObjectNode tmp = getJsonNodeFactory().objectNode();
		ObjectNode discriminator = getJsonNodeFactory().objectNode();
		ObjectNode encounter = getJsonNodeFactory().objectNode();
		ObjectNode demographicUpdateWrapper = getJsonNodeFactory().objectNode();
		
		String patientDobKnown = jsonNode.get("patient_dobKnown") != null ? jsonNode.get("patient_dobKnown").getTextValue()
		        : "";
		String dateOfBirth = null;
		if (patientDobKnown != null && patientDobKnown.equalsIgnoreCase("_1066_No_99DCT")
		        && jsonNode.get("patient_birthDate") != null) {
			dateOfBirth = jsonNode.get("patient_birthDate").getTextValue();
			patientNode.put("patient.birthdate_estimated", "true");
			patientNode.put("patient.birth_date", formatStringDate(dateOfBirth));
			
		}
		
		if (patientDobKnown != null && patientDobKnown.equalsIgnoreCase("_1065_Yes_99DCT")
		        && jsonNode.get("patient_dateOfBirth") != null) {
			dateOfBirth = jsonNode.get("patient_dateOfBirth").getTextValue();
			patientNode.put("patient.birth_date", formatStringDate(dateOfBirth));
		}
		String patientGender = jsonNode.get("patient_sex") != null ? jsonNode.get("patient_sex").getTextValue() : "";
		
		patientNode.put("patient.uuid", jsonNode.get("_id") != null ? jsonNode.get("_id").getTextValue() : "");
		
		// Get patient name details
		patientNode.put("patient.family_name",
		    jsonNode.get("patient_familyName") != null ? jsonNode.get("patient_familyName").getTextValue() : "");
		patientNode.put("patient.given_name", jsonNode.get("patient_firstName") != null ? jsonNode.get("patient_firstName")
		        .getTextValue() : "");
		patientNode.put("patient.middle_name",
		    jsonNode.get("patient_middleName") != null ? jsonNode.get("patient_middleName").getTextValue() : "");
		
		patientNode.put("patient.sex", gender(patientGender));
		
		if (patientDobKnown != null && patientDobKnown.equalsIgnoreCase("_1066_No_99DCT")
		        && jsonNode.get("patient_birthDate") != null) {
			dateOfBirth = jsonNode.get("patient_birthDate").getTextValue();
			demographicsUpdateNode.put("demographicsupdate.birthdate_estimated", "true");
			demographicsUpdateNode.put("demographicsupdate.birth_date", formatStringDate(dateOfBirth));
			
		}
		
		if (patientDobKnown != null && patientDobKnown.equalsIgnoreCase("_1065_Yes_99DCT")
		        && jsonNode.get("patient_dateOfBirth") != null) {
			dateOfBirth = jsonNode.get("patient_dateOfBirth").getTextValue();
			demographicsUpdateNode.put("demographicsupdate.birth_date", formatStringDate(dateOfBirth));
		}
		
		demographicsUpdateNode.put("demographicsupdate.temporal_patient_uuid",
		    jsonNode.get("_id") != null ? jsonNode.get("_id").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.family_name", jsonNode.get("patient_familyName") != null ? jsonNode
		        .get("patient_familyName").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.given_name", jsonNode.get("patient_firstName") != null ? jsonNode
		        .get("patient_firstName").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.middle_name", jsonNode.get("patient_middleName") != null ? jsonNode
		        .get("patient_middleName").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.sex", gender(patientGender));
		demographicsUpdateNode.put("demographicsupdate.county",
		    jsonNode.get("patient_county") != null ? jsonNode.get("patient_county").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.sub_county", jsonNode.get("patient_subcounty") != null ? jsonNode
		        .get("patient_subcounty").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.ward",
		    jsonNode.get("patient_ward") != null ? jsonNode.get("patient_ward").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.sub_location", jsonNode.get("patient_sublocation") != null ? jsonNode
		        .get("patient_sublocation").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.location",
		    jsonNode.get("patient_location") != null ? jsonNode.get("patient_location").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.village",
		    jsonNode.get("patient_village") != null ? jsonNode.get("patient_village").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.landmark",
		    jsonNode.get("patient_landmark") != null ? jsonNode.get("patient_landmark").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.phone_number", jsonNode.get("patient_telephone") != null ? jsonNode
		        .get("patient_telephone").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.alternate_phone_contact",
		    jsonNode.get("patient_alternatePhone") != null ? jsonNode.get("patient_alternatePhone").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.postal_address",
		    jsonNode.get("patient_postalAddress") != null ? jsonNode.get("patient_postalAddress").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.email_address",
		    jsonNode.get("patient_emailAddress") != null ? jsonNode.get("patient_emailAddress").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.nearest_health_center",
		    jsonNode.get("patient_nearesthealthcentre") != null ? jsonNode.get("patient_nearesthealthcentre").getTextValue()
		            : "");
		demographicsUpdateNode.put("demographicsupdate.next_of_kin_name",
		    jsonNode.get("patient_nextofkin") != null ? jsonNode.get("patient_nextofkin").getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.next_of_kin_relationship", jsonNode
		        .get("patient_nextofkinRelationship") != null ? jsonNode.get("patient_nextofkinRelationship").getTextValue()
		        : "");
		demographicsUpdateNode.put("demographicsupdate.next_of_kin_contact",
		    jsonNode.get("patient_nextOfKinPhonenumber") != null ? jsonNode.get("patient_nextOfKinPhonenumber")
		            .getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.next_of_kin_address",
		    jsonNode.get("patient_nextOfKinPostaladdress") != null ? jsonNode.get("patient_nextOfKinPostaladdress")
		            .getTextValue() : "");
		demographicsUpdateNode.put("demographicsupdate.otheridentifier", getIdentifierTypes(jsonNode));
		Long dateFormFilled = jsonNode.get("reported_date") != null ? jsonNode.get("reported_date").getLongValue() : null;
		demographicsUpdateNode.put("dateFormFilled", dateFormFilled);
		
		obs.put(
		    "1054^CIVIL STATUS^99DCT",
		    jsonNode.get("patient_marital_status") != null
		            && !jsonNode.get("patient_marital_status").getTextValue().equalsIgnoreCase("") ? jsonNode
		            .get("patient_marital_status").getTextValue().replace("_", "^").substring(1) : "");
		obs.put(
		    "1542^OCCUPATION^99DCT",
		    jsonNode.get("patient_occupation") != null
		            && !jsonNode.get("patient_occupation").getTextValue().equalsIgnoreCase("") ? jsonNode
		            .get("patient_occupation").getTextValue().replace("_", "^").substring(1) : "");
		obs.put(
		    "1712^HIGHEST EDUCATION LEVEL^99DCT",
		    jsonNode.get("patient_education_level") != null
		            && !jsonNode.get("patient_education_level").getTextValue().equalsIgnoreCase("") ? jsonNode
		            .get("patient_education_level").getTextValue().replace("_", "^").substring(1) : "");
		
		tmp.put("tmp.birthdate_type", "age");
		tmp.put("tmp.age_in_years", jsonNode.get("patient_ageYears") != null ? jsonNode.get("patient_ageYears")
		        .getTextValue() : "");
		discriminator.put("discriminator", "json-demographics-update");
		String creator = jsonNode.path("meta").path("created_by") != null ? jsonNode.path("meta").path("created_by")
		        .getTextValue() : "";
		String providerId = checkProviderNameExists(creator);
		String systemId = confirmUserNameExists(creator);
		
		encounter.put("encounter.location_id", locationId != null ? locationId.toString() : null);
		encounter.put("encounter.provider_id_select", providerId != null ? providerId : " ");
		encounter.put("encounter.provider_id", providerId != null ? providerId : " ");
		encounter.put("encounter.encounter_datetime", convertTime(jsonNode.get("reported_date").getLongValue()));
		encounter.put("encounter.form_uuid", "8898c6e1-5df1-409f-b8ed-c88e6e0f24e9");
		encounter.put("encounter.user_system_id", systemId);
		encounter.put("encounter.device_time_zone", "Africa\\/Nairobi");
		encounter.put("encounter.setup_config_uuid", "2107eab5-5b3a-4de8-9e02-9d97bce635d2");
		
		demographicUpdateWrapper.put("patient", patientNode);
		demographicUpdateWrapper.put("demographicsupdate", demographicsUpdateNode);
		demographicUpdateWrapper.put("observation", obs);
		demographicUpdateWrapper.put("tmp", tmp);
		demographicUpdateWrapper.put("discriminator", discriminator);
		demographicUpdateWrapper.put("encounter", encounter);
		return demographicUpdateWrapper;
	}
	
	public static SimpleDateFormat getSimpleDateFormat(String pattern) {
		return new SimpleDateFormat(pattern);
	}
	
	/**
	 * Returns a patient's address
	 * 
	 * @param patient
	 * @return
	 */
	public ObjectNode getPatientAddress(Patient patient) {
		Set<PersonAddress> addresses = patient.getAddresses();
		//patient address
		ObjectNode addressNode = getJsonNodeFactory().objectNode();
		String postalAddress = "";
		String nationality = "";
		String county = "";
		String sub_county = "";
		String ward = "";
		String landMark = "";
		
		for (PersonAddress address : addresses) {
			if (address.getAddress1() != null) {
				postalAddress = address.getAddress1();
			}
			if (address.getCountry() != null) {
				nationality = address.getCountry() != null ? address.getCountry() : "";
			}
			
			if (address.getCountyDistrict() != null) {
				county = address.getCountyDistrict() != null ? address.getCountyDistrict() : "";
			}
			
			if (address.getStateProvince() != null) {
				sub_county = address.getStateProvince() != null ? address.getStateProvince() : "";
			}
			
			if (address.getAddress4() != null) {
				ward = address.getAddress4() != null ? address.getAddress4() : "";
			}
			if (address.getAddress2() != null) {
				landMark = address.getAddress2() != null ? address.getAddress2() : "";
			}
		}
		
		addressNode.put("NATIONALITY", nationality);
		addressNode.put("COUNTY", county);
		addressNode.put("SUB_COUNTY", sub_county);
		addressNode.put("WARD", ward);
		addressNode.put("NEAREST_LANDMARK", landMark);
		addressNode.put("POSTAL_ADDRESS", postalAddress);
		
		return addressNode;
	}
	
	/**
	 * Manually queue a client into the outgoing queue
	 * 
	 * @param clientId the patient ID
	 * @param purpose the purpose
	 * @return true on success or false on failure
	 */
	public boolean queueClientForOutgoingRegistration(Integer clientId, String purpose) {
		//Trim
		purpose = purpose.trim();
		//Proceed only if the client and purpose dont already exist on DB
		MedicOutgoingRegistrationService medicOutgoingRegistrationService = Context
		        .getService(MedicOutgoingRegistrationService.class);
		
		PatientContactListData data = generateClientPayload(clientId, purpose, "");
		
		if (data != null) {
			MedicOutgoingRegistration queueEntry = medicOutgoingRegistrationService.getRecordByPatientAndPurpose(clientId,
			    purpose);
			if (queueEntry == null) {
				MedicOutgoingRegistration record = new MedicOutgoingRegistration();
				record.setPatientId(clientId);
				record.setChtRef(data.getChtRef());
				record.setKemrRef(data.getKemrRef());
				record.setPurpose(data.getPurpose());
				record.setPayload(data.getContactWrapper().toString());
				record.setStatus(0);
				
				//Save to the outgoing queue
				medicOutgoingRegistrationService.saveOrUpdate(record);
			} else {
				queueEntry.setChtRef(data.getChtRef());
				queueEntry.setKemrRef(data.getKemrRef());
				queueEntry.setPayload(data.getContactWrapper().toString());
				queueEntry.setStatus(0);
				
				//Save to the outgoing queue
				medicOutgoingRegistrationService.saveOrUpdate(queueEntry);
			}
			System.out.println("Afyastat outgoing queue. Saved Record");
			return (true);
		} else {
			System.err.println("Afyastat outgoing queue. NOT Saved");
			return (false);
		}
	}
	
	public PatientContactListData generateClientPayload(Integer clientId, String purpose, String assignee) {
		purpose = purpose.trim();
		assignee = assignee.trim();
		PatientContactListData data = new PatientContactListData();
		//Search for client
		Patient patient = Context.getPatientService().getPatient(clientId);
		if (patient == null) {
			return (null);
		}
		//Build Payload
		JsonNodeFactory factory = getJsonNodeFactory();
		ObjectNode objectWrapper = factory.objectNode();
		ObjectNode fields = factory.objectNode();
		
		String sex = "";
		String dateFormat = "yyyy-MM-dd";
		
		String fullName = "";
		
		if (patient.getGivenName() != null) {
			fullName += patient.getGivenName();
		}
		
		if (patient.getMiddleName() != null) {
			fullName += " " + patient.getMiddleName();
		}
		
		if (patient.getFamilyName() != null) {
			fullName += " " + patient.getFamilyName();
		}
		
		if (patient.getGender() != null) {
			if (patient.getGender().equals("M")) {
				sex = "male";
			} else {
				sex = "female";
			}
		}
		
		objectWrapper.put("_id", patient.getUuid());
		objectWrapper.put("type", "data_record");
		objectWrapper.put("form", "case_information");
		objectWrapper.put("record_purpose", purpose);
		
		objectWrapper.put("content_type", "xml");
		objectWrapper.put("reported_date", patient.getDateCreated().getTime());
		
		PatientIdentifierType chtRefType = Context.getPatientService().getPatientIdentifierTypeByUuid(
		    AfyaStatMetadata._PatientIdentifierType.CHT_RECORD_UUID);
		//PatientIdentifier nationalId = patient.getPatientIdentifier(Utils.NATIONAL_ID);
		PatientIdentifier chtReference = patient.getPatientIdentifier(chtRefType);
		
		PatientIdentifier parentChtRef = null;
		String relType = null;
		String parentName = "";
		
		// get address
		
		ObjectNode address = getPatientAddress(patient);
		String nationality = address.get("NATIONALITY").getTextValue();
		
		String postalAddress = address.get("POSTAL_ADDRESS").getTextValue();
		String county = address.get("COUNTY").getTextValue();
		String subCounty = address.get("SUB_COUNTY").getTextValue();
		String ward = address.get("WARD").getTextValue();
		String landMark = address.get("NEAREST_LANDMARK").getTextValue();
		
		fields.put("needs_sign_off", false);
		fields.put("case_id", patient.getUuid());
		String kemrRef = patient.getUuid();
		fields.put("cht_ref_uuid", chtReference != null ? chtReference.getIdentifier() : "");
		String chtRef = (chtReference != null ? chtReference.getIdentifier() : "");
		
		// add information about the client this contact was listed under
		PatientContact originalContactRecord = htsService.getPatientContactEntryForPatient(patient);
		if (originalContactRecord != null) {
			Patient relatedPatient = originalContactRecord.getPatientRelatedTo(); // we want the cht ref for this client for use in cht/afyastat
			parentChtRef = relatedPatient.getPatientIdentifier(chtRefType);
			if (originalContactRecord.getRelationType() != null) {
				relType = getContactRelation(originalContactRecord.getRelationType());
			}
			
			if (relatedPatient.getGivenName() != null) {
				parentName += relatedPatient.getGivenName();
			}
			
			if (relatedPatient.getMiddleName() != null) {
				parentName += " " + relatedPatient.getMiddleName();
			}
			
			if (relatedPatient.getFamilyName() != null) {
				parentName += " " + relatedPatient.getFamilyName();
			}
		}
		
		fields.put("relation_uuid", parentChtRef != null ? parentChtRef.getIdentifier() : "");
		fields.put("relation_type", relType != null ? relType : "");
		fields.put("relation_name", StringUtils.isNotBlank(parentName) ? parentName : "");
		
		fields.put("patient_familyName", patient.getFamilyName() != null ? patient.getFamilyName() : "");
		fields.put("patient_firstName", patient.getGivenName() != null ? patient.getGivenName() : "");
		fields.put("patient_middleName", patient.getMiddleName() != null ? patient.getMiddleName() : "");
		fields.put("name", fullName);
		fields.put("patient_name", fullName);
		fields.put("patient_sex", sex);
		fields.put("patient_birthDate",
		    patient.getBirthdate() != null ? getSimpleDateFormat(dateFormat).format(patient.getBirthdate()) : "");
		fields.put("patient_dobKnown", "_1066_No_99DCT");
		fields.put("patient_telephone", getPersonAttributeByType(patient, phoneNumberAttrType));
		fields.put("patient_nationality", nationality);
		fields.put("patient_county", county);
		fields.put("patient_subcounty", subCounty);
		fields.put("patient_ward", ward);
		fields.put("location", "");
		fields.put("sub_location", "");
		fields.put("patient_village", "");
		fields.put("patient_landmark", landMark);
		fields.put("patient_residence", postalAddress);
		fields.put("patient_nearesthealthcentre", "");
		
		Encounter encounter = Utils.lastEncounter(patient, et, Arrays.asList(initial, retest));
		if (encounter != null) {
			fields.put("date_tested_positive", getSimpleDateFormat(dateFormat).format(encounter.getEncounterDatetime()));
		}
		
		fields.put("assignee", assignee);
		objectWrapper.put("fields", fields);
		
		ArrayNode patientContactNode = getJsonNodeFactory().arrayNode();
		ArrayNode emptyContactNode = factory.arrayNode();
		ObjectNode responseWrapper = factory.objectNode();
		
		objectWrapper.put("contacts", emptyContactNode);
		patientContactNode.add(objectWrapper);
		
		responseWrapper.put("docs", patientContactNode);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		responseWrapper.put("timestamp", formatter.format(new Date()));
		
		data.setChtRef(chtRef);
		data.setKemrRef(kemrRef);
		data.setPurpose(purpose);
		data.setContactWrapper(responseWrapper);
		
		return (data);
	}
	
	private String relationshipTypeConverter(String relType) {
		String relTypeUuid = null;
		if (relType.equalsIgnoreCase("partner")) {
			relTypeUuid = "007b765f-6725-4ae9-afee-9966302bace4";
		} else if (relType.equalsIgnoreCase("parent") || relType.equalsIgnoreCase("mother")
		        || relType.equalsIgnoreCase("father")) {
			relTypeUuid = "8d91a210-c2cc-11de-8d13-0010c6dffd0f";
		} else if (relType.equalsIgnoreCase("sibling")) {
			relTypeUuid = "8d91a01c-c2cc-11de-8d13-0010c6dffd0f";
		} else if (relType.equalsIgnoreCase("child")) {
			relTypeUuid = "8d91a210-c2cc-11de-8d13-0010c6dffd0f";
		} else if (relType.equalsIgnoreCase("spouse")) {
			relTypeUuid = "d6895098-5d8d-11e3-94ee-b35a4132a5e3";
		} else if (relType.equalsIgnoreCase("co-wife")) {
			relTypeUuid = "2ac0d501-eadc-4624-b982-563c70035d46";
		} else if (relType.equalsIgnoreCase("Injectable drug user")) {
			relTypeUuid = "58da0d1e-9c89-42e9-9412-275cef1e0429";
		} else if (relType.equalsIgnoreCase("guardian")) {
			relTypeUuid = "5f115f62-68b7-11e3-94ee-6bef9086de92";
			
		}
		return relTypeUuid;
	}
	
	/**
	 * get a patient's phone contact
	 * 
	 * @param patient
	 * @return
	 */
	public String getPersonAttributeByType(Patient patient, PersonAttributeType phoneNumberAttrType) {
		return patient.getAttribute(phoneNumberAttrType) != null ? patient.getAttribute(phoneNumberAttrType).getValue() : "";
	}
	
	/**
	 * A class used to hold data from a query process. The global property is set to the current
	 * time and should only be saved at the end of the process
	 */
	public class DataResponseObject {
		
		private Set<Integer> patientList;
		
		private String timestamp;
		
		public DataResponseObject(Set<Integer> patientList, String timestamp) {
			this.patientList = patientList;
			this.timestamp = timestamp;
		}
		
		public Set<Integer> getPatientList() {
			return patientList;
		}
		
		public void setPatientList(Set<Integer> patientList) {
			this.patientList = patientList;
		}
		
		public String getTimestamp() {
			return timestamp;
		}
		
		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}
	}
	
	/**
	 * A class to hold contact list for patients
	 */
	public static class PatientContactListData {
		
		/**
		 * Holds the JSON content
		 */
		private ObjectNode contactWrapper;
		
		/**
		 * Holds the CHT ref
		 */
		private String chtRef;
		
		/**
		 * Holds the KEMR ref
		 */
		private String kemrRef;
		
		/**
		 * Holds the purpose
		 */
		private String purpose;
		
		public ObjectNode getContactWrapper() {
			return contactWrapper;
		}
		
		public void setContactWrapper(ObjectNode contactWrapper) {
			this.contactWrapper = contactWrapper;
		}
		
		public String getChtRef() {
			return chtRef;
		}
		
		public void setChtRef(String chtRef) {
			this.chtRef = chtRef;
		}
		
		public String getKemrRef() {
			return kemrRef;
		}
		
		public void setKemrRef(String kemrRef) {
			this.kemrRef = kemrRef;
		}
		
		public String getPurpose() {
			return purpose;
		}
		
		public void setPurpose(String purpose) {
			this.purpose = purpose;
		}
		
	}
}
