package org.openmrs.module.afyastat.page.controller;

import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

@AppPage("kenyaemr.afyastat.home")
public class AfyastatOutgoingQueuePageController {
	
	public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) {
		
	}
}
