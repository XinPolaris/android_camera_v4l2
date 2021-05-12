//
// Created by Hsj on 2021/5/11.
//

#include "Common.h"
#include <ctime>

static JavaVM *jvm;

long long timeMs() {
    struct timeval time;
    gettimeofday(&time, NULL);
    return (long long)time.tv_sec * 1000 + time.tv_usec / 1000;
}

long long timeUs() {
    struct timeval time;
    gettimeofday(&time, NULL);
    return (long long)time.tv_sec * 1000000 + time.tv_usec;
}

void setVM(JavaVM *vm) {
    jvm = vm;
}

JavaVM *getVM() {
    return jvm;
}

JNIEnv *getEnv() {
    JNIEnv *env = nullptr;
    if (JNI_OK == jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6)) {
        return env;
    } else {
        return nullptr;
    }
}

