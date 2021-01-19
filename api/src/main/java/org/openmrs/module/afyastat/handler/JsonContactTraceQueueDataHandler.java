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
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.exception.StreamProcessorException;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.handler.QueueInfoHandler;
import org.openmrs.module.afyastat.utils.JsonFormatUtils;
import org.openmrs.module.hivtestingservices.api.ContactTrace;
import org.openmrs.module.hivtestingservices.api.HTSService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TODO: Write brief description about the class here.
 */
@Handler(supports = AfyaStatQueueData.class, order = 12)
public class JsonContactTraceQueueDataHandler implements QueueInfoHandler {
	
	private static final String DISCRIMINATOR_VALUE = "json-contacttrace";
	
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private final Log log = LogFactory.getLog(JsonContactTraceQueueDataHandler.class);
	
	private ContactTrace unsavedContactTrace;
	
	private String payload;
	
	private StreamProcessorException queueProcessorException;
	
	@Override
	public void process(final AfyaStatQueueData queueData) throws StreamProcessorException {
		log.info("Processing contact trace form data: " + queueData.getUuid());
		queueProcessorException = new StreamProcessorException();
		try {
			if (validate(queueData)) {
				registerUnsavedContactTrace();
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
		log.info("Processing contact trace form data: " + queueData.getUuid());
		queueProcessorException = new StreamProcessorException();
		try {
			payload = queueData.getPayload();
			unsavedContactTrace = new ContactTrace();
			populateUnsavedContactTraceFromPayload();
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
	
	private void populateUnsavedContactTraceFromPayload() {
		setContactTraceFromPayload();
	}
	
	private void setContactTraceFromPayload() {
		HTSService contact = Context.getService(HTSService.class);
		Date traceDate = JsonFormatUtils.readAsDate(payload, "$['fields']['group_follow_up']['date_last_contact']");
		String contactType = contactTypeConverter(JsonFormatUtils.readAsString(payload,
		    "$['fields']['group_follow_up']['follow_up_type']"));
		String status = contactStatusConverter(JsonFormatUtils.readAsString(payload,
		    "$['fields']['group_follow_up']['status_visit']"));
		String reasonUncontacted = reasonUncontactedConverter(JsonFormatUtils.readAsString(payload,
		    "$['fields']['group_follow_up']['is_not_available_reason_other']"));
		String uniquePatientNo = JsonFormatUtils.readAsString(payload,
		    "$['fields']['group_follow_up']['unique_patient_number']");
		String facilityLinkedTo = JsonFormatUtils.readAsString(payload,
		    "$['fields']['group_follow_up']['facility_linked_to']");
		String healthWorkerHandedTo = JsonFormatUtils.readAsString(payload,
		    "$['fields']['group_follow_up']['health_care_worker_handed_to']");
		String remarks = JsonFormatUtils.readAsString(payload, "$['fields']['group_follow_up']['remarks']");
		String uuid = JsonFormatUtils.readAsString(payload, "$['_id']");
		// Integer contactId = getContactId(JsonFormatUtils.readAsString(payload, "$['fields']['inputs']['contact']['_id']"));
		Boolean voided = false;
		
		unsavedContactTrace.setDate(traceDate);
		unsavedContactTrace.setContactType(contactType);
		unsavedContactTrace.setStatus(status);
		unsavedContactTrace.setReasonUncontacted(reasonUncontacted);
		unsavedContactTrace.setUniquePatientNo(uniquePatientNo);
		unsavedContactTrace.setFacilityLinkedTo(facilityLinkedTo);
		unsavedContactTrace.setHealthWorkerHandedTo(healthWorkerHandedTo);
		unsavedContactTrace.setRemarks(remarks);
		// unsavedContactTrace.setPatientContact(contact.getPatientContactByID(contactId));
		unsavedContactTrace.setUuid(uuid);
		unsavedContactTrace.setVoided(voided);
	}
	
	private void registerUnsavedContactTrace() {
		HTSService htsService = Context.getService(HTSService.class);
		try {
			htsService.saveClientTrace(unsavedContactTrace);
		}
		catch (Exception e) {
			e.printStackTrace();
			
		}
	}
	
	/*private Integer getContactId(String uuid) {
		Integer contactId = null;
		HTSService htsService = Context.getService(HTSService.class);
		PatientContact patientContact = htsService.getPatientContactByUuid(uuid);
		if (patientContact != null) {
			contactId = patientContact.getId();
		}
		return contactId;
		
	}*/
	
	private String contactTypeConverter(String follow_up_type) {
		String contactType = null;
		if (follow_up_type.equalsIgnoreCase("in_person")) {
			contactType = "Physical";
		} else if (follow_up_type.equalsIgnoreCase("phone")) {
			contactType = "Phone";
		}
		return contactType;
	}
	
	private String contactStatusConverter(String status_visit) {
		String contactStatus = null;
		if (status_visit.equalsIgnoreCase("available")) {
			contactStatus = "Contacted and Linked";
		} else if (status_visit.equalsIgnoreCase("available_not_linked")) {
			contactStatus = "Contacted";
		} else if (status_visit.equalsIgnoreCase("moved_away")) {
			contactStatus = "Not Contacted";
		} else if (status_visit.equalsIgnoreCase("responded")) {
			contactStatus = "Contacted and Linked";
		} else if (status_visit.equalsIgnoreCase("responded_not_linked")) {
			contactStatus = "Contacted";
		} else if (status_visit.equalsIgnoreCase("not_found_at_home")) {
			contactStatus = "Not Contacted";
		} else if (status_visit.equalsIgnoreCase("no_locator_information")) {
			contactStatus = "Not Contacted";
		} else if (status_visit.equalsIgnoreCase("wrong_locator_information")) {
			contactStatus = "Not Contacted";
		} else if (status_visit.equalsIgnoreCase("died")) {
			contactStatus = "Not Contacted";
		} else if (status_visit.equalsIgnoreCase("other")) {
			contactStatus = "Not Contacted";
		}
		return contactStatus;
	}
	
	private String reasonUncontactedConverter(String is_not_available_reason_other) {
		String reasonConverter = null;
		if (is_not_available_reason_other.equalsIgnoreCase("phone_off")) {
			reasonConverter = "Calls not going through";
		} else if (is_not_available_reason_other.equalsIgnoreCase("moved_away")) {
			reasonConverter = "Migrated";
		} else if (is_not_available_reason_other.equalsIgnoreCase("person_not_found")) {
			reasonConverter = "Not found at home";
		} else if (is_not_available_reason_other.equalsIgnoreCase("call_not_going_through")) {
			reasonConverter = "Calls not going through";
		} else if (is_not_available_reason_other.equalsIgnoreCase("no_call_locator_info")) {
			reasonConverter = "No locator information";
		} else if (is_not_available_reason_other.equalsIgnoreCase("incorrect_locator_info")) {
			reasonConverter = "Incorrect locator information";
		} else if (is_not_available_reason_other.equalsIgnoreCase("deceased")) {
			reasonConverter = "Died";
		} else if (is_not_available_reason_other.equalsIgnoreCase("other")) {
			reasonConverter = "Others";
		}
		
		return reasonConverter;
	}
	
	@Override
	public boolean accept(final AfyaStatQueueData queueData) {
		return StringUtils.equals(DISCRIMINATOR_VALUE, queueData.getDiscriminator());
	}
	/**
	 * Can't save patients unless they have required OpenMRS IDs
	 */
	
}
