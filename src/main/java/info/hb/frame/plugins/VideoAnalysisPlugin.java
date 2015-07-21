package info.hb.frame.plugins;

import info.hb.frame.parser.VideoFrame;

/**
 * 视频分析插件接口
 *
 * @author wanggang
 *
 */
public interface VideoAnalysisPlugin {

	public void start();

	/**
	 * 以某种方式处理一个视频帧
	 *
	 * @param frame 帧数（从0开始）
	 * @param image 帧图像
	 */
	public void frame(int frame, VideoFrame image);

	public void end();

}
