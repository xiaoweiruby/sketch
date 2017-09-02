/*
 * Copyright (C) 2013 Peng fei Pan <sky@xiaopan.me>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xiaopan.sketch;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.xiaopan.sketch.request.CancelCause;
import me.xiaopan.sketch.request.DisplayHelper;
import me.xiaopan.sketch.request.DisplayRequest;
import me.xiaopan.sketch.request.DownloadHelper;
import me.xiaopan.sketch.request.DownloadListener;
import me.xiaopan.sketch.request.LoadHelper;
import me.xiaopan.sketch.request.LoadListener;
import me.xiaopan.sketch.uri.ApkIconUriModel;
import me.xiaopan.sketch.uri.AppIconUriModel;
import me.xiaopan.sketch.uri.AssetUriModel;
import me.xiaopan.sketch.uri.DrawableUriModel;
import me.xiaopan.sketch.util.SketchUtils;

/**
 * Sketch 是一个功能强大且全面的图片加载器，可以从网络或者本地加载图片，支持 gif、手势缩放以及分块显示超大图
 * <ul>
 * <li>display()：显示图片到 ImageView 上</li>
 * <li>load()：加载图片到内存中</li>
 * <li>download()：下载图片到磁盘上</li>
 * </ul>
 */
public class Sketch {
    public static final String META_DATA_KEY_INITIALIZER = "SKETCH_INITIALIZER";

    private static volatile Sketch instance;

    private Configuration configuration;

    private Sketch(@NonNull Context context) {
        this.configuration = new Configuration(context);
    }

    /**
     * 获取 Sketch 实例
     *
     * @param context 用于初始化 Sketch
     * @return Sketch
     */
    public static Sketch with(@NonNull Context context) {
        if (instance == null) {
            synchronized (Sketch.class) {
                if (instance == null) {
                    Sketch newInstance = new Sketch(context);
                    SLog.i(null, "Version %s %s(%d) -> %s",
                            BuildConfig.BUILD_TYPE, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, newInstance.configuration.getInfo());

                    Initializer initializer = SketchUtils.findInitializer(context);
                    if (initializer != null) {
                        initializer.onInitialize(context.getApplicationContext(), newInstance, newInstance.configuration);
                    }
                    instance = newInstance;
                }
            }
        }
        return instance;
    }

    /**
     * 取消请求
     *
     * @param sketchView 会通过 SketchView 的 Drawable 找到正在执行的请求，然后取消它
     * @return true：当前 SketchView 有正在执行的任务并且取消成功；false：当前 SketchView 没有正在执行的任务
     */
    @SuppressWarnings("unused")
    public static boolean cancel(@NonNull SketchView sketchView) {
        final DisplayRequest displayRequest = SketchUtils.findDisplayRequest(sketchView);
        if (displayRequest != null && !displayRequest.isFinished()) {
            displayRequest.cancel(CancelCause.BE_CANCELLED);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取配置
     *
     * @return Configuration
     */
    @NonNull
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * 根据 URI 下载图片
     *
     * @param uri      图片 Uri，支持以下几种
     *                 <ul>
     *                 <li>http://site.com/image.png  // from Web</li>
     *                 <li>https://site.com/image.png // from Web</li>
     *                 </ul>
     * @param listener 监听下载过程
     * @return DownloadHelper 你可以继续通过 DownloadHelper 设置参数，最后调用其 commit() 方法提交
     */
    @SuppressWarnings("unused")
    public DownloadHelper download(@NonNull String uri, @Nullable DownloadListener listener) {
        return configuration.getHelperFactory().getDownloadHelper(this, uri, listener);
    }

    /**
     * 根据 URI 加载图片
     *
     * @param uri      图片 Uri，支持以下几种
     *                 <ul>
     *                 <li>http://site.com/image.png    // from Web</li>
     *                 <li>https://site.com/image.png   // from Web</li>
     *                 <li>file:///mnt/sdcard/image.png // from SD card</li>
     *                 <li>/mnt/sdcard/image.png    // from SD card</li>
     *                 <li>/mnt/sdcard/app.apk  // from SD card apk file</li>
     *                 <li>content://media/external/audio/albumart/13   // from content provider</li>
     *                 <li>asset://image.png    // from assets</li>
     *                 <li>"drawable://" + R.drawable.image // from drawables (only images, non-9patch)</li>
     *                 </ul>
     * @param listener 监听下载过程
     * @return LoadHelper 你可以继续通过 LoadHelper 设置参数，最后调用其 commit() 方法提交
     */
    public LoadHelper load(@NonNull String uri, @Nullable LoadListener listener) {
        return configuration.getHelperFactory().getLoadHelper(this, uri, listener);
    }

    /**
     * 加载 Asset 中的图片
     *
     * @param assetResName asset 中图片文件的名称
     * @param listener     监听加载过程
     * @return LoadHelper 你可以继续通过 LoadHelper 设置参数，最后调用其 commit() 方法提交
     */
    @SuppressWarnings("unused")
    public LoadHelper loadFromAsset(@NonNull String assetResName, @Nullable LoadListener listener) {
        String uri = AssetUriModel.makeUri(assetResName);
        return configuration.getHelperFactory().getLoadHelper(this, uri, listener);
    }

    /**
     * 加载资源中的图片
     *
     * @param drawableResId 图片资源的ID
     * @param listener      监听加载过程
     * @return LoadHelper 你可以继续通过 LoadHelper 设置参数，最后调用其 commit() 方法提交
     */
    @SuppressWarnings("unused")
    public LoadHelper loadFromResource(@DrawableRes int drawableResId, @Nullable LoadListener listener) {
        String uri = DrawableUriModel.makeUri(drawableResId);
        return configuration.getHelperFactory().getLoadHelper(this, uri, listener);
    }

    /**
     * 加载 URI 指向的图片
     *
     * @param uri      图片 Uri，会通过 ContentResolver().openInputStream(Uri) 方法来读取图片
     * @param listener 监听加载过程
     * @return LoadHelper 你可以继续通过 LoadHelper 设置参数，最后调用其 commit() 方法提交
     */
    @SuppressWarnings("unused")
    public LoadHelper loadFromContent(@NonNull Uri uri, @Nullable LoadListener listener) {
        return configuration.getHelperFactory().getLoadHelper(this, uri.toString(), listener);
    }

    /**
     * 加载 APK 的图标
     *
     * @param filePath APP 的路径
     * @param listener 监听加载过程
     * @return LoadHelper 你可以继续通过 LoadHelper 设置参数，最后调用其 commit() 方法提交
     */
    @SuppressWarnings("unused")
    public LoadHelper loadApkIcon(@NonNull String filePath, LoadListener listener) {
        String uri = ApkIconUriModel.makeUri(filePath);
        return configuration.getHelperFactory().getLoadHelper(this, uri, listener);
    }

    /**
     * 加载 APP 的图标
     *
     * @param packageName APP 的包名
     * @param versionCode APP 的版本号
     * @param listener    监听加载过程
     * @return LoadHelper 你可以继续通过 LoadHelper 设置参数，最后调用其 commit() 方法提交
     */
    @SuppressWarnings("unused")
    public LoadHelper loadAppIcon(String packageName, int versionCode, LoadListener listener) {
        String uri = AppIconUriModel.makeUri(packageName, versionCode);
        return configuration.getHelperFactory().getLoadHelper(this, uri, listener);
    }

    /**
     * 根据 URI 显示图片
     *
     * @param uri        图片Uri，支持以下几种
     *                   <ul>
     *                   <li>http://site.com/image.png    // from Web</li>
     *                   <li>https://site.com/image.png   // from Web</li>
     *                   <li>file:///mnt/sdcard/image.png // from SD card</li>
     *                   <li>/mnt/sdcard/image.png    // from SD card</li>
     *                   <li>/mnt/sdcard/app.apk  // from SD card apk file</li>
     *                   <li>content://media/external/audio/albumart/13   // from content provider</li>
     *                   <li>asset://image.png    // from assets</li>
     *                   <li>"drawable://" + R.drawable.image // from drawables (only images, non-9patch)</li>
     *                   </ul>
     * @param sketchView Sketch 对 ImageView 的规范接口，Sketch 对 ImageView 的规范接口，默认实现是 SketchImageView
     * @return DisplayHelper 你可以继续通过 DisplayHelper 设置参数，最后调用其 commit() 方法提交
     */
    public DisplayHelper display(String uri, SketchView sketchView) {
        return configuration.getHelperFactory().getDisplayHelper(this, uri, sketchView);
    }

    /**
     * 显示Asset中的图片
     *
     * @param assetResName asset中图片文件的名称
     * @param sketchView   Sketch 对 ImageView 的规范接口，Sketch 对 ImageView 的规范接口，默认实现是 SketchImageView
     * @return DisplayHelper 你可以继续通过 DisplayHelper 设置参数，最后调用其 commit() 方法提交
     */
    public DisplayHelper displayFromAsset(String assetResName, SketchView sketchView) {
        String uri = AssetUriModel.makeUri(assetResName);
        return configuration.getHelperFactory().getDisplayHelper(this, uri, sketchView);
    }

    /**
     * 显示资源中的图片
     *
     * @param drawableResId 图片资源的ID
     * @param sketchView    Sketch 对 ImageView 的规范接口，Sketch 对 ImageView 的规范接口，默认实现是 SketchImageView
     * @return DisplayHelper 你可以继续通过 DisplayHelper 设置参数，最后调用其 commit() 方法提交
     */
    public DisplayHelper displayFromResource(int drawableResId, SketchView sketchView) {
        String uri = DrawableUriModel.makeUri(drawableResId);
        return configuration.getHelperFactory().getDisplayHelper(this, uri, sketchView);
    }

    /**
     * 显示来自 ContentProvider 的图片
     *
     * @param uri        来自 ContentProvider 的图片的 Uri，会通过 ContentResolver().openInputStream(Uri) 方法来读取图片
     * @param sketchView Sketch 对 ImageView 的规范接口，Sketch 对 ImageView 的规范接口，默认实现是 SketchImageView
     * @return DisplayHelper 你可以继续通过 DisplayHelper 设置参数，最后调用其 commit() 方法提交
     */
    public DisplayHelper displayFromContent(Uri uri, SketchView sketchView) {
        return configuration.getHelperFactory().getDisplayHelper(this, uri != null ? uri.toString() : null, sketchView);
    }

    /**
     * 显示已安装 APK 文件的图标
     *
     * @param filePath   APK 的路径
     * @param sketchView Sketch 对 ImageView 的规范接口，默认实现是 SketchImageView
     * @return DisplayHelper 你可以继续通过 DisplayHelper 设置参数，最后调用其 commit() 方法提交
     */
    @SuppressWarnings("unused")
    public DisplayHelper displayApkIcon(String filePath, SketchView sketchView) {
        String uri = ApkIconUriModel.makeUri(filePath);
        return configuration.getHelperFactory().getDisplayHelper(this, uri, sketchView);
    }

    /**
     * 显示 APP 的图标
     *
     * @param packageName APP 的包名
     * @param versionCode APP 的版本号
     * @param sketchView  Sketch 对 ImageView 的规范接口，默认实现是 SketchImageView
     * @return DisplayHelper 你可以继续通过 DisplayHelper 设置参数，最后调用其 commit() 方法提交
     */
    public DisplayHelper displayAppIcon(String packageName, int versionCode, SketchView sketchView) {
        String uri = AppIconUriModel.makeUri(packageName, versionCode);
        return configuration.getHelperFactory().getDisplayHelper(this, uri, sketchView);
    }

    /**
     * 修整内存缓存，4.0 以下你需要重写 Application 的 onTrimMemory(int) 方法，然后调用这个方法
     *
     * @param level 修剪级别，对应APP的不同状态，对应 ComponentCallbacks2 里的常量
     * @see android.content.ComponentCallbacks2
     */
    public void onTrimMemory(int level) {
        // ICE_CREAM_SANDWICH以上版本已经自动注册了onTrimMemory监听，因此无需再在你的Application的onTrimMemory方法中调用此方法
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            StackTraceElement[] stackTraceElements = new Exception().getStackTrace();
            if (!SketchUtils.invokeIn(stackTraceElements, Application.class, "onTrimMemory")) {
                return;
            }
        }

        SLog.w(null, "Trim of memory, level= %s", SketchUtils.getTrimLevelName(level));

        configuration.getMemoryCache().trimMemory(level);
        configuration.getBitmapPool().trimMemory(level);
    }

    /**
     * 当内存低时直接清空全部内存缓存，4.0 以下你需要重写 Application 的 onLowMemory 方法，然后调用这个方法
     */
    public void onLowMemory() {
        // ICE_CREAM_SANDWICH以上版本已经自动注册了onLowMemory监听，因此无需再在你的Application的onLowMemory方法中调用此方法
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            StackTraceElement[] stackTraceElements = new Exception().getStackTrace();
            if (!SketchUtils.invokeIn(stackTraceElements, Application.class, "onLowMemory")) {
                return;
            }
        }

        SLog.w(null, "Memory is very low, clean memory cache and bitmap pool");

        configuration.getMemoryCache().clear();
        configuration.getBitmapPool().clear();
    }
}