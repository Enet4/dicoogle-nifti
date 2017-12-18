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
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pt.ua.dicoogle.nifti.util;

/**
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class RuntimeIOException extends RuntimeException {

    /**
     * Creates a new instance of <code>RuntimeIOException</code> without detail message.
     */
    public RuntimeIOException() {
    }


    /**
     * Constructs an instance of <code>RuntimeIOException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public RuntimeIOException(String msg) {
        super(msg);
    }

    public RuntimeIOException(String msg, Throwable thr) {
        super(msg, thr);
    }

    public RuntimeIOException(Throwable thr) {
        super(thr);
    }
}
