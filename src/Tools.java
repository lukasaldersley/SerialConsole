import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Tools {
	public static boolean downloadJarfile(String path, String filename) {
		// String name =
		// path.substring(path.lastIndexOf(System.getProperty("file.separator")));
		try {
			ReadableByteChannel rbc = Channels.newChannel(new URL(path).openStream());
			FileOutputStream fos = new FileOutputStream(filename);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}