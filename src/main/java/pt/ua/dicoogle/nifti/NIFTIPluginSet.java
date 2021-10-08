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
package pt.ua.dicoogle.nifti;

import pt.ua.dicoogle.nifti.ws.NIFTIServletPlugin;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.nifti.config.NIFTIPluginSettings;
import pt.ua.dicoogle.sdk.JettyPluginInterface;
import pt.ua.dicoogle.sdk.PluginSet;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/** The main plugin set.
 * 
 * This is the entry point for all plugins.
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
@PluginImplementation
public class NIFTIPluginSet implements PluginSet {
    // use slf4j for logging purposes
    private static final Logger logger = LoggerFactory.getLogger(NIFTIPluginSet.class);
    
    // We will list each of our plugins as an attribute to the plugin set
    private final NIFTIServletPlugin jettyWeb;
    
    // Additional resources may be added here.
    private ConfigurationHolder settings;
    
    public NIFTIPluginSet() throws IOException {
        logger.info("Initializing NIFTI Plugin Set");

        // construct all plugins here
        this.jettyWeb = new NIFTIServletPlugin();
        
        logger.info("NIFTI Plugin Set is ready");
    }
    
    /** This method is used to retrieve a name for identifying the plugin set. Keep it as a constant value.
     * 
     * @return a unique name for the plugin set
     */
    @Override
    public String getName() {
        return "NIFTI";
    }

    @Override
    public Collection<JettyPluginInterface> getJettyPlugins() {
        return Collections.singleton(this.jettyWeb);
    }

    @Override
    public void shutdown() {
        logger.info("NIFTI plugin is shutting down");
    }

    @Override
    public void setSettings(ConfigurationHolder xmlSettings) {
        this.settings = xmlSettings;
        NIFTIPluginSettings.INSTANCE.configure(settings);
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }
}