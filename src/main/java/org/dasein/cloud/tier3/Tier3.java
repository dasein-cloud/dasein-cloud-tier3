/**
 * Copyright (C) 2012-2013 Dell, Inc.
 * See annotations for authorship information
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.tier3;

import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.dasein.cloud.AbstractCloud;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.tier3.compute.Tier3ComputeServices;
import org.dasein.cloud.tier3.compute.Tier3ComputeTranslations;
import org.json.JSONObject;

/**
 * Add header info here
 * 
 * @version 2013.01 initial version
 * @since 2013.01
 */
public class Tier3 extends AbstractCloud {
	static private final Logger logger = getLogger(Tier3.class);

	static private @Nonnull
	String getLastItem(@Nonnull String name) {
		int idx = name.lastIndexOf('.');

		if (idx < 0) {
			return name;
		} else if (idx == (name.length() - 1)) {
			return "";
		}
		return name.substring(idx + 1);
	}

	static public @Nonnull
	Logger getLogger(@Nonnull Class<?> cls) {
		String pkg = getLastItem(cls.getPackage().getName());

		if (pkg.equals("centurylink")) {
			pkg = "";
		} else {
			pkg = pkg + ".";
		}
		return Logger.getLogger("dasein.cloud.centurylink.std." + pkg + getLastItem(cls.getName()));
	}

	static public @Nonnull
	Logger getWireLogger(@Nonnull Class<?> cls) {
		return Logger.getLogger("dasein.cloud.centurylink.wire." + getLastItem(cls.getPackage().getName()) + "."
				+ getLastItem(cls.getName()));
	}

	public Tier3() {
	}

	@Override
	public @Nonnull
	String getCloudName() {
		ProviderContext ctx = getContext();
		String name = (ctx == null ? null : ctx.getCloudName());

		return (name == null ? "CenturyLink" : name);
	}

	@Override
	public @Nonnull
	DataCenters getDataCenterServices() {
		return new DataCenters(this);
	}
	
	@Override
	public @Nonnull
	String getProviderName() {
		ProviderContext ctx = getContext();
		String name = (ctx == null ? null : ctx.getProviderName());

		return (name == null ? "CenturyLink" : name);
	}

	@Override
	public ComputeServices getComputeServices() {
		return new Tier3ComputeServices(this);
	}
	
	public Tier3ComputeTranslations getComputeTranslations() {
		return new Tier3ComputeTranslations();
	}

	@Override
	public @Nullable
	String testContext() {
		if (logger.isTraceEnabled()) {
			logger.trace("ENTER - " + Tier3.class.getName() + ".testContext()");
		}
		try {
			ProviderContext ctx = getContext();

			if (ctx == null) {
				logger.warn("No context was provided for testing");
				return null;
			}
			if (ctx.getAccountNumber() != null && ctx.getAccountNumber().length() > 4) {
				logger.warn("Invalid account number");
				return null;
			}
			try {
				JSONObject json = new JSONObject();
				json.put("AccountAlias", ctx.getAccountNumber());
				APIResponse response = new APIHandler(this).post("Account/GetAccountDetails/JSON",
						json.toString());
				if (response != null) {
					if (response.getJSON().getBoolean("Success") && response.getJSON().getString("AccountDetails") != null) {
						return response.getJSON().getJSONObject("AccountDetails").getString("AccountAlias");
					}
				}
				return null;
			} catch (Throwable t) {
				logger.error("Error querying API key: " + t.getMessage());
				t.printStackTrace();
				return null;
			}
		} finally {
			if (logger.isTraceEnabled()) {
				logger.trace("EXIT - " + Tier3.class.getName() + ".textContext()");
			}
		}
	}

	@Nonnull
	public void logon() {
		if (logger.isTraceEnabled()) {
			logger.trace("ENTER - " + Tier3.class.getName() + ".logon()");
		}
		try {
			ProviderContext ctx = this.getContext();
			connect(ctx);

			try {
				Properties customProps = ctx.getCustomProperties();

				JSONObject json = new JSONObject();
				json.put("APIKey", customProps.getProperty("APIKey"));
				json.put("Password", customProps.getProperty("Password"));
				new APIHandler(this).post("Auth/Logon/", json.toString());

			} catch (Throwable t) {
				logger.error("Error querying API key: " + t.getMessage());
				t.printStackTrace();
			}

		} finally {
			if (logger.isTraceEnabled()) {
				logger.trace("EXIT - " + Tier3.class.getName() + ".logon()");
			}
		}
	}
}