package com.example.exoplayer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class YWFileUtil {

    /**
     * 压缩得到的文件的后缀名
     */
    public static final String ZIP_SUFFIX = ".zip";
    /**
     * 缓冲器大小
     */
    public static final int BUFFER = 4 * 1024;

    public static final String TAG = "YWFileUtil";

    /**
     * 判断 SD 卡是否可用，这个判断包含了判断 writable 和 Readable
     */
    public static boolean isSDCardEnable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * 获取SD卡应用专属文件目录/storage/emulated/0/Android/data/app_package_name/files
     * 这个目录在android 4.4及以上系统不需要申请SD卡读写权限
     * 因此也不用考虑6.0系统动态申请SD卡读写权限问题，切随应用被卸载后自动清空 不会污染用户存储空间
     *
     * @param context 上下文
     * @return 缓存文件夹 如果没有SD卡或SD卡有问题则返回内部存储目录，/data/data/app_package_name/files
     *         否则优先返回SD卡缓存目录
     */
    public static File getStorageFileDir(Context context) {
        return getStorageFileDir(context, null);
    }


    /**
     * 获取SD卡应用专属文件目录/storage/emulated/0/Android/data/app_package_name/files
     * 这个目录在android 4.4及以上系统不需要申请SD卡读写权限
     * 因此也不用考虑6.0系统动态申请SD卡读写权限问题，切随应用被卸载后自动清空 不会污染用户存储空间
     *
     * @param context 上下文
     * @param type 文件夹类型 可以为空，为空则返回API得到的一级目录
     * @return 缓存文件夹 如果没有SD卡或SD卡有问题则返回内部存储目录，/data/data/app_package_name/files
     *         否则优先返回SD卡缓存目录
     */
    public static File getStorageFileDir(Context context, String type) {
        File appCacheDir = getExternalFileDirectory(context, type);
        if (appCacheDir == null) {
            appCacheDir = getInternalFileDirectory(context, type);
        }

        if (appCacheDir == null) {
            Log.e(TAG, "getStorageFileDir fail , ExternalFile and InternalFile both unavailable ");
        } else {
            if (!appCacheDir.exists() && !appCacheDir.mkdirs()) {
                Log.e(TAG, "getStorageFileDir fail ,the reason is make directory fail !");
            }
        }
        return appCacheDir;
    }

    /**
     * 获取SD卡缓存目录
     *
     * @param context 上下文
     * @param type 文件夹类型 如果为空则返回 /storage/emulated/0/Android/data/app_package_name/files
     *         否则返回对应类型的文件夹如Environment.DIRECTORY_PICTURES 对应的文件夹为 ..
     *         ./data/app_package_name/files/Pictures
     *         {@link Environment#DIRECTORY_MUSIC},
     *         {@link Environment#DIRECTORY_PODCASTS},
     *         {@link Environment#DIRECTORY_RINGTONES},
     *         {@link Environment#DIRECTORY_ALARMS},
     *         {@link Environment#DIRECTORY_NOTIFICATIONS},
     *         {@link Environment#DIRECTORY_PICTURES}, or
     *         {@link Environment#DIRECTORY_MOVIES}.or 自定义文件夹名称
     * @return 缓存目录文件夹 或 null（无SD卡或SD卡挂载失败）
     */
    public static File getExternalFileDirectory(Context context, String type) {
        if (context == null) {
            return null;
        }
        File appFileDir = null;
        if (isSDCardEnable()) {
            if (TextUtils.isEmpty(type)) {
                appFileDir = context.getExternalFilesDir(null);
            } else {
                appFileDir = context.getExternalFilesDir(type);
            }

            if (appFileDir == null) {// 有些手机需要通过自定义目录
                appFileDir = new File(Environment.getExternalStorageDirectory(),
                        "Android/data/" + context.getPackageName() + "/files/" + type);
            }

            if (!appFileDir.exists() && !appFileDir.mkdirs()) {
                Log.e(TAG, "getExternalFileDirectory fail ,the reason is make directory fail ");
            }
        } else {
            Log.e(TAG, "getExternalFileDirectory fail ,the reason is sdCard unMounted ");
        }
        return appFileDir;
    }

    /**
     * 获取内存缓存目录 /data/data/app_package_name/files
     *
     * @param type 子目录，可以为空，为空直接返回一级目录
     * @return 缓存目录文件夹 或 null（创建目录文件失败）
     *         注：该方法获取的目录是能供当前应用自己使用，外部应用没有读写权限，如 系统相机应用
     */
    public static File getInternalFileDirectory(Context context, String type) {
        File appFileDir = null;
        if (TextUtils.isEmpty(type)) {
            appFileDir = context.getFilesDir();// /data/data/app_package_name/files
        } else {
            appFileDir = new File(context.getFilesDir(),
                    type);// /data/data/app_package_name/files/type
        }

        if (!appFileDir.exists() && !appFileDir.mkdirs()) {
            Log.e(TAG, "getInternalFileDirectory fail ,the reason is make directory fail !");
        }
        return appFileDir;
    }

    /**
     * 得到源文件路径的所有文件
     * eg:
     * 输入：
     * h1/h2/h3/34.txt
     * h1/h22
     * 结果：34.txt h22
     *
     * @param dirFile 源文件路径
     */
    public static List<File> getAllFile(File dirFile) {

        List<File> fileList = new ArrayList<File>();

        File[] files = dirFile.listFiles();
        if (files == null) {
            return fileList;
        }
        for (File file : files) {//文件
            if (file.isFile()) {
                fileList.add(file);
                System.out.println("add file:" + file.getName());
            } else {//目录
                if (file.listFiles() != null && file.listFiles().length != 0) {//非空目录
                    fileList.addAll(getAllFile(file));//把递归文件加到fileList中
                } else {//空目录
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }

    /**
     * 遍历所有文件/文件夹
     * 该工具与 getAllFile 有重合的地方
     *
     * @param file 源文件路径
     * @param files 返回集合
     * @param withDir 是否包含目录
     *         eg:
     *         输入：
     *         h1/h2/h3/34.txt
     *         h1/h22
     *         结果：h1 h2 h3 34.txt h22
     */
    public static void flatFileWithDir(File file, List<File> files, boolean withDir) {
        try {
            if (file == null || !file.exists()) {
                return;
            }
            if (withDir) {
                files.add(file);
            } else if (file.isFile()) {
                files.add(file);
            }
            if (!file.isFile()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File aChildren : children) {
                        flatFileWithDir(aChildren, files, withDir);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 复制 srcDir 到 destDir
     *
     * @param srcDir 源文件夹
     * @param destDir 目标文件夹
     * @return 是否成功
     */
    public static boolean copyDir(File srcDir, File destDir, boolean overwrite) {
        if (!srcDir.exists()) {
            return false;
        }

        // 先创建目标文件夹
        if (!mkdirsIfNotExit(destDir)) {
            return false;
        }

        // 遍历源文件夹下每一个文件
        boolean result = true;
        final File[] files = srcDir.listFiles();

        if (files == null) {
            return false;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 如果是文件夹递归
                result = copyDir(file, new File(destDir, file.getName()), overwrite);
            } else if (!file.exists()) {
                Log.e(TAG, "copyDir: file not exists (" + file.getAbsolutePath() + ")");
                result = false;
            } else if (!file.isFile()) {
                Log.e(TAG, "copyDir: file not file (" + file.getAbsolutePath() + ")");
                result = false;
            } else if (!file.canRead()) {
                Log.e(TAG, "copyDir: file cannot read (" + file.getAbsolutePath() + ")");
                result = false;
            } else {
                result = copyFile(file, new File(destDir, file.getName()), overwrite);
            }
        }
        return result;
    }

    /**
     * 复制 srcFile 到 destFile
     *
     * @param srcFile 来源文件
     * @param destFile 目标文件
     * @return 是否成功
     */
    public static boolean copyFile(File srcFile, File destFile) {
        return copyFile(srcFile, destFile, false);
    }

    /**
     * 复制 srcFile 到 destFile
     *
     * @param srcFile 来源文件
     * @param destFile 目标文件
     * @param overwrite 是否覆盖
     * @return 是否成功
     */
    public static boolean copyFile(File srcFile, File destFile, boolean overwrite) {
        try {
            if (!srcFile.exists()) {
                return false;
            }
            if (destFile.exists()) {
                if (overwrite) {
                    destFile.delete();
                } else {
                    return true;
                }
            } else {
                if (mkdirsIfNotExit(destFile.getParentFile())) {
                    destFile.createNewFile();
                }
            }

            FileInputStream input = null;
            FileOutputStream fos = null;
            try {
                input = new FileInputStream(srcFile);
                fos = new FileOutputStream(destFile);
                byte[] block = new byte[1024 * 50];
                int readNumber = -1;
                while ((readNumber = input.read(block)) != -1) {
                    fos.write(block, 0, readNumber);
                }
                fos.flush();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                close(input);
                close(fos);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 不存在的情况下创建 File
     *
     * @param file 文件
     * @return 是否成功
     */
    public static boolean mkdirsIfNotExit(File file) {
        if (file == null) {
            return false;
        }
        if (!file.exists()) {
            synchronized (YWFileUtil.class) {
                return file.mkdirs();
            }
        }
        return true;
    }

    /**
     * 强制删除文件，若失败则延迟 200ms 重试，重试次数 10 次
     * 【注】务必在子线程中调用
     *
     * @param file 待删除文件
     * @return 是否成功
     */
    @WorkerThread
    public static boolean forceDeleteFile(File file) {
        if (!file.exists()) {
            return true;
        }
        boolean result = false;
        int tryCount = 0;
        while (!result && tryCount++ < 10) {
            result = file.delete();
            if (!result) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Log.e("forceDeleteFile", e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * 清空文件（夹）
     * 【注】务必在子线程中调用
     *
     * @param file 待删除文件
     * @return 是否成功
     */
    @WorkerThread
    public static boolean clear(File file) {
        if (file == null) {
            return false;
        }
        if (!file.exists()) {
            return false;
        }
        if (!file.isDirectory()) {
            return YWFileUtil.forceDeleteFile(file);
        } else {
            boolean ret = true;
            try {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    Thread.sleep(1);
                    if (files[i].isDirectory()) {
                        if (!clear(files[i])) {
                            // 只要失败就return false
                            return false;
                        }
                    } else {
                        if (!YWFileUtil.forceDeleteFile(files[i])) {
                            ret = false;
                            break;
                        }
                    }
                }
                final File to = new File(file.getAbsolutePath()
                        + System.currentTimeMillis());
                file.renameTo(to);
                YWFileUtil.forceDeleteFile(to);// 删除空文件夹
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return ret;
        }
    }

    public static boolean writeStream(File file, InputStream is) {
        FileOutputStream os = null;
        try {
            if (file == null || is == null) {
                return false;
            }
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                boolean mkdirs = parentFile.mkdirs();
                if (!mkdirs) {
                    return false;
                }
            }
            if (!file.exists()) {
                boolean newFile = file.createNewFile();
                if (!newFile) {
                    return false;
                }
            }
            os = new FileOutputStream(file);
            int byteCount;
            byte[] bytes = new byte[1024];
            while ((byteCount = is.read(bytes)) != -1) {
                os.write(bytes, 0, byteCount);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(is);
            close(os);
        }
        return false;
    }

    /**
     * 读取文件内容
     *
     * @param path 文件路径
     * @return 内容
     */
    public static String readFile(String path) {
        if (path == null) {
            return null;
        }

        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            bufferedReader = new BufferedReader(new FileReader(path));
            String strTemp;
            while ((strTemp = bufferedReader.readLine()) != null) {
                stringBuilder.append(strTemp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(bufferedReader);
        }
        return stringBuilder.toString();
    }

    /**
     * 从Asset读取文件内容
     *
     * @param fileName 文件名称
     * @return 内容
     */
    public static String readAsset(Context context, String fileName) {
        if (fileName == null || context == null) {
            return null;
        }
        BufferedReader bufferedReader = null;
        InputStream inputStream = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            inputStream = context.getAssets().open(fileName);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String strTemp;
            while ((strTemp = bufferedReader.readLine()) != null) {
                stringBuilder.append(strTemp);
                stringBuilder.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(bufferedReader);
            close(inputStream);
        }
        return stringBuilder.toString();
    }

    /**
     * 内容保存到文件
     *
     * @param destFile 目标文件
     * @param content 内容
     */
    public static boolean save2File(File destFile, String content) {

        if (destFile == null || TextUtils.isEmpty(content)) {
            return false;
        }
        OutputStream outputStream = null;
        try {
            destFile.delete();
            if (!destFile.createNewFile()) {
                return false;
            }
            outputStream = new FileOutputStream(destFile);
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(outputStream);
        }
        return true;
    }

    /**
     * 保存bitmap 到文件
     *
     * @param bitmap bitmap
     * @param filepath filepath
     * @return 保存成功
     */
    public static boolean saveBitmap(Bitmap bitmap, String filepath) {
        Log.i(TAG, "saveBitmap [filepath] = " + filepath);
        CompressFormat format = CompressFormat.JPEG;
        int quality = 100;
        OutputStream stream = null;
        try {
            File file = new File(filepath);
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (!file.exists()) {
                boolean createResult = file.createNewFile();
                if (!createResult) {
                    return false;
                }
            }
            stream = new FileOutputStream(filepath);
            return bitmap.compress(format, quality, stream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            flush(stream);
            close(stream);
        }
        return false;
    }


    /**
     * 关闭输入流
     *
     * @param closeable IO stream
     */
    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Flush输入流
     *
     * @param flushable IO stream
     */
    public static void flush(Flushable flushable) {
        if (flushable == null) {
            return;
        }
        try {
            flushable.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 解压文件
     *
     * @param zipFilePath
     * @param unzipPath
     * @throws IOException
     * @throws ZipException
     */
    public static void unzipFile(String zipFilePath, String unzipPath)
            throws ZipException, IOException {
        File file = new File(zipFilePath);
        ZipFile zipFile = new ZipFile(file);
        File desFile = new File(unzipPath);
        if (!desFile.exists()) {
            desFile.mkdirs();
        }
        try {
            for (Enumeration entries = zipFile.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String zipEntryName = entry.getName();
                InputStream in = zipFile.getInputStream(entry);
                String outPath = (unzipPath + zipEntryName).replaceAll("\\*", "/");
                // 判断路径是否存在,不存在则创建文件路径
                File temp = new File(outPath.substring(0, outPath.lastIndexOf('/')));
                if (!temp.exists()) {
                    temp.mkdirs();
                }
                // 判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                if (new File(outPath).isDirectory()) {
                    continue;
                }
                // 输出文件路径信息
                OutputStream out = null;
                try {
                    out = new FileOutputStream(outPath);
                    byte[] buf1 = new byte[4 * 1024];
                    int len;
                    while ((len = in.read(buf1)) > 0) {
                        out.write(buf1, 0, len);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    throw e;
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (ZipException e) {
            throw e;
        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 获取指定路径下的文件
     */
    public static File getAccessFileOrCreate(String access) {
        try {
            File f = new File(access);
            if (!f.exists()) {
                if (mkdirsIfNotExit(f.getParentFile())) {
                    f.createNewFile();
                }
                return f;
            } else {
                return f;
            }
        } catch (IOException e) {

            Log.e("Utility getAccessFile", e.toString());
        }
        return null;
    }

    public static File getAccessFileOrNull(String access) {
        File f = new File(access);
        if (f.exists()) {
            return f;
        } else {
            return null;
        }
    }

    @Nullable
    public static File generateFileName(@Nullable String name, File directory) {
        if (name == null) {
            return null;
        }

        File file = new File(directory, name);

        if (file.exists()) {
            String fileName = name;
            String extension = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex);
                extension = name.substring(dotIndex);
            }

            int index = 0;

            while (file.exists()) {
                index++;
                name = fileName + '(' + index + ')' + extension;
                file = new File(directory, name);
            }
        }

        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


        return file;
    }
}
