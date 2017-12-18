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

import java.util.function.Function;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import pt.ua.dicoogle.nifti.dicom.CTClassDicomInjector.ImageType;
import pt.ua.dicoogle.nifti.dicom.CTClassDicomInjector.PhotometricInterpretation;

/** Utility class for creating DICOM attribute injection functions.
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class DicomInjectorBuilder {

    private Function<DicomObject, DicomObject> fn;

    public DicomInjectorBuilder() {
        fn = i -> i;
    }
    
    public DicomInjectorBuilder generalClinic(String manufacturer, String institutionName) {
        fn = fn.andThen(new GeneralClinicDicomInjector(manufacturer, institutionName));
        return this;
    }

    public DicomInjectorBuilder CT(ImageType type, PhotometricInterpretation photometricInterpretation) {
        fn = fn.andThen(new CTClassDicomInjector(type, photometricInterpretation));
        return this;
    }
    
    public DicomInjectorBuilder CT() {
        fn = fn.andThen(new CTClassDicomInjector());
        return this;
    }

    public DicomInjectorBuilder MR() {
        fn = fn.andThen(new MRClassDicomInjector());
        return this;
    }
    
    public DicomInjectorBuilder modality(String modality) {
        if (modality == null) {
            throw new NullPointerException("modality");
        }
        switch (modality) {
            case "CT":
                return this.CT();
            case "MR":
                return this.MR();
            case "MRT1":
                fn = fn.andThen(new MRClassDicomInjector(MRClassDicomInjector.ImageType.T1_MAP));
                break;
            case "MRT2":
                fn = fn.andThen(new MRClassDicomInjector(MRClassDicomInjector.ImageType.T2_MAP));
                break;
            default:
                fn = fn.andThen(dcm -> {
                    dcm.putString(Tag.Modality, VR.CS, modality);
                    return dcm;
                });
        }
        return this;
    }

    public DicomInjectorBuilder patient(String patientName, String patientID) {
        fn = fn.andThen(new PatientDicomInjector(patientName, patientID));
        return this;
    }

    public DicomInjectorBuilder bodyPart(String bodyPart) {
        fn = fn.andThen(new BodyPartDicomInjector(bodyPart));
        return this;
    }

    public DicomInjector build() {
        final Function<DicomObject, DicomObject> finalFn = this.fn;
        DicomInjector finalInjector = dcm -> finalFn.apply(dcm);
        this.fn = i -> i;
        return finalInjector;
    }

}
