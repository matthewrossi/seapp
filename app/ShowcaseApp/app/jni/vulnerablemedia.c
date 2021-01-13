#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <jni.h>

jobject
Java_com_example_showcaseapp_UseCase3Activity_getHandleFromJNI
( JNIEnv* env, jobject thiz, jstring jservicename)
{
  const char* bb = (*env)->GetStringUTFChars(env, jservicename, 0);
  jobject hndl = NULL;
  jclass ctx = (*env)->FindClass(env, "android/content/Context");
  jfieldID fid = (*env)->GetStaticFieldID(env, ctx, bb, "Ljava/lang/String;");
  jstring str = (jstring) (*env)->GetStaticObjectField(env, ctx, fid);
  jmethodID mid = (*env)->GetMethodID(env, ctx, "getSystemService","(Ljava/lang/String;)Ljava/lang/Object;");
  hndl = (*env)->CallObjectMethod(env, thiz, mid, str);
  return hndl;
}

jint
Java_com_example_showcaseapp_UseCase3Activity_doSomeAlgebraInJNI
  (JNIEnv* env, jobject thiz, jintArray a)
{
  jint prod = 1;
  jint i = 0;
  jsize len = (*env)->GetArrayLength(env, a);
  jint *body = (*env)->GetIntArrayElements(env, a, 0);

  for (i=0; i<len; i++)
      prod *= body[i];

  (*env)->ReleaseIntArrayElements(env, a, body, 0);

  return prod;
}


