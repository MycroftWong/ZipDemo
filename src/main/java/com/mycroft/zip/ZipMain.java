package com.mycroft.zip;

import com.mycroft.zip.util.CloseUtil;

import java.io.*;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.*;

/**
 * Created by Mycroft on 2016/12/23.
 */
public class ZipMain {

    private static final String DIR_COMPRESSION = "E:\\Documents\\SDK\\rxysdk";
    private static final String DIR_DECOMPRESSION = "E:\\Documents\\SDK\\rxysdk.zip";

    public static void main(String[] args) throws IOException {
        testCompression();
//        testDecompression();
//        testAppend();
        testAppendJava7();
    }

    private static void testCompression() throws IOException {
        final File sourceFile = new File(DIR_COMPRESSION);
        final File destFile = new File(DIR_DECOMPRESSION);

        compress(sourceFile, destFile);
    }

    /**
     * 压缩文件
     *
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    private static void compress(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }

        ZipOutputStream outputStream = null;
        try {
            outputStream = new ZipOutputStream(
                    new CheckedOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)), new CRC32()));

            compressFile(sourceFile, outputStream, "");
        } finally {
            CloseUtil.quietClose(outputStream);
        }
    }

    /**
     * 递归压缩
     *
     * @param file         需要被压缩的文件
     * @param outputStream {@link ZipOutputStream} 压缩输出流
     * @param parentDir    父类路径，用于指定压缩文件名，保留文件路径
     * @throws IOException 异常
     */
    private static void compressFile(File file, ZipOutputStream outputStream, String parentDir) throws IOException {
        if (file.isDirectory()) {

            final File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    compressFile(child, outputStream, parentDir + file.getName() + "/");
                }
            }

        } else if (file.exists()) {
            BufferedInputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(file));

                // 定义一个 ZipEntry, 有一个文件名(实际上代表的是路径)
                ZipEntry entry = new ZipEntry(parentDir + file.getName());
                // 表示准备放入一个 ZipEntry
                outputStream.putNextEntry(entry);

                // 开始写入数据，作为该ZipEntry的内容
                final byte[] buff = new byte[2048];
                int len;

                while ((len = in.read(buff)) > 0) {
                    outputStream.write(buff, 0, len);
                }
                // 关闭这个ZipEntry, 表示完成这个ZipEntry的写入
                outputStream.closeEntry();
            } finally {
                CloseUtil.quietClose(in);
            }
        }
    }

    private static void testDecompression() throws IOException {
        final File sourceFile = new File(DIR_DECOMPRESSION);

        final File destFile = new File("E:/Documents/SDK/tmp");

        decompress(sourceFile, destFile);
    }

    /**
     * 解压缩
     *
     * @param sourceFile zip文件
     * @param destFile   目标文件夹
     * @throws IOException 异常
     */
    private static void decompress(File sourceFile, File destFile) throws IOException {

        if (!destFile.exists()) {
            destFile.mkdirs();
        }

        // 获取zip文件
        ZipFile zipFile = new ZipFile(sourceFile, ZipFile.OPEN_READ);

        // 循环读取其中的entry
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            // 获取 ZipEntry
            ZipEntry zipEntry = enumeration.nextElement();
            String entryName = zipEntry.getName();

            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                // 获取 ZipEntry 的输入流
                inputStream = zipFile.getInputStream(zipEntry);

                // 指定解压缩后的文件路径
                String destPath = destFile.getAbsolutePath() + File.separator + entryName;

                // 解压缩后所在的文件夹
                File outDir = new File(destPath.substring(0, destPath.lastIndexOf('/')));
                if (!outDir.exists()) {
                    outDir.mkdirs();
                }

                if (new File(destPath).isDirectory()) {
                    continue;
                }

                // 进行解压缩(流处理)
                outputStream = new BufferedOutputStream(new FileOutputStream(destPath));
                final byte[] buff = new byte[2048];
                int len;
                while ((len = inputStream.read(buff)) > 0) {
                    outputStream.write(buff, 0, len);
                }
                outputStream.flush();
            } finally {
                CloseUtil.quietClose(outputStream);
                CloseUtil.quietClose(inputStream);
            }
        }
    }

    private static void testAppend() throws IOException {
        final File sourceFile = new File("E:\\Documents\\SDK\\tmp\\rxysdk\\SamanResultListener.java");
        final File destFile = new File(DIR_DECOMPRESSION);

        appendFile(sourceFile, destFile);
    }

    /**
     * Java并不支持向Zip文件中添加新文件
     *
     * @param sourceFile
     * @param zipFile
     * @throws IOException
     */
    private static void appendFile(File sourceFile, File zipFile) throws IOException {
        ZipOutputStream outputStream = null;

        InputStream inputStream = null;
        try {
            outputStream = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(zipFile, true), new CRC32()));

            ZipEntry zipEntry = new ZipEntry("MANI-INF/" + sourceFile.getName());

            outputStream.putNextEntry(zipEntry);

            inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
            final byte[] buff = new byte[2048];
            int len;
            while ((len = inputStream.read(buff)) > 0) {
                outputStream.write(buff, 0, len);
            }
            outputStream.closeEntry();
            outputStream.flush();

        } finally {
            CloseUtil.quietClose(inputStream);
            CloseUtil.quietClose(outputStream);
        }
    }

    private static void testAppendJava7() throws IOException {
        final File sourceFile = new File("E:\\Documents\\SDK\\tmp\\rxysdk\\view\\PagerSlidingTabStrip.java");
        final File destFile = new File(DIR_DECOMPRESSION);

        appendFileJava7(sourceFile, destFile);
    }

    private static void appendFileJava7(File sourceFile, File zipFile) throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");

        Path path = Paths.get(zipFile.getAbsolutePath());


        URI uri = URI.create("jar:" + path.toUri());
        try (FileSystem fileSystem = FileSystems.newFileSystem(uri, env)) {
            // error
//            Path nf = fileSystem.getPath("MANI-INF").resolve(sourceFile.getName());
            Path nf = fileSystem.getPath("rxysdk").resolve(sourceFile.getName());

            try (OutputStream out = Files.newOutputStream(nf, StandardOpenOption.CREATE)) {

                try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile))) {
                    int len;
                    final byte[] buff = new byte[1024];
                    while ((len = inputStream.read(buff)) > 0) {
                        out.write(buff, 0, len);
                    }
                }
            }

/*
            try (Writer writer = Files.newBufferedWriter(nf, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
                writer.write(readFile(sourceFile));
            }
*/
        }
    }

    private static String readFile(File file) throws IOException {
        BufferedReader reader = null;
        String line;
        StringBuilder result = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                result.append(line);
                result.append(System.lineSeparator());
            }
        } finally {
            CloseUtil.quietClose(reader);
        }

        return result.toString();
    }
}
