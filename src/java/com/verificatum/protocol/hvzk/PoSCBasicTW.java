
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

package com.verificatum.protocol.hvzk;

import com.verificatum.arithm.ArithmFormatException;
import com.verificatum.arithm.LargeInteger;
import com.verificatum.arithm.LargeIntegerArray;
import com.verificatum.arithm.PField;
import com.verificatum.arithm.PFieldElement;
import com.verificatum.arithm.PFieldElementArray;
import com.verificatum.arithm.PGroup;
import com.verificatum.arithm.PGroupElement;
import com.verificatum.arithm.PGroupElementArray;
import com.verificatum.arithm.PRing;
import com.verificatum.arithm.PRingElement;
import com.verificatum.arithm.PRingElementArray;
import com.verificatum.arithm.Permutation;
import com.verificatum.crypto.PRG;
import com.verificatum.crypto.RandomSource;
import com.verificatum.eio.ByteTreeBasic;
import com.verificatum.eio.ByteTreeContainer;
import com.verificatum.eio.ByteTreeReader;
import com.verificatum.eio.EIOException;
import com.verificatum.protocol.ProtocolError;
import com.verificatum.util.Pair;

/**
 * Implements the basic functionality of a variation of Terelius and
 * Wikstrom's proof of a shuffle.
 *
 * <p>
 *
 * For clarity, each method is labeled BOTH, PROVER, or VERIFIER
 * depending on which parties normally call the method.
 *
 * @author Douglas Wikstrom
 */
@SuppressWarnings({"PMD.VariableNamingConventions",
                   "PMD.MethodNamingConventions",
                   "PMD.SingletonClassReturningNewInstanceRule"})
public final class PoSCBasicTW {

    // ####################### Context ############################

    /**
     * Source of random bits.
     */
    RandomSource randomSource;

    /**
     * Size of the set that is permuted.
     */
    int size;

    /**
     * Bit length of the challenge.
     */
    int vbitlen;

    /**
     * Bit length of each element in the batching vector.
     */
    int ebitlen;

    /**
     * Pseudo-random generator used to derive the random vector.
     */
    PRG prg;

    /**
     * Decides the statistical distance from the uniform distribution.
     */
    int rbitlen;

    /**
     * Underlying group.
     */
    PGroup pGroup;

    /**
     * Ring associated with the group.
     */
    PRing pRing;

    /**
     * Field associated with the ring.
     */
    PField pField;

    // ################### Instance and witness ###################

    /**
     * Standard generator of the group.
     */
    PGroupElement g;

    /**
     * Array of "independent" generators.
     */
    PGroupElementArray h;

    /**
     * Commitment of a permutation.
     */
    PGroupElementArray u;

    /**
     * Random exponents used to form the permutation commitment.
     */
    PRingElementArray r;

    /**
     * Permutation committed to.
     */
    Permutation pi;

    // ################# Message 1 (verifier) #####################

    /**
     * Vector of random exponents.
     */
    PFieldElementArray e;

    // ################# Message 2 (prover) #######################

    /**
     * Bridging commitments used to build up a product in the
     * exponent.
     */
    PGroupElementArray B;

    /**
     * Proof commitment used for the bridging commitments.
     */
    PGroupElement Ap;

    /**
     * Proof commitments for the bridging commitments.
     */
    PGroupElementArray Bp;

    /**
     * Proof commitment for proving sum of random components.
     */
    PGroupElement Cp;

    /**
     * Proof commitment for proving product of random components.
     */
    PGroupElement Dp;

    // ########### Secret values for bridging commitment #######

    /**
     * Inversely permuted random vector.
     */
    PFieldElementArray ipe;

    /**
     * Randomness to form the bridging commitments.
     */
    PRingElementArray b;

    /**
     * Randomness to form the last bridging commitment in a different
     * way.
     */
    PRingElement d;

    // ######### Randomizers and blinders of the prover ########

    /**
     * Randomizer for inner product of r and ipe.
     */
    PRingElement alpha;

    /**
     * Randomizer for b.
     */
    PRingElementArray beta;

    /**
     * Randomizer for sum of the elements in r.
     */
    PRingElement gamma;

    /**
     * Randomizer for opening last element of B.
     */
    PRingElement delta;

    /**
     * Randomizer for inverse permuted batching vector.
     */
    PFieldElementArray epsilon;

    // ################## Message 3 (Verifier) ##################

    /**
     * Challenge from the verifier.
     */
    PFieldElement v;

    // ################## Message 4 (Prover) ##################

    /**
     * Reply for bridging commitment blinder.
     */
    PRingElement k_A;

    /**
     * Reply for bridging commitments blinders.
     */
    PRingElementArray k_B;

    /**
     * Reply for sum of random vector components blinder.
     */
    PRingElement k_C;

    /**
     * Reply for product of random vector components blinder.
     */
    PRingElement k_D;

    /**
     * Reply for the inverse permuted random vector.
     */
    PFieldElementArray k_E;

    /**
     * BOTH: Constructor to instantiate the protocol.
     *
     * @param vbitlen Bit length of the challenge.
     * @param ebitlen Bit length of each component in random
     * vector.
     * @param rbitlen Decides the statistical distance from the
     * uniform distribution.
     * @param prg Pseudo-random generator used to derive random prime
     * vector.
     * @param randomSource Source of randomness.
     */
    public PoSCBasicTW(final int vbitlen, final int ebitlen,
                       final int rbitlen, final PRG prg,
                       final RandomSource randomSource) {
        this.vbitlen = vbitlen;
        this.ebitlen = ebitlen;
        this.rbitlen = rbitlen;
        this.prg = prg;
        this.randomSource = randomSource;

        // This is not needed, but it make things more explicit.
        this.e = null;
        this.B = null;
        this.Ap = null;
        this.Bp = null;
        this.Cp = null;
        this.Dp = null;
        this.ipe = null;
        this.b = null;
        this.d = null;
        this.alpha = null;
        this.beta = null;
        this.gamma = null;
        this.delta = null;
        this.epsilon = null;
        this.k_A = null;
        this.k_B = null;
        this.k_C = null;
        this.k_D = null;
        this.k_E = null;
    }

    /**
     * VERIFIER: Initializes the instance.
     *
     * @param g Standard generator used in permutation commitments.
     * @param h "Independent" generators used in permutation
     * commitments.
     * @param u Permutation commitment.
     */
    public void setInstance(final PGroupElement g,
                            final PGroupElementArray h,
                            final PGroupElementArray u) {
        this.g = g;
        this.h = h;
        this.u = u;
        this.r = null;
        this.pi = null;

        this.size = h.size();
        this.pGroup = g.getPGroup();
        this.pRing = pGroup.getPRing();
        this.pField = pRing.getPField();
    }

    /**
     * PROVER: Initializes the instance and the witness.
     *
     * @param g Standard generator used in permutation commitments.
     * @param h "Independent" generators used in permutation
     * commitments.
     * @param u Permutation commitment.
     * @param r Random exponents used to form the permutation
     * commitment.
     * @param pi Permutation committed to.
     */
    public void setInstance(final PGroupElement g,
                            final PGroupElementArray h,
                            final PGroupElementArray u,
                            final PRingElementArray r,
                            final Permutation pi) {
        setInstance(g, h, u);
        this.r = r;
        this.pi = pi;
    }

    /**
     * BOTH: Extracts the random vector from a seed. This is useful
     * when the honest verifier is replaced by a coin tossing protocol
     * or when this protocol is used as a subprotocol.
     *
     * @param prgSeed Seed to the pseudorandom generator used to
     * extract the random vector.
     */
    public void setBatchVector(final byte[] prgSeed) {
        prg.setSeed(prgSeed);
        final LargeIntegerArray lia = LargeIntegerArray.random(size, ebitlen,
                                                               prg);
        this.e = pField.unsafeToElementArray(lia);
    }

    /**
     * PROVER: Generates the commitment of the prover.
     *
     * @param prgSeed Seed used to extract the random vector.
     * @return Representation of the commitments.
     */
    public ByteTreeBasic commit(final byte[] prgSeed) {

        setBatchVector(prgSeed);

        // ################# Permuted Batching Vector #############

        ipe = e.permute(pi.inv());

        // ################# Bridging Commitments #################

        // When using Pedersen commitments we use the standard
        // generator g and the first element in the list of
        // "independent generators.

        final PGroupElement h0 = h.get(0);

        // The array of bridging commitments is of the form:
        //
        // B_0 = g^{b_0} * h0^{e_0'} (1)
        // B_i = g^{b_i} * B_{i-1}^{e_i'} (2)
        //
        // where we generate the b array as follows:
        b = pRing.randomElementArray(size, randomSource, rbitlen);

        // Thus, we form the committed product of the inverse permuted
        // random exponents.
        //
        // To be able to use fixed-base exponentiation, this is,
        // however, computed as:
        //
        // B_i = g^{x_i} * h0^{y_i}
        //
        // where x_i and y_i are computed as follows.

        // x is computed using a method call that is equivalent to the
        // recursive code in the following comment:
        //
        // PRingElement[] bs = b.elements();
        // PRingElement[] ipes = ipe.elements();
        // PRingElement[] xs = new PRingElement[size];
        // xs[0] = bs[0];
        // for (int i = 1; i < size; i++) {
        // xs[i] = xs[i - 1].mul(ipes[i]).add(bs[i]);
        // }
        // PRingElementArray x = pRing.toElementArray(xs);
        // d = xs[size-1];

        final Pair<PRingElementArray, PRingElement> p = b.recLin(ipe);
        final PRingElementArray x = p.first;
        d = p.second;

        // Compute aggregated products:
        //
        // e_0', e_0'*e_1', e_0'*e_1'*e_2', ...
        //
        final PRingElementArray y = ipe.prods();
        final PGroupElementArray g_exp_x = g.exp(x);
        final PGroupElementArray h0_exp_y = h0.exp(y);
        B = g_exp_x.mul(h0_exp_y);

        // Free temporary variables.
        g_exp_x.free();
        h0_exp_y.free();

        // ################# Proof Commitments ####################

        // During verification, the verifier computes:
        //
        // A = \prod u_i^{e_i} (3)
        //
        // and requires that it equals:
        //
        // g^{<r,e'>} * \prod h_i^{e_i'} (4)
        //
        // We must show that we can open (3) as (4). For that purpose
        // we generate randomizers.

        alpha = pRing.randomElement(randomSource, rbitlen);

        // The bit length of each component of e (and e') is
        // bounded. Thus, we can sample its randomizers as follows.

        final int epsilonBitLength = ebitlen + vbitlen + rbitlen;

        final LargeIntegerArray epsilonIntegers =
            LargeIntegerArray.random(size, epsilonBitLength, randomSource);
        epsilon = pField.toElementArray(epsilonIntegers);
        epsilonIntegers.free();

        // Next we compute the corresponding blinder.
        //
        // A' = g^{\alpha} * \prod h_i^{\epsilon_i}
        //
        Ap = g.exp(alpha).mul(h.expProd(epsilon));

        // During verification, the verifier also requires that (1)
        // and (2) holds. Thus, we choose new randomizers,

        beta = pRing.randomElementArray(size, randomSource, rbitlen);

        // and form corresponding blinders.
        //
        // B_0' = g^{\beta_0'} * h0^{\epsilon_0}
        // B_i' = g^{\beta_i'} * B_{i-1}^{\epsilon_i}
        //
        // PGroupElementArray B_shift = B.shiftPush(h0);
        // PGroupElementArray g_exp_beta = g.exp(beta);
        // PGroupElementArray B_shift_exp_epsilon =
        // B_shift.exp(epsilon);
        // Bp = g_exp_beta.mul(B_shift_exp_epsilon);
        // B_shift.free();
        // g_exp_beta.free();
        // B_shift_exp_epsilon.free();

        final PRingElementArray xp = x.shiftPush(x.getPRing().getZERO());
        final PRingElementArray yp = y.shiftPush(y.getPRing().getONE());

        y.free();
        x.free();

        final PRingElementArray xp_mul_epsilon = xp.mul(epsilon);
        final PRingElementArray beta_add_prod = beta.add(xp_mul_epsilon);
        final PGroupElementArray g_exp_beta_add_prod = g.exp(beta_add_prod);
        final PRingElementArray yp_mul_epsilon = yp.mul(epsilon);
        final PGroupElementArray h0_exp_yp_mul_epsilon = h0.exp(yp_mul_epsilon);

        Bp = g_exp_beta_add_prod.mul(h0_exp_yp_mul_epsilon);

        h0_exp_yp_mul_epsilon.free();
        yp_mul_epsilon.free();
        g_exp_beta_add_prod.free();
        beta_add_prod.free();
        xp_mul_epsilon.free();
        yp.free();
        xp.free();

        // The verifier also requires that the prover knows c=\sum r_i
        // such that
        //
        // \prod u_i / \prod h_i = g^c
        //
        // so we generate a randomizer \gamma and blinder as follows.
        //
        // C' = g^{\gamma}
        //
        gamma = pRing.randomElement(randomSource, rbitlen);
        Cp = g.exp(gamma);

        // Finally, the verifier requires that
        //
        // B_{N-1} / g^{\prod e_i} = g^{d}
        //
        // so we generate a randomizer \delta and blinder as follows.
        //
        // D' = g^{\delta}
        //
        delta = pRing.randomElement(randomSource, rbitlen);
        Dp = g.exp(delta);

        // ################### Byte tree ##########################

        return new ByteTreeContainer(B.toByteTree(),
                                     Ap.toByteTree(),
                                     Bp.toByteTree(),
                                     Cp.toByteTree(),
                                     Dp.toByteTree());
    }

    /**
     * VERIFIER: Sets the commitment.
     *
     * @param btr Commitment from the prover.
     * @return Representation of the commitments.
     */
    public ByteTreeBasic setCommitment(final ByteTreeReader btr) {

        boolean malformed = false;
        try {

            B = pGroup.toElementArray(size, btr.getNextChild());
            Ap = pGroup.toElement(btr.getNextChild());
            Bp = pGroup.toElementArray(size, btr.getNextChild());
            Cp = pGroup.toElement(btr.getNextChild());
            Dp = pGroup.toElement(btr.getNextChild());

        } catch (final EIOException eioe) {
            malformed = true;
        } catch (final ArithmFormatException afe) {
            malformed = true;
        }

        // If anything is malformed we set it to suitable
        // predetermined trivial value.
        if (malformed) {

            B.free();
            B = pGroup.toElementArray(size, pGroup.getONE());

            Ap = pGroup.getONE();

            Bp.free();
            Bp = pGroup.toElementArray(size, pGroup.getONE());

            Cp = pGroup.getONE();
            Dp = pGroup.getONE();
        }

        return new ByteTreeContainer(B.toByteTree(),
                                     Ap.toByteTree(),
                                     Bp.toByteTree(),
                                     Cp.toByteTree(),
                                     Dp.toByteTree());
    }

    /**
     * Returns the bit length of challenges.
     *
     * @return Bit length of challenge.
     */
    public int getVbitlen() {
        return vbitlen;
    }

    /**
     * VERIFIER: Sets the challenge. This is useful if the challenge
     * is generated jointly.
     *
     * @param integerChallenge Challenge of verifier.
     */
    public void setChallenge(final LargeInteger integerChallenge) {
        if (!(0 <= integerChallenge.compareTo(LargeInteger.ZERO)
              && integerChallenge.bitLength() <= vbitlen)) {
            throw new ProtocolError("Malformed challenge!");
        }
        this.v = pField.toElement(integerChallenge);
    }

    /**
     * Computes the reply of the prover to the given challenge, i.e.,
     * the second message of the prover.
     *
     * @param integerChallenge Challenge of verifier.
     * @return Reply of prover.
     */
    public ByteTreeBasic reply(final LargeInteger integerChallenge) {

        setChallenge(integerChallenge);

        // Initialize the special exponents.
        final PRingElement a = r.innerProduct(ipe);
        final PRingElement c = r.sum();

        // Compute the replies as:
        //
        // k_A = a * v + \alpha
        // k_{B,i} = vb_i + \beta_i
        // k_C = vc + \gamma
        // k_D = vd + \delta
        // k_{E,i} = ve_i' + \epsilon_i
        //
        k_A = a.mulAdd(v, alpha);
        k_B = b.mulAdd(v, beta);
        k_C = c.mulAdd(v, gamma);
        k_D = d.mulAdd(v, delta);
        k_E = (PFieldElementArray) ipe.mulAdd(v, epsilon);

        final ByteTreeContainer reply =
            new ByteTreeContainer(k_A.toByteTree(),
                                  k_B.toByteTree(),
                                  k_C.toByteTree(),
                                  k_D.toByteTree(),
                                  k_E.toByteTree());
        return reply;
    }

    /**
     * VERIFIER: Verifies the reply of the prover and outputs true or
     * false depending on if the reply was accepted or not.
     *
     * @param btr Reply of the prover.
     * @return <code>true</code> if the reply is accepted and
     *         <code>false</code> otherwise.
     */
    public boolean verify(final ByteTreeReader btr) {

        // Read and parse the replies.
        try {

            k_A = pRing.toElement(btr.getNextChild());
            k_B = pRing.toElementArray(size, btr.getNextChild());
            k_C = pRing.toElement(btr.getNextChild());
            k_D = pRing.toElement(btr.getNextChild());
            k_E = pField.toElementArray(size, btr.getNextChild());

        } catch (final EIOException eio) {
            return false;
        } catch (final ArithmFormatException afe) {
            return false;
        }

        final PGroupElement h0 = h.get(0);

        // Compute A, C and D.
        final PGroupElement A = u.expProd(e);
        final PGroupElement C = u.prod().div(h.prod());
        final PGroupElement D = B.get(size - 1).div(h0.exp(e.prod()));

        // Assume prover makes us accept.
        boolean verdict = true;

        // Verify that prover knows a=<r,e'> and e' such that:
        //
        // A = \prod u_i^{e_i} = g^a * \prod h_i^{e_i'}
        //

        if (!A.expMul(v, Ap).equals(g.exp(k_A).mul(h.expProd(k_E)))) {
            verdict = false;
        }

        if (verdict) {

            // Verify that prover knows b and e' such that:
            //
            // B_0 = g^{b_0} * h0^{e_0'}
            // B_i = g^{b_i} * B_{i-1}^{e_i'}
            //
            final PGroupElementArray B_exp_v = B.exp(v);
            final PGroupElementArray leftSide = B_exp_v.mul(Bp);

            final PGroupElementArray g_exp_k_B = g.exp(k_B);
            final PGroupElementArray B_shift = B.shiftPush(h0);
            final PGroupElementArray B_shift_exp_k_E = B_shift.exp(k_E);
            final PGroupElementArray rightSide = g_exp_k_B.mul(B_shift_exp_k_E);

            final boolean B_res = leftSide.equals(rightSide);

            B_exp_v.free();
            leftSide.free();
            g_exp_k_B.free();
            B_shift.free();
            B_shift_exp_k_E.free();
            rightSide.free();

            if (!B_res) {
                verdict = false;
            }
        }

        // Verify that prover knows c=\sum r_i such that:
        //
        // C = \prod u_i / \prod h_i = g^c
        //
        if (verdict && !C.expMul(v, Cp).equals(g.exp(k_C))) {
            verdict = false;
        }

        // Verify that prover knows d such that:
        //
        // D = B_{N-1} / g^{\prod e_i} = g^d
        //
        if (verdict && !D.expMul(v, Dp).equals(g.exp(k_D))) {
            verdict = false;
        }
        return verdict;
    }

    /**
     * VERIFIER: Returns the reply that must already have been
     * processed.
     *
     * @return Reply processed by the verifier.
     */
    public ByteTreeBasic getReply() {
        return new ByteTreeContainer(k_A.toByteTree(),
                                     k_B.toByteTree(),
                                     k_C.toByteTree(),
                                     k_D.toByteTree(),
                                     k_E.toByteTree());
    }

    /**
     * Explicitly free resources allocated by this instance. It is the
     * responsibility of the programmer to not call this method and
     * then later use the instance.
     */
    public void free() {
        if (e != null) {
            e.free();
        }
        if (b != null) {
            b.free();
        }
        if (B != null) {
            B.free();
        }
        if (Bp != null) {
            Bp.free();
        }
        if (ipe != null) {
            ipe.free();
        }
        if (beta != null) {
            beta.free();
        }
        if (epsilon != null) {
            epsilon.free();
        }
        if (k_B != null) {
            k_B.free();
        }
        if (k_E != null) {
            k_E.free();
        }
    }
}