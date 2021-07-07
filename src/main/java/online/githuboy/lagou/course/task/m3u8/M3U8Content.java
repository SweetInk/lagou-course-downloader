package online.githuboy.lagou.course.task.m3u8;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * M3U8文件内容
 *
 * @author suchu
 * @date 2021/6/23
 */
@Getter
public class M3U8Content {
    private final static Pattern pattern = Pattern.compile("(URI=\")(.*)(\")");
    private final String raw;
    private final Map<String, String> attrs;
    private final List<HSInfo> tsList;
    private final String localKeyName = "video.key";
    private String baseUrl = "";
    private int tsIndex = 1;
    private String keyUrl;
    @Setter
    private byte[] key;

    public M3U8Content(String baseUrl, String raw) {
        this.raw = raw;
        this.baseUrl = baseUrl;
        tsList = new ArrayList<>();
        attrs = new LinkedHashMap<>();
        try {
            parse();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重新构建m3u8文件内容
     *
     * @return
     */
    public String rebuildM3U8Raw() {
        StringBuilder stringBuilder = new StringBuilder(256);
        if (!attrs.containsKey("#EXTM3U")) {
            stringBuilder.append("#EXTM3U").append("\n");
        }
        attrs.entrySet().stream().filter(entry -> !entry.getKey().equals("#EXT-X-ENDLIST")).forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("#EXT-X-KEY".equals(key)) {
                value = value.replaceAll("(URI=\")(.*)(\")", "$1" + localKeyName + "$3");
            }
            stringBuilder.append(entry.getKey()).append(":").append(value).append("\n");
        });
        tsList.forEach(tsInfo -> {
            stringBuilder.append(tsInfo.getExtInfo()).append("\n");
            stringBuilder.append(tsInfo.getIndexName()).append("\n");
        });
        stringBuilder.append("#EXT-X-ENDLIST");
        return stringBuilder.toString();
    }

    /**
     * 解析m3u8文件内容
     *
     * @throws MalformedURLException
     */
    private void parse() throws MalformedURLException {
        StringTokenizer tokenizer = new StringTokenizer(raw, "\n");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith("#EXT-X-")) {
                int tokenIndex = token.indexOf(":");
                if (tokenIndex != -1) {
                    String key = token.substring(0, tokenIndex);
                    String value = token.substring(tokenIndex + 1);
                    attrs.put(key, value);
                }
            } else if (token.startsWith("#EXTINF")) {
                String extInfo = token;
                if (tokenizer.hasMoreTokens()) {
                    token = token.replaceAll(",", "");
                    String[] split = token.split(":");
                    float duration = 0f;
                    if (split.length > 0) {
                        duration = Float.parseFloat(split[1]);
                    }
                    String path = tokenizer.nextToken();
                    if (!path.startsWith("http")) {
                        if (path.startsWith("/"))
                            path = baseUrl + path;
                        else
                            path = baseUrl + "/" + path;
                    }
                    String fileName = parseFileName(path);
                    HSInfo hsInfo = new HSInfo();
                    hsInfo.setIndexName((tsIndex++) + ".ts");
                    hsInfo.setExtInfo(extInfo);
                    hsInfo.setUrl(path);
                    hsInfo.setDuration(duration);
                    hsInfo.setFileName(fileName);
                    tsList.add(hsInfo);
                }
            }
        }
        tryParseKeyUrl();
    }

    /**
     * 尝试解析keyUrl
     */
    private void tryParseKeyUrl() {
        String value = this.attrs.get("#EXT-X-KEY");
        if (StrUtil.isNotBlank(value)) {
            Matcher matcher = pattern.matcher(value);
            while (matcher.find()) {
                String group = matcher.group(2);
                if (null != group) {
                    keyUrl = group;
                    break;
                }
            }
        }
    }

    private String parseFileName(String url) throws MalformedURLException {
        URL url1 = new URL(url);
        String path = url1.getPath();
        String[] split = path.split("/");
        return split[split.length - 1];
    }

    @Getter
    @Setter
    public static class HSInfo {
        private String indexName;
        private String extInfo;
        private String url;
        private String fileName;
        private float duration;
    }
}
