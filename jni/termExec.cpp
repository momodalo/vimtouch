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

#include <map>

extern "C" {
#include "vim.h"
#include "gpm.h"
int AndroidMain(int argc, char**argv);
void android_mch_exit(int);
extern int fake_gpm_fd[2];
};

static pthread_mutex_t global_mutex;

static JNIEnv* global_env;
static JavaVM* global_vm;
static jclass class_fileDescriptor;
static jfieldID field_fileDescriptor_descriptor;
static jmethodID method_fileDescriptor_init;
static jclass class_Exec;
static jmethodID method_Exec_showDialog;
static jmethodID method_Exec_getDialogState;
static jmethodID method_Exec_quit;
static std::map<pthread_t, char**> thread_data;
static int thread_exit_val = 0;

extern "C" void android_exit(int exit_value)
{
    // simulate mch_exit first
    android_mch_exit(exit_value);

    // XXX: should we use exit_value somehow?
    LOGI("android_exit(%d)", exit_value);
    global_env->CallStaticVoidMethod(class_Exec, method_Exec_quit);
}

void pth_exit(int n)
{
    thread_exit_val = n;
    pthread_exit(&thread_exit_val);
}

extern "C" {
void vimtouch_lock(){
     pthread_mutex_lock (&global_mutex);
}

void vimtouch_unlock(){
     pthread_mutex_unlock (&global_mutex);
}
}

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

        AndroidMain(argv[1] ? 2 : 1, (char**)argv);

    global_vm->DetachCurrentThread();
    LOGE("thread leave");
    pthread_mutex_destroy(&global_mutex);
    pth_exit(0);
}

static int create_subprocess(const char *cmd, const char *arg0, const char *arg1, char **envp,
    int* pProcessId)
{
    char *devname;
    char tmpdir[PATH_MAX];
    char terminfodir[PATH_MAX];

    sprintf((char*)default_vimruntime_dir, "%s/vim/", cmd);
    sprintf((char*)default_vim_dir, "%s/vim/", cmd);
    sprintf(tmpdir, "%s/tmp", cmd);
    sprintf(terminfodir, "%s/terminfo", cmd);

    pipe(fake_gpm_fd);

    int ptm = open("/dev/ptmx", O_RDWR);
    if (ptm < 0)
    {
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
    pthread_mutex_init(&global_mutex, NULL);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

    pthread_create(&thread_id, &attr, thread_wrapper, (void*)thread_arg);
    pthread_attr_destroy(&attr);
    *pProcessId = (int) thread_id;
    thread_data.insert(std::make_pair(thread_id, thread_arg));

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
    char const* cmd_str = cmd ? env->GetStringUTFChars(cmd, NULL) : NULL;
    char const* arg0_str = arg0 ? env->GetStringUTFChars(arg0, NULL) : NULL;
    char const* arg1_str = arg1 ? env->GetStringUTFChars(arg1, NULL) : NULL;
    LOGI("cmd_str = '%s'", cmd_str);
    LOGI("arg0_str = '%s'", arg0_str);
    LOGI("arg1_str = '%s'", arg1_str);

    int num_env_vars = envVars ? env->GetArrayLength(envVars) : 0;
    char **envp = NULL;
    char const* tmp = NULL;

    if (num_env_vars > 0)
    {
        envp = (char **)malloc((num_env_vars + 1) * sizeof(char*));

        if (!envp)
        {
            throwOutOfMemoryError(env, "Couldn't allocate envp array");
            return NULL;
        }

        for (int i = 0; i < num_env_vars; ++i)
        {
            jobject obj = env->GetObjectArrayElement(envVars, i);
            jstring env_var = reinterpret_cast<jstring>(obj);
            tmp = env->GetStringUTFChars(env_var, 0);

            if (tmp == NULL)
            {
                throwOutOfMemoryError(env, "Couldn't get env var from array");
                return NULL;
            }

            envp[i] = strdup(tmp);

            if (envp[i] == NULL)
            {
                throwOutOfMemoryError(env, "Couldn't strdup() env var");
                return NULL;
            }

            env->ReleaseStringUTFChars(env_var, tmp);
        }

        envp[num_env_vars] = NULL;
    }

    int procId;
    int ptm = create_subprocess(cmd_str, arg0_str, arg1_str, envp, &procId);

    env->ReleaseStringUTFChars(cmd, cmd_str);
    env->ReleaseStringUTFChars(arg0, arg0_str);
    env->ReleaseStringUTFChars(arg1, arg1_str);

    for (int i = 0; i < num_env_vars; ++i)
        free(envp[i]);

    free(envp);

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

    vimtouch_lock();
    out_flush();
    shell_resized_check();
    redraw_later(CLEAR);
    vimtouch_unlock();
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
    vimtouch_lock();
    //redraw_later(NOT_VALID);
    update_screen(0);
    //setcursor();
    out_flush();
    vimtouch_unlock();
}

static void vimtouch_Exec_moveCursor(JNIEnv *env, jobject clazz,
    jint row, jint col) {
    //windgoto(row, col);
    VimEvent e;
    e.type = VIM_EVENT_TYPE_GPM;
    Gpm_Event* gpm = &e.event.gpm;
    gpm->x = col;
    gpm->y = row;
    gpm->type = GPM_DOWN;
    gpm->buttons = GPM_B_LEFT;
    gpm->clicks = 0;
    gpm->modifiers = 0;

    write(fake_gpm_fd[1],(void*)&e, sizeof(e));
    gpm->type = GPM_UP;
    gpm->buttons = GPM_B_LEFT;
    write(fake_gpm_fd[1],(void*)&e, sizeof(e));
}

static int vimtouch_Exec_scrollBy(JNIEnv *env, jobject clazz,
    jint line) {
    int do_scroll = line;
    vimtouch_lock();
    scroll_redraw(do_scroll > 0, do_scroll>0?do_scroll:-do_scroll);
    update_screen(0);
    out_flush();
    vimtouch_unlock();
    return line;
}

static int vimtouch_Exec_getState(JNIEnv *env, jobject clazz) {
    return State;
}

static int vimtouch_Exec_getCursorCol(JNIEnv *env, jobject clazz) {
    return curwin->w_cursor.col;
}

static void vimtouch_Exec_setCursorCol(JNIEnv *env, jobject clazz, int col) {
    curwin->w_cursor.col = col;
    setcursor();
}

static jstring vimtouch_Exec_getCurrentLine(JNIEnv *env, jobject clazz, int size) {
    u_char* line = ml_get_curline();
    if(size <= 0)
        return env->NewStringUTF((const char*)line);

    char* buf = strndup((const char*)line, size);
    jstring result = env->NewStringUTF((const char*)buf);
    free(buf);
    return result;
}

static void vimtouch_Exec_lineReplace(JNIEnv *env, jobject clazz,
    jstring line) {
    const char* str = line?env->GetStringUTFChars(line, NULL):NULL;
    if(!str) return;
    ml_replace(curwin->w_cursor.lnum,(char_u*)str, TRUE);
    changed_lines(curwin->w_cursor.lnum, 0, curwin->w_cursor.lnum, 1L);
    updateScreen();
    setcursor();
    out_flush();
}

static void vimtouch_Exec_updateScreen(JNIEnv *env, jobject clazz) {
    updateScreen();
}

static void vimtouch_Exec_doCommand(JNIEnv *env, jobject clazz, jstring cmd){
    if(!cmd) return;
    const char* str = env->GetStringUTFChars(cmd, NULL);
    if(!str) return;

    VimEvent e;
    e.type = VIM_EVENT_TYPE_CMD;
    strcpy(e.event.cmd, str);
    write(fake_gpm_fd[1],(void*)&e, sizeof(e));

    env->ReleaseStringUTFChars(cmd, str);
}

static int vimtouch_Exec_waitFor(JNIEnv *env, jobject clazz, jint procId)
{
    int* status;
    pthread_join((pthread_t)procId, (void**)&status);
    std::map<pthread_t, char**>::iterator data = thread_data.find(procId);

    if (data != thread_data.end())
    {
        free(data->second[0]);
        free(data->second[1]);
        free(data->second);
    }

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
    { "setCursorCol", "(I)V",
        (void*) vimtouch_Exec_setCursorCol}, 
    { "getCursorCol", "()I",
        (void*) vimtouch_Exec_getCursorCol}, 
    { "getState", "()I",
        (void*) vimtouch_Exec_getState}, 
    { "getCurrentLine", "(I)Ljava/lang/String;",
        (void*) vimtouch_Exec_getCurrentLine}, 
    { "lineReplace", "(Ljava/lang/String;)V",
        (void*) vimtouch_Exec_lineReplace},
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
    method_Exec_quit = env->GetStaticMethodID(class_Exec, "quit", "()V");
    if (method_Exec_quit == NULL) {
        LOGE("Can't find Exec.quit");
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
