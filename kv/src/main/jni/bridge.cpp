//
// Created by 陈古松 on 2021/1/22.
//

#include <jni.h>
#include "util/jni.h"
#include "util/log.h"
#include "KV.h"
#include "Buf.h"
#include "atomic"
#include <map>

using namespace EmoKV;

static int registerNativeMethods(
        JNIEnv* env,
        const char* className,
        JNINativeMethod* methods,
        int methodNum)
{
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == nullptr) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, methods, methodNum) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static jlong initKV(
        JNIEnv *env,
        jobject instance,
        jstring dir,
        jlong index_init_space,
        jlong key_init_space,
        jlong value_init_space,
        jfloat hash_factor,
        jint update_count_to_auto_compact
){
    auto kv_dir = jstringToString(env, dir);
    KV* kv = KV::make(
            kv_dir,
            index_init_space,
            key_init_space,
            value_init_space,
            hash_factor,
            update_count_to_auto_compact
    );
    return (jlong) kv;
}

static jbyteArray get(JNIEnv *env, jobject instance, jlong handle, jbyteArray array){
    KV* kv =  reinterpret_cast<KV *>(handle);
    jsize key_len = env->GetArrayLength(array);
    auto *key_ptr = env->GetByteArrayElements(array, nullptr);
    env->GetByteArrayRegion(array, 0, key_len, key_ptr);

    std::unique_ptr<Buf> key(new Buf(reinterpret_cast<const uint8_t *>(key_ptr), (size_t)key_len, false));
    std::unique_ptr<Buf> ret = kv->Get(std::move(key));
    jbyteArray jret = nullptr;
    if(ret != nullptr){
        auto ret_len = (jsize) ret->len();
        jret = env->NewByteArray(ret_len);
        env->SetByteArrayRegion(
                jret, 0, ret_len, reinterpret_cast<const jbyte*>(ret->ptr()));
    }
    env->ReleaseByteArrayElements(array, key_ptr, 0);
    return jret;
}

static jboolean put(JNIEnv *env, jobject instance, jlong handle, jbyteArray jkey, jbyteArray jvalue){
    KV* kv =  reinterpret_cast<KV *>(handle);
    jsize key_len = env->GetArrayLength(jkey);
    jsize value_len = env->GetArrayLength(jvalue);
    auto *key_ptr = env->GetByteArrayElements(jkey, nullptr);
    auto *value_ptr = env->GetByteArrayElements(jvalue, nullptr);
    std::unique_ptr<Buf> key(new Buf(reinterpret_cast<const uint8_t *>(key_ptr), (size_t)key_len, false));
    std::unique_ptr<Buf> value(new Buf(reinterpret_cast<const uint8_t *>(value_ptr), (size_t)value_len, false));
    bool ret = kv->Put(std::move(key), std::move(value));
    env->ReleaseByteArrayElements(jkey, key_ptr, 0);
    env->ReleaseByteArrayElements(jvalue, value_ptr, 0);
    return ret;
}

static void del(JNIEnv *env, jobject instance, jlong handle, jbyteArray array){
    KV* kv =  reinterpret_cast<KV *>(handle);
    jsize key_len = env->GetArrayLength(array);
    auto *key_ptr = env->GetByteArrayElements(array, nullptr);
    env->GetByteArrayRegion(array, 0, key_len, key_ptr);

    std::unique_ptr<Buf> key(new Buf(reinterpret_cast<const uint8_t *>(key_ptr), (size_t)key_len, false));
    kv->Del(std::move(key));
    env->ReleaseByteArrayElements(array, key_ptr, 0);
}

static void compact(JNIEnv *env, jobject instance, jlong handle){
    KV* kv =  reinterpret_cast<KV *>(handle);
    kv->Compact();
}

static void close(JNIEnv *env, jobject instance, jlong handle){
    KV* kv =  reinterpret_cast<KV *>(handle);
    delete kv;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void* reserved) {
    LOG_I("Emo KV JNI OnLoad");
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) (&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    JNINativeMethod emoKVMethods[] = {
            {"nInit", "(Ljava/lang/String;JJJFI)J", (void *) initKV},
            {"nGet", "(J[B)[B", (void *) get},
            {"nPut", "(J[B[B)Z", (void *) put},
            {"nDelete", "(J[B)V", (void *) del},
            {"nCompact", "(J)V", (void *) compact},
            {"nClose", "(J)V", (void *) close}
    };

    registerNativeMethods(env,"cn/qhplus/emo/kv/EmoKV",emoKVMethods, N_ELEM(emoKVMethods)
    );

    return JNI_VERSION_1_6;

}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    LOG_I("Emo KV JNI_OnUnload");
}