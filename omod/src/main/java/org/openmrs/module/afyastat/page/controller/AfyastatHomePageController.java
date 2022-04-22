package org.openmrs.module.afyastat.page.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.afyastat.api.service.InfoService;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.ErrorInfo;
import org.openmrs.module.afyastat.model.ErrorMessagesInfo;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.util.PrivilegeConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@AppPage("kenyaemr.afyastat.home")
public class AfyastatHomePageController {
	
	public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) {
		
		InfoService infoService = Context.getService(InfoService.class);
		
		// Get the data
		List<AfyaStatQueueData> queueDataList = infoService.getAllQueueData();
		List<ErrorInfo> allErrors = infoService.getAllErrorsExcludingRegistrationErrors();
		List<ErrorInfo> allRegistrationErrors = infoService.getAllRegistrationErrors();
		
		Collections.sort(queueDataList, new Comparator<AfyaStatQueueData>() {
			
			@Override
			public int compare(AfyaStatQueueData a, AfyaStatQueueData b) {
				//return a.getDateProcessed().before(b.getDateProcessed()) ? -1 : a.getDateProcessed() == b.getDateProcessed() ? 0 : 1;
				return a.getDateCreated().after(b.getDateCreated()) ? -1 : a.getDateCreated() == b.getDateCreated() ? 0 : 1;
			}
		});
		
		Collections.sort(allErrors, new Comparator<ErrorInfo>() {
			
			@Override
			public int compare(ErrorInfo a, ErrorInfo b) {
				//return a.getDateProcessed().before(b.getDateProcessed()) ? -1 : a.getDateProcessed() == b.getDateProcessed() ? 0 : 1;
				return a.getDateProcessed().after(b.getDateProcessed()) ? -1
				        : a.getDateProcessed() == b.getDateProcessed() ? 0 : 1;
			}
		});
		List<SimpleObject> queueList = new ArrayList<SimpleObject>();
		List<SimpleObject> errorList = new ArrayList<SimpleObject>();
		List<SimpleObject> registrationErrorList = new ArrayList<SimpleObject>();
		
		ObjectMapper objectMapper = new ObjectMapper();
		for (AfyaStatQueueData qObj : queueDataList) {
			String clientName = qObj.getClientName();
			if ((clientName == null || clientName.trim().equalsIgnoreCase(""))
			        && qObj.getDiscriminator().equalsIgnoreCase("json-registration")) {
				clientName = "";
				JsonNode jsonNode = null;
				try {
					jsonNode = objectMapper.readTree(qObj.getPayload());
				}
				catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				
				ObjectNode patientObj = (ObjectNode) jsonNode.get("patient");
				String givenName = patientObj.get("patient.given_name").asText();
				clientName += (givenName == null) ? "" : givenName;
				String familyName = " " + patientObj.get("patient.family_name").asText();
				clientName += (familyName == null) ? "" : (" " + familyName);
				String middleName = " " + patientObj.get("patient.middle_name").asText();
				clientName += (middleName == null) ? "" : (" " + middleName);
				clientName = clientName.trim();
			}
			clientName = (clientName == null) ? "" : clientName;
			SimpleObject queueObject = SimpleObject.create("id", qObj.getId(), "uuid", qObj.getUuid(), "patientUuid", qObj
			        .getPatientUuid(), "discriminator", qObj.getDiscriminator(), "provider", qObj.getProvider().getName(),
			    "dateSubmitted", ui.formatDatePretty(qObj.getDateCreated()), "formName", qObj.getFormName()
			            .equalsIgnoreCase("Unknown name") ? "Registration" : qObj.getFormName(), "clientName", clientName);
			queueList.add(queueObject);
		}
		
		for (ErrorInfo eObj : allErrors) {
			List<String> errorMessages = new ArrayList<String>();
			
			for (ErrorMessagesInfo info : eObj.getErrorMessages()) {
				errorMessages.add(info.getMessage());
			}
			String clientName = eObj.getClientName();
			clientName = (clientName == null) ? "" : clientName;
			SimpleObject errorObject = SimpleObject.create("id", eObj.getId(), "uuid", eObj.getUuid(), "patientUuid", eObj
			        .getPatientUuid(), "message", StringUtils.join(errorMessages, ", "), "discriminator", eObj
			        .getDiscriminator(), "provider", eObj.getProvider().getName(), "clientName", clientName,
			    "dateProcessed", ui.formatDatePretty(eObj.getDateProcessed()), "formName", eObj.getFormName()
			            .equalsIgnoreCase("Unknown name") ? "Registration" : eObj.getFormName());
			errorList.add(errorObject);
		}
		
		for (ErrorInfo eObj : allRegistrationErrors) {
			List<String> errorMessages = new ArrayList<String>();
			
			for (ErrorMessagesInfo info : eObj.getErrorMessages()) {
				errorMessages.add(info.getMessage());
			}
			String clientName = eObj.getClientName();
			clientName = (clientName == null) ? "" : clientName;
			SimpleObject errorObject = SimpleObject.create("id", eObj.getId(), "uuid", eObj.getUuid(), "patientUuid", eObj
			        .getPatientUuid(), "message", StringUtils.join(errorMessages, ", "), "discriminator", eObj
			        .getDiscriminator(), "provider", eObj.getProvider().getName(), "clientName", clientName,
			    "dateProcessed", ui.formatDatePretty(eObj.getDateProcessed()), "formName", eObj.getFormName()
			            .equalsIgnoreCase("Unknown name") ? "Registration" : eObj.getFormName());
			registrationErrorList.add(errorObject);
		}
		
		Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
		
		String regStr = "select count(*) from medic_error_data where discriminator='json-registration';";
		String allErrorsSql = "select count(*) from medic_error_data;";
		String queueData = "select count(*) from medic_queue_data;";
		Long totalErrors = (Long) Context.getAdministrationService().executeSQL(allErrorsSql, true).get(0).get(0);
		Long registrationErrors = (Long) Context.getAdministrationService().executeSQL(regStr, true).get(0).get(0);
		Long queueDataTotal = (Long) Context.getAdministrationService().executeSQL(queueData, true).get(0).get(0);
		
		model.put("queueList", ui.toJson(queueList));
		model.put("queueListSize", queueList.size());
		
		model.put("generalErrorList", ui.toJson(errorList));
		model.put("generalErrorListSize", errorList.size());
		
		model.put("registrationErrorList", ui.toJson(registrationErrorList));
		model.put("registrationErrorListSize", registrationErrorList.size());
		
		model.put("totalErrors", totalErrors.intValue());
		model.put("registrationErrors", registrationErrors.intValue());
		model.put("queueData", queueDataTotal.intValue());
		Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
	}
}
