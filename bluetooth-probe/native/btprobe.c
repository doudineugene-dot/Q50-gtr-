/* Read-only Linux Bluetooth ABI probe. No controller commands or remote writes.
 * ABI reference: bluez/bluez 5.43 lib/hci.h and Linux include/net/bluetooth/hci.h.
 * Only socket creation, RAW observer bind, local socket filters and GET ioctls.
 */
#define _POSIX_C_SOURCE 200809L
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <time.h>
#include <poll.h>
#include <dirent.h>
#ifndef STANDALONE
#include <jni.h>
#endif

#define BT_FAMILY 31
#define BT_HCI 1
#define BT_L2CAP 0
#define BT_RFCOMM 3
#define SOL_HCI 0
#define MAX_DEVICES 16
#define MAX_CONNECTIONS 32
#define MAX_CAPTURE_BYTES (16*1024*1024)
#define GET_DEV_LIST _IOR('H',210,int)
#define GET_DEV_INFO _IOR('H',211,int)
#define GET_CONN_LIST _IOR('H',212,int)

struct bt_addr { uint8_t b[6]; };
struct dev_stats {uint32_t err_rx,err_tx,cmd_tx,evt_rx,acl_tx,acl_rx,sco_tx,sco_rx,byte_rx,byte_tx;};
struct dev_info {
    uint16_t id;char name[8];struct bt_addr addr;uint32_t flags;uint8_t type,features[8];
    uint32_t pkt_type,link_policy,link_mode;uint16_t acl_mtu,acl_pkts,sco_mtu,sco_pkts;struct dev_stats stat;
};
struct dev_req {uint16_t id;uint32_t opt;};
struct dev_list {uint16_t count;struct dev_req devs[MAX_DEVICES];};
struct conn_info {uint16_t handle;struct bt_addr addr;uint8_t type,out;uint16_t state;uint32_t link_mode;};
struct conn_list {uint16_t id,count;struct conn_info conns[MAX_CONNECTIONS];};
/* Kernel 2.6.37 sockaddr_hci has only family + device (4 bytes). Bind RAW with
 * this common prefix rather than the later 6-byte channel ABI. Never USER mode. */
struct hci_address {sa_family_t family;uint16_t dev;};
struct hci_filter {uint32_t type_mask,event_mask[2];uint16_t opcode;};
typedef char assert_dev_size[(sizeof(struct dev_info)==92)?1:-1];
typedef char assert_conn_size[(sizeof(struct conn_info)==16)?1:-1];
typedef char assert_dev_offset[(offsetof(struct dev_list,devs)==4)?1:-1];

static long long now_ms(void) {
    struct timespec t;if(clock_gettime(CLOCK_MONOTONIC,&t)<0)return -1;
    return (long long)t.tv_sec*1000+t.tv_nsec/1000000;
}
static void failure(FILE *out,const char *op) {int e=errno;fprintf(out,"%s errno=%d (%s)\n",op,e,strerror(e));fflush(out);}
static void address(FILE *out,struct bt_addr a) {int i;for(i=5;i>=0;i--)fprintf(out,"%02X%s",a.b[i],i?":":"");}
static int capability(FILE *out,int type,int proto,const char *label) {
    int fd=socket(BT_FAMILY,type,proto);
    if(fd<0)failure(out,label);else fprintf(out,"%s socket=OK (local capability only)\n",label);
    return fd;
}
static void connections(FILE *out,int fd,uint16_t dev) {
    struct conn_list list;unsigned i;memset(&list,0,sizeof(list));list.id=dev;list.count=MAX_CONNECTIONS;
    if(ioctl(fd,GET_CONN_LIST,&list)<0){failure(out,"HCIGETCONNLIST");return;}
    fprintf(out,"hci%u connections=%u\n",dev,list.count);
    for(i=0;i<list.count && i<MAX_CONNECTIONS;i++) {
        struct conn_info *c=&list.conns[i];fprintf(out," handle=0x%04x remote=",c->handle);address(out,c->addr);
        fprintf(out," type=0x%02x out=%u state=%u link_mode=0x%x (not attributed to EVI)\n",c->type,c->out,c->state,c->link_mode);
    }
}
static void device(FILE *out,int fd,uint16_t id) {
    struct dev_info d;unsigned j;memset(&d,0,sizeof(d));d.id=id;
    if(ioctl(fd,GET_DEV_INFO,&d)<0){failure(out,"HCIGETDEVINFO");return;}
    fprintf(out,"ms=%lld dev=%u name=%.8s address=",now_ms(),id,d.name);address(out,d.addr);
    fprintf(out," flags=0x%x type=0x%x cached_features=",d.flags,d.type);
    for(j=0;j<8;j++)fprintf(out,"%02x",d.features[j]);
    fprintf(out," cached_LE_bit=%u acl_mtu=%u generic_byte_rx=%u generic_acl_rx=%u\n",!!(d.features[4]&0x40),d.acl_mtu,d.stat.byte_rx,d.stat.acl_rx);
    connections(out,fd,id);fflush(out);
}
static void host_file(FILE *out,const char *path) {
    FILE *in=fopen(path,"r");unsigned char buf[4096];size_t n,total=0;
    fprintf(out,"\n== %s ==\n",path);if(!in){failure(out,"fopen");return;}
    while(total<262144 && (n=fread(buf,1,sizeof(buf),in))>0){fwrite(buf,1,n,out);total+=n;}
    if(total>=262144)fprintf(out,"\n[FILE LIMIT]\n");fclose(in);
}
static void host_inventory(FILE *out) {
    const char *paths[]={"/proc/version","/proc/self/status","/proc/mounts","/proc/modules","/proc/net/unix",
        "/proc/net/bluetooth/l2cap","/proc/net/bluetooth/rfcomm","/proc/net/bluetooth/hci",NULL};
    DIR *proc;struct dirent *entry;int i,pids=0;
    for(i=0;paths[i];i++)host_file(out,paths[i]);
    proc=opendir("/proc");if(!proc){failure(out,"opendir /proc");return;}
    while((entry=readdir(proc))!=NULL && pids<64) {
        char path[1024],cmd[4096];FILE *in;size_t n;DIR *fds;struct dirent *fd;int fcount=0;
        if(entry->d_name[0]<'0' || entry->d_name[0]>'9')continue;
        snprintf(path,sizeof(path),"/proc/%s/cmdline",entry->d_name);in=fopen(path,"r");if(!in)continue;
        n=fread(cmd,1,sizeof(cmd)-1,in);fclose(in);for(i=0;i<(int)n;i++)if(!cmd[i])cmd[i]=' ';cmd[n]=0;
        if(!strstr(cmd,"bluetooth") && !strstr(cmd,"bluez") && !strstr(cmd,"BTASound") && !strstr(cmd,"audio_ps") &&
            !strstr(cmd,"tel_proc") && !strstr(cmd,"dbus") && !strstr(cmd,"vr30") && !strstr(cmd,"ecutek") && !strstr(cmd,"cMmp"))continue;
        pids++;fprintf(out,"\nPID %s CMD %s\n",entry->d_name,cmd);
        snprintf(path,sizeof(path),"/proc/%s/maps",entry->d_name);host_file(out,path);
        snprintf(path,sizeof(path),"/proc/%s/status",entry->d_name);host_file(out,path);
        snprintf(path,sizeof(path),"/proc/%s/fd",entry->d_name);fds=opendir(path);
        if(!fds){failure(out,"opendir PID fd");continue;}
        while((fd=readdir(fds))!=NULL && fcount<256) {
            char link[1024];ssize_t len;if(fd->d_name[0]=='.')continue;fcount++;
            snprintf(path,sizeof(path),"/proc/%s/fd/%s",entry->d_name,fd->d_name);
            len=readlink(path,link,sizeof(link)-1);if(len<0){failure(out,"readlink fd");continue;}link[len]=0;
            fprintf(out,"fd %s -> %s\n",fd->d_name,link);
        }closedir(fds);
    }closedir(proc);fflush(out);
}
static int collect(const char *directory,int seconds) {
    char path[1024];FILE *out,*packets;struct dev_list list;struct pollfd polls[MAX_DEVICES];
    uint16_t ids[MAX_DEVICES];int ctl,temp,count=0,result=0;unsigned i;long long start,next,end;size_t captured=0;
    if(seconds<1 || seconds>90 || strlen(directory)>900)return 2;
    snprintf(path,sizeof(path),"%s/native.txt",directory);out=fopen(path,"w");if(!out)return 3;
    snprintf(path,sizeof(path),"%s/hci-packets.jsonl",directory);packets=fopen(path,"w");if(!packets){fclose(out);return 4;}
    fprintf(out,"read-only RAW observer; no EVI attribution; uid=%u gid=%u ABI pointer=%u\n",(unsigned)getuid(),(unsigned)getgid(),(unsigned)sizeof(void*));
    host_inventory(out);
    temp=capability(out,SOCK_SEQPACKET,BT_L2CAP,"AF_BLUETOOTH L2CAP");if(temp>=0)close(temp);
    temp=capability(out,SOCK_STREAM,BT_RFCOMM,"AF_BLUETOOTH RFCOMM");if(temp>=0)close(temp);
    ctl=capability(out,SOCK_RAW,BT_HCI,"AF_BLUETOOTH HCI RAW");
    if(ctl<0){result=5;goto finish;}
    memset(&list,0,sizeof(list));list.count=MAX_DEVICES;
    if(ioctl(ctl,GET_DEV_LIST,&list)<0){failure(out,"HCIGETDEVLIST");result=6;goto close_ctl;}
    fprintf(out,"controllers=%u (origin built-in/external must be checked against sysfs)\n",list.count);
    for(i=0;i<list.count && i<MAX_DEVICES;i++) {
        int fd,one=1;struct hci_address addr;struct hci_filter filter;
        device(out,ctl,list.devs[i].id);
        fd=socket(BT_FAMILY,SOCK_RAW,BT_HCI);if(fd<0){failure(out,"observer socket");continue;}
        memset(&addr,0,sizeof(addr));addr.family=BT_FAMILY;addr.dev=list.devs[i].id;
        memset(&filter,0,sizeof(filter));filter.type_mask=(1u<<2)|(1u<<4);filter.event_mask[0]=filter.event_mask[1]=0xffffffff;
        if(setsockopt(fd,SOL_HCI,2,&filter,sizeof(filter))<0 || bind(fd,(struct sockaddr*)&addr,sizeof(addr))<0){failure(out,"observer filter/bind RAW");close(fd);continue;}
        if(setsockopt(fd,SOL_HCI,1,&one,sizeof(one))<0)failure(out,"HCI_DATA_DIR (direction will be unknown)");
        polls[count].fd=fd;polls[count].events=POLLIN;polls[count].revents=0;ids[count]=addr.dev;count++;
    }
    start=now_ms();if(start<0){failure(out,"clock_gettime");result=7;goto close_observers;}end=start+(long long)seconds*1000;next=start;
    while(now_ms()<end) {
        long long ms=now_ms();int ready;
        if(ms>=next) {
            for(i=0;i<list.count && i<MAX_DEVICES;i++)device(out,ctl,list.devs[i].id);
            next=ms+15000;
        }
        ready=poll(polls,(nfds_t)count,250);if(ready<0){if(errno==EINTR)continue;failure(out,"poll");result=8;break;}
        for(i=0;i<(unsigned)count;i++)if(polls[i].revents&POLLIN) {
            uint8_t data[4096];union{struct cmsghdr align;char b[128];}control;
            struct iovec vec;struct msghdr msg;struct cmsghdr *c;ssize_t n;int dir=-1;size_t j;
            memset(&msg,0,sizeof(msg));vec.iov_base=data;vec.iov_len=sizeof(data);msg.msg_iov=&vec;msg.msg_iovlen=1;msg.msg_control=control.b;msg.msg_controllen=sizeof(control.b);
            n=recvmsg(polls[i].fd,&msg,MSG_DONTWAIT);if(n<0){if(errno!=EAGAIN && errno!=EINTR)failure(out,"recvmsg");continue;}
            for(c=CMSG_FIRSTHDR(&msg);c;c=CMSG_NXTHDR(&msg,c))if(c->cmsg_level==SOL_HCI && c->cmsg_type==1 && c->cmsg_len>=CMSG_LEN(sizeof(int)))memcpy(&dir,CMSG_DATA(c),sizeof(int));
            if(captured+(size_t)n*2+160>MAX_CAPTURE_BYTES){fprintf(out,"[CAPTURE LIMIT] stop observer; traffic totals are incomplete\n");goto close_observers;}
            fprintf(packets,"{\"ms\":%lld,\"dev\":%u,\"dir\":%d,\"truncated\":%s,\"hex\":\"",now_ms(),ids[i],dir,(msg.msg_flags&MSG_TRUNC)?"true":"false");
            for(j=0;j<(size_t)n;j++)fprintf(packets,"%02x",data[j]);fprintf(packets,"\"}\n");captured+=(size_t)n*2+160;
            fflush(packets);
        } else if(polls[i].revents&(POLLERR|POLLHUP|POLLNVAL)) {
            fprintf(out,"hci%u observer lost revents=0x%x\n",ids[i],polls[i].revents);close(polls[i].fd);polls[i].fd=-1;
        }
    }
close_observers:
    for(i=0;i<(unsigned)count;i++)if(polls[i].fd>=0)close(polls[i].fd);
close_ctl:
    close(ctl);
finish:
    fprintf(out,"finished result=%d captured_file_budget_bytes=%lu; ECUTEK RPM/BOOST=UNAVAILABLE\n",result,(unsigned long)captured);
    fclose(packets);fclose(out);return result;
}

#ifdef STANDALONE
int main(int argc,char **argv) {
    char *tail;long seconds;
    if(argc!=3){fprintf(stderr,"usage: %s existing-output-directory seconds(1..90)\n",argv[0]);return 2;}
    seconds=strtol(argv[2],&tail,10);if(*tail || seconds<1 || seconds>90)return 2;
    return collect(argv[1],(int)seconds);
}
#else
JNIEXPORT jint JNICALL Java_ru_q50_ecutekprobe_NativeProbe_capture(JNIEnv *env,jclass cls,jstring directory,jint seconds) {
    const char *path;int code;(void)cls;path=(*env)->GetStringUTFChars(env,directory,NULL);if(!path)return 2;
    code=collect(path,seconds);(*env)->ReleaseStringUTFChars(env,directory,path);return code;
}
JNIEXPORT jstring JNICALL Java_ru_q50_ecutekprobe_NativeProbe_readLink(JNIEnv *env,jclass cls,jstring input) {
    const char *path;char link[2048];ssize_t len;(void)cls;path=(*env)->GetStringUTFChars(env,input,NULL);if(!path)return NULL;
    len=readlink(path,link,sizeof(link)-1);if(len<0)snprintf(link,sizeof(link),"[errno=%d %s]",errno,strerror(errno));else link[len]=0;
    (*env)->ReleaseStringUTFChars(env,input,path);return (*env)->NewStringUTF(env,link);
}
#endif
