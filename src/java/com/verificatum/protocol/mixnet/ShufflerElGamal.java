
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

import java.io.File;

import com.verificatum.arithm.PGroupElement;
import com.verificatum.eio.ByteTreeReader;
import com.verificatum.eio.ByteTreeReaderF;
import com.verificatum.protocol.ProtocolError;
import com.verificatum.protocol.distr.IndependentGeneratorsFactory;
import com.verificatum.protocol.distr.IndependentGeneratorsIFactory;
import com.verificatum.protocol.distr.IndependentGeneratorsROFactory;
import com.verificatum.protocol.elgamal.ProtocolElGamal;
import com.verificatum.protocol.hvzk.CCPoSFactory;
import com.verificatum.protocol.hvzk.CCPoSWFactory;
import com.verificatum.protocol.hvzk.PoSCFactory;
import com.verificatum.protocol.hvzk.PoSCTWFactory;
import com.verificatum.protocol.hvzk.PoSFactory;
import com.verificatum.protocol.hvzk.PoSTWFactory;
import com.verificatum.ui.UI;
import com.verificatum.ui.info.PrivateInfo;
import com.verificatum.ui.info.ProtocolInfo;


/**
 * Implements a shuffle, i.e., a protocol that takes a list of El
 * Gamal ciphertexts and simultaneously re-encrypts and permutes the
 * ciphertexts to form its output. This breaks the correspondence
 * between input ciphertexts and output ciphertexts.
 *
 * @author Douglas Wikstrom
 */
public class ShufflerElGamal extends ProtocolElGamal {

    /**
     * Factory for protocol used to generate independent generator.
     */
    protected IndependentGeneratorsFactory igsFactory;

    /**
     * Factory for creating proofs of shuffles.
     */
    protected PoSCFactory poscFactory;

    /**
     * Factory for creating commitment-consistent proofs of shuffles.
     */
    protected CCPoSFactory ccposFactory;

    /**
     * Factory for creating simple proofs of shuffles.
     */
    protected PoSFactory posFactory;

    /**
     * Public key used during pre-computation.
     */
    protected PGroupElement publicKey;

    /**
     * Initializes a shuffler.
     *
     * @param privateInfo Private info of this party.
     * @param protocolInfo Protocol info of this party.
     * @param ui User interface.
     */
    public ShufflerElGamal(final PrivateInfo privateInfo,
                           final ProtocolInfo protocolInfo,
                           final UI ui) {
        super(privateInfo, protocolInfo, ui);
        init();
    }

    /**
     * Creates a child shuffler that shares some basic variables and
     * the keys of its parent, but not scope on the bulletin board or
     * working directory. This is used to implement sessions.
     *
     * @param sid Session identifier of this instance.
     * @param prot Protocol which invokes this one.
     * @param nizkp Destination directory for random oracle
     * proofs. Note that this directory is deleted when {@link
     * #deleteState()} is called.
     */
    public ShufflerElGamal(final String sid,
                           final ProtocolElGamal prot,
                           final File nizkp) {
        super(sid, prot, null, nizkp);
        init();
    }

    /**
     * Initializes this instance for usage.
     */
    public void init() {

        poscFactory = new PoSCTWFactory();
        ccposFactory = new CCPoSWFactory();
        posFactory = new PoSTWFactory();

        if (nonInteractiveProofs) {

            igsFactory = new IndependentGeneratorsROFactory();


        } else {

            igsFactory = new IndependentGeneratorsIFactory();
        }

        final File file = getFile("PublicKey");
        if (file.exists()) {

            final ByteTreeReader btr = new ByteTreeReaderF(file);
            publicKey = getCiphPGroup(keyPGroup, 1).unsafeToElement(btr);
            btr.close();

        } else {
            publicKey = null;
        }
    }

    /**
     * Set the public key used during shuffling. This can only be
     * called once.
     *
     * @param publicKey Public key used in subsequent shuffles.
     */
    public void setPublicKey(final PGroupElement publicKey) {

        File file;

        file = getFile("PublicKey");
        publicKey.toByteTree().unsafeWriteTo(file);

        this.publicKey = publicKey;
    }

    /**
     * Returns the full public key of this shuffler.
     *
     * @return Public key.
     */
    public PGroupElement getPublicKey() {
        return publicKey;
    }

    /**
     * Returns a child shuffler session that shares some basic
     * variables and the keys of its parent, but not scope on the
     * bulletin board or working directory. This is used to implement
     * sessions.
     *
     * @param auxsid Session identifier for random oracle proofs.
     * @param nizkp Destination directory for random oracle
     * proofs. Note that this directory is deleted when {@link
     * #deleteState()} is called.
     * @return Session.
     */
    public ShufflerElGamalSession getSession(final String auxsid,
                                             final File nizkp) {

        if ("".equals(auxsid)) {
            throw new ProtocolError("Attempting to use empty auxiliary "
                                    + "session identifier!");
        }
        if (publicKey == null) {
            throw new ProtocolError("Attempt to create session before "
                                    + "setting public key!");
        }
        return new ShufflerElGamalSession(auxsid, this, auxsid, nizkp);
    }
}
