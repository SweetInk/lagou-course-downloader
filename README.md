# lagou-course-downloader
拉勾网课程视频下载工具

本程序仅供学习交流使用

# 前置要求

1. **已购买**拉钩上的[视频课程](https://kaiwu.lagou.com/)

2. **成功登陆拉钩网**

# 其他

课程的视频托管在腾讯云，[相关文档](https://cloud.tencent.com/document/product/266/14424)

视频元数据API接口:`https://playvideo.qcloud.com/getplayinfo/v2/{appId}/{fileId}`

视频数据使用`AES-CBC-128`加密/解密，通过分析js获取，视频的密钥在视频的m3u8文件中有地址。[相关文档](https://cloud.tencent.com/document/product/266/9638)

视频片段通过`ffmpeg`合并

视频课程信息在视频首页html中的`<script>`标签里。

程序默认下载`FHD`全高清视频源

# 如何使用

1. 成功登陆拉钩网后

2. 浏览器打开调试工具

3. 打开课程首页

![](http://ww1.sinaimg.cn/large/005ViNx8ly1g5mdhltkh8j31yy0mf11q.jpg)

把上图中Cookie值，复制粘贴到`CookieStore`中`cookie` 字段中.

```java
String courseUrl = "视频课程首页url";
String savePath = "视频保存位置";
Downloader downloader = new Downloader(courseUrl, savePath);
downloader.start();
```
