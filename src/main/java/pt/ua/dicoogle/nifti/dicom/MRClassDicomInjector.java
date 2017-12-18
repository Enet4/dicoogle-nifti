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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;

/**
 * A DICOM injector for attributes specific to an MR image storage file.
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class MRClassDicomInjector implements DicomInjector {

    private static final String MR_SOP_CLASS_UID = "1.2.840.10008.5.1.4.1.1.4";

    public static enum ImageType {

        DENSITY_MAP, DIFFUSION_MAP, IMAGE_ADDITION, MODULUS_SUBTRACT, MPR, OTHER,
        PHASE_MAP, PHASE_SUBTRACT, PROJECTION_IMAGE, T1_MAP, T2_MAP, VELOCITY_MAP;

        public String dicomValue() {
            return this.name().replace('_', ' ');
        }
    }

    public static enum PhotometricInterpretation {

        MONOCHROME1, MONOCHROME2;
    }

    public static enum ScanningSequence {
        SE, // Spin Echo
        IR, // Inversion Recovery
        GR, // Gradient Recalled
        EP, // Echo Planar
        RM; // Research Mode
    }

    public static enum SequenceVariant {
        SK,   // segmented k-space
        MTC,  // magnetization transfer contrast
        SS,   // steady state
        TRSS, // time reversed steady state
        SP,   // spoiled
        MP,   // MAG prepared
        OSP,  // oversampling phase
        NONE; // no sequence variant
    }

    private final ImageType type;
    private final PhotometricInterpretation photoInterpret;
    private final List<ScanningSequence> scanningSequences;
    private final List<SequenceVariant> sequenceVariants;

    public MRClassDicomInjector(ImageType type, PhotometricInterpretation photoInterpret, List<ScanningSequence> scanningSequence, List<SequenceVariant> sequenceVariant) {
        this.type = type;
        this.photoInterpret = photoInterpret;
        this.scanningSequences = new ArrayList<>(scanningSequence);
        this.sequenceVariants = new ArrayList<>(sequenceVariant);
    }

    public MRClassDicomInjector(ImageType type, PhotometricInterpretation photoInterpret, ScanningSequence scanningSequence, SequenceVariant sequenceVariant) {
        this(type, photoInterpret, Collections.singletonList(scanningSequence), Collections.singletonList(sequenceVariant));
    }

    public MRClassDicomInjector(ImageType type) {
        this(type, PhotometricInterpretation.MONOCHROME2, ScanningSequence.RM, SequenceVariant.NONE);
    }

    public MRClassDicomInjector() {
        this(ImageType.OTHER, PhotometricInterpretation.MONOCHROME2, ScanningSequence.RM, SequenceVariant.NONE);
    }

    @Override
    public DicomObject apply(DicomObject obj) {
        obj.putString(Tag.Modality, VR.CS, "MR");
        obj.putString(Tag.SOPClassUID, VR.UI, MR_SOP_CLASS_UID);
        obj.putString(Tag.ImageType, VR.CS, this.type.dicomValue());
        obj.putString(Tag.PhotometricInterpretation, VR.CS, this.photoInterpret.name());

        putCSElements(obj, Tag.ScanningSequence, this.scanningSequences);
        putCSElements(obj, Tag.SequenceVariant, this.sequenceVariants);

        // Type 2 attributes
        placeholdElement(obj, Tag.ScanOptions, VR.CS); // 1-n
        placeholdElement(obj, Tag.MRAcquisitionType, VR.CS); // 1
        placeholdElement(obj, Tag.RepetitionTime, VR.DS); // 1
        placeholdElement(obj, Tag.EchoTime, VR.DS);
        placeholdElement(obj, Tag.EchoTrainLength, VR.US);
        placeholdElement(obj, Tag.InversionTime, VR.DS);
        
        return obj;
    }

    // put multiple CS elements from a collection of enumerates into a DICOM object
    private static <T extends Enum<T>> void putCSElements(DicomObject obj, int tag, Collection<T> elements) {
        String[] arr = new String[elements.size()];
        int i = 0;
        for (T elem : elements) {
            arr[i] = elem.name();
            i++;
        }
        obj.putStrings(tag, VR.CS, arr);
    }
    
    // create empty element if it does not exist yet
    private static void placeholdElement(DicomObject obj, int tag, VR vr) {
        if (!obj.contains(tag)) {
            obj.putNull(tag, vr);
        }
    }

}
