#ifndef NODEONANDROID_NATIVE_LIB_H
#define NODEONANDROID_NATIVE_LIB_H

#include <jni.h>
#include <android/log.h>
#include <vector>

#define LOGD(...) (__android_log_print(ANDROID_LOG_DEBUG, "SOULSIGN_NODE", __VA_ARGS__))

extern "C" {
JNIEXPORT void JNICALL
Java_cn_inu1255_soulsign_NodeService_startNode(JNIEnv *env, jobject instance, jobjectArray args);
}

namespace nodeonandroid {
    void redirectStreamsToPipe();

    void startLoggingFromPipe();

    std::vector<char> makeContinuousArray(JNIEnv *env, jobjectArray fromArgs);

    std::vector<char *> getArgv(std::vector<char> &fromContinuousArray);
}

#endif