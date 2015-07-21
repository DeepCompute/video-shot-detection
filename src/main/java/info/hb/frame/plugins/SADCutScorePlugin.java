package info.hb.frame.plugins;

import info.hb.frame.parser.VideoFrame;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * SAD（Sum of Absolute Differences）削减得分插件
 *
 * 基于绝对差值和方法，针对每一帧，对比计算前一帧，生成一个削减得分，并将结果写入CSV文件中。
 */
public class SADCutScorePlugin implements VideoAnalysisPlugin {

	private VideoFrame last;
	private PrintWriter csvFile;

	public SADCutScorePlugin(File csvFile) throws IOException {
		this(new PrintWriter(new FileWriter(csvFile)));
	}

	public SADCutScorePlugin(final PrintWriter writer) {
		this.csvFile = writer;
	}

	@Override
	public void start() {
		csvFile.println("frame,score,processingTime");
	}

	@Override
	public void frame(final int frame, final VideoFrame image) {
		final long start = System.currentTimeMillis();
		long score = score(last, image);
		final long timeTaken = System.currentTimeMillis() - start;

		csvFile.println(frame + "," + score + "," + timeTaken);

		// 将当前帧作为上一帧存储
		last = image;
	}

	@Override
	public void end() {
		csvFile.close();
	}

	/**
	 * 计算两个帧的差异得分
	 *
	 * @param a  一个帧
	 * @param b  另一个帧（两个帧的大小相同）
	 */
	private final long score(final VideoFrame a, final VideoFrame b) {
		// 如果其中一个帧为null，则得分为最大Long值
		if (a == null || b == null)
			return Long.MAX_VALUE;

		long score = 0;

		// 用于存储x,y的RGB像素值
		final short[] aPixel = new short[3];
		final short[] bPixel = new short[3];

		for (int x = 0; x < a.getHeight(); x++) {
			for (int y = 0; y < a.getWidth(); y++) {
				a.getRGB(aPixel, x, y);
				b.getRGB(bPixel, x, y);
				// 计算同一位置的得分
				final int delta = scoreNorm1(aPixel, bPixel);

				score += delta;
			}
		}
		return score;
	}

	/**
	 * 一阶范式
	 */
	public static final int scoreNorm1(final short[] a, final short[] b) {
		return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
	}

	/**
	 * Cos余弦
	 */
	public static final double scoreCos(final short[] a, final short[] b) {
		return CosCore.cos(a, b, a.length);
	}

	/**
	 * 两个像素（经过编码的值，也就是将一个像素点的RGB值编码为1个值）之间的差异得分
	 *
	 * @param a 一个像素值（使用0xAARRGGBB编码）
	 * @param b 一个像素值（使用0xAARRGGBB编码）
	 */
	public static final int score(final int a, final int b) {
		// 直接比较
		if (a == b)
			return 0;

		// 从A像素点中提取RGB通道值
		final int aRed = (a >> 16) & 0xFF;
		final int aGreen = (a >> 8) & 0xFF;
		final int aBlue = (a) & 0xFF;

		// 从B像素点中提取RGB通道值
		final int bRed = (b >> 16) & 0xFF;
		final int bGreen = (b >> 8) & 0xFF;
		final int bBlue = (b) & 0xFF;

		// 计算一阶范式
		final int dRed = Math.abs(aRed - bRed);
		final int dGreen = Math.abs(aGreen - bGreen);
		final int dBlue = Math.abs(aBlue - bBlue);

		return dRed + dGreen + dBlue;
	}

}
