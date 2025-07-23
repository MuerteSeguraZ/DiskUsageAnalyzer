package DiskUsageAnalyzer.utils;

import java.io.*;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

public class XZUtils {

    public static void compressXZ(File source, File target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             XZCompressorOutputStream xzOut = new XZCompressorOutputStream(bos)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                xzOut.write(buffer, 0, len);
            }
        }
    }

    public static void decompressXZ(File source, File target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             BufferedInputStream bis = new BufferedInputStream(fis);
             XZCompressorInputStream xzIn = new XZCompressorInputStream(bis);
             FileOutputStream fos = new FileOutputStream(target)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = xzIn.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
    }
}
