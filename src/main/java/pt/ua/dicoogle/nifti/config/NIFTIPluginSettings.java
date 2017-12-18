/**
 * Copyright (C) 2017 UA.PT Bioinformatics - http://bioinformatics.ua.pt
 *
 * This file is part of Dicoogle NIfTI-1 file converter (dicoogle-nifti).
 *
 * dicoogle-nifti is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * dicoogle-nifti is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 */

package pt.ua.dicoogle.nifti.config;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/**
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public enum NIFTIPluginSettings {
    INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger(NIFTIPluginSettings.class);
    
    private String uidRoot;
    private String storageScheme;
    private boolean enabled;
    
    public synchronized void configure(ConfigurationHolder config) {
        XMLConfiguration settings = config.getConfiguration();
        uidRoot = settings.getString("uid-root", "1.2.351.99999");
        enabled = settings.getBoolean("enabled", true);
        storageScheme = settings.getString("storage-scheme", "file");
    }
    
    public synchronized String getUidRoot() {
        return uidRoot;
    }

    public synchronized String getStorageScheme() {
        return storageScheme;
    }

    public synchronized void setStorageScheme(String scheme) {
        this.storageScheme = scheme;
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
}
