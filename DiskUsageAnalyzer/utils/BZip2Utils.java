package DiskUsageAnalyzer.utils;

import java.io.*;
import org.apache.commons.compress.compressors.bzip2.*;

public class BZip2Utils {

    public static void compressBzip2(File source, File target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             BZip2CompressorOutputStream bzip2Out = new BZip2CompressorOutputStream(bos)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                bzip2Out.write(buffer, 0, len);
            }
        }
    }

    public static void decompressBzip2(File source, File target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             BufferedInputStream bis = new BufferedInputStream(fis);
             BZip2CompressorInputStream bzip2In = new BZip2CompressorInputStream(bis);
             FileOutputStream fos = new FileOutputStream(target)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = bzip2In.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
    }
}
