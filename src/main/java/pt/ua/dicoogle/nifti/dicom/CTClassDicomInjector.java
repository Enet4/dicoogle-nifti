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

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;

/** A DICOM injector for attributes specific to a CT image storage file.
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class CTClassDicomInjector implements DicomInjector {
    private static final String CT_SOP_CLASS_UID = "1.2.840.10008.5.1.4.1.1.2";
    
    public static enum ImageType {
        AXIAL,     /// identifies a CT Axial Image
        LOCALIZER; /// identifies a CT Localizer Image
    }

    public static enum PhotometricInterpretation {
        MONOCHROME1, MONOCHROME2;
    }

    private final ImageType type;
    private final PhotometricInterpretation photoInterpret;
    
    public CTClassDicomInjector(ImageType type, PhotometricInterpretation photoInterpret) {
        this.type = type;
        this.photoInterpret = photoInterpret;
    }
    
    public CTClassDicomInjector() {
        this(ImageType.AXIAL, PhotometricInterpretation.MONOCHROME2);
    }
    
    @Override
    public DicomObject apply(DicomObject obj) {
        obj.putString(Tag.Modality, VR.CS, "CT");
        obj.putString(Tag.SOPClassUID, VR.UI, CT_SOP_CLASS_UID);
        obj.putString(Tag.ImageType, VR.CS, this.type.name());
        obj.putString(Tag.PhotometricInterpretation, VR.CS, this.photoInterpret.name());
        if (!obj.contains(Tag.RescaleIntercept)) {
            obj.putDouble(Tag.RescaleIntercept, VR.DS, 0.0);
        }
        if (!obj.contains(Tag.RescaleSlope)) {
            obj.putDouble(Tag.RescaleSlope, VR.DS, 1.0);
        }

        // Type 2 attributes
        placeholdElement(obj, Tag.KVP, VR.DS);
        placeholdElement(obj, Tag.AcquisitionNumber, VR.IS);
        
        return obj;
    }
    
    // create empty element if it does not exist yet
    private static void placeholdElement(DicomObject obj, int tag, VR vr) {
        if (!obj.contains(tag)) {
            obj.putNull(tag, vr);
        }
    }

}
