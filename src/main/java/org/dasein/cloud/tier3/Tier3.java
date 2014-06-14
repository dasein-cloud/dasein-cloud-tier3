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

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.dasein.cloud.AbstractCloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.tier3.compute.Tier3ComputeServices;
import org.dasein.cloud.tier3.compute.Tier3ComputeTranslations;
import org.dasein.cloud.tier3.network.Tier3NetworkServices;
import org.dasein.cloud.tier3.network.Tier3NetworkTranslations;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.time.Hour;
import org.dasein.util.uom.time.TimePeriod;
import org.json.JSONException;
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
	public NetworkServices getNetworkServices() {
		return new Tier3NetworkServices(this);
	}

	public Tier3NetworkTranslations getNetworkTranslations() {
		return new Tier3NetworkTranslations();
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
				APIResponse response = new APIHandler(this).post("Account/GetAccountDetails/JSON", json.toString());
				response.validate();
				if (response.getJSON().getString("AccountDetails") != null) {
					return response.getJSON().getJSONObject("AccountDetails").getString("AccountAlias");
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
	public String logon() throws CloudException, InternalException {
		if (logger.isTraceEnabled()) {
			logger.trace("ENTER - " + Tier3.class.getName() + ".logon()");
		}
		String sessionId = null;
		try {
			byte[][] keys = (byte[][]) getContext().getConfigurationValue("apiAccessKey");
			String apiKey = new String(keys[0], "utf-8");
			String apiPass = new String(keys[1], "utf-8");

			Cache<String> cacheComp = Cache.getInstance(this, "sessionComparables", String.class,
					CacheLevel.CLOUD_ACCOUNT);
			Iterable<String> sessionComparables = cacheComp.get(this.getContext());

			Boolean refreshSession = false;
			if (sessionComparables == null
					|| !sessionComparables.iterator().next().equals(getContext().getAccountNumber() + apiKey + apiPass)) {
				cacheComp.put(getContext(), Arrays.asList(getContext().getAccountNumber() + apiKey + apiPass));
				refreshSession = true;
			}

			Cache<String> cache = Cache.getInstance(this, "sessionIds", String.class, CacheLevel.CLOUD_ACCOUNT,
					new TimePeriod<Hour>(4, TimePeriod.HOUR));
			Iterable<String> sessionIds = cache.get(this.getContext());
			if (sessionIds == null || refreshSession) {
				JSONObject json = new JSONObject();
				json.put("APIKey", apiKey);
				json.put("Password", apiPass);
				APIResponse response = new APIHandler(this).post("Auth/Logon/", json.toString());
				if (response != null && response.getJSON() != null && response.getJSON().has("Session")) {
					sessionId = response.getJSON().getString("Session");
				} else {
					throw new InternalException("Error obtaining session with supplied credentials");
				}
				cache.put(getContext(), Arrays.asList(sessionId));
			} else {
				sessionId = sessionIds.iterator().next();
			}
			return sessionId;

		} catch (JSONException e) {
			throw new CloudException(e);

		} catch (UnsupportedEncodingException e) {
			throw new CloudException(e);

		} finally {
			if (logger.isTraceEnabled()) {
				logger.trace("EXIT - " + Tier3.class.getName() + ".logon()");
			}
		}
	}

	public JSONObject getDeploymentStatus(int requestId) throws CloudException, InternalException {
		if (logger.isTraceEnabled()) {
			logger.trace("ENTER - " + Tier3.class.getName() + ".getDeploymentStatus()");
		}
		try {

			APIHandler method = new APIHandler(this);
			JSONObject post = new JSONObject();
			post.put("RequestId", requestId);
			APIResponse response = method.post("Blueprint/GetDeploymentStatus/JSON", post.toString());

			if (response == null) {
				throw new CloudException("Could not retrieve server build request");
			}

			JSONObject json = response.getJSON();
			if (json.has("Success") && !json.getBoolean("Success")) {
				throw new CloudException(json.getString("Message"));
			}

			return json;

		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			if (logger.isTraceEnabled()) {
				logger.trace("EXIT - " + Tier3.class.getName() + ".getDeploymentStatus()");
			}
		}
	}

	public long parseTimestamp(String time) throws CloudException {
		if (time == null) {
			return 0L;
		}
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

		if (time.length() > 0) {
			try {
				return fmt.parse(time).getTime();
			} catch (ParseException e) {
				fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				try {
					return fmt.parse(time).getTime();
				} catch (ParseException encore) {
					fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					try {
						return fmt.parse(time).getTime();
					} catch (ParseException again) {
						try {
							return fmt.parse(time).getTime();
						} catch (ParseException whynot) {
							fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							try {
								return fmt.parse(time).getTime();
							} catch (ParseException because) {
								throw new CloudException("Could not parse date: " + time);
							}
						}
					}
				}
			}
		}
		return 0L;
	}

	@Override
	public ContextRequirements getContextRequirements() {
		return new ContextRequirements(new ContextRequirements.Field("apiAccessKey",
				ContextRequirements.FieldType.KEYPAIR));
	}
}