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

package pt.ua.dicoogle.nifti.dicom;

import java.util.function.UnaryOperator;
import org.dcm4che2.data.DicomObject;

/** Sub-interface for an operation that takes a DICOM object and injects it
 * with more data.
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
@FunctionalInterface
public interface DicomInjector extends UnaryOperator<DicomObject> {

    @Override
    DicomObject apply(DicomObject obj);
}
