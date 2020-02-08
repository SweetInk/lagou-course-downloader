package online.githuboy.lagou.course.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PublicUtils {
    public static void dumpInputStream(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line = br.readLine();
        while (null != line) {
            System.out.println(line);
            line = br.readLine();
        }
    }

}
