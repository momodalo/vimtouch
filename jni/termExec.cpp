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
#include "signatures.h"
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
#include <string>

extern "C" {
#include "vim.h"
#include "gpm.h"
int AndroidMain(int argc, char**argv);
void android_mch_exit(int);
};

#define DEF_JNI(func, ...) \
    vimtouch_Exec_ ## func (JNIEnv *env, jobject clazz, __VA_ARGS__)

// Need a separate version without arguments, since __VA_ARGS__ can't handle
// zero args for some reason:
#define DEF_JNI0(func) \
    vimtouch_Exec_ ## func (JNIEnv *env, jobject clazz)

#define DECL_JNI(func) \
    { #func, jni_signature_ ## func, (void*) vimtouch_Exec_ ## func }


static pthread_mutex_t global_mutex;

static JNIEnv* global_env;
static JavaVM* global_vm;
static int global_pid;
static int global_tid;
static jclass class_fileDescriptor;
static jfieldID field_fileDescriptor_descriptor;
static jmethodID method_fileDescriptor_init;
static jclass class_Exec;
static jmethodID method_Exec_quit;
static std::map<pthread_t, char**> thread_data;
static int thread_exit_val = 0;
static int global_socket = -1;

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

static int vimStart = 0;
static void vimtouch_Exec_startVim (JNIEnv *env, jobject clazz)
{
    vimStart = 1;
    LOGE("VimStart");
}

static void *thread_wrapper ( void* value)
{
    LOGE("thread wrapper");
    int pts;
    char** thread_arg = (char**)value;

    global_vm->AttachCurrentThread(&global_env, NULL);

    /*
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
        */

    while(!vimStart){usleep(100000);}

    int pid = fork();
    if(pid < 0) {
        LOGE("- fork failed: %s -\n", strerror(errno));
        return NULL;
    }

    if(pid == 0){

        int pts;

        setsid();
        
        pts = open(thread_arg[0], O_RDWR);
        if(pts < 0) exit(-1);

        dup2(pts, 0);
        dup2(pts, 1);
        dup2(pts, 2);

        char* argv[6];
        char path[1024];
        char sock[1024];
        sprintf(sock, "%s/%s", thread_arg[1], thread_arg[2]);
        sprintf(path, "%s/../lib/libvimexec.so", thread_arg[1]);

        int i=0;
        /*
        argv[i++] = "/system/xbin/su";
        argv[i++] = "-c";
        */
        argv[i++] = path;
        argv[i++] = sock;
        argv[i++] = (char*)thread_arg[3];
        argv[i++] = NULL;

        //AndroidMain(2, (char**)argv);
        chmod(path, 0000755);
        execv(argv[0] ,argv);
        //exit(-1);
    } else {
        global_pid = (int) pid;
    }

    global_vm->DetachCurrentThread();
    LOGE("thread leave");
    //pthread_mutex_destroy(&global_mutex);
    //pth_exit(0);
}


static int create_subprocess(const char *cmd, const char* sock, const char *arg0, const char *arg1, char **envp)
{
    char *devname;
    char tmpdir[PATH_MAX];
    char terminfodir[PATH_MAX];

    /*
    sprintf((char*)default_vimruntime_dir, "%s/vim/", cmd);
    sprintf((char*)default_vim_dir, "%s/vim/", cmd);
    */
    sprintf(tmpdir, "%s/tmp", cmd);
    sprintf(terminfodir, "%s/terminfo", cmd);

    //pipe(fake_gpm_fd);

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

    char path[PATH_MAX];
    sprintf(path, "%s/bin/"TARGET_ARCH_ABI"/:%s", cmd, getenv("PATH"));
    setenv("PATH", path, 1);

    char** thread_arg = (char**)malloc(sizeof(char*)*3);
    thread_arg[0] = strdup(devname);
    thread_arg[1] = strdup(cmd);
    thread_arg[2] = strdup(sock);
    if(arg0){
        struct stat st;
        if(stat(arg0, &st) == 0){
            if(S_ISDIR(st.st_mode)) chdir(arg0);
            if(S_IFREG&st.st_mode){
                char* pdir = strdup(arg0);
                char* end = strrchr(pdir,'/');
                *end = NULL;
                chdir(pdir);
            }

        }
        thread_arg[3] = strdup(arg0);
    }else {
        chdir(getenv("HOME"));
        thread_arg[3] = NULL;
    }

    pthread_t thread_id;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_mutex_init(&global_mutex, NULL);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

    pthread_create(&thread_id, &attr, thread_wrapper, (void*)thread_arg);
    pthread_attr_destroy(&attr);
    //*pProcessId = (int) thread_id;
    global_tid = (int)thread_id;
    thread_data.insert(std::make_pair(thread_id, thread_arg));

    return ptm;
    

}

static int throwOutOfMemoryError(JNIEnv *env, const char *message)
{
    jclass exClass;
    const char *className = "java/lang/OutOfMemoryError";

    exClass = env->FindClass(className);
    return env->ThrowNew(exClass, message);
}


static jobject DEF_JNI(nativeCreateSubprocess,
                       jstring cmd, jstring sock, jstring arg0, jstring arg1,
                       jobjectArray envVars)
{
    char const* cmd_str = cmd ? env->GetStringUTFChars(cmd, NULL) : NULL;
    char const* sock_str = sock ? env->GetStringUTFChars(sock, NULL) : NULL;
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

    int ptm = create_subprocess(cmd_str, sock_str, arg0_str, arg1_str, envp);

    env->ReleaseStringUTFChars(cmd, cmd_str);
    env->ReleaseStringUTFChars(sock, sock_str);
    if(arg0 != NULL) env->ReleaseStringUTFChars(arg0, arg0_str);
    if(arg1 != NULL) env->ReleaseStringUTFChars(arg1, arg1_str);

    for (int i = 0; i < num_env_vars; ++i)
        free(envp[i]);

    free(envp);

    jobject result = env->NewObject(class_fileDescriptor, method_fileDescriptor_init);
    
    if (!result) {
        LOGE("Couldn't create a FileDescriptor.");
    }
    else {
        env->SetIntField(result, field_fileDescriptor_descriptor, ptm);
    }
    
    return result;
}

static void DEF_JNI(setPtyWindowSize,
                    jobject fileDescriptor, jint row, jint col,
                    jint xpixel, jint ypixel)
{
    int fd = env->GetIntField(fileDescriptor, field_fileDescriptor_descriptor);

    if (env->ExceptionOccurred() != NULL) {
        return;
    }

    struct winsize sz;
    sz.ws_row = row;
    sz.ws_col = col;
    sz.ws_xpixel = xpixel;
    sz.ws_ypixel = ypixel;
    ioctl(fd, TIOCSWINSZ, &sz);

    //block updateScreen here
    /*
    vimtouch_lock();
    vimtouch_unlock();
    */

    if(global_socket < 0) return;
    VimEvent e;
    e.type = VIM_EVENT_TYPE_RESIZE;
    write(global_socket,(void*)&e, sizeof(e));
}

static void DEF_JNI(setPtyUTF8Mode, jobject fileDescriptor, jboolean utf8Mode)
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
    if(global_socket < 0) return;

    VimEvent e;
    e.type = VIM_EVENT_TYPE_UPDATE;
    write(global_socket,(void*)&e, sizeof(e));
}

static void DEF_JNI(mouseDrag, jint row, jint col)
{
    if(global_socket < 0) return;
    //windgoto(row, col);
    VimEvent e;
    e.type = VIM_EVENT_TYPE_GPM;
    Gpm_Event* gpm = &e.event.gpm;
    gpm->x = col;
    gpm->y = row;
    gpm->type = GPM_DRAG;
    gpm->buttons = GPM_B_LEFT;
    gpm->clicks = 0;
    gpm->modifiers = 0;
    write(global_socket,(void*)&e, sizeof(e));
}

static void DEF_JNI(mouseDown, jint row, jint col)
{
    if(global_socket < 0) return;
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
    write(global_socket,(void*)&e, sizeof(e));
}

static void DEF_JNI(mouseUp, jint row, jint col)
{
    if(global_socket < 0) return;

    VimEvent e;
    e.type = VIM_EVENT_TYPE_GPM;
    Gpm_Event* gpm = &e.event.gpm;
    gpm->x = col;
    gpm->y = row;
    gpm->type = GPM_UP;
    gpm->buttons = GPM_B_LEFT;
    write(global_socket,(void*)&e, sizeof(e));
}

static int DEF_JNI(scrollBy, jint line)
{
    if(global_socket < 0) return 0;

    VimEvent e;
    e.type = VIM_EVENT_TYPE_SCROLL;
    e.event.num = line;
    write(global_socket,(void*)&e, sizeof(e));
    updateScreen();
    return line;
}

static void DEF_JNI(setCursorPos, int row, int col)
{
    if(global_socket < 0) return;
    VimEvent e;
    e.type = VIM_EVENT_TYPE_CURSOR;
    e.event.nums[0] = col;
    e.event.nums[1] = row;
    write(global_socket,(void*)&e, sizeof(e));
}

static void DEF_JNI(setCursorCol, int col)
{
    if(global_socket < 0) return;
    VimEvent e;
    e.type = VIM_EVENT_TYPE_SETCOL;
    e.event.num = col;
    write(global_socket,(void*)&e, sizeof(e));
}

static jstring DEF_JNI(getCurrentLine, int size)
{
    /*
    if(global_socket < 0) return NULL;

    u_char* line = ml_get_curline();
    if(size <= 0)
        return env->NewStringUTF((const char*)line);

    char* buf = strndup((const char*)line, size);
    jstring result = env->NewStringUTF((const char*)buf);
    free(buf);
    return result;
    */
    return NULL;
}

static void DEF_JNI(lineReplace, jstring line)
{
    if(global_socket < 0) return ;

    const char* str = line?env->GetStringUTFChars(line, NULL):NULL;
    if(!str) return;
    
    VimEvent e;
    e.type = VIM_EVENT_TYPE_RELINE;
    strcpy(e.event.cmd,str);
    write(global_socket,(void*)&e, sizeof(e));
    updateScreen();
}

static void DEF_JNI0(updateScreen)
{
    updateScreen();
}

static void DEF_JNI0(getHistory)
{
    if(global_socket < 0) return;

    VimEvent e;
    e.type = VIM_EVENT_TYPE_HISTORY;
    write(global_socket,(void*)&e, sizeof(e));
}

static void DEF_JNI(doCommand, jstring cmd)
{
    if(global_socket < 0) return ;

    if(!cmd) return;
    const char* str = env->GetStringUTFChars(cmd, NULL);
    if(!str) return;

    VimEvent e;
    e.type = VIM_EVENT_TYPE_CMD;
    strcpy(e.event.cmd, str);
    write(global_socket,(void*)&e, sizeof(e));

    env->ReleaseStringUTFChars(cmd, str);
}

static int DEF_JNI0(nativeWait)
{
    int status;

    pthread_join((pthread_t)global_tid, NULL);

    waitpid(global_pid, &status, 0);
    int result = 0;
    if (WIFEXITED(status)) {
        result = WEXITSTATUS(status);
    }
    return result;
}

static void DEF_JNI(close, jobject fileDescriptor)
{
    int fd = env->GetIntField(fileDescriptor, field_fileDescriptor_descriptor);

    if (env->ExceptionOccurred() != NULL) {
        return;
    }

    close(fd);
}

static jstring DEF_JNI0(getCurrBuffer)
{
    /*
    std::string result;

    // line numbers are 1-based in Vim buffers
    for (int lnum = 1; lnum <= curbuf->b_ml.ml_line_count; ++lnum)
    {
        result += (char const*) ml_get_buf(curbuf, lnum, FALSE);
        result += "\n";
    }

    return env->NewStringUTF(result.c_str());
    */
    return NULL;
}

static void DEF_JNI(setTab, int nr)
{
    if(global_socket < 0) return;
    VimEvent e;
    e.type = VIM_EVENT_TYPE_SETTAB;
    e.event.nums[0] = nr+1;
    write(global_socket,(void*)&e, sizeof(e));
}

static jstring DEF_JNI0(getcwd)
{
    if(global_socket < 0) return NULL;

    char buf[PATH_MAX];
    getcwd(buf,PATH_MAX);
    return env->NewStringUTF((const char*)buf);
}


static void DEF_JNI(returnDialog, int s)
{
    if(global_socket < 0) return;
    VimEvent e;
    e.type = VIM_EVENT_TYPE_DIALOG;
    e.event.nums[0] = s;
    write(global_socket,(void*)&e, sizeof(e));
}


static void DEF_JNI(returnClipText, jstring text)
{
    if(global_socket < 0) return ;

    if(!text) return;
    const char* str = env->GetStringUTFChars(text, NULL);
    if(!str) return;

    VimEvent e;
    e.type = VIM_EVENT_TYPE_CLIPBOARD;
    strcpy(e.event.cmd, str);
    write(global_socket,(void*)&e, sizeof(e));

    env->ReleaseStringUTFChars(text, str);
}


void DEF_JNI ( setSocket,int fd)
{
    global_socket = dup(fd);
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


static JNINativeMethod method_table[] = {
    DECL_JNI(nativeCreateSubprocess),
    DECL_JNI(setPtyWindowSize),
    DECL_JNI(setPtyUTF8Mode),
    DECL_JNI(mouseDown),
    DECL_JNI(mouseDrag),
    DECL_JNI(mouseUp),
    DECL_JNI(scrollBy),
    DECL_JNI(setCursorCol),
    DECL_JNI(setCursorPos),
    DECL_JNI(getCurrentLine),
    DECL_JNI(lineReplace),
    DECL_JNI(updateScreen),
    DECL_JNI(doCommand),
    DECL_JNI(nativeWait),
    DECL_JNI(close),
    DECL_JNI(getCurrBuffer),
    DECL_JNI(startVim),
    DECL_JNI(setTab),
    DECL_JNI(getcwd),
    DECL_JNI(getHistory),
    DECL_JNI(setSocket),
    DECL_JNI(returnClipText),
    DECL_JNI(returnDialog),
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
    static const char *classPathName = "net/momodalo/app/vimtouch/Exec";

    if (!registerNativeMethods(env, classPathName, method_table,
                               ARRLEN(method_table)))
        return JNI_FALSE;

    /* get class */
    jclass clazz = env->FindClass(classPathName);

    class_Exec = (jclass)env->NewGlobalRef(clazz);

    if (class_Exec == NULL) {
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
