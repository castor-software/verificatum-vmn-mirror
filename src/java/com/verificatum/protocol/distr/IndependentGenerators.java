
/*
 * Copyright 2008-2018 Douglas Wikstrom
 *
 * This file is part of Verificatum Mix-Net (VMN).
 *
 * VMN is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * VMN is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with VMN. If not, see <http://www.gnu.org/licenses/>.
 */

package com.verificatum.protocol.distr;

import com.verificatum.arithm.PGroup;
import com.verificatum.arithm.PGroupElementArray;
import com.verificatum.ui.Log;

/**
 * Represents a protocol which jointly generates a list of
 * "independent" generators.
 *
 * @author Douglas Wikstrom
 */
public interface IndependentGenerators {

    /**
     * Generate the independent generators.
     *
     * @param log Logging context.
     * @param pGroup Group to which the generator belongs.
     * @param numberOfGenerators Number of generators.
     * @return Independent generators.
     */
    PGroupElementArray generate(Log log, PGroup pGroup, int numberOfGenerators);
}