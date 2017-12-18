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
package pt.ua.dicoogle.nifti.dicom;

import java.util.regex.Pattern;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.ElementDictionary;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class DicomInjectorTest {
    
    private static final SpecificCharacterSet DEFAULT_CHARSET = new SpecificCharacterSet("UTF-8");
    private static final Pattern UID_PATTERN = Pattern.compile("(\\d|\\.)+");
    private static ElementDictionary dict;
    private DicomObject dcm;

    @BeforeClass
    public static void initialize() {
        dict = ElementDictionary.getDictionary();
    }
    
    @Before
    public void setup() {
        dcm = new BasicDicomObject();
    }
    
    @Test
    public void test() {
        DicomInjector injector = new CTClassDicomInjector();
        
        dcm = injector.apply(dcm);
        
        checkStringElement(dcm, "Modality", VR.CS, "CT");
        checkStringElement(dcm, Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.2");
        checkElement(dcm, "ImageType", VR.CS);
        checkElement(dcm, "PhotometricInterpretation", VR.CS);
    }
    
    @Test
    public void testBuilderCT() {
        DicomInjector injector = new DicomInjectorBuilder()
                .patient("Esquina^Jose", "222")
                .generalClinic("DicoogleIsBestPACS", "UA.PT Bioinformatics")
                .bodyPart("WHOLEBODY")
                .modality("CT")
                .build();
        
        dcm = injector.apply(dcm);
        
        checkStringElement(dcm, "Modality", VR.CS, "CT");
        checkStringElement(dcm, Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.2");
        checkElement(dcm, "ImageType", VR.CS, 1);
        checkElement(dcm, "PhotometricInterpretation", VR.CS, 1);
        
        checkStringElement(dcm, "PatientName", VR.PN, "Esquina^Jose");
        checkStringElement(dcm, "Manufacturer", VR.LO, "DicoogleIsBestPACS");
        checkStringElement(dcm, "BodyPartExamined", VR.CS, "WHOLEBODY");
    }

    @Test
    public void testBuilderMR() {
        DicomInjector injector = new DicomInjectorBuilder()
                .patient("Esquina^Jose", "222")
                .generalClinic("DicoogleIsBestPACS", "UA.PT Bioinformatics")
                .modality("MR")
                .build();
        
        dcm = injector.apply(dcm);
        
        checkStringElement(dcm, "Modality", VR.CS, "MR");
        checkStringElement(dcm, Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.4");
        checkElement(dcm, "ImageType", VR.CS, 1);
        checkElement(dcm, "PhotometricInterpretation", VR.CS, 1);
        checkElement(dcm, "ScanningSequence", VR.CS);
        
        checkStringElement(dcm, "PatientName", VR.PN, "Esquina^Jose");
        checkStringElement(dcm, "Manufacturer", VR.LO, "DicoogleIsBestPACS");
    }
    
    private void checkUid(DicomObject dcm, String tagName) {
        DicomElement e = dcm.get(dict.tagForName(tagName));
        assertNotNull(tagName + " not null", e);
        assertEquals(tagName + " is UI", VR.UI, e.vr());
        assertTrue(tagName + " is sufficiently long", e.getString(DEFAULT_CHARSET, true).length() >= 6);
        assertTrue(tagName + " follows UID pattern", UID_PATTERN.asPredicate().test(e.getString(DEFAULT_CHARSET, true)));
    }

    private void checkElement(DicomObject dcm, String tagName, VR expectedVR, int expectedMult) {
        DicomElement e = dcm.get(dict.tagForName(tagName));
        assertNotNull(tagName + " not null", e);
        assertEquals(tagName + " is " + expectedVR, expectedVR, e.vr());
        assertEquals(tagName + " contains " + expectedMult + " element(s)", expectedMult, e.vm(DEFAULT_CHARSET));
    }
    private void checkElement(DicomObject dcm, String tagName, VR expectedVR) {
        DicomElement e = dcm.get(dict.tagForName(tagName));
        assertNotNull(tagName + " not null", e);
        assertEquals(tagName + " is " + expectedVR, expectedVR, e.vr());
    }

    private void checkStringElement(DicomObject dcm, String tagName, VR expectedVR, String expectedValue) {
        DicomElement e = dcm.get(dict.tagForName(tagName));
        assertNotNull(tagName + " not null", e);
        assertEquals(tagName + " is " + expectedVR, expectedVR, e.vr());
        assertEquals(tagName + " has expected value", expectedValue, e.getString(DEFAULT_CHARSET, true));
    }

    private void checkStringElement(DicomObject dcm, int tag, VR expectedVR, String expectedValue) {
        DicomElement e = dcm.get(tag);
        assertNotNull("not null", e);
        assertEquals(" is " + expectedVR, expectedVR, e.vr());
        assertEquals(" has expected value", expectedValue, e.getString(DEFAULT_CHARSET, true));
    }
}
