#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <jni.h>

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

// simple utility to get a handle to a service
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

// function to demonstrate the confinement of the .so shared library
jobjectArray
Java_com_example_showcaseapp_UseCase3Activity_doBindProcessToNetwork
( JNIEnv* env, jobject thiz, jstring jservicename)
{
  // get a handle to the connectivity manager
  const char* bb = (*env)->GetStringUTFChars(env, jservicename, 0);
  jobject hndl = NULL;
  jclass ctx = (*env)->FindClass(env, "android/content/Context");
  jfieldID fid = (*env)->GetStaticFieldID(env, ctx, bb, "Ljava/lang/String;");
  jstring str = (jstring) (*env)->GetStaticObjectField(env, ctx, fid);
  jmethodID mid = (*env)->GetMethodID(env, ctx, "getSystemService","(Ljava/lang/String;)Ljava/lang/Object;");
  hndl = (*env)->CallObjectMethod(env, thiz, mid, str);

  // get all available networks
  jclass con_manager = (*env)->GetObjectClass(env, hndl);
  jmethodID mid_getc = (*env)->GetMethodID(env, con_manager, "getAllNetworks", "()[Landroid/net/Network;");
  jobjectArray nets = (*env)->CallObjectMethod(env, hndl, mid_getc);

  // try to bindProcessToNetwork via current net obj
  int net_len = (*env)->GetArrayLength(env, nets);
  jmethodID mid_bindp = (*env)->GetMethodID(env, con_manager, "bindProcessToNetwork", "(Landroid/net/Network;)Z");
  jboolean success = 0;
  for (int i=0; i < net_len; i++){
      jobject curr_net = (*env)->GetObjectArrayElement(env, nets, i);
      success = (*env)->CallBooleanMethod(env, hndl, mid_bindp, curr_net);
      if (success)
          break; // the current process was succesfully bound to the network
  }

  // build result
  jclass classObject = (*env)->FindClass(env, "java/lang/Object");
  jobjectArray outJNIArray = (*env)->NewObjectArray(env, 2, classObject, NULL);
  (*env)->SetObjectArrayElement(env, outJNIArray, 0, hndl);
  if (success)
      (*env)->SetObjectArrayElement(env, outJNIArray, 1, hndl);
  else
      (*env)->SetObjectArrayElement(env, outJNIArray, 1, NULL);

  return outJNIArray;
}


