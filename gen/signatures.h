#ifndef SIGNATURES_H__
#define SIGNATURES_H__

char const* jni_signature_nativeCreateSubprocess = 
	"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)Ljava/io/FileDescriptor;";

char const* jni_signature_startVim = 
	"()V";

char const* jni_signature_setPtyWindowSize = 
	"(Ljava/io/FileDescriptor;IIII)V";

char const* jni_signature_setPtyUTF8Mode = 
	"(Ljava/io/FileDescriptor;Z)V";

char const* jni_signature_scrollBy = 
	"(I)I";

char const* jni_signature_nativeWait = 
	"()I";

char const* jni_signature_close = 
	"(Ljava/io/FileDescriptor;)V";

char const* jni_signature_updateScreen = 
	"()V";

char const* jni_signature_doCommand = 
	"(Ljava/lang/String;)V";

char const* jni_signature_mouseDown = 
	"(II)V";

char const* jni_signature_mouseDrag = 
	"(II)V";

char const* jni_signature_mouseUp = 
	"(II)V";

char const* jni_signature_lineReplace = 
	"(Ljava/lang/String;)V";

char const* jni_signature_getCurrentLine = 
	"(I)Ljava/lang/String;";

char const* jni_signature_setCursorCol = 
	"(I)V";

char const* jni_signature_setCursorPos = 
	"(II)V";

char const* jni_signature_getCurrBuffer = 
	"()Ljava/lang/String;";

char const* jni_signature_setTab = 
	"(I)V";

char const* jni_signature_getcwd = 
	"()Ljava/lang/String;";

char const* jni_signature_setSocket = 
	"(I)V";

char const* jni_signature_returnClipText = 
	"(Ljava/lang/String;)V";

char const* jni_signature_returnDialog = 
	"(I)V";

char const* jni_signature_returnExtensionResult = 
	"(Ljava/lang/String;)V";

char const* jni_signature_sendAndroidEvent = 
	"(ILjava/lang/String;)V";

char const* jni_signature_getHistory = 
	"()V";

#endif // SIGNATURES_H__
