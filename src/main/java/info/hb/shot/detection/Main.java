package info.hb.shot.detection;

import info.hb.ffmpeg.engine.FFmpegEngine;
import info.hb.frame.plugins.SADCutScorePlugin;

import java.io.File;

public class Main {

	public static void main(String[] args) throws Exception {
		final FFmpegEngine ffmpeg = new FFmpegEngine(new File("/usr/bin/ffmpeg"));

		// 设置处理时常，默认是全部处理
		ffmpeg.setLimitSeconds(60);

		// Write the analysis to sad.csv
		SADCutScorePlugin plugin = new SADCutScorePlugin(new File("sad.csv"));

		final long start = System.currentTimeMillis();
		ffmpeg.analyse(new File("/home/wanggang/develop/deeplearning/test-videos/test6.mp4"), plugin);
		final long timeTaken = System.currentTimeMillis() - start;

		System.out.println("Processing took: " + timeTaken + " ms");
	}

}
