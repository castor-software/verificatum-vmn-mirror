
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

package com.verificatum.protocol.mixnet;

import com.verificatum.protocol.com.BullBoardBasicGen;
import com.verificatum.protocol.elgamal.ProtocolElGamalGen;

/**
 * Defines the additional information fields and default values that
 * stored in the protocol and private info files used by a shuffling
 * protocol {@link ShufflerElGamal}.
 *
 * @author Douglas Wikstrom
 */
public final class ShufflerElGamalGen extends ProtocolElGamalGen {

    /**
     * Creates an instance for a given implementation of a bulletin
     * board.
     *
     * @param bbbg Adds the values needed by the particular
     * instantiation of bulletin board used.
     */
    public ShufflerElGamalGen(final BullBoardBasicGen bbbg) {
        super(bbbg);
    }

    /**
     * Creates an instance for the default bulletin board.
     */
    public ShufflerElGamalGen() {
        super();
    }
}
