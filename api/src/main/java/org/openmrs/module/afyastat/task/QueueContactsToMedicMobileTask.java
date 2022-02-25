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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.util.MedicDataExchange;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * Periodically queues registered contacts to Afyastat
 */
public class QueueContactsToMedicMobileTask extends AbstractTask {
	
	private Log log = LogFactory.getLog(getClass());
	
	/**
	 * @see AbstractTask#execute()
	 */
	public void execute() {
		Context.openSession();
		try {
			
			MedicDataExchange medicDataExchange = new MedicDataExchange();
			
			//Queue the contacts
			medicDataExchange.queueContacts();
			
			//Queue the peer educators and peers
			medicDataExchange.queueKpPeerPeerEductorList();
			
			//test payload
			//medicDataExchange.getContacts();
			
			System.out.println("Successfully queued contacts for Afyastat");
			log.info("Successfully queued contacts for Afyastat");
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Afyastat QUEUE contact list task could not be executed!", e);
		}
	}
}
