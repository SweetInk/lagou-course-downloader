package online.githuboy.lagou.course;

import online.githuboy.lagou.course.decrypt.alibaba.EncryptUtils;
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
}
