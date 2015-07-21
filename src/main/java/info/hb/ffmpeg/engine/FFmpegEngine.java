package info.hb.ffmpeg.engine;

import info.hb.frame.parser.FFmpegPPMParser;
import info.hb.frame.parser.VideoFrame;
import info.hb.frame.plugins.VideoAnalysisPlugin;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.imaging.ImageReadException;

import com.peterphi.std.util.HexHelper;

/**
 * 调用Linux下面的FFmpeg命令
 *
 * @author wanggang
 *
 */
public class FFmpegEngine {

	private final File ffmpeg;
	private int limitSeconds = 0;

	private static final FFmpegPPMParser PPM_PARSER = new FFmpegPPMParser();

	public FFmpegEngine(final File ffmpeg) {
		this.ffmpeg = ffmpeg;
	}

	/**
	 * 设置视频处理时常限制，单位为秒;
	 * 0代表没有限制
	 */
	public void setLimitSeconds(int limit) {
		this.limitSeconds = limit;
	}

	public void analyse(final File file, final VideoAnalysisPlugin plugin) {
		DigestInputStream dis = null;
		try {
			final Process process = spawn(file);

			eatStream(process.getErrorStream());

			final InputStream is = process.getInputStream();
			analyse(is, plugin);
		} catch (ImageReadException e) {
			throw new IOError(e);
		} catch (IOException e) {
			throw new IOError(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (dis != null) {
				System.out.println("SHA1 Digest of read data: " + HexHelper.toHex(dis.getMessageDigest().digest()));
			}
		}
	}

	private VideoFrame parse(InputStream is) throws IOException, ImageReadException {
		// 查看是否处于EOF，否在读取数据流时会出现异常
		is.mark(1);

		final int read = is.read();
		if (read == -1) {
			return null; // 处于EOF
		} else {
			is.reset();

			return PPM_PARSER.parse(new DataInputStream(is));
		}
	}

	protected void analyse(final InputStream is, final VideoAnalysisPlugin plugin) throws IOException,
			ImageReadException {
		if (!is.markSupported())
			throw new IllegalArgumentException("PPM Input Stream MUST support mark!");

		plugin.start();

		for (int frame = 0;; frame++) {
			System.err.println(frame);
			final VideoFrame img = parse(is);

			if (img != null)
				plugin.frame(frame, img);
			else
				break;
		}

		plugin.end();
	}

	/**
	 * 启动单个线程来消耗数据流中的内容，用完后丢弃
	 */
	private void eatStream(final InputStream is) {

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					final byte[] data = new byte[1024];
					while (true) {
						final int read = is.read(data);

						// 在EOF处挂掉
						if (read == -1)
							break;
					}
				} catch (IOException e) {
					throw new IOError(e);
				}
			}

		});

		thread.setDaemon(true);
		thread.start();
	}

	private Process spawn(final File file) throws IOException {
		final ProcessBuilder pb = new ProcessBuilder(getFfmpegCommand(file));

		return pb.start();
	}

	/**
	 * 获取FFmpeg命令
	 * ffmpeg -i test-videos/test1.ts -vf select="eq(pict_type\\,PICT_TYPE_I)"
	 * -vsync 2 -s 640x480 -f image2 output/thumbnails-%02d.jpeg
	 */
	private List<String> getFfmpegCommand(final File file) {
		List<String> cmd = new ArrayList<>();
		cmd.add(ffmpeg.getAbsolutePath());
		if (limitSeconds > 0) {
			cmd.add("-t");
			cmd.add(Integer.toString(limitSeconds));
		}
		cmd.add("-i");
		cmd.add(file.getAbsolutePath());
		cmd.add("-f");
		cmd.add("image2pipe");
		cmd.add("-vcodec");
		cmd.add("ppm");
		cmd.add("-");

		return cmd;
	}

}
