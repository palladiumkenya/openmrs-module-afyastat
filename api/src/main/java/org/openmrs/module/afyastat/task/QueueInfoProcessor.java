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
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.api.service.InfoService;
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
			InfoService infoService = Context.getService(InfoService.class);
			List<AfyaStatQueueData> queueDataList = infoService.getAllQueueData();
			List<QueueInfoHandler> queueDataHandlers = HandlerUtil.getHandlersForType(QueueInfoHandler.class,
			    AfyaStatQueueData.class);
			for (QueueInfoHandler queueDataHandler : queueDataHandlers) {
				Iterator<AfyaStatQueueData> queueDataIterator = queueDataList.iterator();
				while (queueDataIterator.hasNext()) {
					AfyaStatQueueData afyaStatQueueData = queueDataIterator.next();
					try {
						if (queueDataHandler.accept(afyaStatQueueData)) {
							queueDataHandler.process(afyaStatQueueData);
							queueDataIterator.remove();
							// archive them after we're done processing the queue data.
							createArchiveData(afyaStatQueueData, "Queue data processed successfully!");
							infoService.purgeQueueData(afyaStatQueueData);
						}
					}
					catch (Exception e) {
						log.error("Unable to process queue data due to: " + e.getMessage(), e);
						if (afyaStatQueueData.getLocation() == null) {
							Location location = extractLocationFromPayload(afyaStatQueueData.getPayload());
							afyaStatQueueData.setLocation(location);
						}
						if (afyaStatQueueData.getProvider() == null) {
							Provider provider = extractProviderFromPayload(afyaStatQueueData.getPayload());
							afyaStatQueueData.setProvider(provider);
						}
						if (afyaStatQueueData.getFormName() == null) {
							String formName = extractFormNameFromPayload(afyaStatQueueData.getPayload());
							afyaStatQueueData.setFormName(formName);
						}
						if (afyaStatQueueData.getPatientUuid() == null) {
							String patientUuid = extractPatientUuidFromPayload(afyaStatQueueData.getPayload());
							if (patientUuid == null) {
								afyaStatQueueData.setPatientUuid("");
							}
							afyaStatQueueData.setPatientUuid(patientUuid);
						}
						createErrorData(afyaStatQueueData, (StreamProcessorException) e);
						infoService.purgeQueueData(afyaStatQueueData);
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
		ArchiveInfo archiveInfo = new ArchiveInfo(queueData);
		archiveInfo.setMessage(message);
		archiveInfo.setDateArchived(new Date());
		Context.getService(InfoService.class).saveArchiveData(archiveInfo);
	}
	
	private void createErrorData(final AfyaStatQueueData queueData, StreamProcessorException exception) {
		ErrorInfo errorInfo = new ErrorInfo(queueData);
		errorInfo.setDateProcessed(new Date());
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
		errorInfo.setMessage("Unable to process queue data");
		errorInfo.setErrorMessages(errorMessage);
		Context.getService(InfoService.class).saveErrorData(errorInfo);
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
		Form form = Context.getFormService().getFormByUuid(formUuid);
		if (form != null && form.getName() != null) {
			return form.getName();
		} else {
			return null;
		}
		
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
