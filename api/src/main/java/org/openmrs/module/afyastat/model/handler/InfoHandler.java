package org.openmrs.module.afyastat.model.handler;

import org.openmrs.module.afyastat.model.AfyaData;

/**
 */
public interface InfoHandler {
	
	/**
	 * Flag whether the current data handler can handle certain data.
	 * 
	 * @param data the data.
	 * @return true if the handler can handle the data.
	 */
	boolean accept(final AfyaData data);
	
	/**
	 * Handler that will be executed when a data is retrieved.
	 * 
	 * @param data the data.
	 */
	void handleGet(final AfyaData data);
	
	/**
	 * Handler that will be executed when a data is saved.
	 * 
	 * @param data the data.
	 */
	void handleSave(final AfyaData data);
	
	/**
	 * Handler that will be executed when a data is deleted.
	 * 
	 * @param data the data.
	 */
	void handleDelete(final AfyaData data);
	
}
