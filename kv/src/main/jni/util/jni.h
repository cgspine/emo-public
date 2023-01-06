#ifndef EMO_JNI_H
#define EMO_JNI_H

#include <jni.h>
#include <string>

# define N_ELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

namespace EmoKV {

    static std::string jstringToString(JNIEnv *env, jstring jstr) {
        if(jstr){
            const char *chars = env->GetStringUTFChars(jstr, nullptr);
            if(chars){
                std::string str(chars);
                env->ReleaseStringUTFChars(jstr, chars);
                return str;
            }
        }
        return "";
    }

    static jstring string2jstring(JNIEnv *env, const std::string &str) {
        return env->NewStringUTF(str.c_str());
    }

    static std::string stringFieldValue(JNIEnv *env, jobject intance, const char *fieldName) {
        jclass cls = env->GetObjectClass(intance);
        jfieldID field = env->GetFieldID(cls, fieldName, "Ljava/lang/String;");
        auto fieldValue = (jstring) (env->GetObjectField(intance, field));
        return jstringToString(env, fieldValue);
    }

    static int intFieldValue(JNIEnv *env, jobject intance, const char *fieldName) {
        jclass cls = env->GetObjectClass(intance);
        jfieldID field = env->GetFieldID(cls, fieldName, "I");
        return env->GetIntField(intance, field);
    }

    static bool boolFieldValue(JNIEnv *env, jobject intance, const char *fieldName) {
        jclass cls = env->GetObjectClass(intance);
        jfieldID field = env->GetFieldID(cls, fieldName, "Z");
        return env->GetBooleanField(intance, field);
    }
}

#endif //EMO_JNI_H
