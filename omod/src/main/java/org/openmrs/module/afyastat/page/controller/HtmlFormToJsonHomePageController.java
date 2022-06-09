package org.openmrs.module.afyastat.page.controller;

import org.codehaus.jackson.node.ArrayNode;
import org.openmrs.module.afyastat.htmltojson.HtmlFormUtil;
import org.openmrs.module.kenyacore.form.FormManager;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.resource.ResourceFactory;

@AppPage("kenyaemr.afyastat.htmlToJson")
public class HtmlFormToJsonHomePageController {
	
	public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, @SpringBean FormManager formManager,
	        @SpringBean ResourceFactory resourceFactory, PageModel model) {
		
		ArrayNode allForms = HtmlFormUtil.getAllForms(formManager, resourceFactory);
		model.put("hfeForms", allForms);
		model.put("hfeFormsSize", allForms.size());
		
	}
}
