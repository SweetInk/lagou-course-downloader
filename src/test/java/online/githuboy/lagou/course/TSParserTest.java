package online.githuboy.lagou.course;

import lombok.SneakyThrows;
import online.githuboy.lagou.course.decrypt.alibaba.TSParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;

/**
 * @author suchu
 * @date 2021/6/1
 */
public class TSParserTest {
    @SneakyThrows
    public void testParseFromInputStream() {
        TSParser parser = new TSParser();
        File inputFile = new File("C:\\Users\\edian\\ltest\\101750a3840e050dda58e2e321c7e0dd-hd-encrypt-stream-00001.ts");
        RandomAccessFile file = new RandomAccessFile(inputFile, "rw");
        FileInputStream file2 = new FileInputStream(inputFile);
        TSParser.TSStream stream = parser.fromInputStream(file2);
        stream.PIDList();
        byte[] keys = new byte[]{(byte) 150, 88, (byte) 228, 50, (byte) 140, 73, 66, 113, 56, 73, 2, 43, 10, 53, (byte) 189, (byte) 182};
        parser.decrypt(stream, keys);
        stream.dumpToFile("D:\\lagou\\test.ts");
    }
}
