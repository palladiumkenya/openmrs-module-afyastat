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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.metadata.AfyaStatMetadata;
import org.openmrs.module.afyastat.model.MedicOutgoingRegistration;
import org.openmrs.module.afyastat.util.MedicDataExchange;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * Periodically pushes registered contacts to Afyastat
 */
public class PushContactsToMedicMobileTask extends AbstractTask {
	
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
				System.out
				        .println("Afyastat POST contact list to CHT: Please set credentials for pushing contacts to Medic Mobile CHT");
				return;
			}
			
			GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject(
			    AfyaStatMetadata.AFYASTAT_CONTACT_LIST_LAST_FETCH_TIMESTAMP);
			if (globalPropertyObject == null) {
				System.out.println("Afyastat POST contact list to CHT: Missing required global property: "
				        + AfyaStatMetadata.AFYASTAT_CONTACT_LIST_LAST_FETCH_TIMESTAMP);
				return;
			}
			
			MedicDataExchange e = new MedicDataExchange();
			
			// check if there are item(s) to post (limit to 50)
			List<MedicOutgoingRegistration> payloads = e.getQueuedPayloads(50);
			if (payloads == null) {
				System.out
				        .println("Afyastat POST contact list to CHT: Could not fetch registered contacts from the database. Please contact your system administrator for more troubleshooting");
				log.error("Afyastat POST contact list to CHT: Could not fetch registered contacts from the database. Please contact your system administrator for more troubleshooting");
				return;
			}
			
			if (payloads != null && payloads.size() > 0) {
				hasData = true;
			}
			
			System.out.println("Afyastat POST contact list to CHT: CHT Post request. Records found: " + payloads.size());
			
			if (serverUrl != null && username != null && pwd != null && hasData) {
				
				//loop
				Iterator<MedicOutgoingRegistration> payloadIterator = payloads.iterator();
				while (payloadIterator.hasNext()) {
					//HTTP Client
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
						MedicOutgoingRegistration medicOutgoingRegistration = payloadIterator.next();
						StringEntity userEntity = new StringEntity(medicOutgoingRegistration.getPayload());
						postRequest.setEntity(userEntity);
						
						//Send the request; It will immediately return the response in HttpResponse object if any
						HttpResponse response = httpClient.execute(postRequest);
						
						//verify the valid error code first
						int statusCode = response.getStatusLine().getStatusCode();
						
						if (statusCode != 200 && statusCode != 201) {
							System.err.println("Afyastat POST contact list to CHT: Failed with HTTP error code : "
							        + statusCode);
							log.info("Afyastat POST contact list to CHT: Failed with HTTP error code : " + statusCode);
						} else {
							//mark the upload as successfull
							e.setContactQueuePayloadStatus(medicOutgoingRegistration.getId(), 1);
							
							System.out
							        .println("Afyastat POST contact list to CHT: Successfully pushed a contact to Afyastat");
							log.info("Afyastat POST contact list to CHT: Successfully pushed a contact to Afyastat");
						}
					}
					catch (Exception bn) {
						System.err.println("Afyastat POST contact list to CHT: Failed to push a contact to Afyastat: "
						        + bn.getMessage());
						log.info("Afyastat POST contact list to CHT: Failed to push a contact to Afyastat: "
						        + bn.getMessage());
						bn.printStackTrace();
					}
					finally {
						//Important: Close the connection
						httpClient.close();
					}
				}
				//end loop
				
				System.out.println("Afyastat POST contact list to CHT: Finished sending contacts to Afyastat");
				log.info("Afyastat POST contact list to CHT: Finished sending contacts to Afyastat");
			} else {
				System.out.println("Afyastat POST contact list to CHT: No contacts to send to Afyastat");
				log.info("Afyastat POST contact list to CHT: No contacts to send to Afyastat");
			}
			
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Afyastat POST contact list to CHT: Task could not be executed!", e);
		}
	}
}
