#define socket fake_socket
#define close fake_close
#define ioctl fake_ioctl
#define getsockopt fake_getsockopt
#include "../native/q50probe.c"
#include <assert.h>
static int denied,closes;
int fake_socket(int family,int type,int protocol){assert(family==31);assert(type==SOCK_RAW || type==SOCK_STREAM || type==SOCK_SEQPACKET);assert(protocol==0 || protocol==1 || protocol==3);if(denied){errno=EACCES;return -1;}return 42;}
int fake_close(int fd){assert(fd==42);closes++;return 0;}
int fake_ioctl(int fd,unsigned long request,...){assert(fd==42);va_list args;va_start(args,request);void *data=va_arg(args,void*);va_end(args);
    if(request==_IOR('H',211,int)){struct dev_info *d=data;assert(d->id==7);d->features[4]=0x40;return 0;}
    assert(request==_IOR('H',210,int));struct dev_list *d=data;assert(d->count==16);d->count=1;d->dev[0].id=7;d->dev[0].options=3;return 0;}
int fake_getsockopt(int fd,int level,int option,void *data,socklen_t *len){assert(fd==42 && level==6 && option==1);memset(data,0,*len);*len=12;return 0;}
int main(void){char out[8192]="";capability(out,sizeof(out),SOCK_RAW,1,"HCI");assert(strstr(out,"HCI id=7"));assert(strstr(out,"cached_LE_bit=1"));assert(closes==1);
    capability(out,sizeof(out),SOCK_SEQPACKET,0,"L2CAP");assert(strstr(out,"does not prove LE CoC"));assert(closes==2);
    denied=1;capability(out,sizeof(out),SOCK_RAW,1,"DENIED");assert(strstr(out,"errno=13"));assert(closes==2);
    char tiny[4]="";append(tiny,sizeof(tiny),"long string");assert(strlen(tiny)==3);
    puts("PASS: native capability success/denial, HCI IDs, L2CAP non-proof, close discipline, bounded output");return 0;}
