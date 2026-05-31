package DiskUsageAnalyzer.utils;

import java.io.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.*;

public class TarUtils {

    public static void compressToTar(File source, File target) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(target);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(bos)) {

            addFileToTar(tarOut, source, "");
        }
    }

    private static void addFileToTar(TarArchiveOutputStream tarOut, File file, String base) throws IOException {
        String entryName = base + file.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(file, entryName);
        tarOut.putArchiveEntry(tarEntry);

        if (file.isFile()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    tarOut.write(buffer, 0, len);
                }
            }
            tarOut.closeArchiveEntry();
        } else if (file.isDirectory()) {
            tarOut.closeArchiveEntry();
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToTar(tarOut, child, entryName + "/");
                }
            }
        }
    }

    public static void extractTar(File tarFile, File destDir) throws IOException {
        try (FileInputStream fis = new FileInputStream(tarFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(bis)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                File destPath = new File(destDir, entry.getName());

                if (entry.isDirectory()) {
                    destPath.mkdirs();
                } else {
                    destPath.getParentFile().mkdirs();
                    try (OutputStream os = new FileOutputStream(destPath)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = tarIn.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }
}
