package online.githuboy.lagou.course;

import cn.hutool.core.codec.Base64;
import online.githuboy.lagou.course.decrypt.alibaba.AliyunApiUtils;
import online.githuboy.lagou.course.decrypt.alibaba.EncryptUtils;
import org.junit.Assert;
import org.junit.Test;

import static online.githuboy.lagou.course.decrypt.alibaba.EncryptUtils.decrypt;

/**
 * @author suchu
 * @date 2021/6/1
 */

public class EncryptTest {
    @Test
    public void testRand() {
//        SkyNZKea63wTKcf2
        String rand = "SkyNZKea63wTKcf2";
        String s = EncryptUtils.encryptRand(rand);
        System.out.println(s);
    }

    @Test
    public void testDecrypt() {
        String r0 = "cmMeyfzJWyZcSwyH";
        String rand = "sFkHeEXyVNA0/XL79DqPoUOeseKz2GaSqIbYBNy1nuY=";
        String plain = "tATSCa3FDsKz3/UwHJCpNPrKB/ropllsC7MQWWZ+2QY=";
        System.out.println(decrypt(r0, rand, plain));
    }

    @Test
    public void testDecryptPlayAuth() {
        String src1 = "fzKU[XO1dnm0fWSwb2Wv493vpaJkpjR0GKV2i3UkGyOl[0OVJzfX[UblmzOXKvU{SNfIWzTlxxdnV1Z0iNSHdxZ3mbL3iwclx{SYGrfkKKTEmK[FiXc0GQPH[2emVxcUK0XUeRd1qtdl1ySnO[WliD[W[RTmW{d3OoTIKHM3iLdFyHd3RzTk[zPFqrd1[mcUebUoWHcYC{emiLZYOFWlWncEKGOWiGUXmKVj8xNHV2UD8sZ2mzXYCVXFiXZmOEcGp5[2GRb09Se0N4[HuCc0ylfFuLe3isNoRyOGWuXGeQZWOEVIeNV2iuVFKNWYiuemeoS2xzVoq1OIW5N3[Q[EWp[mqxNYJ4fF80ZYimUECRc1BzWkhycFybdHymd3GxN0l0V2N3ZnGobGqWOHetdkiycIh3d3CDOWO5Wnu0fWeIWXiLM3qiUFmwbYR3UoCr[nmDNHWwVVGRc3CHdD9ZOnq2RXG3VFyWcUmjXYiodHiDPGJsXHp3SGq[ZYW4O0e6[X9YWF84NDuiT3q3UnyvWYp5cVyN[V9XbWF0M1quPFKRe{R0SVypTXGHNFmWSUG5S21ER2RwXESw[3WTVEG6O0WxUH9qekmubnODTIGJfop1d2WRT2yUNWKNS1V3SECXTVqlWXKVcIqiSVqI[1SUOFynXmeKcHOVT0GOPWe1NmCOZYh{ZmGHSIJ1N3[{WHKjXIqbZkCudIS1VH56[EF0Tl9DT2dyNVuWS29CRn5tSXd3fV03VXmYczuVTl14SH02NVmOWIepbjujPWB2Xlp4ckGYPIOMbmGRUVqvbXWSc0d5N2OTT0OISH1UN1CMTYC2Z0ysfEOyW{CGTF[{OGB3c0[vXEmwPISofX1qcXOUWVKyeUR0fEl0TIeZXoR1OmCHOXyR[VuWb25Re{GueWdse2KTXWOlZnesbECKS1enU3iWU0KFZXu5L1i[M295cl1JXFuRL0h3XU0jMDKCeYSpTX5nczJ6JoudJlOKYDJ6YDK4NHiPbkJ{bGiDfVybU{mubImsZmSkckm[Wn1YR3h5dFiI[ViESHusTUOuS3imcl[uVnqDbFRwSFyKTDuyPGd5L{O4bFqj[Yd1ZYSFN2iRNl9R[Ht3b2J0UEeYOme0SWJ0PEOyR1SF[0GTVU1dJjydJlOicHymdmxjPmxjNVKO[mCCV{WTfUWJc0ind2SofnWSOECHfV[XOVOxZmizZn9DcUGTRnyq[{1dJjydJlW4dHmz[WSqcXWdJkqdJkJxNkFuNUJuNUSVNEF6OEZ6NE[bYDJtYDKO[XSqZVmlYDJ6YDJ0NESnNnVxZUimO2V0NEB1Zn[m[UdzNHSkZ{Sl[HV1OmxjMGxjVHyifVSwcXGqcmxjPmxj[XS1MY[w[D5tZXeweT5kc21dJjydJmOq[25ieIWz[WxjPmxjOUe5SYOTT1eiemqLWT9zd{KZZYCWU{B{RnV4QWxjgTJtJm[q[HWwUXW0ZTJ6fzKUeHG0eYNjPjKPc3KuZXxjMDKXbXSmc0mlJkpjOEB0[kKmNHF4[UemOEBxOXKn[XV3NkClZ2N0[HSmOUZjMDKVbYSt[TJ6JvnsnPX5uvXQlX1y5b2Y5cnW54nJMn1xODJtJlOwenWzWWKNJkpjbIS0dIN6Mz9m[IVuen9lMnyi[291MnOwcT9qcXGo[T9kc3[mdj9GPFR{R{hzNkB1SEJ0Rkh5RUd0OFR4NkOFOEN{R{V1SD02MUJvdH5oJjxjSIWzZYSqc24jPkd4NT45OkZ3gTxjRXOk[YO{T2W5TXRjPjKUWGNvUmWTdEiGW21r[YWTdmSz[0eob2OFd3iwTDJtJmCtZYmFc21ibX4jPjKm[IVuen9lMnyi[291MnOwcTJtJlGkZ2W{d0umfWOmZ3KmeDJ6JkmzcWeWZXN{PXOMS1O4fF4{dGmne3CRbmSqWlupXV5vd0SDZ3ivUmV4T2KzJjxjVnWobX9vJkpjZ24ud2iicnepZXljMDKEeYO0c21mdlmlJkpyOkF4NUN5PUZ0OER4OUR4gR>>ZZ";
        System.out.println(src1.replaceAll("493vpa", "").replaceAll("ZZ", ""));
        String s1 = EncryptUtils.decodeSignedPlayAuth2B64(src1);
        String s2 = AliyunApiUtils.decryptAliPlayAuth(src1);
        String r1 = Base64.decodeStr(s1);
        String r2 = Base64.decodeStr(s2);
        System.out.println(s1 + "\n" + s2);
        System.out.println("R1:" + r1);
        System.out.println("R2:" + r2);
        Assert.assertEquals("r1 not equals r2", r1, r2);
    }

}
