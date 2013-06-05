/**
 * Copyright (C) 2013 Dell, Inc.
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

package org.dasein.cloud.tier3.compute;

import org.dasein.cloud.compute.AbstractComputeServices;
import org.dasein.cloud.tier3.Tier3;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 5/28/13 6:24 PM</p>
 *
 * @author George Reese
 */
public class Tier3ComputeServices extends AbstractComputeServices {
    private Tier3 provider;

    public Tier3ComputeServices(Tier3 provider) { this.provider = provider; }
}