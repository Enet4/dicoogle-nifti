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
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import niftijio.niftijio.NiftiHeader;
import org.dcm4che2.data.DicomObject;
import niftijio.niftijio.NiftiVolume;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import pt.ua.dicoogle.nifti.config.NIFTIPluginSettings;
import pt.ua.dicoogle.nifti.dicom.DicomInjector;
import pt.ua.dicoogle.nifti.dicom.WindowDicomInjector;
import pt.ua.dicoogle.nifti.util.RuntimeIOException;

/**
 * An implementation for a NIFTI to DICOM converter. This implementation is not thread-safe.
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class NIFTIConverterImpl implements NIFTIConverter {

    private final Random rnd;
    private final DicomObject prototype;

    public NIFTIConverterImpl(DicomObject prototype) {
        this.rnd = new Random();
        this.prototype = new BasicDicomObject(prototype);
    }
    
    public NIFTIConverterImpl() {
        this.rnd = new Random();
        this.prototype = new BasicDicomObject();
        this.prototype.putString(Tag.TransferSyntaxUID, VR.UI, "1.2.840.10008.1.2.1"); // Little Endian Explicit
    }

    private static class Pos {
        final int dim;
        final int nz;
        final String seriesUid;

        public Pos(int dim, int nz, String seriesUid) {
            this.dim = dim;
            this.nz = nz;
            this.seriesUid = seriesUid;
        }
    }

    @Override
    public Stream<DicomObject> convert(InputStream content) throws RuntimeIOException {
        try {
            NiftiVolume volume = NiftiVolume.read(content);
            
            DicomInjector windowInjector = createWindowInjector(volume);
            
            final String studyInstanceUid = NIFTIPluginSettings.INSTANCE.getUidRoot() + '.' + generateUidTerm(8);
            return IntStream.range(0, volume.data.dimension())
                    .mapToObj(dim -> new Pos(dim, 0, studyInstanceUid + '.' + generateUidTerm(8)))
                    .flatMap(pos -> IntStream.range(0, volume.data.sizeZ())
                            .mapToObj(z -> new Pos(pos.dim, z, pos.seriesUid)))
                    .map(pos -> convertSlice(volume, pos.nz, pos.dim, studyInstanceUid, pos.seriesUid))
                    .map(windowInjector);
        } catch (IOException|IllegalArgumentException ex) {
            throw new RuntimeIOException(ex);
        }
    }

    private DicomObject convertSlice(NiftiVolume volume, int nz, int dim, String studyInstanceUid, String seriesInstanceUid) {
        final String instanceUid = seriesInstanceUid + '.' + generateUidTerm(8);
        return convertSlice(volume, nz, dim, studyInstanceUid, seriesInstanceUid, instanceUid);
    }

    private DicomObject convertSlice(NiftiVolume volume, int nz, int dim, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid) {
        DicomObject obj = new BasicDicomObject(this.prototype);

        putPixelData(obj, volume, nz, dim);
        
        obj.putString(Tag.StudyInstanceUID, VR.UI, studyInstanceUid);
        obj.putString(Tag.SeriesInstanceUID, VR.UI, seriesInstanceUid);
        obj.putString(Tag.SOPInstanceUID, VR.UI, sopInstanceUid);
        obj.putString(Tag.FrameOfReferenceUID, VR.UI, sopInstanceUid + ".0");
        obj.putInt(Tag.InstanceNumber, VR.IS, nz+1);

        return new NIFTIDicomInjector(volume.header).apply(obj);
    }

    private String generateUidTerm(int size) {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < size; i++) {
            r.append((char) ('0' + this.rnd.nextInt(10)));
        }
        return r.toString();
    }

    /**
     * Take the pixel data of a slice.
     *
     * @param volume the NIFTI volume
     * @param nz the z axis to take the slice from
     * @param dim dimension to take the slice from
     * @return an array containing the pixel data, ready to be put in a DICOM object
     */
    private void putPixelData(DicomObject obj, NiftiVolume volume, int nz, int dim) {
        int sizeof_unit = NiftiHeader.bytesPerVoxel(volume.header.datatype);
        if (sizeof_unit > 2) {
            sizeof_unit = 2;
        }
        final int slice_size = volume.data.sizeX() * volume.data.sizeY() * sizeof_unit;
        byte[] pixeldata = new byte[slice_size];
        final float slope = volume.header.scl_slope;
        final float offset = volume.header.scl_inter;
        int index = 0;
        for (int y = 0; y < volume.data.sizeY(); y++) {
            for (int x = 0; x < volume.data.sizeX(); x++) {
                double v = volume.data.get(x, y, nz, dim);
                v = (v - offset) / slope;
                switch (volume.header.datatype) {
                    case NiftiHeader.NIFTI_TYPE_INT8:
                    case NiftiHeader.NIFTI_TYPE_UINT8: {
                        pixeldata[index] = (byte)v;
                        break;
                    }
                    case NiftiHeader.NIFTI_TYPE_INT16:
                    case NiftiHeader.NIFTI_TYPE_UINT16: {
                        short s = (short)v;
                        writeShort(pixeldata, index, s);
                        break;
                    }
                    case NiftiHeader.NIFTI_TYPE_INT32:
                    case NiftiHeader.NIFTI_TYPE_UINT32: {
                        // the maximum bit storage supported for Pixel Data is 16
                        // therefore, an unfortunate precision reduction must take place
                        int i = (int)v >>> 16;
                        writeShort(pixeldata, index, (short)i);
                        break;
                    }
//                    case NiftiHeader.NIFTI_TYPE_INT64:
//                    case NiftiHeader.NIFTI_TYPE_UINT64: {
//                        long l = (long)v;
//                        writeLong(pixeldata, index, (short)l);
//                        break;
//                    }
                    case NiftiHeader.NIFTI_TYPE_FLOAT32: {
                        obj.putFloat(Tag.RescaleIntercept, VR.DS, -1024);
                        obj.putFloat(Tag.RescaleSlope, VR.DS, 1);
                        short s = (short)((float)v + 1024);
                        writeShort(pixeldata, index, s);
                        break;
                    }
                    case NiftiHeader.NIFTI_TYPE_FLOAT64: {
                        obj.putFloat(Tag.RescaleIntercept, VR.DS, -1024);
                        obj.putFloat(Tag.RescaleSlope, VR.DS, 1);
                        short s = (short)(v + 1024);
                        writeShort(pixeldata, index, s);
                        break;
                    }
                    default:
                        throw new RuntimeIOException("Unsupported NIFTI data type " + NiftiHeader.decodeDatatype(volume.header.datatype));
                }
                index += sizeof_unit;
            }
        }
        obj.putBytes(Tag.PixelData, sizeof_unit == 1 ? VR.OB : VR.OW, pixeldata, false);
    }

    private WindowDicomInjector createWindowInjector(NiftiVolume volume) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        for (int d = 0 ; d < volume.data.dimension() ; d++) {
            for (int z = 0 ; z < volume.data.sizeZ() ; z++) {
                for (int y = 0; y < volume.data.sizeY(); y++) {
                    for (int x = 0; x < volume.data.sizeX(); x++) {
                        double v = volume.data.get(x, y, z, d);
                        if (min > v) {
                            min = v;
                        }
                        if (max < v) {
                            max = v;
                        }
                    }
                }
            }
        }
        double center = (max + min) / 2;
        double width = max - min;
        return new WindowDicomInjector(center, width);
    }

    private static void writeShort(byte[] arr, int offset, short value) {
        arr[offset] = (byte)(value & 0x0FF);
        arr[offset + 1] = (byte)(value >>> 8);
    }

    private static void writeInt(byte[] arr, int offset, int value) {
        arr[offset] = (byte)(value & 0x0FF);
        arr[offset + 1] = (byte)((value >>> 8)&0x0FF);
        arr[offset + 2] = (byte)((value >>> 16)&0x0FF);
        arr[offset + 3] = (byte)((value >>> 24));
    }

    private static void writeLong(byte[] arr, int offset, long value) {
        arr[offset] = (byte)(value & 0x0FF);
        arr[offset + 1] = (byte)((value >>> 8)&0x0FF);
        arr[offset + 2] = (byte)((value >>> 16)&0x0FF);
        arr[offset + 3] = (byte)((value >>> 24)&0x0FF);
        arr[offset + 4] = (byte)((value >>> 32)&0x0FF);
        arr[offset + 5] = (byte)((value >>> 40)&0x0FF);
        arr[offset + 6] = (byte)((value >>> 48)&0x0FF);
        arr[offset + 7] = (byte)((value >>> 56));
    }

    /** Inject DICOM attributes based on the NIFTI-1 header.
     */
    public static class NIFTIDicomInjector implements DicomInjector {

        private final NiftiHeader header;

        public NIFTIDicomInjector(NiftiHeader header) {
            this.header = header;
        }

        @Override
        public DicomObject apply(DicomObject obj) {

            obj.putInt(Tag.Columns, VR.US, header.dim[1]);
            obj.putInt(Tag.Rows, VR.US, header.dim[2]);
            obj.putInt(Tag.ImagesInAcquisition, VR.IS, header.dim[3]);
            obj.putInt(Tag.SamplesPerPixel, VR.US, header.dim[4]);

            obj.putString(Tag.StudyDescription, VR.LO, header.descrip.toString().replace('\\', '!'));

            int bitsAllocated = header.bitpix;
            double slope = header.scl_slope;
            if (bitsAllocated > 16) {
                slope *= header.bitpix / 16.0;
                bitsAllocated = 16;
            }
            obj.putInt(Tag.BitsAllocated, VR.US, bitsAllocated);
            obj.putInt(Tag.BitsStored, VR.US, bitsAllocated);
            obj.putInt(Tag.HighBit, VR.US, bitsAllocated-1);
            obj.putInt(Tag.PixelRepresentation, VR.US,
                    isUnsigned(header.datatype) ? 0 : 1
            );
            if (!obj.contains(Tag.RescaleSlope)) {
                obj.putDouble(Tag.RescaleSlope, VR.DS, slope);
            }
            if (!obj.contains(Tag.RescaleIntercept)) {
                obj.putFloat(Tag.RescaleIntercept, VR.DS, header.scl_inter);
            }

            // use volume.header.pixdim to provide dimensions
            obj.putDouble(Tag.SliceThickness, VR.DS, header.pixdim[3]);
            obj.putDoubles(Tag.PixelSpacing, VR.DS,
                    new double[]{header.pixdim[1], header.pixdim[2]});

            // image position and orientation vectors
            obj.putDoubles(Tag.ImageOrientation, VR.DS,
                    new double[]{0 , 0, 0}); // TODO
            obj.putDoubles(Tag.ImagePosition, VR.DS,
                    new double[]{0, 0, 0}); // TODO
            return obj;
        }

        private static boolean isUnsigned(short datatype) {
            switch (datatype) {
                case NiftiHeader.NIFTI_TYPE_UINT8:
                case NiftiHeader.NIFTI_TYPE_UINT16:
                case NiftiHeader.NIFTI_TYPE_UINT32:
                case NiftiHeader.NIFTI_TYPE_UINT64:
                    return true;
            }
            return false;
        }
    }
}
