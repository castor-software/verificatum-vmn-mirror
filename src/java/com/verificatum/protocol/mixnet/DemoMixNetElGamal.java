
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

import com.verificatum.arithm.ArithmFormatException;
import com.verificatum.arithm.PGroup;
import com.verificatum.arithm.PGroupElement;
import com.verificatum.arithm.PGroupElementArray;
import com.verificatum.arithm.PPGroup;
import com.verificatum.arithm.PPGroupElement;
import com.verificatum.arithm.PRing;
import com.verificatum.arithm.PRingElementArray;
import com.verificatum.eio.ByteTreeReader;
import com.verificatum.protocol.demo.Demo;
import com.verificatum.protocol.demo.DemoError;
import com.verificatum.protocol.demo.DemoException;
import com.verificatum.protocol.demo.DemoProtocol;
import com.verificatum.protocol.demo.DemoProtocolElGamalFactory;
import com.verificatum.protocol.elgamal.ProtocolElGamal;
import com.verificatum.ui.UI;
import com.verificatum.ui.info.InfoException;
import com.verificatum.ui.info.PrivateInfo;
import com.verificatum.ui.info.ProtocolInfo;
import com.verificatum.ui.opt.Opt;

/**
 * Demonstrates {@link MixNetElGamal}.
 *
 * @author Douglas Wikstrom
 */
@SuppressWarnings({"PMD.SignatureDeclareThrowsException",
                   "PMD.AvoidCatchingThrowable"})
public class DemoMixNetElGamal extends DemoProtocolElGamalFactory {

    /**
     * Creates a root protocol.
     */
    public DemoMixNetElGamal() {
        gen = new MixNetElGamalGen();
    }

    // These methods are documented in DemoProtocolFactory.java.

    @Override
    public DemoProtocol newProtocol(final PrivateInfo privateInfo,
                                    final ProtocolInfo protocolInfo,
                                    final UI ui)
        throws Exception {
        return new ExecMixNetElGamal(privateInfo, protocolInfo, ui);
    }

    @Override
    public void generateProtocolInfo(final ProtocolInfo pi,
                                     final Demo demo,
                                     final Opt opt)
        throws InfoException {
        super.generateProtocolInfo(pi, demo, opt);
        pi.addValue(MixNetElGamal.WIDTH, 1);
        pi.addValue(MixNetElGamal.MAXCIPH, 0);
    }

    @Override
    public void verify(final DemoProtocol... servers) throws Exception {

        final ExecMixNetElGamal server = (ExecMixNetElGamal) servers[1];

        for (int l = 0; l < 4; l++) {
            if (server.plaintexts.equals(server.plaintextsOut[l])) {
                throw new DemoException("Shuffle modified plaintexts!");
            }
        }
    }

    /**
     * Turns {@link IndependentGenerator} into a runnable object.
     */
    static class ExecMixNetElGamal extends MixNetElGamal
        implements DemoProtocol {

        /**
         * Plaintexts that are encrypted, shuffled, and decrypted
         * during the execution of the protocol.
         */
        protected PGroupElementArray plaintexts;

        /**
         * Output plaintexts.
         */
        protected PGroupElementArray[] plaintextsOut;

        /**
         * Creates a runnable wrapper for the protocol.
         *
         * @param privateInfo Information about this party.
         * @param protocolInfo Information about the protocol
         * executed, including information about other
         * parties.
         * @param ui User interface.
         * @throws Exception If the info instances are malformed.
         */
        ExecMixNetElGamal(final PrivateInfo privateInfo,
                          final ProtocolInfo protocolInfo,
                          final UI ui)
            throws Exception {
            super(privateInfo, protocolInfo, ui);
        }

        @Override
        public void run() {
            try {

                startServers();
                setup();

                plaintextsOut = new PGroupElementArray[4];

                generatePublicKey();
                final PGroupElement publicKey = getPublicKey();

                // This executes tests with width 1 and 2 and
                // with/without pre-computation.
                for (int l = 0; l < 2; l++) {

                    final int width = l + 1;

                    final PGroup plainPGroup = getPlainPGroup(pGroup, width);
                    final PRing plainPRing = plainPGroup.getPRing();

                    final PPGroup ciphPGroup = getCiphPGroup(pGroup, width);

                    PPGroupElement widePublicKey;

                    getFile("nizkp");

                    PGroupElementArray ciphertexts;

                    if (j == 1) {

                        // Generate plaintexts.
                        final PGroupElement[] plaintextArray =
                            new PGroupElement[10];
                        for (int i = 0; i < plaintextArray.length; i++) {
                            plaintextArray[i] = plainPGroup.getg().exp(i);
                        }
                        plaintexts =
                            plainPGroup.toElementArray(plaintextArray);

                        // Generate ciphertexts.
                        final PRingElementArray r =
                            plainPRing.randomElementArray(10,
                                                          randomSource,
                                                          rbitlen);

                        widePublicKey =
                            ProtocolElGamal.getWidePublicKey(publicKey, width);

                        final PGroupElementArray u =
                            widePublicKey.project(0).exp(r);
                        final PGroupElementArray t =
                            widePublicKey.project(1).exp(r);
                        final PGroupElementArray v = t.mul(plaintexts);
                        t.free();

                        ciphertexts = ciphPGroup.product(u, v);

                        // Publish ciphertexts.
                        ui.getLog().info("Publish demo ciphertexts.");
                        bullBoard.publish("Ciphertexts" + l,
                                          ciphertexts.toByteTree(),
                                          ui.getLog());
                    } else {

                        // Read ciphertexts.
                        final ByteTreeReader ciphertextsReader =
                            bullBoard.waitFor(1, "Ciphertexts" + l,
                                              ui.getLog());

                        try {
                            ciphertexts =
                                ciphPGroup.toElementArray(0, ciphertextsReader);
                        } catch (final ArithmFormatException afe) {
                            throw new DemoError("Failed to read ciphertexts!",
                                                afe);
                        } finally {
                            ciphertextsReader.close();
                        }
                    }

                    final MixNetElGamalSession session =
                        getSession("mysid" + l);

                    // Process ciphertexts.
                    if (l >= 2) {
                        session.precomp(ui.getLog(), width, 15);
                    }
                    plaintextsOut[l] =
                        session.mix(ui.getLog(), width, ciphertexts);
                }
                shutdown(ui.getLog());

            } catch (final Throwable e) {
                throw new DemoError("Unable to run demonstration!", e);
            }
        }
    }
}
