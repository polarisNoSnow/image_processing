package com.polaris.image.core;

import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.RealSense.frame;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;

import com.polaris.image.util.GeneralContants;
import com.polaris.image.util.ImageUtil;

import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import com.polaris.image.util.GeneralContants;

/**
 * 将源视频推送到指定位置
 *
 * @author 北辰不落雪
 * @date 2019年2月12日 上午9:48:55
 * @Description
 */
public class ImageRecognition {
    public static void main(String[] args) throws Exception, InterruptedException {
        try {
        	//rtmp://58.200.131.2:1935/livetv/hunantv
            //recordCamera(GeneralContants.DESTOP_PATH + "video.mp4", 30);
        	openCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //openCamera();
    }

    /**
     * 打开摄像头并展示图像
     *
     * @throws Exception
     * @throws InterruptedException
     */
    public static void openCamera() throws Exception, InterruptedException {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();   //开始获取摄像头数据
        grabber.setAudioChannels(1);
        grabber.setFrameRate(1);
        CanvasFrame canvas = new CanvasFrame("人脸检测");//新建一个窗口
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Frame frame;
        while ((frame = grabber.grab()) != null) {
            if (!canvas.isEnabled()) {//窗口是否关闭
                grabber.stop();//停止抓取
                System.exit(-1);//退出
            }
            canvas.showImage(frame);//获取摄像头图像并放到窗口上显示， 这里的Frame frame=grabber.grab(); frame是一帧视频图像
            //Thread.sleep(10);//100毫秒刷新一次图像
        }
    }

    /**
     * 按帧录制本机摄像头视频（边预览边录制，停止预览即停止录制）
     *
     * @param outputFile -视频录制的文件保存路径，也可以是rtsp或者rtmp等流媒体服务器发布地址
     * @param frameRate  - 视频帧率
     * @throws Exception
     * @throws InterruptedException
     * @throws org.bytedeco.javacv.FrameRecorder.Exception
     * @author eguid
     */
    public static void recordCamera(String outputFile, double frameRate)
            throws Exception, InterruptedException, org.bytedeco.javacv.FrameRecorder.Exception {
        Loader.load(opencv_objdetect.class);
        FrameGrabber grabber = null;
        boolean flag = true;
        while (flag) {
            try {
                grabber = FrameGrabber.createDefault(0);//本机摄像头默认0，这里使用javacv的抓取器，至于使用的是ffmpeg还是opencv，请自行查看源码
                grabber.start();//开启抓取器
                flag = false;
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("无法获取本机摄像头");
            }
        }

        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();//转换器
        IplImage grabbedImage = converter.convert(grabber.grab());
        int width = grabbedImage.width();
        int height = grabbedImage.height();

        FrameRecorder recorder = FrameRecorder.createDefault(outputFile, width, height);
        recorder.setAudioChannels(1);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // avcodec.AV_CODEC_ID_H264，编码
        recorder.setFormat("flv");//封装格式，如果是推送到rtmp就必须是flv封装格式
        recorder.setFrameRate(frameRate);

        recorder.start();//开启录制器
        long startTime = 0;
        long videoTS = 0;
        CanvasFrame frame = new CanvasFrame("视频录制", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);

        Frame rotatedFrame = converter.convert(grabbedImage);
        while (frame.isVisible() && (grabbedImage = converter.convert(grabber.grab())) != null) {
          
            /**
             * 符号化
             */
           /* BufferedImage bufferedImage = Java2DFrameUtils.toBufferedImage(grabbedImage);
            if (bufferedImage != null) {
                BufferedImage newImage = ImageUtil.symbolization(bufferedImage);
                grabbedImage = Java2DFrameUtils.toIplImage(newImage);
            }*/

            rotatedFrame = converter.convert(grabbedImage);
            frame.showImage(rotatedFrame);
            
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
            videoTS = 1000 * (System.currentTimeMillis() - startTime);
            recorder.setTimestamp(videoTS);
            recorder.record(rotatedFrame);
            Thread.sleep(40);
        }
        frame.dispose();
        recorder.stop();
        recorder.release();
        grabber.stop();

    }

}
