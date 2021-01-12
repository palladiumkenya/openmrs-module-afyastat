package org.openmrs.module.afyastat.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.afyastat.util.MedicDataExchange;
import org.openmrs.module.afyastat.util.Utils;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * The main controller.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/edata")
public class MobileApplicationRestController extends BaseRestController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@RequestMapping(method = RequestMethod.POST, value = "/medicregistration")
	@ResponseBody
	public Object receiveSHR(HttpServletRequest request) {
		
		String requestBody = null;
		try {
			requestBody = Utils.fetchRequestBody(request.getReader());
		}
		catch (IOException e) {
			return new SimpleObject().add("ServerResponse", "Error extracting request body");
		}
		
		if (requestBody != null) {
			MedicDataExchange shr = new MedicDataExchange();
			return shr.processIncomingRegistration(requestBody);
			
		}
		return new SimpleObject().add("Report", "The request could not be interpreted properly");
	}
	
	/**
	 * processes incoming medic queue data
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/medicformsdata")
	// end point for medic queue data
	@ResponseBody
	public Object processMedicQueueData(HttpServletRequest request) {
		String requestBody = null;
		try {
			requestBody = Utils.fetchRequestBody(request.getReader());
		}
		catch (IOException e) {
			return new SimpleObject().add("ServerResponse", "Error extracting request body");
		}
		
		if (requestBody != null) {
			MedicDataExchange shr = new MedicDataExchange();
			return shr.processIncomingFormData(requestBody);
			
		}
		return new SimpleObject().add("Report", "The request could not be interpreted properly");
	}
	
	/**
	 * processes incoming medic contacts data
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/mediccontactsdata")
	// end point for medic contacts data
	@ResponseBody
	public Object processMedicContactsData(HttpServletRequest request) {
		String requestBody = null;
		try {
			requestBody = Utils.fetchRequestBody(request.getReader());
		}
		catch (IOException e) {
			return new SimpleObject().add("ServerResponse", "Error extracting request body");
		}
		
		if (requestBody != null) {
			MedicDataExchange shr = new MedicDataExchange();
			return shr.addContactListToDataqueue(requestBody);
			
		}
		return new SimpleObject().add("Report", "The request could not be interpreted properly");
	}
	
	/**
	 * processes incoming medic contacts trace data
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/mediccontacttracedata")
	// end point for medic contact trace data
	@ResponseBody
	public Object processMedicContactTraceData(HttpServletRequest request) {
		String requestBody = null;
		try {
			requestBody = Utils.fetchRequestBody(request.getReader());
		}
		catch (IOException e) {
			return new SimpleObject().add("ServerResponse", "Error extracting request body");
		}
		
		if (requestBody != null) {
			MedicDataExchange shr = new MedicDataExchange();
			return shr.addContactTraceToDataqueue(requestBody);
			
		}
		return new SimpleObject().add("Report", "The request could not be interpreted properly");
	}
	
	/**
	 * processes demographics updates
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/medicdemographicupdates")
	// end point for processing demographic updates
	@ResponseBody
	public Object processDemographicUpdates(HttpServletRequest request) {
		String requestBody = null;
		try {
			requestBody = Utils.fetchRequestBody(request.getReader());
		}
		catch (IOException e) {
			return new SimpleObject().add("ServerResponse", "Error extracting request body");
		}
		
		if (requestBody != null) {
			MedicDataExchange shr = new MedicDataExchange();
			return shr.processDemographicsUpdate(requestBody);
			
		}
		return new SimpleObject().add("Report", "The request could not be interpreted properly");
	}
	
	/**
	 * processes peer calender form
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/medicpeercalendar")
	// end point for processing peer calender information
	@ResponseBody
	public Object processPeerCalenderForm(HttpServletRequest request) {
		String requestBody = null;
		try {
			requestBody = Utils.fetchRequestBody(request.getReader());
		}
		catch (IOException e) {
			return new SimpleObject().add("ServerResponse", "Error extracting request body");
		}
		
		if (requestBody != null) {
			MedicDataExchange shr = new MedicDataExchange();
			return shr.processPeerCalenderFormData(requestBody);
			
		}
		return new SimpleObject().add("Report", "The request could not be interpreted properly");
	}
}
