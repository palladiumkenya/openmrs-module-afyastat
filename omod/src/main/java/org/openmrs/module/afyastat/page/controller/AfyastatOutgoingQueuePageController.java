package org.openmrs.module.afyastat.page.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.afyastat.api.service.MedicOutgoingRegistrationService;
import org.openmrs.module.afyastat.model.MedicOutgoingRegistration;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.util.PrivilegeConstants;
import java.text.SimpleDateFormat;

@AppPage("kenyaemr.afyastat.home")
public class AfyastatOutgoingQueuePageController {
	
	public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) {
		MedicOutgoingRegistrationService medicOutgoingRegistrationService = Context
		        .getService(MedicOutgoingRegistrationService.class);
		
		//Get all queue records with status 0 i.e pending sync
		List<MedicOutgoingRegistration> outgoingQueueList = medicOutgoingRegistrationService.getRecordsByStatus(0);
		
		Collections.sort(outgoingQueueList, new Comparator<MedicOutgoingRegistration>() {
			
			@Override
			public int compare(MedicOutgoingRegistration a, MedicOutgoingRegistration b) {
				return a.getDateCreated().after(b.getDateCreated()) ? -1 : a.getDateCreated() == b.getDateCreated() ? 0 : 1;
			}
		});
		
		List<SimpleObject> queueList = new ArrayList<SimpleObject>();
		
		for (MedicOutgoingRegistration qObj : outgoingQueueList) {
			Person person = Context.getPersonService().getPerson(qObj.getPatientId());
			
			String clientName = person.getGivenName();
			clientName = (clientName == null) ? "" : clientName;
			
			String familyName = person.getFamilyName();
			clientName += (familyName == null) ? "" : (" " + familyName);
			
			String middleName = person.getMiddleName();
			clientName += (middleName == null) ? "" : (" " + middleName);
			
			clientName = (clientName == null) ? "" : clientName;
			
			String dateCreated = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(qObj.getDateCreated());
			
			SimpleObject queueObject = SimpleObject.create("id", qObj.getId(), "uuid", qObj.getUuid(), "patientUuid",
			    qObj.getPatientId(), "purpose", qObj.getPurpose(), "dateCreated", dateCreated, "clientName", clientName,
			    "hasEntry", true, "status", (qObj.getStatus() == 0) ? "Pending" : "Sent");
			queueList.add(queueObject);
		}
		
		Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
		
		String queueDataCount = "select count(*) from kenyaemr_afyastat_medicoutgoingregistration;";
		String sentDataCount = "select count(*) from kenyaemr_afyastat_medicoutgoingregistration where status=1;";
		Long queueDataTotal = (Long) Context.getAdministrationService().executeSQL(queueDataCount, true).get(0).get(0);
		Long sentDataTotal = (Long) Context.getAdministrationService().executeSQL(sentDataCount, true).get(0).get(0);
		
		model.put("queueList", ui.toJson(queueList));
		model.put("queueListSize", queueList.size());
		model.put("totalQueueCount", queueDataTotal.intValue());
		model.put("sentQueueCount", sentDataTotal.intValue());
		
		Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
	}
	
}
