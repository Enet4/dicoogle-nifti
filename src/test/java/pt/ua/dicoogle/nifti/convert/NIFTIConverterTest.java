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
package pt.ua.dicoogle.nifti.convert;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.ElementDictionary;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.VR;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import pt.ua.dicoogle.nifti.dicom.DicomInjector;
import pt.ua.dicoogle.nifti.dicom.PatientDicomInjector;

/**
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class NIFTIConverterTest {
    
    public NIFTIConverterTest() {
    }
    private static final Pattern UID_PATTERN = Pattern.compile("(\\d|\\.)+");
    private static final int TAG_Modality = 0x0008_0060; // (0008,0060) Modality
    private InputStream content;
    
    @Before
    public void setup() throws IOException {
        content = new GZIPInputStream(NIFTIConverterTest.class.getResourceAsStream("test.nii.gz"));
    }
    
    @Test
    public void checkHardcodedTags() {
        // this was simply to make sure that the Endianness of the hex literals is the same
        // as the one used for describing DICOM tags (Big Endian, group first)
        assertEquals(ElementDictionary.getDictionary().tagForName("Modality"), TAG_Modality);
    }
    
    @Test
    public void testSimple() throws IOException {
        NIFTIConverter converter = new NIFTIConverterImpl();
        
        List<DicomObject> objects = converter.convert(content).collect(Collectors.toList());
        assertNotNull(objects);
        assertTrue(objects.size() > 0);
        
        for (DicomObject dcm : objects) {
            checkUid(dcm, "StudyInstanceUID");
            checkUid(dcm, "SeriesInstanceUID");
            checkUid(dcm, "SOPInstanceUID");

            DicomElement e = dcm.get(ElementDictionary.getDictionary().tagForName("Modality"));
            assertNull(e);
 
            e = dcm.get(ElementDictionary.getDictionary().tagForName("PixelData"));
            assertNotNull(e);
            assertTrue(e.vr() == VR.OB || e.vr() == VR.OW);

            e = dcm.get(ElementDictionary.getDictionary().tagForName("BitsAllocated"));
            assertNotNull(e);
            assertEquals(VR.US, e.vr());
        }
    }

    @Test
    public void testWithPatientInjector() throws IOException {
        NIFTIConverterImpl converter = new NIFTIConverterImpl();
        DicomInjector injector = new PatientDicomInjector("Patient^Anonymous", "123456789");
        
        List<DicomObject> objects = converter.convert(content)
                .map(injector)
                .collect(Collectors.toList());
        assertNotNull(objects);
        assertTrue(objects.size() > 0);

        final ElementDictionary dict = ElementDictionary.getDictionary();
        
        for (DicomObject dcm : objects) {
            checkUid(dcm, "StudyInstanceUID");
            checkUid(dcm, "SeriesInstanceUID");
            checkUid(dcm, "SOPInstanceUID");

            DicomElement e = dcm.get(dict.tagForName("PatientName"));
            assertNotNull(e);
            assertEquals(VR.PN, e.vr());
            assertEquals("Patient^Anonymous", e.getString(new SpecificCharacterSet("UTF-8"), true));
            
            e = dcm.get(dict.tagForName("PatientID"));
            assertNotNull(e);
            assertEquals(VR.LO, e.vr());
            assertEquals("123456789", e.getString(new SpecificCharacterSet("UTF-8"), true).trim());
        }
    }
    
    private void checkUid(DicomObject dcm, String tagName) {
        DicomElement e = dcm.get(ElementDictionary.getDictionary().tagForName(tagName));
        assertNotNull(tagName + " not null", e);
        assertEquals(tagName + " is UI", VR.UI, e.vr());
        assertTrue(tagName + " is sufficiently long", e.getString(new SpecificCharacterSet("UTF-8"), true).length() >= 6);
        assertTrue(tagName + " follows UID pattern", UID_PATTERN.asPredicate().test(e.getString(new SpecificCharacterSet("UTF-8"), true)));
    }
}
