
#include "gpm.h"
#include <unistd.h>
#include <fcntl.h>

int gpm_flag = 0;
int gpm_fd = 0;

int fake_gpm_fd[2];

int Gpm_Open(Gpm_Connect *c, int a){
    gpm_fd = fake_gpm_fd[0];
    fcntl(gpm_fd , F_SETFL, O_NONBLOCK);
    gpm_flag = 1;
    return 1;
}

int Gpm_Close(){
    return 0;
}

int Gpm_GetEvent(Gpm_Event *e){
    if(read(gpm_fd, (void*)e, sizeof(Gpm_Event)) == sizeof(Gpm_Event)){
        return 1;
    }
    return 0;
}
