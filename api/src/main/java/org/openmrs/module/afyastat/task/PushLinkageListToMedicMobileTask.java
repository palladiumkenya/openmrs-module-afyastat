/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.afyastat.task;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.metadata.AfyaStatMetadata;
import org.openmrs.module.afyastat.util.MedicDataExchange;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Periodically pushes clients who turned positive but haven't been linked to care
 */
public class PushLinkageListToMedicMobileTask extends AbstractTask {
	
	private Log log = LogFactory.getLog(getClass());
	
	/**
	 * @see AbstractTask#execute()
	 */
	public void execute() {
		Context.openSession();
		try {
			
			GlobalProperty chtServerName = Context.getAdministrationService().getGlobalPropertyObject(
			    AfyaStatMetadata.MEDIC_MOBILE_SERVER_URL);
			GlobalProperty chtUser = Context.getAdministrationService().getGlobalPropertyObject(
			    AfyaStatMetadata.MEDIC_MOBILE_USER);
			GlobalProperty chtPwd = Context.getAdministrationService().getGlobalPropertyObject(
			    AfyaStatMetadata.MEDIC_MOBILE_PWD);
			
			boolean hasData = false;
			
			String serverUrl = chtServerName.getPropertyValue();
			String username = chtUser.getPropertyValue();
			String pwd = chtPwd.getPropertyValue();
			
			if (serverUrl == null || username == null || pwd == null) {
				System.out.println("Please set credentials for pushing contacts to Medic Mobile CHT");
				return;
			}
			
			MedicDataExchange e = new MedicDataExchange();
			
			GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject(
			    AfyaStatMetadata.AFYASTAT_LINKAGE_LIST_LAST_FETCH_TIMESTAMP);
			if (globalPropertyObject == null) {
				System.out.println("Missing required global property: "
				        + AfyaStatMetadata.AFYASTAT_LINKAGE_LIST_LAST_FETCH_TIMESTAMP);
				return;
			}
			
			// check if there are item(s) to post
			ObjectNode positiveNotLinkedWrapper = e.getLinkageList();
			
			if (positiveNotLinkedWrapper == null) {
				System.out
				        .println("Could not fetch linkage clients from the database. Please contact your system administrator for more troubleshooting");
				log.error("Could not fetch linkage clients from the database. Please contact your system administrator for more troubleshooting");
				return;
			}
			ArrayNode docs = (ArrayNode) positiveNotLinkedWrapper.get("docs");
			String nextFetchTimestamp = positiveNotLinkedWrapper.get("timestamp").getTextValue();
			
			if (positiveNotLinkedWrapper != null && docs.size() > 0) {
				hasData = true;
			}
			
			System.out.println("Afyastat linkage list. Records found: " + docs.size());
			
			if (serverUrl != null && username != null && pwd != null && hasData) {
				String payload = positiveNotLinkedWrapper.toString();
				CloseableHttpClient httpClient = HttpClients.createDefault();
				
				try {
					//Define a postRequest request
					HttpPost postRequest = new HttpPost(serverUrl);
					
					//Set the API media type in http content-type header
					postRequest.addHeader("content-type", "application/json");
					
					String auth = username.trim() + ":" + pwd.trim();
					byte[] encodedAuth = Base64.encodeBase64(auth.getBytes("UTF-8"));
					String authHeader = "Basic " + new String(encodedAuth);
					postRequest.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
					
					//Set the request post body
					StringEntity userEntity = new StringEntity(payload);
					postRequest.setEntity(userEntity);
					
					//Send the request; It will immediately return the response in HttpResponse object if any
					HttpResponse response = httpClient.execute(postRequest);
					
					//verify the valid error code first
					int statusCode = response.getStatusLine().getStatusCode();
					
					if (statusCode != 200 && statusCode != 201) {
						throw new RuntimeException("Failed with HTTP error code : " + statusCode);
					}
					
					// save this at the end just so that we take care of instances when there is no connectivity
					globalPropertyObject.setPropertyValue(nextFetchTimestamp);
					Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
					
					System.out.println("Successfully pushed list of clients for linkage to Afyastat");
					log.info("Successfully pushed list of clients for linkage to Afyastat");
				}
				finally {
					//Important: Close the connect
					httpClient.close();
				}
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Afyastat linkage list task could not be executed!", e);
		}
	}
	
}
