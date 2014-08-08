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
package org.dasein.cloud.tier3.compute.image;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.*;

import java.util.Collections;
import java.util.Locale;

/**
* Description
* <p>Created by stas: 06/08/2014 13:07</p>
*
* @author Stas Maksimov
* @version 2014.08 initial version
* @since 2014.08
*/
class Tier3ImageCapabilities implements ImageCapabilities {

    @Override
    public String getRegionId() {
        return provider.getContext().getRegionId();
    }

    @Override
    public String getAccountNumber() {
        return provider.getContext().getAccountNumber();
    }

    @Override
    public boolean supportsPublicLibrary(ImageClass cls) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageSharingWithPublic() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageSharing() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageCapture(MachineImageType type) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsDirectImageUpload() throws CloudException, InternalException {
        return false;
    }

    @Override
    public Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
        return Collections.singletonList(MachineImageType.VOLUME);
    }

    @Override
    public Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
        return Collections.singletonList(ImageClass.MACHINE);
    }

    @Override
    public Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException,
            InternalException {
        return Collections.emptyList();
    }

    @Override
    public Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Override
    public String getProviderTermForImage(Locale locale, ImageClass cls) {
        return "template";
    }

    @Override
    public String getProviderTermForCustomImage(Locale locale, ImageClass cls) {
        return getProviderTermForImage(locale, cls);
    }

    @Override
    public boolean canImage(VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canBundle(VmState fromState) throws CloudException, InternalException {
        return false;
    }
}
