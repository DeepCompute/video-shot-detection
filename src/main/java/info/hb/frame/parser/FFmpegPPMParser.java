package info.hb.frame.parser;

import java.io.DataInputStream;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

/**
 * 用于FFmpeg产生的 8-bit P6 PPM 数据解析器 (并非任意的PPM图像)
 */
public class FFmpegPPMParser {

	/**
	 * FFmpeg使用的域分割字符
	 */
	private static final int FIELD_SEPARATOR = 0x0A;

	/**
	 * 空白字符，FFmpeg在维度域中的宽和高之间使用
	 */
	private static final int WHITESPACE = 0x20;

	public VideoFrame parse(DataInputStream dis) throws IOException {
		final String format = readASCII(dis);

		if (!StringUtils.equals(format, "P6"))
			throw new IllegalArgumentException("Unrecognised format: parser only understands PPM P6 but got " + format);

		final int width = readASCIINumber(dis);
		final int height = readASCIINumber(dis);
		final int maxIntensity = readASCIINumber(dis);

		if (maxIntensity != 255)
			throw new IllegalArgumentException(
					"Unrecognised format: parser only understands 8-bit PPM P6 but got max intensity " + maxIntensity);

		VideoFrame frame = new VideoFrame(height, width);

		populate(frame, dis);

		return frame;
	}

	public void populate(VideoFrame frame, DataInputStream dis) throws IOException {
		final int height = frame.getHeight();
		final int width = frame.getWidth();

		final byte[] framebuffer = new byte[height * width * 3];

		// 将整个帧读到内存中
		dis.readFully(framebuffer);

		populate(frame, framebuffer);
	}

	public void populate(final VideoFrame frame, final byte[] buffer) throws IOException {
		final int height = frame.getHeight();
		final int width = frame.getWidth();

		int offset = 0;
		for (int x = 0; x < height; x++) {
			for (int y = 0; y < width; y++) {
				final short red = (short) (buffer[offset] & 0xFF);
				final short green = (short) (buffer[offset + 1] & 0xFF);
				final short blue = (short) (buffer[offset + 2] & 0xFF);

				frame.setRGB(x, y, red, green, blue);

				offset += 3;
			}
		}
	}

	private String readASCII(DataInputStream dis) throws IOException {
		// 假设最大字符串长度
		final char[] str = new char[5];

		// 读取所有非空白字符，出现空白字符停止读取
		int offset = 0;
		while (true) {
			final int val = dis.readUnsignedByte();

			if (val == FIELD_SEPARATOR || val == WHITESPACE)
				break;

			// TODO 确保val的值域在ASCII A-Za-z0-9之间?

			if (offset == str.length)
				throw new IllegalStateException("Error parsing ASCII: too much data. got as far as '"
						+ new String(str, 0, offset) + "'");
			str[offset++] = (char) val;
		}

		return new String(str, 0, offset);
	}

	private int readASCIINumber(DataInputStream dis) throws IOException {
		final String str = readASCII(dis);

		return Integer.parseInt(str);
	}

}
