/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008 - 2011 LSC Project 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2011 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */

package org.lsc.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.lsc.Configuration;
import org.lsc.LscAttributes;
import org.lsc.configuration.objects.Task;
import org.lsc.configuration.objects.connection.Database;
import org.lsc.configuration.objects.services.SrcDatabase;
import org.lsc.exception.LscConfigurationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.exception.LscServiceInitializationException;

/**
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 *
 */
public class SimpleJdbcSrcService extends AbstractJdbcService implements IAsynchronousService {

	private final String requestNameForList;
	private final String requestNameForNextId;
	private final String requestNameForObject;
	
	private int interval;

	/**
	 * Simple JDBC source service that gets SQL request names from lsc.properties
	 * and calls the appropriate SQL requests defined in sql-map-config.d
	 * 
	 * @deprecated
	 * @param props Configuration properties
	 * @throws LscServiceInitializationException 
	 */
	@Deprecated
	public SimpleJdbcSrcService(Properties props, String beanClassName) throws LscServiceException {
		super(Configuration.getAsProperties("src.database"));
		requestNameForList = props.getProperty("requestNameForList");
		requestNameForObject = props.getProperty("requestNameForObject");
		requestNameForNextId = props.getProperty("requestNameForNextId");
		
		try {
			// check that we have all parameters, or abort
			Configuration.assertPropertyNotEmpty("requestNameForList", requestNameForList, this.getClass().getName());
			Configuration.assertPropertyNotEmpty("requestNameForObject", requestNameForObject, this.getClass().getName());
			
			if(props.getProperty("interval") != null) {
				interval = Integer.parseInt(props.getProperty("interval"));
			} else {
				interval = -1;
			}
		} catch (NumberFormatException e) {
			throw new LscServiceInitializationException(e);
		} catch (LscConfigurationException e) {
			throw new LscServiceConfigurationException(e);
		}
	}

	/**
	 * Simple JDBC source service that gets SQL request names from lsc.properties
	 * and calls the appropriate SQL requests defined in sql-map-config.d
	 * 
	 * @param task Initialized task containing all necessary pieces of information to initiate connection
	 * 				and load settings 
	 * @throws LscServiceInitializationException 
	 */
	public SimpleJdbcSrcService(final Task task) throws LscServiceException {
		super((Database)task.getSourceService().getConnection(), task.getBean());
		SrcDatabase serviceConf = (SrcDatabase)task.getSourceService();
		requestNameForList = serviceConf.getRequestNameForList();
		requestNameForObject = serviceConf.getRequestNameForObject();
		requestNameForNextId = serviceConf.getRequestNameForNextId();
		
		interval = serviceConf.getInterval();
	}

	/* (non-Javadoc)
	 * @see org.lsc.service.AbstractJdbcService#getRequestNameForList()
	 */
	@Override
	public String getRequestNameForList() {
		return requestNameForList;
	}

	/* (non-Javadoc)
	 * @see org.lsc.service.AbstractJdbcService#getRequestNameForObject()
	 */
	@Override
	public String getRequestNameForObject() {
		return requestNameForObject;
	}

	/* (non-Javadoc)
	 * @see org.lsc.service.AbstractJdbcService#getRequestNameForId()
	 */
	@Override
	public String getRequestNameForNextId() {
		return requestNameForNextId;
	}

	static int count = 0;

	@SuppressWarnings("unchecked")
	public Entry<String, LscAttributes> getNextId() {
		Map<String, Object> idMap;
		try {
			idMap = (Map<String, Object>) sqlMapper.queryForObject(getRequestNameForNextId());
			String key = getMapKey(idMap, count++);
			Map<String, LscAttributes> ret = new HashMap<String, LscAttributes>();
			ret.put(key, new LscAttributes(idMap));
			return ret.entrySet().iterator().next();
		} catch (SQLException e) {
			LOGGER.warn("Error while looking for next entry ({})", e);
			LOGGER.debug(e.toString(), e);
		}
		
		return null;
	}
	
	public long getInterval() {
		return interval;
	}
}
