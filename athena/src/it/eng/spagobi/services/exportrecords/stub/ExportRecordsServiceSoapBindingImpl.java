/* SpagoBI, the Open Source Business Intelligence suite

 * Copyright (C) 2012 Engineering Ingegneria Informatica S.p.A. - SpagoBI Competency Center
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0, without the "Incompatible With Secondary Licenses" notice.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package it.eng.spagobi.services.exportrecords.stub;

import org.apache.log4j.Logger;

public class ExportRecordsServiceSoapBindingImpl implements it.eng.spagobi.services.exportrecords.stub.ExportRecordsService {

	static protected Logger logger = Logger.getLogger(ExportRecordsServiceSoapBindingImpl.class);

	public java.lang.String processRecords(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException {
		logger.debug("******************");
		logger.debug("Service START");
		logger.debug("Records: " + in0);
		logger.debug("Operation: " + in1);
		logger.debug("Service END");
		logger.debug("******************");
		return "Records processed properly";
	}

}
