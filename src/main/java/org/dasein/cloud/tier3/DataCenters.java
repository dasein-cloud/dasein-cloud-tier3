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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.dc.*;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Unimplemented centurylink class
 * 
 * @author David Young
 * @version 2013.01 initial version
 * @since 2013.01
 */
public class DataCenters implements DataCenterServices {
    static private final Logger logger = Tier3.getLogger(DataCenters.class);

    private Tier3 provider;
    private volatile transient DCCapabilities capabilities;

    DataCenters(@Nonnull Tier3 provider) {
        this.provider = provider;
    }

    @Override
    public @Nonnull DataCenterCapabilities getCapabilities() throws InternalException, CloudException {
        if( capabilities == null ) {
            capabilities = new DCCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    public @Nullable
    DataCenter getDataCenter(@Nonnull String dataCenterId) throws InternalException, CloudException {
        for (Region region : listRegions()) {
            for (DataCenter dc : listDataCenters(region.getProviderRegionId())) {
                if (dataCenterId.equals(dc.getProviderDataCenterId())) {
                    return dc;
                }
            }
        }
        return null;
    }

    @Override
    @Deprecated
    public @Nonnull String getProviderTermForDataCenter(@Nonnull Locale locale) {
        try {
            return getCapabilities().getProviderTermForDataCenter(locale);
        } catch( InternalException e ) {
        } catch( CloudException e ) {
        }
        return "data center";
    }

    @Override
    @Deprecated
    public @Nonnull String getProviderTermForRegion(@Nonnull Locale locale) {
        try {
            return getCapabilities().getProviderTermForRegion(locale);
        } catch( InternalException e ) {
        } catch( CloudException e ) {
        }
        return "region";
    }

    @Override
    public @Nullable
    Region getRegion(@Nonnull String providerRegionId) throws InternalException, CloudException {
        for (Region r : listRegions()) {
            if (providerRegionId.equals(r.getProviderRegionId())) {
                return r;
            }
        }
        return null;
    }

    @Override
    public @Nonnull
    Collection<DataCenter> listDataCenters(@Nonnull String providerRegionId) throws InternalException, CloudException {
        APITrace.begin(provider, "listDataCenters");
        try {
            Region region = getRegion(providerRegionId);
            if (region == null) {
                throw new CloudException("No such region: " + providerRegionId);
            }

            ProviderContext ctx = provider.getContext();
            if (ctx == null) {
                throw new NoContextException();
            }

            Cache<DataCenter> cache = Cache.getInstance(provider, "dataCenters", DataCenter.class,
                    CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
            Collection<DataCenter> dcList = (Collection<DataCenter>) cache.get(ctx);

            if (dcList == null) {
                dcList = new ArrayList<DataCenter>();

                logger.info("Get data center locations for " + providerRegionId + " for account "
                        + ctx.getAccountNumber());

                APIHandler method = new APIHandler(provider);
                APIResponse response = method.post("Account/GetLocations/JSON", "");
                response.validate();

                JSONObject json = response.getJSON();
                if (json.has("Locations")) {
                    for (int i = 0; i < json.getJSONArray("Locations").length(); i++) {
                        JSONObject location = json.getJSONArray("Locations").getJSONObject(i);
                        DataCenter dc = new DataCenter();
                        String apiLocation = location.getString("Alias");

                        dc.setActive(true);
                        dc.setAvailable(true);
                        dc.setName(apiLocation);
                        dc.setProviderDataCenterId(apiLocation);
                        dc.setRegionId(providerRegionId);
                        dcList.add(dc);
                    }
                }

                cache.put(ctx, dcList);
            }
            return dcList;
        } catch (JSONException e) {
            throw new CloudException(e);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public Collection<Region> listRegions() throws InternalException, CloudException {
        APITrace.begin(provider, "listRegions");
        try {
            ProviderContext ctx = provider.getContext();
            if (ctx == null) {
                throw new NoContextException();
            }

            Cache<Region> cache = Cache.getInstance(provider, "regions", Region.class, CacheLevel.CLOUD_ACCOUNT,
                    new TimePeriod<Day>(1, TimePeriod.DAY));
            Collection<Region> regions = (Collection<Region>) cache.get(ctx);

            if (regions == null) {
                regions = new ArrayList<Region>();

                APIHandler method = new APIHandler(provider);
                APIResponse response = method.post("Account/GetLocations/JSON", "");
                response.validate();

                JSONObject json = response.getJSON();
                if (json.has("Locations")) {
                    for (int i = 0; i < json.getJSONArray("Locations").length(); i++) {
                        JSONObject location = json.getJSONArray("Locations").getJSONObject(i);
                        String apiRegion = location.getString("Region");

                        Region region = new Region(apiRegion, apiRegion, true, true);
                        if (apiRegion.contains(" ")) {
                            region.setJurisdiction(apiRegion.substring(1, apiRegion.indexOf(" ")));
                        } else {
                            region.setJurisdiction(apiRegion);
                        }
                        regions.add(region);
                    }
                }

                cache.put(ctx, regions);
            }
            return regions;

        } catch (JSONException e) {
            throw new CloudException(e);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Collection<ResourcePool> listResourcePools( String providerDataCenterId ) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    @Override
    public @Nullable ResourcePool getResourcePool( String providerResourcePoolId ) throws InternalException, CloudException {
        return null;
    }

    @Override
    public @Nonnull Collection<StoragePool> listStoragePools() throws InternalException, CloudException {
        return Collections.emptyList();
    }
}
