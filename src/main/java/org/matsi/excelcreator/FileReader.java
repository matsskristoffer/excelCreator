package org.matsi.excelcreator;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileReader {

  private static final Logger logger = LogManager.getLogger();

  private FileReader() {
  }

  public static byte[] readFromFile(String fileName) throws IOException {
    byte[] data;
    try (InputStream inputStream = FileReader.class.getClassLoader().getResourceAsStream(fileName)) {

      assert inputStream != null;
      data = ByteStreams.toByteArray(inputStream);

    } catch (IOException e) {
      logger.error(e.getMessage());
      throw new IOException(e);
    }
    return data;
  }

}
