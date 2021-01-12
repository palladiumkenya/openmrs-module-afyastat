/*

*/
/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
/*

package org.openmrs.module.afyastat.task;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
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
import org.openmrs.module.hivtestingservices.metadata.HTSMetadata;
import org.openmrs.module.hivtestingservices.util.MedicDataExchange;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.List;

*/
/**
 * Periodically refreshes ETL tables
 */
/*

public class PushContactsToMedicMobileTask extends AbstractTask {

 private Log log = LogFactory.getLog(getClass());

 */
/**
 * @see AbstractTask#execute()
 */
/*

public void execute() {
 Context.openSession();
 try {

     if (!Context.isAuthenticated()) {
         authenticate();
     }

     GlobalProperty lastPatientEntry = Context.getAdministrationService().getGlobalPropertyObject(HTSMetadata.MEDIC_MOBILE_LAST_PATIENT_ENTRY);
     String lastContactRegistrationIdsql = "select max(patient_id) last_id from kenyaemr_hiv_testing_patient_contact where voided=0 and patient_id is not null and contact_listing_decline_reason='CHT';";
     List<List<Object>> lastContactRegistrationRs = Context.getAdministrationService().executeSQL(lastContactRegistrationIdsql, true);
     Integer lastPatientId = (Integer) lastContactRegistrationRs.get(0).get(0);
     lastPatientId = lastPatientId != null ? lastPatientId : 0;


     GlobalProperty lastContactEntry = Context.getAdministrationService().getGlobalPropertyObject(HTSMetadata.MEDIC_MOBILE_LAST_PATIENT_CONTACT_ENTRY);
     GlobalProperty chtServerName = Context.getAdministrationService().getGlobalPropertyObject(HTSMetadata.MEDIC_MOBILE_SERVER_URL);
     GlobalProperty chtUser = Context.getAdministrationService().getGlobalPropertyObject(HTSMetadata.MEDIC_MOBILE_USER);
     GlobalProperty chtPwd = Context.getAdministrationService().getGlobalPropertyObject(HTSMetadata.MEDIC_MOBILE_PWD);
     String lastContactSql = "select max(id) last_id from kenyaemr_hiv_testing_patient_contact where voided=0;";
     List<List<Object>> maxOrderId = Context.getAdministrationService().executeSQL(lastContactSql, true);

     Integer lastId = (Integer) maxOrderId.get(0).get(0);
     lastId = lastId != null ? lastId : 0;

     String lastContactIdStr = lastContactEntry != null && lastContactEntry.getValue() != null ? lastContactEntry.getValue().toString() : "";
     Integer gpLastContactId = StringUtils.isNotBlank(lastContactIdStr) ? Integer.parseInt(lastContactIdStr) : 0;

     String lastPatientIdStr = lastPatientEntry != null && lastPatientEntry.getValue() != null ? lastPatientEntry.getValue().toString() : "";
     Integer gpLastPatientId = StringUtils.isNotBlank(lastPatientIdStr) ? Integer.parseInt(lastPatientIdStr) : 0;

     boolean hasData = false;

     String serverUrl = chtServerName.getPropertyValue();
     String username = chtUser.getPropertyValue();
     String pwd = chtPwd.getPropertyValue();

     if (serverUrl == null || username == null || pwd == null) {
         System.out.println("Please set credentials for pushing contacts to Medic Mobile CHT");
         return;

     }

     MedicDataExchange e = new MedicDataExchange();


     // check if there are item(s) to post
     ObjectNode contactWrapper = e.getContacts(gpLastContactId, lastId, gpLastPatientId, lastPatientId);
     ArrayNode docs = (ArrayNode) contactWrapper.get("docs");
     System.out.println("Data: " + contactWrapper.toString());

     if (contactWrapper != null && docs.size() > 0) {
         hasData = true;
     }

     System.out.println("CHT Post request. Records found: " + docs.size());
     //System.out.println("CHT Post request : " + docs.toString());

     if (serverUrl != null && username != null && pwd != null && hasData) {
         String payload = contactWrapper.toString();
         CloseableHttpClient httpClient = HttpClients.createDefault();


         try {
             //Define a postRequest request
             HttpPost postRequest = new HttpPost(serverUrl);

             //Set the API media type in http content-type header
             postRequest.addHeader("content-type", "application/json");

             String auth = username.trim() + ":" + pwd.trim();
             byte[] encodedAuth = Base64.encodeBase64(
                     auth.getBytes("UTF-8"));
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
             //lastContactEntry.setPropertyValue(lastId.toString());
             lastPatientEntry.setPropertyValue(lastPatientId.toString());

             //Context.getAdministrationService().saveGlobalProperty(lastContactEntry);
             Context.getAdministrationService().saveGlobalProperty(lastPatientEntry);

             System.out.println("Successfully pushed contacts to Medic Mobile CHT");
             log.info("Successfully pushed contacts to Medic Mobile CHT");
         } finally {
             //Important: Close the connect
             httpClient.close();
         }
     }
 } catch (Exception e) {
     throw new IllegalArgumentException("Medic Mobile contact list POST task could not be executed!", e);
 }
}

}*/
