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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.tuple.Pair;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.nifti.config.NIFTIPluginSettings;
import pt.ua.dicoogle.nifti.convert.NIFTIConverter;
import pt.ua.dicoogle.nifti.convert.NIFTIConverterImpl;
import pt.ua.dicoogle.nifti.dicom.DicomInjectorBuilder;
import pt.ua.dicoogle.nifti.util.RuntimeIOException;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;

/** Main web service.
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class NIFTIConvertWebServlet  extends HttpServlet implements PlatformCommunicatorInterface {
    private static final Logger logger = LoggerFactory.getLogger(NIFTIConvertWebServlet.class);
    
    private final ForkJoinPool pool = new ForkJoinPool(1);
    
    private static class NiftiFileEntry<S extends InputStream> {
        final String name;
        final S inputStream;

        public NiftiFileEntry(String name, S inputStream) {
            this.name = name;
            this.inputStream = inputStream;
        }
    }
    
    private DicooglePlatformInterface platform;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response)
                    throws ServletException, IOException {
        response.setStatus(418, "I'm a teapot");
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String modality = req.getParameter("modality");
        String manufacturer = req.getParameter("manufacturer");
        String institutionName = req.getParameter("institutionName");
        String patientName = req.getParameter("patientName");
        String patientID = req.getParameter("patientID");
        String bodyPart = req.getParameter("bodyPart");
        String filenameType = req.getParameter("type");

        Stream<NiftiFileEntry<InputStream>> niftiObjects;
        int nFiles;
        resp.setContentType("application/json");
        if (req.getContentType() == null) {
            JSONObject reply = new JSONObject();
            reply.put("error", "no content");
            resp.getWriter().print(reply.toString());
            resp.setStatus(400);
            return;
        } else if (req.getContentType().startsWith("multipart/form-data")) {
            // for now we just retrieve the first valid part
            Collection<Part> parts = req.getParts().stream().filter( part -> {
                if (part.getContentType() == null) {
                    logger.warn("Unknown content type retrieved in part named {}, ignoring", part.getName());
                    return false;
                }
                return true;
            }).collect(Collectors.toList());

            if (parts == null || parts.isEmpty()) {
                JSONObject reply = new JSONObject();
                reply.put("error", "no valid content in multipart entity");
                resp.getWriter().print(reply.toString());
                resp.setStatus(400);
                return;
            }
            nFiles = parts.size();

            parts = parts.stream()
                    .filter( part -> {
                        String ctype = part.getContentType();
                        if (ctype == null) {
                            logger.warn("Unknown content type retrieved, ignoring");
                            return false;
                        }
                        return true;
                    }).collect(Collectors.toList());
            nFiles = parts.size();
            niftiObjects = parts.stream().sequential()
                    .map( part -> {
                        String ctype = part.getContentType();
                        String name = part.getName();
                        try {
                            return new NiftiFileEntry<InputStream>(name, isGzip(ctype)
                                    ? new GZIPInputStream(part.getInputStream())
                                    : part.getInputStream());
                        } catch (IOException ex) {
                            logger.warn("Failed to fetch file", ex);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull);
        } else {
            nFiles = 1;
            niftiObjects = Stream.of(new NiftiFileEntry<InputStream>(null, isGzip(req.getContentType())
                    ? new GZIPInputStream(req.getInputStream())
                    : req.getInputStream()));
        }

        if (modality == null) {
            modality = "CT";
        }

        if (manufacturer == null) {
            manufacturer = "UNKNOWN";
        }

        if (institutionName == null) {
            institutionName = "UNKNOWN";
        }
        
        if (patientName == null) {
            patientName = "Patient^Anonymous";
        }

        if (patientID == null) {
            patientID = UUID.randomUUID().toString();
        }
        
        if (null != bodyPart) bodyPart = convertBodyPartId(bodyPart);

        NIFTIConverter converter = new NIFTIConverterImpl();
        UnaryOperator<DicomObject> injector = new DicomInjectorBuilder()
                .patient(patientName, patientID)
                .generalClinic(manufacturer, institutionName)
                .bodyPart(bodyPart)
                .modality(modality.toUpperCase())
                .build();

        // fetch storage interface
        StorageInterface storage = platform.getStorageForSchema(NIFTIPluginSettings.INSTANCE.getStorageScheme());
        // will do this synchronously for now
        try {

            List<String> uris = pool.submit(() -> niftiObjects
                    .flatMap(o -> {
                        try {
                            return converter.convert(o.inputStream)
                                    .map(dcm -> injectSeriesInfo(dcm, o.name))
                                    .map(dcm -> {
                                        if ("visceral".equals(filenameType)) {
                                            return dcm;
                                        } else {
                                            return dcm;
                                        }
                                    });
                        } catch (RuntimeIOException ex) {
                            logger.warn("Failed to convert NIFTI file", ex);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(injector)
                    .map(dcm -> storage.store(dcm))
                    .filter(Objects::nonNull)
                    .map(URI::toString)
                    .collect(Collectors.toList())).get();
            
            JSONObject reply = new JSONObject();
            reply.element("status", "COMPLETED");
            reply.element("nNiftiFiles", nFiles);
            reply.element("dcmFiles", uris);
            resp.getWriter().print(reply.toString());
            resp.setStatus(200);
        } catch (InterruptedException | ExecutionException ex) {
            logger.warn("Interrupted", ex);
            JSONObject reply = new JSONObject();
            reply.element("status", "interrupted");
            reply.element("message", ex.getMessage());
            resp.getWriter().print(reply.toString());
            resp.setStatus(500);
        }
    }

    private static DicomObject injectSeriesInfo(DicomObject obj, String filename) {
        if (filename == null) return obj;
        if (filename.endsWith(".nii.gz")) {
            filename = filename.substring(0, filename.length() - 7);
        }

        obj.putString(Tag.SeriesDescription, VR.LO, filename);
        int n = filename.indexOf("_");
        if (n != 1) {
            if (n > 16) {
                n = 16;
            }
            String an = filename.substring(0, n);
            obj.putString(Tag.AccessionNumber, VR.SH, an);
        }
        return obj;
    }

    private static DicomObject injectBodyPart(DicomObject obj, String filename) {
        if (filename == null) return obj;
        if (filename.endsWith(".nii.gz")) {
            filename = filename.substring(0, filename.length() - 7);
        }

        String[] parts = filename.split("_");

        if (parts.length >= 3) {
            String bodyPart = convertBodyPartId(parts[2]);
            if (bodyPart == null) {
                obj.putNull(Tag.BodyPartExamined, VR.CS);
            } else {
                obj.putString(Tag.BodyPartExamined, VR.CS, bodyPart);
            }
        }
        return obj;
    }

    private static boolean isGzip(String contentType) {
        return contentType != null && contentType.split(";")[0].trim().endsWith("gzip");
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface core) {
        this.platform = core;
    }


    private static String convertBodyPartId(String arg) {
        switch (arg) {
            case "Ab":
                return "ABDOMEN";
            case "Th":
                return "CHEST";
            case "ThAb":
                return "CHESTABDOMEN";
            case "Wb":
                return "WHOLEBODY";
            case "undefined":
            default:
                return null;
        }
    }
}
