# Zip

在看apk文件打包，分渠道的时候看到关于zip压缩文件的应用，之前关于这点知识的研究比较肤浅，感觉很少有机会能用到，而且也特别简单，所以就没有太深入。正好这里用到，就简单总结一下。

apk本身就是进行zip压缩后成的包，所以对于一个apk的修改，我们可以直接将其作为一个zip文件来处理。

## Zip与GZip的区别

摘自百度知道[GZIP 与zip区别](https://zhidao.baidu.com/question/202067445.html)
>GZIP 与zip区别主要是适应系统不同，还有就是压缩率不一样；
普遍使用的是zip压缩，Windows系统下就用zip
gzip为高压，可以把文件压缩得更小，便于放网盘或者网上供人下载；gzip是Linux下面用的格式，一般在Linux下解压，如果用Windows下的程序解压有可能丢失其中某些文件或属性。

gzip多用于http协议中网络数据的压缩，zip则常在操作系统中对文件进行压缩。gzip当然也可以对文件进行压缩，不过还是用常用的zip比较合适。如果需要高度压缩，可以使用gzip.

## GZip的使用

如下实例，演示了压缩字符串的方法，对于它其他的用法不讨论。

```Java
/**
 * 压缩字符串到输出流中
 *
 * @param data 压缩的字符串数据
 * @param outputStream 输出流
 * @throws IOException 异常
 */
public void gzip(String data, OutputStream outputStream) throws IOException {
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
```

## Zip的使用

- 文件压缩分为两部分：压缩和解压缩
- zip文件中，以entry表示一个单独被压缩的文件
- 在zip文件中其实没有文件层次，所有压缩的文件都是横向排列，使用entry的name作为文件路径的协议，在压缩与解压缩时都需要处理路径的问题

### 压缩

```Java
/**
 * 压缩文件
 *
 * @param sourceFile
 * @param destFile
 * @throws IOException
 */
public void compress(File sourceFile, File destFile) throws IOException {
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
public void compressFile(File file, ZipOutputStream outputStream, String parentDir) throws IOException {
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
```

上面是示例代码，压缩时，使用```ZipOutputStream```建立一个输出流，然后写入不同的```ZipEntry```, 首先使用```ZipOutputStream#putNextEntry(ZipEntry)```表示开始压缩一个文件，接下来按常规流处理方式，写入数据，数据压缩完毕之后，调用```ZipOutputStream#closeEntry()```方法表示完成了这个文件的压缩。

需要注意的是，一个```ZipEntry```有一个name, 这个name使用相对路径的格式表示文件的层次结构，所以我们在写入时，需要注意这个name的构造。

流程：开启一个输出流 -> 单独压缩一个文件(循环)

### 解压缩

```Java
/**
 * 解压缩
 *
 * @param sourceFile zip文件
 * @param destFile   目标文件夹
 * @throws IOException 异常
 */
public void decompress(File sourceFile, File destFile) throws IOException {

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
```

流程：获取一个压缩文件 -> 解压缩 (循环)

如上所示，和压缩一个文件的流程完全相反，构造一个```ZipFile```对象，循环获取其中的```ZipEntry```, 获取其输入流，然后将其内容读出来即可。

### 向zip添加文件

文章[过去3小时，现在1分钟打包900个应用渠道包](http://www.jianshu.com/p/2b38eb1c9d8a)中使用的分渠道的方法是向apk(zip文件)中添加一个名字不同的文件来作为不同渠道的标识。其使用了python向zip文件添加压缩文件，方法简单。但是如果使用java来实现呢？

很多网上给出的解决方案都是重新解压然后再次压缩，这个当然非常耗资源，同时这样也会破坏apk原本的签名，所以不是好的解决方案。

stackoverflow中有相应的解决方案[Sign up
Appending files to a zip file with Java](http://stackoverflow.com/questions/2223434/appending-files-to-a-zip-file-with-java), [Add file to a folder that is inside a zip file java](http://stackoverflow.com/questions/17083662/add-file-to-a-folder-that-is-inside-a-zip-file-java), 能够解决上面提出的问题。在这两个解决方案中使用了java 7的特性关于操作文件系统的功能，暂时没有深入了解，不过仍然存在一个问题。如果我想把一个文件添加到zip中没有的一个“文件夹”中呢。使用这种方法就会报错，关于具体的原因，之后再说。不过这点来说，第一次会觉得python比较有用处的地方。

## 参考

[Java压缩文件夹成Zip文件和解压缩Zip文件的实现](http://www.jianshu.com/p/1535d3b13236)

[Sign up
Appending files to a zip file with Java](http://stackoverflow.com/questions/2223434/appending-files-to-a-zip-file-with-java)

[Add file to a folder that is inside a zip file java](http://stackoverflow.com/questions/17083662/add-file-to-a-folder-that-is-inside-a-zip-file-java)

[
Add Files to Existing ZIP Archive in Java – Example Program](http://thinktibits.blogspot.tw/2013/02/Add-Files-to-Existing-ZIP-Archive-in-Java-Example-Program.html)