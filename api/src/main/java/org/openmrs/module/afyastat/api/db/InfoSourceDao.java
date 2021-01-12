package org.openmrs.module.afyastat.api.db;

import org.openmrs.module.afyastat.model.AfyaDataSource;

import java.util.List;

/**
 */
public interface InfoSourceDao extends SingleClassInfoDao<AfyaDataSource> {
	
	/**
	 * Return the data source with the given uuid.
	 * 
	 * @param uuid the data source uuid.
	 * @return the data source with the matching uuid.
	 * @should return data with matching uuid.
	 * @should return null when no data with matching uuid.
	 */
	AfyaDataSource getDataSourceByUuid(final String uuid);
	
	/**
	 * Get data source with matching search term for particular page.
	 * 
	 * @param search the search term.
	 * @param pageNumber the page number.
	 * @param pageSize the size of the page.
	 * @return list of data source for the page.
	 */
	List<AfyaDataSource> getPagedDataSources(final String search, final Integer pageNumber, final Integer pageSize);
	
	/**
	 * Get the total number of data source with matching search term.
	 * 
	 * @param search the search term.
	 * @return total number of data source in the database.
	 */
	Number countDataSource(final String search);
}
