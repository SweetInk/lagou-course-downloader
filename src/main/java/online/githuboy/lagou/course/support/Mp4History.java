package online.githuboy.lagou.course.support;

import online.githuboy.lagou.course.utils.ReadTxt;

import java.util.HashSet;
import java.util.Set;

/**
 * mp4视频下载历史信息记录到文件中
 *
 * @author eric
 */
public class Mp4History {

    private static volatile Set<String> historySet = new HashSet<>();

    /**
     * 记录已经下载过的视频id，不要重复下载了。
     */
    static String filePath = "mp4.txt";

    static {
        loadHistory();
    }

    /**
     * 下载完成之后追加到历史文件
     *
     * @param mp4Id
     */
    public static void append(String mp4Id) {
        historySet.add(mp4Id);
        new ReadTxt().writeFile(filePath, mp4Id);
    }

    public static Set<String> loadHistory() {
        Set<String> set = new ReadTxt().readFile(filePath);
        historySet.addAll(set);
        return historySet;
    }

    public static boolean contains(String id) {
        return historySet.contains(id);
    }

}
