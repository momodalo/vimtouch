/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "TermExec"

#include <jni.h>
#include "common.h"
#include <linux/threads.h>
#include <pthread.h>

#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <termios.h>

extern "C" {
#include "vim.h"
#include "gpm.h"
int AndroidMain(int argc, char**argv);
extern int fake_gpm_fd[2];
jmp_buf longjmp_env;

void android_exit(int exit_value)
{
    // TODO: use exit_value
    LOGE("android_exit()");
    longjmp(longjmp_env, 13);
}
};

static JNIEnv* global_env;
static JavaVM* global_vm;
static jclass class_fileDescriptor;
static jfieldID field_fileDescriptor_descriptor;
static jmethodID method_fileDescriptor_init;
static jclass class_Exec;
static jmethodID method_Exec_showDialog;
static jmethodID method_Exec_getDialogState;
static int thread_exit_val = 0;

void pth_exit(int n)
{
    thread_exit_val = n;
    pthread_exit(&thread_exit_val);
}


class String8 {
public:
    String8() {
        mString = 0;
    }
    
    ~String8() {
        if (mString) {
            free(mString);
        }
    }

    void set(const uint16_t* o, size_t numChars) {
        mString = (char*) malloc(numChars + 1);
        for (size_t i = 0; i < numChars; i++) {
            mString[i] = (char) o[i];
        }
        mString[numChars] = '\0';
    }
    
    const char* string() {
        return mString;
    }
private:
    char* mString;
};

static void *thread_wrapper ( void* value)
{
    LOGE("thread wrapper");
    int pts;
    char** thread_arg = (char**)value;

    global_vm->AttachCurrentThread(&global_env, NULL);

        setsid();
        
        pts = open(thread_arg[0], O_RDWR);
        if(pts < 0){
            LOGE("PTY open failed");
            pth_exit(-1);
        }

        dup2(pts, 0);
        dup2(pts, 1);
        dup2(pts, 2);

        char* argv[3];
        argv[0] = (char*)"vim";
        argv[1] = (char*)thread_arg[1];
        argv[2] = NULL;

        int val = setjmp(longjmp_env);

        if (val == 0)
            AndroidMain(argv[1]?2:1, (char**)argv);

        free(thread_arg[0]);
        free(thread_arg[1]);
        free(thread_arg);

    global_vm->DetachCurrentThread();
    LOGE("thread leave");
    pth_exit(0);
}

static int create_subprocess(const char *cmd, const char *arg0, const char *arg1, char **envp,
    int* pProcessId)
{
    char *devname;
    int ptm;
    pid_t pid;
    char tmpdir[PATH_MAX];
    char terminfodir[PATH_MAX];

    sprintf((char*)default_vimruntime_dir, "%s/vim/", cmd);
    sprintf((char*)default_vim_dir, "%s/vim/", cmd);
    sprintf(tmpdir, "%s/tmp", cmd);
    sprintf(terminfodir, "%s/terminfo", cmd);

    pipe(fake_gpm_fd);

    ptm = open("/dev/ptmx", O_RDWR); // | O_NOCTTY);
    if(ptm < 0){
        LOGE("[ cannot open /dev/ptmx - %s ]\n",strerror(errno));
        return -1;
    }
    fcntl(ptm, F_SETFD, FD_CLOEXEC);

    if(grantpt(ptm) || unlockpt(ptm) ||
       ((devname = (char*) ptsname(ptm)) == 0)){
        LOGE("[ trouble with /dev/ptmx - %s ]\n", strerror(errno));
        return -1;
    }

    setenv("TMPDIR", tmpdir, 1);
    setenv("TERM", "linux", 1);
    //setenv("HOME", cmd, 1);
    setenv("TERMINFO", terminfodir, 1);

    if (envp) {
        for (; *envp; ++envp) {
            putenv(*envp);
        }
    }


    char** thread_arg = (char**)malloc(sizeof(char*)*2);
    thread_arg[0] = strdup(devname);
    if(arg0)
        thread_arg[1] = strdup(arg0);
    else 
        thread_arg[1] = NULL;

    pthread_t thread_id;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

    pthread_create(&thread_id, &attr, thread_wrapper, (void*)thread_arg);
    pthread_attr_destroy(&attr);
    *pProcessId = (int) thread_id;

    return ptm;

    /*
    pid = fork();
    if(pid < 0) {
        LOGE("- fork failed: %s -\n", strerror(errno));
        return -1;
    }

    if(pid == 0){
        close(ptm);

        int pts;

        setsid();
        
        pts = open(devname, O_RDWR);
        if(pts < 0) exit(-1);

        dup2(pts, 0);
        dup2(pts, 1);
        dup2(pts, 2);

        char* argv[3];
        argv[0] = (char*)"vim";
        argv[1] = (char*)arg0;
        argv[2] = NULL;

        AndroidMain(2, (char**)argv);
        exit(-1);
    } else {
        *pProcessId = (int) pid;
        return ptm;
    }
    */
}

static int throwOutOfMemoryError(JNIEnv *env, const char *message)
{
    jclass exClass;
    const char *className = "java/lang/OutOfMemoryError";

    exClass = env->FindClass(className);
    return env->ThrowNew(exClass, message);
}


static jobject vimtouch_Exec_createSubProcess(JNIEnv *env, jobject clazz,
    jstring cmd, jstring arg0, jstring arg1, jobjectArray envVars, jintArray processIdArray)
{
    const jchar* str = cmd ? env->GetStringCritical(cmd, 0) : 0;
    String8 cmd_8;
    if (str) {
        cmd_8.set(str, env->GetStringLength(cmd));
        env->ReleaseStringCritical(cmd, str);
    }

    str = arg0 ? env->GetStringCritical(arg0, 0) : 0;
    const char* arg0Str = 0;
    String8 arg0_8;
    if (str) {
        arg0_8.set(str, env->GetStringLength(arg0));
        env->ReleaseStringCritical(arg0, str);
        arg0Str = arg0_8.string();
    }

    str = arg1 ? env->GetStringCritical(arg1, 0) : 0;
    const char* arg1Str = 0;
    String8 arg1_8;
    if (str) {
        arg1_8.set(str, env->GetStringLength(arg1));
        env->ReleaseStringCritical(arg1, str);
        arg1Str = arg1_8.string();
    }

    int size = envVars ? env->GetArrayLength(envVars) : 0;
    char **envp = NULL;
    String8 tmp_8;
    if (size > 0) {
        envp = (char **)malloc((size+1)*sizeof(char *));
        if (!envp) {
            throwOutOfMemoryError(env, "Couldn't allocate envp array");
            return NULL;
        }
        for (int i = 0; i < size; ++i) {
            jstring var = reinterpret_cast<jstring>(env->GetObjectArrayElement(envVars, i));
            str = env->GetStringCritical(var, 0);
            if (!str) {
                throwOutOfMemoryError(env, "Couldn't get env var from array");
                return NULL;
            }
            tmp_8.set(str, env->GetStringLength(var));
            env->ReleaseStringCritical(var, str);
            envp[i] = strdup(tmp_8.string());
        }
        envp[size] = NULL;
    }


    int procId;
    int ptm = create_subprocess(cmd_8.string(), arg0Str, arg1Str, envp, &procId);
    
    if (processIdArray) {
        int procIdLen = env->GetArrayLength(processIdArray);
        if (procIdLen > 0) {
            jboolean isCopy;
    
            int* pProcId = (int*) env->GetPrimitiveArrayCritical(processIdArray, &isCopy);
            if (pProcId) {
                *pProcId = procId;
                env->ReleasePrimitiveArrayCritical(processIdArray, pProcId, 0);
            }
        }
    }
    
    jobject result = env->NewObject(class_fileDescriptor, method_fileDescriptor_init);
    
    if (!result) {
        LOGE("Couldn't create a FileDescriptor.");
    }
    else {
        env->SetIntField(result, field_fileDescriptor_descriptor, ptm);
    }
    
    return result;
}

static void vimtouch_Exec_setPtyWindowSize(JNIEnv *env, jobject clazz,
    jobject fileDescriptor, jint row, jint col, jint xpixel, jint ypixel)
{
    int fd;
    struct winsize sz;

    fd = env->GetIntField(fileDescriptor, field_fileDescriptor_descriptor);

    if (env->ExceptionOccurred() != NULL) {
        return;
    }
    
    sz.ws_row = row;
    sz.ws_col = col;
    sz.ws_xpixel = xpixel;
    sz.ws_ypixel = ypixel;
    
    ioctl(fd, TIOCSWINSZ, &sz);
    out_flush();
    shell_resized_check();
    redraw_later(CLEAR);
    //update_screen(CLEAR);
    //setcursor();
    //out_flush();
}

static void vimtouch_Exec_setPtyUTF8Mode(JNIEnv *env, jobject clazz,
    jobject fileDescriptor, jboolean utf8Mode)
{
    int fd;
    struct termios tios;

    fd = env->GetIntField(fileDescriptor, field_fileDescriptor_descriptor);

    if (env->ExceptionOccurred() != NULL) {
        return;
    }

    tcgetattr(fd, &tios);
    if (utf8Mode) {
        tios.c_iflag |= IUTF8;
    } else {
        tios.c_iflag &= ~IUTF8;
    }
    tcsetattr(fd, TCSANOW, &tios);
}

static void updateScreen()
{
    //redraw_later(NOT_VALID);
    update_screen(0);
    //setcursor();
    out_flush();
}

static void vimtouch_Exec_moveCursor(JNIEnv *env, jobject clazz,
    jint row, jint col) {
    //windgoto(row, col);
    Gpm_Event e;
    e.x = col;
    e.y = row;
    e.type = GPM_DOWN;
    e.buttons = GPM_B_LEFT;
    e.clicks = 0;
    e.modifiers = 0;

    write(fake_gpm_fd[1],(void*)&e, sizeof(e));
    e.type = GPM_UP;
    e.buttons = GPM_B_LEFT;
    write(fake_gpm_fd[1],(void*)&e, sizeof(e));
}

static int vimtouch_Exec_scrollBy(JNIEnv *env, jobject clazz,
    jint line) {
    int do_scroll = line;
    scroll_redraw(do_scroll > 0, do_scroll>0?do_scroll:-do_scroll);
    return line;
}

static void vimtouch_Exec_updateScreen(JNIEnv *env, jobject clazz) {
    updateScreen();
}

static void vimtouch_Exec_doCommand(JNIEnv *env, jobject clazz, jstring cmd){
    if(!cmd) return;
    const char* str = env->GetStringUTFChars(cmd, NULL);
    if(!str) return;

    do_cmdline_cmd((char_u *)str);

    env->ReleaseStringUTFChars(cmd, str);
}

static int vimtouch_Exec_waitFor(JNIEnv *env, jobject clazz, jint procId)
{
    int* status;
    pthread_join((pthread_t)procId, (void**)&status);

    return *status;
}

static void vimtouch_Exec_close(JNIEnv *env, jobject clazz, jobject fileDescriptor)
{
    int fd = env->GetIntField(fileDescriptor, field_fileDescriptor_descriptor);

    if (env->ExceptionOccurred() != NULL) {
        return;
    }

    close(fd);
}

extern "C" {

int vimtouch_Exec_getDialogState() 
{
    jint result = global_env->CallStaticIntMethod(class_Exec, method_Exec_getDialogState);
    return result;
}

void vimtouch_Exec_showDialog(
	int	type,
	char_u	*title,
	char_u	*message,
	char_u	*buttons,
	int	default_button,
	char_u	*textfield){

    jstring titleStr = global_env->NewStringUTF((const char*)(title));
    jstring messageStr = global_env->NewStringUTF((const char*)(message));
    jstring buttonsStr = global_env->NewStringUTF((const char*)(buttons));
    jstring textfieldStr = global_env->NewStringUTF((const char*)(textfield));

    global_env->CallStaticVoidMethod(class_Exec, method_Exec_showDialog, 
            type, titleStr, messageStr, buttonsStr, default_button, textfieldStr);

    global_env->DeleteLocalRef(titleStr);
    global_env->DeleteLocalRef(messageStr);
    global_env->DeleteLocalRef(buttonsStr);
    global_env->DeleteLocalRef(textfieldStr);
}

}

static int register_FileDescriptor(JNIEnv *env)
{
    jclass clazz = env->FindClass("java/io/FileDescriptor");

    class_fileDescriptor = (jclass)env->NewGlobalRef(clazz);

    if (class_fileDescriptor == NULL) {
        LOGE("Can't find java/io/FileDescriptor");
        return -1;
    }

    field_fileDescriptor_descriptor = env->GetFieldID(class_fileDescriptor, "descriptor", "I");

    if (field_fileDescriptor_descriptor == NULL) {
        LOGE("Can't find FileDescriptor.descriptor");
        return -1;
    }

    method_fileDescriptor_init = env->GetMethodID(class_fileDescriptor, "<init>", "()V");
    if (method_fileDescriptor_init == NULL) {
        LOGE("Can't find FileDescriptor.init");
        return -1;
     }
     return 0;
}


static const char *classPathName = "net/momodalo/app/vimtouch/Exec";

static JNINativeMethod method_table[] = {
    { "createSubprocess", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[I)Ljava/io/FileDescriptor;",
        (void*) vimtouch_Exec_createSubProcess },
    { "setPtyWindowSize", "(Ljava/io/FileDescriptor;IIII)V",
        (void*) vimtouch_Exec_setPtyWindowSize},
    { "setPtyUTF8Mode", "(Ljava/io/FileDescriptor;Z)V",
        (void*) vimtouch_Exec_setPtyUTF8Mode},
    { "moveCursor", "(II)V",
        (void*) vimtouch_Exec_moveCursor},
    { "scrollBy", "(I)I",
        (void*) vimtouch_Exec_scrollBy},
    { "updateScreen", "()V",
        (void*) vimtouch_Exec_updateScreen},
    { "doCommand", "(Ljava/lang/String;)V",
        (void*) vimtouch_Exec_doCommand},
    { "waitFor", "(I)I",
        (void*) vimtouch_Exec_waitFor},
    { "close", "(Ljava/io/FileDescriptor;)V",
        (void*) vimtouch_Exec_close}
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
    if (!registerNativeMethods(env, classPathName, method_table, 
                 sizeof(method_table) / sizeof(method_table[0]))) {
        return JNI_FALSE;
    }
  
    /* get class */
    jclass clazz = env->FindClass(classPathName);

    class_Exec = (jclass)env->NewGlobalRef(clazz);

    if (class_Exec == NULL) {
        return -1;
    }

    method_Exec_showDialog = env->GetStaticMethodID(class_Exec, "showDialog", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V");
    if (method_Exec_showDialog == NULL) {
        LOGE("Can't find Exec.showDialog");
        return -1;
    }
    method_Exec_getDialogState = env->GetStaticMethodID(class_Exec, "getDialogState", "()I");
    if (method_Exec_getDialogState == NULL) {
        LOGE("Can't find Exec.getDialogState");
        return -1;
    }

    return JNI_TRUE;
}


// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */
 
typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;
    
    LOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;
    global_env = env;
    global_vm = vm;
    
    if ((result = register_FileDescriptor(env)) < 0) {
        LOGE("ERROR: registerFileDescriptor failed");
        goto bail;
    }

    if (registerNatives(env) != JNI_TRUE) {
        LOGE("ERROR: registerNatives failed");
        goto bail;
    }
    
    result = JNI_VERSION_1_4;
    
bail:
    return result;
}
