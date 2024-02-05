/****************************************************************************
Copyright (c) 2006, Colorado School of Mines and others. All rights reserved.
This program and accompanying materials are made available under the terms of
the Common Public License - v1.0, which accompanies this distribution, and is
available at http://www.eclipse.org/legal/cpl-v10.html
 ****************************************************************************/
package jsex.voronoi;


import jse.atom.XYZ;
import jse.code.collection.DoublePair;
import jse.parallel.DoubleArrayCache;

/**
 * Robust geometric predicates.
 * <p>
 * These geometric predicates are notoriously susceptible to roundoff error. For
 * example, the simplest and fastest test to determine whether a point c is left
 * of a line defined by two points a and b may fail when all three points are
 * nearly co-linear.
 * <p>
 * Therefore, each predicate is implemented by two types of methods. One method
 * is fast, but may yield incorrect answers. The other method is slower, because
 * it (1) computes a bound on the roundoff error and (2) reverts to an exact
 * algorithm if the fast method might yield the wrong answer.
 * <p>
 * Most applications should use the slower exact methods. The fast methods are
 * provided only for comparison.
 * <p>
 * These predicates are adapted from those developed by Jonathan Shewchuk, 1997,
 * Delaunay Refinement Mesh Generation: Ph.D. dissertation, Carnegie Mellon
 * University. (Currently, the methods here do not use Shewchuk's adaptive
 * four-stage pipeline. Instead, only two - the fastest and the exact stages -
 * are used.)
 *
 * @author Dave Hale, Colorado School of Mines
 * @version 2001.04.03, 2006.08.02
 */
public class Geometry {
    
    /**
     * Computes the center of the sphere defined by the points a, b, c, and d. The
     * latter are assumed to be in CCW order, such that the method
     * {@link #leftOfPlane} would return a positive number.
     *
     * @param po array containing (x,y,z) coordinates of center.
     */
    public static void centerSphere(double xa, double ya, double za, double xb, double yb, double zb, double xc,
                                    double yc, double zc, double xd, double yd, double zd, XYZ po) {
        double adx = xa - xd;
        double bdx = xb - xd;
        double cdx = xc - xd;
        double ady = ya - yd;
        double bdy = yb - yd;
        double cdy = yc - yd;
        double adz = za - zd;
        double bdz = zb - zd;
        double cdz = zc - zd;
        double ads = adx * adx + ady * ady + adz * adz;
        double bds = bdx * bdx + bdy * bdy + bdz * bdz;
        double cds = cdx * cdx + cdy * cdy + cdz * cdz;
        double scale = 0.5 / leftOfPlane(xa, ya, za, xb, yb, zb, xc, yc, zc, xd, yd, zd);
        po.mX = xd
            + scale * (ads * (bdy * cdz - cdy * bdz) + bds * (cdy * adz - ady * cdz) + cds * (ady * bdz - bdy * adz));
        po.mY = yd
            + scale * (ads * (bdz * cdx - cdz * bdx) + bds * (cdz * adx - adz * cdx) + cds * (adz * bdx - bdz * adx));
        po.mZ = zd
            + scale * (ads * (bdx * cdy - cdx * bdy) + bds * (cdx * ady - adx * cdy) + cds * (adx * bdy - bdx * ady));
    }
    
    /**
     * Determines if a point e is inside the sphere defined by the points a, b, c,
     * and d. The latter are assumed to be in CCW order, such that the method
     * {@link #leftOfPlane} would return a positive number.
     *
     * @return positive, if inside the sphere; negative, if outside the sphere;
     *         zero, otherwise.
     */
    public static double inSphere(double xa, double ya, double za, double xb, double yb, double zb, double xc,
                                  double yc, double zc, double xd, double yd, double zd, double xe, double ye,
                                  double ze) {
        double aex = xa - xe;
        double bex = xb - xe;
        double cex = xc - xe;
        double dex = xd - xe;
        double aey = ya - ye;
        double bey = yb - ye;
        double cey = yc - ye;
        double dey = yd - ye;
        double aez = za - ze;
        double bez = zb - ze;
        double cez = zc - ze;
        double dez = zd - ze;
        
        double aexbey = aex * bey;
        double bexaey = bex * aey;
        double ab = aexbey - bexaey;
        double bexcey = bex * cey;
        double cexbey = cex * bey;
        double bc = bexcey - cexbey;
        double cexdey = cex * dey;
        double dexcey = dex * cey;
        double cd = cexdey - dexcey;
        double dexaey = dex * aey;
        double aexdey = aex * dey;
        double da = dexaey - aexdey;
        
        double aexcey = aex * cey;
        double cexaey = cex * aey;
        double ac = aexcey - cexaey;
        double bexdey = bex * dey;
        double dexbey = dex * bey;
        double bd = bexdey - dexbey;
        
        double abc = aez * bc - bez * ac + cez * ab;
        double bcd = bez * cd - cez * bd + dez * bc;
        double cda = cez * da + dez * ac + aez * cd;
        double dab = dez * ab + aez * bd + bez * da;
        
        double alift = aex * aex + aey * aey + aez * aez;
        double blift = bex * bex + bey * bey + bez * bez;
        double clift = cex * cex + cey * cey + cez * cez;
        double dlift = dex * dex + dey * dey + dez * dez;
        
        double det = dlift * abc - clift * dab + (blift * cda - alift * bcd);
        
        if (aez < 0.0) {
            aez = -aez;
        }
        if (bez < 0.0) {
            bez = -bez;
        }
        if (cez < 0.0) {
            cez = -cez;
        }
        if (dez < 0.0) {
            dez = -dez;
        }
        if (aexbey < 0.0) {
            aexbey = -aexbey;
        }
        if (bexaey < 0.0) {
            bexaey = -bexaey;
        }
        if (bexcey < 0.0) {
            bexcey = -bexcey;
        }
        if (cexbey < 0.0) {
            cexbey = -cexbey;
        }
        if (cexdey < 0.0) {
            cexdey = -cexdey;
        }
        if (dexcey < 0.0) {
            dexcey = -dexcey;
        }
        if (dexaey < 0.0) {
            dexaey = -dexaey;
        }
        if (aexdey < 0.0) {
            aexdey = -aexdey;
        }
        if (aexcey < 0.0) {
            aexcey = -aexcey;
        }
        if (cexaey < 0.0) {
            cexaey = -cexaey;
        }
        if (bexdey < 0.0) {
            bexdey = -bexdey;
        }
        if (dexbey < 0.0) {
            dexbey = -dexbey;
        }
        double permanent = ((cexdey + dexcey) * bez + (dexbey + bexdey) * cez + (bexcey + cexbey) * dez) * alift
            + ((dexaey + aexdey) * cez + (aexcey + cexaey) * dez + (cexdey + dexcey) * aez) * blift
            + ((aexbey + bexaey) * dez + (bexdey + dexbey) * aez + (dexaey + aexdey) * bez) * clift
            + ((bexcey + cexbey) * aez + (cexaey + aexcey) * bez + (aexbey + bexaey) * cez) * dlift;
        double errbound = INSERRBOUND * permanent;
        if (det > errbound || -det > errbound) {
            return det;
        }
        
        return inSphereExact(xa, ya, za, xb, yb, zb, xc, yc, zc, xd, yd, zd, xe, ye, ze);
    }
    
    
    
    /**
     * Determines if a point d is left of the plane defined by the points a, b, and
     * c. The latter are assumed to be in CCW order, as viewed from the right side
     * of the plane.
     *
     * @return positive, if left of plane; negative, if right of plane; zero,
     *         otherwise.
     */
    public static double leftOfPlane(double xa, double ya, double za, double xb, double yb, double zb, double xc,
                                     double yc, double zc, double xd, double yd, double zd) {
        double adx = xa - xd;
        double bdx = xb - xd;
        double cdx = xc - xd;
        double ady = ya - yd;
        double bdy = yb - yd;
        double cdy = yc - yd;
        double adz = za - zd;
        double bdz = zb - zd;
        double cdz = zc - zd;
        
        double bdxcdy = bdx * cdy;
        double cdxbdy = cdx * bdy;
        
        double cdxady = cdx * ady;
        double adxcdy = adx * cdy;
        
        double adxbdy = adx * bdy;
        double bdxady = bdx * ady;
        
        double det = adz * (bdxcdy - cdxbdy) + bdz * (cdxady - adxcdy) + cdz * (adxbdy - bdxady);
        
        if (adz < 0.0) {
            adz = -adz;
        }
        if (bdz < 0.0) {
            bdz = -bdz;
        }
        if (cdz < 0.0) {
            cdz = -cdz;
        }
        if (bdxcdy < 0.0) {
            bdxcdy = -bdxcdy;
        }
        if (cdxbdy < 0.0) {
            cdxbdy = -cdxbdy;
        }
        if (cdxady < 0.0) {
            cdxady = -cdxady;
        }
        if (adxcdy < 0.0) {
            adxcdy = -adxcdy;
        }
        if (adxbdy < 0.0) {
            adxbdy = -adxbdy;
        }
        if (bdxady < 0.0) {
            bdxady = -bdxady;
        }
        double permanent = (bdxcdy + cdxbdy) * adz + (cdxady + adxcdy) * bdz + (adxbdy + bdxady) * cdz;
        double errbound = O3DERRBOUND * permanent;
        if (det > errbound || -det > errbound) {
            return det;
        }
        
        return leftOfPlaneExact(xa, ya, za, xb, yb, zb, xc, yc, zc, xd, yd, zd);
    }
    
    
    private static final double EPSILON;
    private static final double INCERRBOUND;
    private static final double INSERRBOUND;
    private static final double IOSERRBOUND;
    private static final double O2DERRBOUND;
    private static final double O3DERRBOUND;
    private static final double SPLITTER;
    static {
        double epsilon = 1.0;
        double splitter = 1.0;
        boolean everyOther = true;
        do {
            epsilon *= 0.5;
            if (everyOther) {
                splitter *= 2.0;
            }
            everyOther = !everyOther;
        } while (1.0 + epsilon != 1.0);
        splitter += 1.0;
        EPSILON = epsilon;
        SPLITTER = splitter;
        O2DERRBOUND = 4.0 * EPSILON;
        O3DERRBOUND = 8.0 * EPSILON;
        INCERRBOUND = 11.0 * EPSILON;
        INSERRBOUND = 17.0 * EPSILON;
        IOSERRBOUND = 19.0 * EPSILON;
    }
    
    
    @SuppressWarnings("UnnecessaryLocalVariable")
    private static double inSphereExact(double xa, double ya, double za, double xb, double yb, double zb, double xc,
                                        double yc, double zc, double xd, double yd, double zd, double xe, double ye,
                                        double ze) {
        DoublePair t = new DoublePair(0.0, 0.0);
        twoDiff(xa, xe, t);
        double aex = t.mFirst;
        double aextail = t.mSecond;
        twoDiff(ya, ye, t);
        double aey = t.mFirst;
        double aeytail = t.mSecond;
        twoDiff(za, ze, t);
        double aez = t.mFirst;
        double aeztail = t.mSecond;
        twoDiff(xb, xe, t);
        double bex = t.mFirst;
        double bextail = t.mSecond;
        twoDiff(yb, ye, t);
        double bey = t.mFirst;
        double beytail = t.mSecond;
        twoDiff(zb, ze, t);
        double bez = t.mFirst;
        double beztail = t.mSecond;
        twoDiff(xc, xe, t);
        double cex = t.mFirst;
        double cextail = t.mSecond;
        twoDiff(yc, ye, t);
        double cey = t.mFirst;
        double ceytail = t.mSecond;
        twoDiff(zc, ze, t);
        double cez = t.mFirst;
        double ceztail = t.mSecond;
        twoDiff(xd, xe, t);
        double dex = t.mFirst;
        double dextail = t.mSecond;
        twoDiff(yd, ye, t);
        double dey = t.mFirst;
        double deytail = t.mSecond;
        twoDiff(zd, ze, t);
        double dez = t.mFirst;
        double deztail = t.mSecond;
        
        final double[]
              t8a     = DoubleArrayCache.getArray(8    )
            , t8b     = DoubleArrayCache.getArray(8    )
            , t16a    = DoubleArrayCache.getArray(16   )
            , t16b    = DoubleArrayCache.getArray(16   )
            , t16c    = DoubleArrayCache.getArray(16   )
            , t16d    = DoubleArrayCache.getArray(16   )
            , t16e    = DoubleArrayCache.getArray(16   )
            , t16f    = DoubleArrayCache.getArray(16   )
            , t32a    = DoubleArrayCache.getArray(32   )
            , t32b    = DoubleArrayCache.getArray(32   )
            , t64a    = DoubleArrayCache.getArray(64   )
            , t64b    = DoubleArrayCache.getArray(64   )
            , t64c    = DoubleArrayCache.getArray(64   )
            , t128a   = DoubleArrayCache.getArray(128  )
            , t192a   = DoubleArrayCache.getArray(192  )
            , t384a   = DoubleArrayCache.getArray(384  )
            , t384b   = DoubleArrayCache.getArray(384  )
            , t384c   = DoubleArrayCache.getArray(384  )
            , t384d   = DoubleArrayCache.getArray(384  )
            , t384e   = DoubleArrayCache.getArray(384  )
            , t384f   = DoubleArrayCache.getArray(384  )
            , t768a   = DoubleArrayCache.getArray(768  )
            , t768b   = DoubleArrayCache.getArray(768  )
            , t768c   = DoubleArrayCache.getArray(768  )
            , t768d   = DoubleArrayCache.getArray(768  )
            , t768e   = DoubleArrayCache.getArray(768  )
            , t768f   = DoubleArrayCache.getArray(768  )
            , t768g   = DoubleArrayCache.getArray(768  )
            , t768h   = DoubleArrayCache.getArray(768  )
            , t768i   = DoubleArrayCache.getArray(768  )
            , t1536a  = DoubleArrayCache.getArray(1536 )
            , t1536b  = DoubleArrayCache.getArray(1536 )
            , t1536c  = DoubleArrayCache.getArray(1536 )
            , t2304a  = DoubleArrayCache.getArray(2304 )
            , t2304b  = DoubleArrayCache.getArray(2304 )
            , t2304c  = DoubleArrayCache.getArray(2304 )
            , t4608a  = DoubleArrayCache.getArray(4608 )
            , t6912a  = DoubleArrayCache.getArray(6912 )
            , t6912b  = DoubleArrayCache.getArray(6912 )
            , t6912c  = DoubleArrayCache.getArray(6912 )
            , t6912d  = DoubleArrayCache.getArray(6912 )
            , t13824a = DoubleArrayCache.getArray(13824)
            , t13824b = DoubleArrayCache.getArray(13824)
            , t27648a = DoubleArrayCache.getArray(27648)
            ;
        
        
        double[] ab = t16a;
        twoTwoProduct(aex, aextail, bey, beytail, t8a);
        double negate = -aey;
        double negatetail = -aeytail;
        twoTwoProduct(bex, bextail, negate, negatetail, t8b);
        int ablen = expansionSumZeroElimFast(8, t8a, 8, t8b, ab);
        
        double[] bc = t16b;
        twoTwoProduct(bex, bextail, cey, ceytail, t8a);
        negate = -bey;
        negatetail = -beytail;
        twoTwoProduct(cex, cextail, negate, negatetail, t8b);
        int bclen = expansionSumZeroElimFast(8, t8a, 8, t8b, bc);
        
        double[] cd = t16c;
        twoTwoProduct(cex, cextail, dey, deytail, t8a);
        negate = -cey;
        negatetail = -ceytail;
        twoTwoProduct(dex, dextail, negate, negatetail, t8b);
        int cdlen = expansionSumZeroElimFast(8, t8a, 8, t8b, cd);
        
        double[] da = t16d;
        twoTwoProduct(dex, dextail, aey, aeytail, t8a);
        negate = -dey;
        negatetail = -deytail;
        twoTwoProduct(aex, aextail, negate, negatetail, t8b);
        int dalen = expansionSumZeroElimFast(8, t8a, 8, t8b, da);
        
        double[] ac = t16e;
        twoTwoProduct(aex, aextail, cey, ceytail, t8a);
        negate = -aey;
        negatetail = -aeytail;
        twoTwoProduct(cex, cextail, negate, negatetail, t8b);
        int aclen = expansionSumZeroElimFast(8, t8a, 8, t8b, ac);
        
        double[] bd = t16f;
        twoTwoProduct(bex, bextail, dey, deytail, t8a);
        negate = -bey;
        negatetail = -beytail;
        twoTwoProduct(dex, dextail, negate, negatetail, t8b);
        int bdlen = expansionSumZeroElimFast(8, t8a, 8, t8b, bd);
        
        int t32alen, t32blen, t64alen, t64blen, t64clen, t128len, t192len;
        t32alen = scaleExpansionZeroElim(cdlen, cd, -bez, t32a);
        t32blen = scaleExpansionZeroElim(cdlen, cd, -beztail, t32b);
        t64alen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64a);
        t32alen = scaleExpansionZeroElim(bdlen, bd, cez, t32a);
        t32blen = scaleExpansionZeroElim(bdlen, bd, ceztail, t32b);
        t64blen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64b);
        t32alen = scaleExpansionZeroElim(bclen, bc, -dez, t32a);
        t32blen = scaleExpansionZeroElim(bclen, bc, -deztail, t32b);
        t64clen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64c);
        t128len = expansionSumZeroElimFast(t64alen, t64a, t64blen, t64b, t128a);
        t192len = expansionSumZeroElimFast(t64clen, t64c, t128len, t128a, t192a);
        
        int t384alen = scaleExpansionZeroElim(t192len, t192a, aex, t384a);
        int t768alen = scaleExpansionZeroElim(t384alen, t384a, aex, t768a);
        int t384blen = scaleExpansionZeroElim(t192len, t192a, aextail, t384b);
        int t768blen = scaleExpansionZeroElim(t384blen, t384b, aex, t768b);
        for (int i = 0; i < t768blen; ++i) {
            t768b[i] *= 2.0;
        }
        int t768clen = scaleExpansionZeroElim(t384blen, t384b, aextail, t768c);
        int t1536alen = expansionSumZeroElimFast(t768alen, t768a, t768blen, t768b, t1536a);
        int t2304alen = expansionSumZeroElimFast(t1536alen, t1536a, t768clen, t768c, t2304a);
        
        int t384clen = scaleExpansionZeroElim(t192len, t192a, aey, t384c);
        int t768dlen = scaleExpansionZeroElim(t384clen, t384c, aey, t768d);
        int t384dlen = scaleExpansionZeroElim(t192len, t192a, aeytail, t384d);
        int t768elen = scaleExpansionZeroElim(t384dlen, t384d, aey, t768e);
        for (int i = 0; i < t768elen; ++i) {
            t768e[i] *= 2.0;
        }
        int t768flen = scaleExpansionZeroElim(t384dlen, t384d, aeytail, t768f);
        int t1536blen = expansionSumZeroElimFast(t768dlen, t768d, t768elen, t768e, t1536b);
        int t2304blen = expansionSumZeroElimFast(t1536blen, t1536b, t768flen, t768f, t2304b);
        
        int t384elen = scaleExpansionZeroElim(t192len, t192a, aez, t384e);
        int t768glen = scaleExpansionZeroElim(t384elen, t384e, aez, t768g);
        int t384flen = scaleExpansionZeroElim(t192len, t192a, aeztail, t384f);
        int t768hlen = scaleExpansionZeroElim(t384flen, t384f, aez, t768h);
        for (int i = 0; i < t768hlen; ++i) {
            t768h[i] *= 2.0;
        }
        int t768ilen = scaleExpansionZeroElim(t384flen, t384f, aeztail, t768i);
        int t1536clen = expansionSumZeroElimFast(t768glen, t768g, t768hlen, t768h, t1536c);
        int t2304clen = expansionSumZeroElimFast(t1536clen, t1536c, t768ilen, t768i, t2304c);
        
        int t4608alen = expansionSumZeroElimFast(t2304alen, t2304a, t2304blen, t2304b, t4608a);
        int t6912alen = expansionSumZeroElimFast(t2304clen, t2304c, t4608alen, t4608a, t6912a);
        
        t32alen = scaleExpansionZeroElim(dalen, da, cez, t32a);
        t32blen = scaleExpansionZeroElim(dalen, da, ceztail, t32b);
        t64alen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64a);
        t32alen = scaleExpansionZeroElim(aclen, ac, dez, t32a);
        t32blen = scaleExpansionZeroElim(aclen, ac, deztail, t32b);
        t64blen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64b);
        t32alen = scaleExpansionZeroElim(cdlen, cd, aez, t32a);
        t32blen = scaleExpansionZeroElim(cdlen, cd, aeztail, t32b);
        t64clen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64c);
        t128len = expansionSumZeroElimFast(t64alen, t64a, t64blen, t64b, t128a);
        t192len = expansionSumZeroElimFast(t64clen, t64c, t128len, t128a, t192a);
        t384alen = scaleExpansionZeroElim(t192len, t192a, bex, t384a);
        t768alen = scaleExpansionZeroElim(t384alen, t384a, bex, t768a);
        t384blen = scaleExpansionZeroElim(t192len, t192a, bextail, t384b);
        t768blen = scaleExpansionZeroElim(t384blen, t384b, bex, t768b);
        for (int i = 0; i < t768blen; ++i) {
            t768b[i] *= 2.0;
        }
        t768clen = scaleExpansionZeroElim(t384blen, t384b, bextail, t768c);
        t1536alen = expansionSumZeroElimFast(t768alen, t768a, t768blen, t768b, t1536a);
        t2304alen = expansionSumZeroElimFast(t1536alen, t1536a, t768clen, t768c, t2304a);
        t384clen = scaleExpansionZeroElim(t192len, t192a, bey, t384c);
        t768dlen = scaleExpansionZeroElim(t384clen, t384c, bey, t768d);
        t384dlen = scaleExpansionZeroElim(t192len, t192a, beytail, t384d);
        t768elen = scaleExpansionZeroElim(t384dlen, t384d, bey, t768e);
        for (int i = 0; i < t768elen; ++i) {
            t768e[i] *= 2.0;
        }
        t768flen = scaleExpansionZeroElim(t384dlen, t384d, beytail, t768f);
        t1536blen = expansionSumZeroElimFast(t768dlen, t768d, t768elen, t768e, t1536b);
        t2304blen = expansionSumZeroElimFast(t1536blen, t1536b, t768flen, t768f, t2304b);
        t384elen = scaleExpansionZeroElim(t192len, t192a, bez, t384e);
        t768glen = scaleExpansionZeroElim(t384elen, t384e, bez, t768g);
        t384flen = scaleExpansionZeroElim(t192len, t192a, beztail, t384f);
        t768hlen = scaleExpansionZeroElim(t384flen, t384f, bez, t768h);
        for (int i = 0; i < t768hlen; ++i) {
            t768h[i] *= 2.0;
        }
        t768ilen = scaleExpansionZeroElim(t384flen, t384f, beztail, t768i);
        t1536clen = expansionSumZeroElimFast(t768glen, t768g, t768hlen, t768h, t1536c);
        t2304clen = expansionSumZeroElimFast(t1536clen, t1536c, t768ilen, t768i, t2304c);
        t4608alen = expansionSumZeroElimFast(t2304alen, t2304a, t2304blen, t2304b, t4608a);
        int t6912blen = expansionSumZeroElimFast(t2304clen, t2304c, t4608alen, t4608a, t6912b);
        
        t32alen = scaleExpansionZeroElim(ablen, ab, -dez, t32a);
        t32blen = scaleExpansionZeroElim(ablen, ab, -deztail, t32b);
        t64alen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64a);
        t32alen = scaleExpansionZeroElim(bdlen, bd, -aez, t32a);
        t32blen = scaleExpansionZeroElim(bdlen, bd, -aeztail, t32b);
        t64blen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64b);
        t32alen = scaleExpansionZeroElim(dalen, da, -bez, t32a);
        t32blen = scaleExpansionZeroElim(dalen, da, -beztail, t32b);
        t64clen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64c);
        t128len = expansionSumZeroElimFast(t64alen, t64a, t64blen, t64b, t128a);
        t192len = expansionSumZeroElimFast(t64clen, t64c, t128len, t128a, t192a);
        t384alen = scaleExpansionZeroElim(t192len, t192a, cex, t384a);
        t768alen = scaleExpansionZeroElim(t384alen, t384a, cex, t768a);
        t384blen = scaleExpansionZeroElim(t192len, t192a, cextail, t384b);
        t768blen = scaleExpansionZeroElim(t384blen, t384b, cex, t768b);
        for (int i = 0; i < t768blen; ++i) {
            t768b[i] *= 2.0;
        }
        t768clen = scaleExpansionZeroElim(t384blen, t384b, cextail, t768c);
        t1536alen = expansionSumZeroElimFast(t768alen, t768a, t768blen, t768b, t1536a);
        t2304alen = expansionSumZeroElimFast(t1536alen, t1536a, t768clen, t768c, t2304a);
        t384clen = scaleExpansionZeroElim(t192len, t192a, cey, t384c);
        t768dlen = scaleExpansionZeroElim(t384clen, t384c, cey, t768d);
        t384dlen = scaleExpansionZeroElim(t192len, t192a, ceytail, t384d);
        t768elen = scaleExpansionZeroElim(t384dlen, t384d, cey, t768e);
        for (int i = 0; i < t768elen; ++i) {
            t768e[i] *= 2.0;
        }
        t768flen = scaleExpansionZeroElim(t384dlen, t384d, ceytail, t768f);
        t1536blen = expansionSumZeroElimFast(t768dlen, t768d, t768elen, t768e, t1536b);
        t2304blen = expansionSumZeroElimFast(t1536blen, t1536b, t768flen, t768f, t2304b);
        t384elen = scaleExpansionZeroElim(t192len, t192a, cez, t384e);
        t768glen = scaleExpansionZeroElim(t384elen, t384e, cez, t768g);
        t384flen = scaleExpansionZeroElim(t192len, t192a, ceztail, t384f);
        t768hlen = scaleExpansionZeroElim(t384flen, t384f, cez, t768h);
        for (int i = 0; i < t768hlen; ++i) {
            t768h[i] *= 2.0;
        }
        t768ilen = scaleExpansionZeroElim(t384flen, t384f, ceztail, t768i);
        t1536clen = expansionSumZeroElimFast(t768glen, t768g, t768hlen, t768h, t1536c);
        t2304clen = expansionSumZeroElimFast(t1536clen, t1536c, t768ilen, t768i, t2304c);
        t4608alen = expansionSumZeroElimFast(t2304alen, t2304a, t2304blen, t2304b, t4608a);
        int t6912clen = expansionSumZeroElimFast(t2304clen, t2304c, t4608alen, t4608a, t6912c);
        
        t32alen = scaleExpansionZeroElim(bclen, bc, aez, t32a);
        t32blen = scaleExpansionZeroElim(bclen, bc, aeztail, t32b);
        t64alen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64a);
        t32alen = scaleExpansionZeroElim(aclen, ac, -bez, t32a);
        t32blen = scaleExpansionZeroElim(aclen, ac, -beztail, t32b);
        t64blen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64b);
        t32alen = scaleExpansionZeroElim(ablen, ab, cez, t32a);
        t32blen = scaleExpansionZeroElim(ablen, ab, ceztail, t32b);
        t64clen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64c);
        t128len = expansionSumZeroElimFast(t64alen, t64a, t64blen, t64b, t128a);
        t192len = expansionSumZeroElimFast(t64clen, t64c, t128len, t128a, t192a);
        t384alen = scaleExpansionZeroElim(t192len, t192a, dex, t384a);
        t768alen = scaleExpansionZeroElim(t384alen, t384a, dex, t768a);
        t384blen = scaleExpansionZeroElim(t192len, t192a, dextail, t384b);
        t768blen = scaleExpansionZeroElim(t384blen, t384b, dex, t768b);
        for (int i = 0; i < t768blen; ++i) {
            t768b[i] *= 2.0;
        }
        t768clen = scaleExpansionZeroElim(t384blen, t384b, dextail, t768c);
        t1536alen = expansionSumZeroElimFast(t768alen, t768a, t768blen, t768b, t1536a);
        t2304alen = expansionSumZeroElimFast(t1536alen, t1536a, t768clen, t768c, t2304a);
        t384clen = scaleExpansionZeroElim(t192len, t192a, dey, t384c);
        t768dlen = scaleExpansionZeroElim(t384clen, t384c, dey, t768d);
        t384dlen = scaleExpansionZeroElim(t192len, t192a, deytail, t384d);
        t768elen = scaleExpansionZeroElim(t384dlen, t384d, dey, t768e);
        for (int i = 0; i < t768elen; ++i) {
            t768e[i] *= 2.0;
        }
        t768flen = scaleExpansionZeroElim(t384dlen, t384d, deytail, t768f);
        t1536blen = expansionSumZeroElimFast(t768dlen, t768d, t768elen, t768e, t1536b);
        t2304blen = expansionSumZeroElimFast(t1536blen, t1536b, t768flen, t768f, t2304b);
        t384elen = scaleExpansionZeroElim(t192len, t192a, dez, t384e);
        t768glen = scaleExpansionZeroElim(t384elen, t384e, dez, t768g);
        t384flen = scaleExpansionZeroElim(t192len, t192a, deztail, t384f);
        t768hlen = scaleExpansionZeroElim(t384flen, t384f, dez, t768h);
        for (int i = 0; i < t768hlen; ++i) {
            t768h[i] *= 2.0;
        }
        t768ilen = scaleExpansionZeroElim(t384flen, t384f, deztail, t768i);
        t1536clen = expansionSumZeroElimFast(t768glen, t768g, t768hlen, t768h, t1536c);
        t2304clen = expansionSumZeroElimFast(t1536clen, t1536c, t768ilen, t768i, t2304c);
        t4608alen = expansionSumZeroElimFast(t2304alen, t2304a, t2304blen, t2304b, t4608a);
        int t6912dlen = expansionSumZeroElimFast(t2304clen, t2304c, t4608alen, t4608a, t6912d);
        
        ablen = expansionSumZeroElimFast(t6912alen, t6912a, t6912blen, t6912b, t13824a);
        cdlen = expansionSumZeroElimFast(t6912clen, t6912c, t6912dlen, t6912d, t13824b);
        int t27648alen = expansionSumZeroElimFast(ablen, t13824a, cdlen, t13824b, t27648a);
        
        double out = t27648a[t27648alen - 1];
        
        // 计算完完成归还临时数组
        DoubleArrayCache.returnArray(t8a    );
        DoubleArrayCache.returnArray(t8b    );
        DoubleArrayCache.returnArray(t16a   );
        DoubleArrayCache.returnArray(t16b   );
        DoubleArrayCache.returnArray(t16c   );
        DoubleArrayCache.returnArray(t16d   );
        DoubleArrayCache.returnArray(t16e   );
        DoubleArrayCache.returnArray(t16f   );
        DoubleArrayCache.returnArray(t32a   );
        DoubleArrayCache.returnArray(t32b   );
        DoubleArrayCache.returnArray(t64a   );
        DoubleArrayCache.returnArray(t64b   );
        DoubleArrayCache.returnArray(t64c   );
        DoubleArrayCache.returnArray(t128a  );
        DoubleArrayCache.returnArray(t192a  );
        DoubleArrayCache.returnArray(t384a  );
        DoubleArrayCache.returnArray(t384b  );
        DoubleArrayCache.returnArray(t384c  );
        DoubleArrayCache.returnArray(t384d  );
        DoubleArrayCache.returnArray(t384e  );
        DoubleArrayCache.returnArray(t384f  );
        DoubleArrayCache.returnArray(t768a  );
        DoubleArrayCache.returnArray(t768b  );
        DoubleArrayCache.returnArray(t768c  );
        DoubleArrayCache.returnArray(t768d  );
        DoubleArrayCache.returnArray(t768e  );
        DoubleArrayCache.returnArray(t768f  );
        DoubleArrayCache.returnArray(t768g  );
        DoubleArrayCache.returnArray(t768h  );
        DoubleArrayCache.returnArray(t768i  );
        DoubleArrayCache.returnArray(t1536a );
        DoubleArrayCache.returnArray(t1536b );
        DoubleArrayCache.returnArray(t1536c );
        DoubleArrayCache.returnArray(t2304a );
        DoubleArrayCache.returnArray(t2304b );
        DoubleArrayCache.returnArray(t2304c );
        DoubleArrayCache.returnArray(t4608a );
        DoubleArrayCache.returnArray(t6912a );
        DoubleArrayCache.returnArray(t6912b );
        DoubleArrayCache.returnArray(t6912c );
        DoubleArrayCache.returnArray(t6912d );
        DoubleArrayCache.returnArray(t13824a);
        DoubleArrayCache.returnArray(t13824b);
        DoubleArrayCache.returnArray(t27648a);
        
        return out;
    }
    
    @SuppressWarnings("UnnecessaryLocalVariable")
    private static double leftOfPlaneExact(double xa, double ya, double za, double xb, double yb, double zb, double xc,
                                           double yc, double zc, double xd, double yd, double zd) {
        DoublePair t = new DoublePair(0.0, 0.0);
        twoDiff(xa, xd, t);
        double adx = t.mFirst;
        double adxtail = t.mSecond;
        twoDiff(ya, yd, t);
        double ady = t.mFirst;
        double adytail = t.mSecond;
        twoDiff(za, zd, t);
        double adz = t.mFirst;
        double adztail = t.mSecond;
        twoDiff(xb, xd, t);
        double bdx = t.mFirst;
        double bdxtail = t.mSecond;
        twoDiff(yb, yd, t);
        double bdy = t.mFirst;
        double bdytail = t.mSecond;
        twoDiff(zb, zd, t);
        double bdz = t.mFirst;
        double bdztail = t.mSecond;
        twoDiff(xc, xd, t);
        double cdx = t.mFirst;
        double cdxtail = t.mSecond;
        twoDiff(yc, yd, t);
        double cdy = t.mFirst;
        double cdytail = t.mSecond;
        twoDiff(zc, zd, t);
        double cdz = t.mFirst;
        double cdztail = t.mSecond;
        
        final double[]
              t8a     = DoubleArrayCache.getArray(8  )
            , t8b     = DoubleArrayCache.getArray(8  )
            , t8c     = DoubleArrayCache.getArray(8  )
            , t8d     = DoubleArrayCache.getArray(8  )
            , t8e     = DoubleArrayCache.getArray(8  )
            , t8f     = DoubleArrayCache.getArray(8  )
            , t16a    = DoubleArrayCache.getArray(16 )
            , t32a    = DoubleArrayCache.getArray(32 )
            , t32b    = DoubleArrayCache.getArray(32 )
            , t64a    = DoubleArrayCache.getArray(64 )
            , t64b    = DoubleArrayCache.getArray(64 )
            , t64c    = DoubleArrayCache.getArray(64 )
            , t128a   = DoubleArrayCache.getArray(128)
            , t192a   = DoubleArrayCache.getArray(192)
            ;
        
        
        double[] axby = t8a;
        twoTwoProduct(adx, adxtail, bdy, bdytail, axby);
        double negate = -ady;
        double negatetail = -adytail;
        double[] bxay = t8b;
        twoTwoProduct(bdx, bdxtail, negate, negatetail, bxay);
        
        double[] bxcy = t8c;
        twoTwoProduct(bdx, bdxtail, cdy, cdytail, bxcy);
        negate = -bdy;
        negatetail = -bdytail;
        double[] cxby = t8d;
        twoTwoProduct(cdx, cdxtail, negate, negatetail, cxby);
        
        double[] cxay = t8e;
        twoTwoProduct(cdx, cdxtail, ady, adytail, cxay);
        negate = -cdy;
        negatetail = -cdytail;
        double[] axcy = t8f;
        twoTwoProduct(adx, adxtail, negate, negatetail, axcy);
        
        int t16alen, t32alen, t32blen;
        t16alen = expansionSumZeroElimFast(8, bxcy, 8, cxby, t16a);
        t32alen = scaleExpansionZeroElim(t16alen, t16a, adz, t32a);
        t32blen = scaleExpansionZeroElim(t16alen, t16a, adztail, t32b);
        int t64alen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64a);
        
        t16alen = expansionSumZeroElimFast(8, cxay, 8, axcy, t16a);
        t32alen = scaleExpansionZeroElim(t16alen, t16a, bdz, t32a);
        t32blen = scaleExpansionZeroElim(t16alen, t16a, bdztail, t32b);
        int t64blen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64b);
        
        t16alen = expansionSumZeroElimFast(8, axby, 8, bxay, t16a);
        t32alen = scaleExpansionZeroElim(t16alen, t16a, cdz, t32a);
        t32blen = scaleExpansionZeroElim(t16alen, t16a, cdztail, t32b);
        int t64clen = expansionSumZeroElimFast(t32alen, t32a, t32blen, t32b, t64c);
        
        int t128alen = expansionSumZeroElimFast(t64alen, t64a, t64blen, t64b, t128a);
        int t192alen = expansionSumZeroElimFast(t128alen, t128a, t64clen, t64c, t192a);
        
        double out = t192a[t192alen - 1];
        
        // 计算完完成归还临时数组
        DoubleArrayCache.returnArray(t8a  );
        DoubleArrayCache.returnArray(t8b  );
        DoubleArrayCache.returnArray(t8c  );
        DoubleArrayCache.returnArray(t8d  );
        DoubleArrayCache.returnArray(t8e  );
        DoubleArrayCache.returnArray(t8f  );
        DoubleArrayCache.returnArray(t16a );
        DoubleArrayCache.returnArray(t32a );
        DoubleArrayCache.returnArray(t32b );
        DoubleArrayCache.returnArray(t64a );
        DoubleArrayCache.returnArray(t64b );
        DoubleArrayCache.returnArray(t64c );
        DoubleArrayCache.returnArray(t128a);
        DoubleArrayCache.returnArray(t192a);
        
        return out;
    }
    
    private static int scaleExpansionZeroElim(int elen, double[] e, double b, double[] h) {
        DoublePair t = new DoublePair(0.0, 0.0);
        split(b, t);
        double bhi = t.mFirst;
        double blo = t.mSecond;
        twoProduct1Presplit(e[0], b, bhi, blo, t);
        double q = t.mFirst;
        double hh = t.mSecond;
        int hindex = 0;
        if (hh != 0) {
            h[hindex++] = hh;
        }
        for (int eindex = 1; eindex < elen; ++eindex) {
            double enow = e[eindex];
            twoProduct1Presplit(enow, b, bhi, blo, t);
            double product1 = t.mFirst;
            double product0 = t.mSecond;
            twoSum(q, product0, t);
            double sum = t.mFirst;
            hh = t.mSecond;
            if (hh != 0) {
                h[hindex++] = hh;
            }
            twoSumFast(product1, sum, t);
            q = t.mFirst;
            hh = t.mSecond;
            if (hh != 0) {
                h[hindex++] = hh;
            }
        }
        if (q != 0.0 || hindex == 0) {
            h[hindex++] = q;
        }
        return hindex;
    }
    
    private static int expansionSumZeroElimFast(int elen, double[] e, int flen, double[] f, double[] h) {
        double q, qnew, hh;
        DoublePair t = new DoublePair(0.0, 0.0);
        double enow = e[0];
        double fnow = f[0];
        int eindex = 0;
        int findex = 0;
        if (fnow > enow == fnow > -enow) {
            q = enow;
            ++eindex;
            if (eindex < elen) {
                enow = e[eindex];
            }
        } else {
            q = fnow;
            ++findex;
            if (findex < flen) {
                fnow = f[findex];
            }
        }
        int hindex = 0;
        if (eindex < elen && findex < flen) {
            if (fnow > enow == fnow > -enow) {
                twoSumFast(enow, q, t);
                qnew = t.mFirst;
                hh = t.mSecond;
                ++eindex;
                if (eindex < elen) {
                    enow = e[eindex];
                }
            } else {
                twoSumFast(fnow, q, t);
                qnew = t.mFirst;
                hh = t.mSecond;
                ++findex;
                if (findex < flen) {
                    fnow = f[findex];
                }
            }
            q = qnew;
            if (hh != 0.0) {
                h[hindex++] = hh;
            }
            while (eindex < elen && findex < flen) {
                if (fnow > enow == fnow > -enow) {
                    twoSum(q, enow, t);
                    qnew = t.mFirst;
                    hh = t.mSecond;
                    ++eindex;
                    if (eindex < elen) {
                        enow = e[eindex];
                    }
                } else {
                    twoSum(q, fnow, t);
                    qnew = t.mFirst;
                    hh = t.mSecond;
                    ++findex;
                    if (findex < flen) {
                        fnow = f[findex];
                    }
                }
                q = qnew;
                if (hh != 0.0) {
                    h[hindex++] = hh;
                }
            }
        }
        while (eindex < elen) {
            twoSum(q, enow, t);
            qnew = t.mFirst;
            hh = t.mSecond;
            ++eindex;
            if (eindex < elen) {
                enow = e[eindex];
            }
            q = qnew;
            if (hh != 0.0) {
                h[hindex++] = hh;
            }
        }
        while (findex < flen) {
            twoSum(q, fnow, t);
            qnew = t.mFirst;
            hh = t.mSecond;
            ++findex;
            if (findex < flen) {
                fnow = f[findex];
            }
            q = qnew;
            if (hh != 0.0) {
                h[hindex++] = hh;
            }
        }
        if (q != 0.0 || hindex == 0) {
            h[hindex++] = q;
        }
        return hindex;
    }
    
    private static void twoTwoProduct(double a1, double a0, double b1, double b0, double[] x) {
        double u0, u1, u2, ui, uj, uk, ul, um, un;
        DoublePair t = new DoublePair(0.0, 0.0);
        split(a0, t);
        double a0hi = t.mFirst;
        double a0lo = t.mSecond;
        split(b0, t);
        double b0hi = t.mFirst;
        double b0lo = t.mSecond;
        twoProduct2Presplit(a0, a0hi, a0lo, b0, b0hi, b0lo, t);
        ui = t.mFirst;
        x[0] = t.mSecond;
        split(a1, t);
        double a1hi = t.mFirst;
        double a1lo = t.mSecond;
        twoProduct2Presplit(a1, a1hi, a1lo, b0, b0hi, b0lo, t);
        uj = t.mFirst;
        u0 = t.mSecond;
        twoSum(ui, u0, t);
        uk = t.mFirst;
        u1 = t.mSecond;
        twoSumFast(uj, uk, t);
        ul = t.mFirst;
        u2 = t.mSecond;
        split(b1, t);
        double b1hi = t.mFirst;
        double b1lo = t.mSecond;
        twoProduct2Presplit(a0, a0hi, a0lo, b1, b1hi, b1lo, t);
        ui = t.mFirst;
        u0 = t.mSecond;
        twoSum(u1, u0, t);
        uk = t.mFirst;
        x[1] = t.mSecond;
        twoSum(u2, uk, t);
        uj = t.mFirst;
        u1 = t.mSecond;
        twoSum(ul, uj, t);
        um = t.mFirst;
        u2 = t.mSecond;
        twoProduct2Presplit(a1, a1hi, a1lo, b1, b1hi, b1lo, t);
        uj = t.mFirst;
        u0 = t.mSecond;
        twoSum(ui, u0, t);
        un = t.mFirst;
        u0 = t.mSecond;
        twoSum(u1, u0, t);
        ui = t.mFirst;
        x[2] = t.mSecond;
        twoSum(u2, ui, t);
        uk = t.mFirst;
        u1 = t.mSecond;
        twoSum(um, uk, t);
        ul = t.mFirst;
        u2 = t.mSecond;
        twoSum(uj, un, t);
        uk = t.mFirst;
        u0 = t.mSecond;
        twoSum(u1, u0, t);
        uj = t.mFirst;
        x[3] = t.mSecond;
        twoSum(u2, uj, t);
        ui = t.mFirst;
        u1 = t.mSecond;
        twoSum(ul, ui, t);
        um = t.mFirst;
        u2 = t.mSecond;
        twoSum(u1, uk, t);
        ui = t.mFirst;
        x[4] = t.mSecond;
        twoSum(u2, ui, t);
        uk = t.mFirst;
        x[5] = t.mSecond;
        twoSum(um, uk, t);
        x[7] = t.mFirst;
        x[6] = t.mSecond;
    }
    
    private static void split(double a, DoublePair t) {
        double c = SPLITTER * a;
        double abig = c - a;
        t.mFirst = c - abig;
        t.mSecond = a - t.mFirst;
    }
    private static void twoDiff(double a, double b, DoublePair t) {
        double x = a - b;
        double bvirt = a - x;
        double avirt = x + bvirt;
        double bround = bvirt - b;
        double around = a - avirt;
        t.mFirst = x;
        t.mSecond = around + bround;
    }
    private static void twoSum(double a, double b, DoublePair t) {
        double x = a + b;
        double bvirt = x - a;
        double avirt = x - bvirt;
        double bround = b - bvirt;
        double around = a - avirt;
        t.mFirst = x;
        t.mSecond = around + bround;
    }
    private static void twoSumFast(double a, double b, DoublePair t) {
        double x = a + b;
        double bvirt = x - a;
        t.mFirst = x;
        t.mSecond = b - bvirt;
    }
    private static void twoProduct1Presplit(double a, double b, double bhi, double blo, DoublePair t) {
        split(a, t);
        double ahi = t.mFirst;
        double alo = t.mSecond;
        t.mFirst = a * b;
        double err1 = t.mFirst - ahi * bhi;
        double err2 = err1 - alo * bhi;
        double err3 = err2 - ahi * blo;
        t.mSecond = alo * blo - err3;
    }
    private static void twoProduct2Presplit(double a, double ahi, double alo, double b, double bhi, double blo, DoublePair t) {
        t.mFirst = a * b;
        double err1 = t.mFirst - ahi * bhi;
        double err2 = err1 - alo * bhi;
        double err3 = err2 - ahi * blo;
        t.mSecond = alo * blo - err3;
    }
}
