package com.sjxm.springbootinit.utils;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class VideoPostGenerateUtil {

    /**
     * 从OSS视频URL提取首帧并上传到OSS
     * @param videoUrl 视频URL
     * @param aliOssUtil OSS工具类实例
     * @return OSS中的访问URL
     */
    public static String extractAndUploadThumbnail(String videoUrl, AliOssUtil aliOssUtil) {
        FFmpegFrameGrabber grabber = null;
        Java2DFrameConverter converter = null;
        ByteArrayOutputStream outputStream = null;

        try {
            // 初始化视频帧获取器，使用HTTP流
            grabber = new FFmpegFrameGrabber(videoUrl);
            // 设置格式为"mp4"，这对于HTTP流是必需的
            grabber.setFormat("mp4");
            // 可选：设置用户代理，某些服务器可能需要
            grabber.setOption("user_agent", "Mozilla/5.0");
            // 可选：设置超时，单位微秒
            grabber.setOption("timeout", "30000000"); // 30秒
            grabber.start();

            // 初始化转换器
            converter = new Java2DFrameConverter();

            // 获取第一帧
            Frame frame = grabber.grabImage();
            if (frame == null) {
                throw new RuntimeException("无法获取视频帧");
            }

            // 将Frame转换为BufferedImage
            BufferedImage bufferedImage = converter.convert(frame);
            if (bufferedImage == null) {
                throw new RuntimeException("无法转换视频帧");
            }

            // 将BufferedImage转换为字节数组
            outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            // 生成OSS中的对象名称
            String objectName = "video/cover/" + UUID.randomUUID().toString() + ".jpg";

            // 上传到OSS并返回访问URL
            return aliOssUtil.upload(imageBytes, objectName);

        } catch (Exception e) {
            System.err.println("处理视频封面时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("处理视频封面失败", e);
        } finally {
            // 释放资源
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
            } catch (Exception e) {
                System.err.println("关闭资源时发生错误: " + e.getMessage());
            }
        }
    }
}