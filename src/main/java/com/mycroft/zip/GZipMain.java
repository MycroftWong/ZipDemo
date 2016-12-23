package com.mycroft.zip;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * GZip虽然和Zip名字上差不多，不过两者的功能却不太相同，GZip用于压缩字符串，通常作为网络传输，Zip则是用来压缩文件
 * Created by Mycroft on 2016/12/23.
 */
public class GZipMain {

    private static final String DATA = "大富科技奥克d兰的戴假发辽阔的九分裤垃圾a快递分拣奥克兰的饥饿哦器大a富科技奥克兰的戴假发辽阔的九分裤垃圾a快递分拣奥克a兰的饥饿哦器大富科技奥克兰的戴假发辽阔的九分裤垃圾快递分拣奥克兰的饥饿哦器大富科技奥克兰的戴假发辽阔的九分裤垃圾快递分拣奥克兰的饥饿哦器";

    public static void main(String[] args) throws IOException {
        final long start = System.currentTimeMillis();
//        gzipFile();
//        zipFile();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream out = new GZIPOutputStream(byteArrayOutputStream);
        out.write(DATA.getBytes());
        out.flush();
        out.close();

        final String result = ungzip(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        System.out.println(result);

        final long end = System.currentTimeMillis();
        System.out.println(end - start);

    }

    /**
     * 压缩字符串到输出流中
     *
     * @param data         压缩的字符串数据
     * @param outputStream 输出流
     * @throws IOException 异常
     */
    private static void gzip(String data, OutputStream outputStream) throws IOException {

        GZIPOutputStream out = new GZIPOutputStream(outputStream);

        out.write(data.getBytes());

        out.flush();
        out.close();
    }

    /**
     * 解压缩的字符串
     *
     * @param inputStream 输入流
     * @return 解压缩的结果
     * @throws IOException 异常
     */
    private static String ungzip(InputStream inputStream) throws IOException {
        GZIPInputStream in = new GZIPInputStream(inputStream);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final byte[] buff = new byte[20];
        int len;
        while ((len = in.read(buff)) > 0) {
            outputStream.write(buff, 0, len);
        }

        in.close();

        outputStream.flush();
        outputStream.close();

        return new String(outputStream.toByteArray());
    }

    private static void gzipFile() throws IOException {
        final File file = new File("D:\\Tmp\\ideaIU-2016.3.exe");

        final File outFile = new File("D:\\Tmp\\ideaIU-2016.3.gzip");

        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
             GZIPOutputStream outputStream = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)))) {
            final byte[] buff = new byte[2048];
            int len;

            while ((len = inputStream.read(buff)) > 0) {
                outputStream.write(buff, 0, len);
            }
            outputStream.flush();
        }
    }

    private static void zipFile() throws IOException {
        final File file = new File("D:\\Tmp\\ideaIU-2016.3.exe");

        final File outFile = new File("D:\\Tmp\\ideaIU-2016.3.zip");
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
             ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)))) {

            outputStream.putNextEntry(new ZipEntry("nothing.exe"));

            final byte[] buff = new byte[2048];
            int len;

            while ((len = inputStream.read(buff)) > 0) {
                outputStream.write(buff, 0, len);
            }
            outputStream.closeEntry();
            outputStream.flush();
        }
    }
}
