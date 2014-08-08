/**
 * Copyright (C) 2009-2013 Dell, Inc.
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

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.dc.DataCenterCapabilities;

import java.util.Locale;

/**
 * Describes Tier3 data centers capabilities
 * <p>Created by stas: 06/08/2014 10:21</p>
 *
 * @author Stas Maksimov
 * @version 2014.08 initial version
 * @since 2014.08
 */
public class DCCapabilities extends AbstractCapabilities<Tier3> implements DataCenterCapabilities {
    public DCCapabilities( Tier3 provider ) {
        super(provider);
    }

    @Override
    public String getProviderTermForDataCenter( Locale locale ) {
        return "data center";
    }

    @Override
    public String getProviderTermForRegion( Locale locale ) {
        return "region";
    }
    @Override
    public boolean supportsAffinityGroups() {
        return false;
    }

    @Override
    public boolean supportsResourcePools() {
        return false;
    }

    @Override
    public boolean supportsStoragePools() {
        return false;
    }
}
