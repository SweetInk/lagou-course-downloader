# lagou-course-downloader
拉勾网课程视频下载工具

本程序仅供学习交流使用

最后更新时间: 2020年8月7日

# 更新日志

1. 支持最新的拉钩教育视频下载.


# 前置要求

1. **已购买**拉钩上的[视频课程](https://kaiwu.lagou.com/)

2. **成功登陆拉钩网**

# 其他

Lagou课程的视频现托管在阿里云，[相关文档](https://help.aliyun.com/product/29932.html?spm=a2c4g.11186623.3.1.3a082168qYWI6d)

视频元数据API接口文档:`https://help.aliyun.com/document_detail/56124.html?spm=a2c4g.11186623.2.30.14487fbfjBfxAC`

视频片段使用`AES-CBC-128`加密/解密，通过分析js获取，视频的密钥在视频的m3u8文件中有地址。[相关文档](https://cloud.tencent.com/document/product/266/9638)

~~视频片段通过`ffmpeg`合并~~
现在直接获取视频的mp4地址，跳过了合成（当然也可以）

视频课程信息~~在视频首页html中的`<script>`标签里。~~ 现在通过`https://gate.lagou.com/v1/neirong/kaiwu/getCourseLessons?courseId={0}` 获取

程序默认下载`FHD`全高清视频源

# 如何使用

1. 成功登陆拉钩网后

2. 浏览器打开调试工具

3. 打开课程首页

![](http://ww1.sinaimg.cn/large/005ViNx8ly1g5mdhltkh8j31yy0mf11q.jpg)

把上图中Cookie值，复制粘贴到`CookieStore`中`cookie` 字段中.

```java
String courseId = "视频课程首页url";
String savePath = "视频保存位置";
Downloader downloader = new Downloader(courseId, savePath);
downloader.start();
```
