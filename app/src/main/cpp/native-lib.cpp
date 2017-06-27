#include <jni.h>
#include <string>

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <android/log.h>
#define TAG "jnicode"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)

static AAsset* asset;       // the video source
static long inStreamSize;   // size of the video

/**
 * Our com.google.android.exoplayer2.upstream.DataSource.open()
 *
 * @param env the JNIEnv
 * @param am AssetManager from Activity (for opening the ts file)
 * @param filename Of the ts file to open (w/in assets)
 * @return size of file in bytes
 */
extern "C"
JNIEXPORT jlong JNICALL
Java_com_badquery_exojni_MainActivity_openFile(JNIEnv* env, jclass, jobject am, jstring filename) {
    const char *utf8 = env->GetStringUTFChars(filename, NULL);
    LOGV("opening %s", utf8);
    AAssetManager* mgr = AAssetManager_fromJava(env, am);
    asset = AAssetManager_open(mgr, utf8, AASSET_MODE_STREAMING);
    env->ReleaseStringUTFChars(filename, utf8);
    if (NULL == asset) {
        LOGV("Asset not found");
        return JNI_FALSE;
    }
    inStreamSize = AAsset_getLength(asset);
    inStreamSize -= (inStreamSize%188);
    LOGV("asset size: %lu",inStreamSize);
    return inStreamSize;
}

/**
* Our com.google.android.exoplayer2.upstream.DataSource.read()
* There are more efficient means, but this is pretty legit for a demo
* (https://developer.android.com/training/articles/perf-jni.html#faq_sharing)
*
* @param env the JNIEnv
* @param buffer Write to this byte[]
* @param offset Start writing at byte[offset]
* @param readLength Write until byte[offset+readLength]
* @return Number of bytes written into the buffer
*/
extern "C"
JNIEXPORT jint JNICALL
Java_com_badquery_exojni_MainActivity_readFile(JNIEnv *env,jclass,jbyteArray buffer, jint offset, jint readLength){
    if(!inStreamSize) {
        return 0; // all done reading for this demo...
    }
    else if(inStreamSize < readLength) {
        readLength = inStreamSize; // almost done reading...
    }
    char *ret = new char[readLength]; // inefficient but its a demo...
    AAsset_read(asset,ret,readLength);
    env->SetByteArrayRegion( buffer, offset, readLength, (const jbyte*)ret );
    delete[] ret;
    inStreamSize -= readLength;
    return readLength;
}

/**
 * Our com.google.android.exoplayer2.upstream.DataSource.close()
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_badquery_exojni_MainActivity_closeFile(JNIEnv*,jclass){
    LOGV("closing");
    if(asset) {
        AAsset_close(asset);
        asset = NULL;
    }
}