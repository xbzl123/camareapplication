//
// Created by Administrator on 2023/9/12.
//


#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include "android/log.h"
#include <string>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
extern "C" {
#include "x264.h"
#include "jni.h"
}

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "h264-encode", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO  , "h264-encode", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "h264-encode", __VA_ARGS__)

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_camareapplication_MainActivity_h264EncodeEncode(
        JNIEnv *env, jclass thiz,
        jint width, jint height,
        jstring yuv_path, jstring h264_path, jint yuv_csp) {

    int ret = 0;
    pid_t pid = fork();
    LOGI("the num of pid is %d",pid);

    if (pid == 0) { // 子进程
        if (width == 0 || height == 0) {
            LOGE("width or height cannot be zero!");
        }
        const char *yuv_file_path = env->GetStringUTFChars(yuv_path, JNI_FALSE);
        const char *h264_file_path = env->GetStringUTFChars(h264_path, JNI_FALSE);

        if (!yuv_file_path) {
            LOGE("yuv path cannot be null");
            return -1;
        }
        if (!h264_file_path) {
            LOGE("h264 path cannot be null");
            return -1;
        }
        // 打开yuv
        FILE *yuv_file = fopen(yuv_file_path, "rb");
        if (yuv_file == NULL) {
            LOGE("cannot open yuv file");
            return -1;
        }
        FILE *h264_file = fopen(h264_file_path, "wb");
        if (h264_file == NULL) {
            LOGE("cannot open h264 file");
            return -1;
        }
        // 设置x264处理的yuv格式默认为YUV420
        int csp = X264_CSP_I420;
        switch (yuv_csp) {
            case 0:
                csp = X264_CSP_I420;
                break;
            case 1:
                csp = X264_CSP_I422;
                break;
            case 2:
                csp = X264_CSP_I444;
                break;
            default:
                csp = X264_CSP_I420;
        }

        LOGI("the params is success:\n %dx%d %s %s:", width, height, yuv_file_path, h264_file_path);

        int frame_number = 0;
        // 处理h264单元数据
        int i_nal = 0;
        x264_nal_t *nal = NULL;
        // x264
        x264_t *h = NULL;
        x264_param_t *param = (x264_param_t *) malloc(sizeof(x264_param_t));
        x264_picture_t *pic_in = (x264_picture_t *) (malloc(sizeof(x264_picture_t)));
        x264_picture_t *pic_out = (x264_picture_t *) (malloc(sizeof(x264_picture_t)));

        // 初始化编码参数
        x264_param_default(param);
        param->i_width = width;
        param->i_height = height;
        param->i_csp = csp;
        // 配置处理级别
        x264_param_apply_profile(param, x264_profile_names[2]);
        // 通过配置的参数打开编码器
        h = x264_encoder_open(param);

        x264_picture_init(pic_out);
        x264_picture_alloc(pic_in, param->i_csp, param->i_width, param->i_height);
        // 编码前每一帧的字节大小
        int size = param->i_width * param->i_height;

        // 计算视频帧数
        fseek(yuv_file, 0, SEEK_END);
        switch (csp) {
            case X264_CSP_I444:
                // YUV444
                frame_number = ftell(yuv_file) / (size * 3);
                break;
            case X264_CSP_I422:
                // YUV422
                frame_number = ftell(yuv_file) / (size * 2);
                break;
            case X264_CSP_I420:
                //YUV420
                frame_number = ftell(yuv_file) / (size * 3 / 2);
                break;
            default:
                LOGE("Colorspace Not Support.");
                return -1;
        }
        fseek(yuv_file, 0, SEEK_SET);
        // 循环执行编码
        for (int i = 0; i < frame_number; i++) {
            switch (csp) {
                case X264_CSP_I444:
                    fread(pic_in->img.plane[0], size, 1, yuv_file);
                    fread(pic_in->img.plane[1], size, 1, yuv_file);
                    fread(pic_in->img.plane[2], size, 1, yuv_file);
                    break;
                case X264_CSP_I422:
                    fread(pic_in->img.plane[0], size, 1, yuv_file);
                    fread(pic_in->img.plane[1], size / 2, 1, yuv_file);
                    fread(pic_in->img.plane[2], size / 2, 1, yuv_file);
                    break;
                case X264_CSP_I420:
                    fread(pic_in->img.plane[0], size, 1, yuv_file);
                    fread(pic_in->img.plane[1], size / 4, 1, yuv_file);
                    fread(pic_in->img.plane[2], size / 4, 1, yuv_file);
                    break;
            }
            pic_in->i_pts = i;
            // 对每一帧执行编码
            ret = x264_encoder_encode(h, &nal, &i_nal, pic_in, pic_out);
            if (ret < 0) {
                LOGE("x264 encode error");
                return -1;
            }
            LOGI("encode frame:%5d", i);
            // 将编码数据循环写入目标文件
            for (int j = 0; j < i_nal; ++j) {
                fwrite(nal[j].p_payload, 1, nal[j].i_payload, h264_file);
            }
        }

        // 冲刷缓冲区，不执行可能造成数据不完整
        int i = 0;
        while (1) {
            ret = x264_encoder_encode(h, &nal, &i_nal, NULL, pic_out);
            if (ret == 0) {
                break;
            }
            LOGD("flush 1 frame");
            // 将编码数据循环写入目标文件
            for (int j = 0; j < i_nal; ++j) {
                fwrite(nal[j].p_payload, 1, nal[j].i_payload, h264_file);
            }
            i++;
        }

        x264_picture_clean(pic_in);
        x264_encoder_close(h);
        // 释放分配的空间
        free(pic_in);
        free(pic_out);
        free(param);
        // 关闭文件输入
        fclose(yuv_file);
        fclose(h264_file);
    }
    return ret;
}
extern "C" JNIEXPORT void JNICALL
Java_com_example_camareapplication_MainActivity_encodeH264Pic(JNIEnv *env, jobject instance,
                                                              jint inputWidth, jint inputHeight,
                                                              jstring inputPath, jstring outputPath) {
    const char *inputPathStr = env->GetStringUTFChars(inputPath, 0);
    const char *outputPathStr = env->GetStringUTFChars(outputPath, 0);

// 初始化 x264 库
//    x264_t *encoder = x264_encoder_open(X264_CODEC_ID_H264, inputWidth, inputHeight);
//    if (!encoder) {
//        LOGE("Failed to open x264 encoder");
//        return;
//    }
//
//// 设置编码参数
//    x264_param_t params;
//    x264_param_default_preset(&params, "veryfast", "h264");
//    params.i_width = inputWidth;
//    params.i_height = inputHeight;
//    params.i_fps_num = 30;
//    params.i_fps_den = 1;
//    params.i_bitrate = 500000; // 设置码率
//
//// 打开输出文件
//    FILE *outputFile = fopen(outputPathStr, "wb");
//    if (!outputFile) {
//        LOGE("Failed to open output file");
//        x264_encoder_close(encoder);
//        return;
//    }
//
//// 开始编码
//    x264_encoder_encode(encoder, &params, NULL, NULL);
//
//// 写入输出文件
//    x264_nal_t *nal = x264_encoder_nal_out(encoder);
//    while (nal) {
//        fwrite(nal->p_payload, 1, nal->i_payload, outputFile);
//        nal = x264_encoder_nal_out(encoder);
//    }
//
//// 关闭文件
//    fclose(outputFile);
//
//// 关闭 x264 库
//    x264_encoder_close(encoder);
//
//// 释放资源
//    env->ReleaseStringUTFChars(inputPath, inputPathStr);
//    env->ReleaseStringUTFChars(outputPath, outputPathStr);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_camareapplication_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    pid_t pid = fork();
    LOGI("the num of pid is %d",pid);

    if (pid == 0) { // 子进程
    }

    return env->NewStringUTF(hello.c_str());
}