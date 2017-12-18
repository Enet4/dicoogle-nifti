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
package pt.ua.dicoogle.nifti.ws;

import javax.servlet.MultipartConfigElement;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.dicoogle.sdk.JettyPluginInterface;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/** Example of a Jetty Servlet plugin.
 *
 * @author Luís A. Bastião Silva - <bastiao@ua.pt>
 */
public class NIFTIServletPlugin implements JettyPluginInterface, PlatformCommunicatorInterface {
    private static final Logger logger = LoggerFactory.getLogger(NIFTIServletPlugin.class);
    
    private static final String NIFTI_FILE_PATH = System.getProperty("java.io.tmpdir");

    private boolean enabled;
    private ConfigurationHolder settings;
    private DicooglePlatformInterface platform;
    private final NIFTIConvertWebServlet wsConvert;
    
    public NIFTIServletPlugin() {
        this.wsConvert = new NIFTIConvertWebServlet();
        this.enabled = true;
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface pi) {
        this.platform = pi;
        // since web service is not a plugin interface, the platform interface must be provided manually
        this.wsConvert.setPlatformProxy(pi);
    }

    @Override
    public String getName() {
        return "NIFTI";
    }

    @Override
    public boolean enable() {
        this.enabled = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.enabled = false;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setSettings(ConfigurationHolder settings) {
        this.settings = settings;
        // use settings here
        
    }

    @Override
    public ConfigurationHolder getSettings() {
        return settings;
    }


    @Override
    public HandlerList getJettyHandlers() {

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/nifti");
        
        ServletHolder convertServletHolder = new ServletHolder(this.wsConvert); 
        convertServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(NIFTI_FILE_PATH));
        handler.addServlet(convertServletHolder, "/convert");

//        URL url = NIFTIServletPlugin.class.getResource("/WEBAPP");
//        logger.debug("Retrieving web app from \"{}\"", url);
//        String directoryToServeAssets = url.toString();
//        
//        final WebAppContext webpages = new WebAppContext(directoryToServeAssets, "/dashboardSample");
//        webpages.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "true"); // disables directory listing
//        webpages.setInitParameter("useFileMappedBuffer", "false");
//        webpages.setInitParameter("cacheControl", "max-age=0, public");
//
//        webpages.setWelcomeFiles(new String[]{"index.html"});

        HandlerList l = new HandlerList();
        l.addHandler(handler);
        //l.addHandler(webpages);

        return l;
    }

}
