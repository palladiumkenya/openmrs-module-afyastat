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
package org.openmrs.module.afyastat.task;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.api.service.AfyaStatDataService;
import org.openmrs.module.afyastat.exception.StreamProcessorException;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.ArchiveInfo;
import org.openmrs.module.afyastat.model.ErrorInfo;
import org.openmrs.module.afyastat.model.ErrorMessagesInfo;
import org.openmrs.module.afyastat.model.handler.QueueInfoHandler;
import org.openmrs.util.HandlerUtil;

import java.util.*;

/**
 */
public class QueueInfoProcessor {
	
	private final Log log = LogFactory.getLog(QueueInfoProcessor.class);
	
	private static Boolean isRunning = false;
	
	public void processQueueData() {
		if (!isRunning) {
			processAllQueueData();
		} else {
			log.info("Queue data processor aborting (another processor already running)!");
		}
	}
	
	private void processAllQueueData() {
		Context.openSession();
		try {
			isRunning = true;
			log.info("Starting up queue data processor ...");
			AfyaStatDataService dataService = Context.getService(AfyaStatDataService.class);
			List<AfyaStatQueueData> queueDataList = dataService.getAllQueueData();
			List<QueueInfoHandler> queueDataHandlers = HandlerUtil.getHandlersForType(QueueInfoHandler.class,
			    AfyaStatQueueData.class);
			for (QueueInfoHandler queueDataHandler : queueDataHandlers) {
				Iterator<AfyaStatQueueData> queueDataIterator = queueDataList.iterator();
				while (queueDataIterator.hasNext()) {
					AfyaStatQueueData queueData = queueDataIterator.next();
					try {
						if (queueDataHandler.accept(queueData)) {
							queueDataHandler.process(queueData);
							queueDataIterator.remove();
							// archive them after we're done processing the queue data.
							createArchiveData(queueData, "Queue data processed successfully!");
							dataService.purgeQueueData(queueData);
						}
					}
					catch (Exception e) {
						log.error("Unable to process queue data due to: " + e.getMessage(), e);
						if (queueData.getLocation() == null) {
							Location location = extractLocationFromPayload(queueData.getPayload());
							queueData.setLocation(location);
						}
						if (queueData.getProvider() == null) {
							Provider provider = extractProviderFromPayload(queueData.getPayload());
							queueData.setProvider(provider);
						}
						if (queueData.getFormName() == null) {
							String formName = extractFormNameFromPayload(queueData.getPayload());
							queueData.setFormName(formName);
						}
						if (queueData.getPatientUuid() == null) {
							String patientUuid = extractPatientUuidFromPayload(queueData.getPayload());
							if (patientUuid == null) {
								queueData.setPatientUuid("");
							}
							queueData.setPatientUuid(patientUuid);
						}
						createErrorData(queueData, (StreamProcessorException) e);
						dataService.purgeQueueData(queueData);
					}
				}
			}
		}
		finally {
			isRunning = false;
			log.info("Stopping up queue data processor ...");
		}
	}
	
	private void createArchiveData(final AfyaStatQueueData queueData, final String message) {
		ArchiveInfo archiveData = new ArchiveInfo(queueData);
		archiveData.setMessage(message);
		archiveData.setDateArchived(new Date());
		Context.getService(AfyaStatDataService.class).saveArchiveData(archiveData);
	}
	
	private void createErrorData(final AfyaStatQueueData queueData, StreamProcessorException exception) {
		ErrorInfo errorData = new ErrorInfo(queueData);
		errorData.setDateProcessed(new Date());
		Set errorMessage = new HashSet();
		for (Exception e : exception.getAllException()) {
			ErrorMessagesInfo error = new ErrorMessagesInfo();
			String message = e.getMessage();
			System.out.println("message=================" + message);
			if (message == null) {
				message = "Queue data was processed but the processor unable to determine the cause of the error.";
			}
			error.setMessage(message);
			errorMessage.add(error);
		}
		errorData.setMessage("Unable to process queue data");
		errorData.setErrorMessages(errorMessage);
		Context.getService(AfyaStatDataService.class).saveErrorData(errorData);
	}
	
	private Provider extractProviderFromPayload(String payload) {
		String providerString = readAsString(payload, "$['encounter']['encounter.provider_id']");
		return Context.getProviderService().getProviderByIdentifier(providerString);
	}
	
	private Location extractLocationFromPayload(String payload) {
		String locationString = readAsString(payload, "$['encounter']['encounter.location_id']");
		int locationId = NumberUtils.toInt(locationString, -999);
		return Context.getLocationService().getLocation(locationId);
	}
	
	private String extractFormNameFromPayload(String payload) {
		String formUuid = readAsString(payload, "$['encounter']['encounter.form_uuid']");
		/* MuzimaFormService muzimaFormService = Context.getService(MuzimaFormService.class);
		 MuzimaForm muzimaForm = muzimaFormService.getFormByUuid(formUuid);
		 if(muzimaForm != null) {
		     return muzimaForm.getName();
		 } else {
		     return null;
		 }*/
		
		return null;
	}
	
	private String extractPatientUuidFromPayload(String payload) {
		return readAsString(payload, "$['patient']['patient.uuid']");
	}
	
	/**
	 * Read string value from the json object.
	 * 
	 * @param jsonObject the json object.
	 * @param path the path inside the json object.
	 * @return the string value in the json object. When the path is invalid, by default will return
	 *         null.
	 */
	private String readAsString(final String jsonObject, final String path) {
		String returnedString = null;
		try {
			returnedString = JsonPath.read(jsonObject, path);
		}
		catch (Exception e) {
			log.info("Unable to read string value with path: " + path + " from: " + String.valueOf(jsonObject));
		}
		return returnedString;
	}
}
