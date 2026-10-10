/* Local kernel/socket capability inspection only. Linux Bluetooth UAPI values:
 * bluez lib/bluetooth/{bluetooth,hci,l2cap}.h and Linux v2.6.37 hci_sock.h.
 * These are socket ABI constants, never EcuTek UUIDs, PSMs or packet bytes.
 */
#include <jni.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <errno.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <poll.h>
#include <time.h>
#include <sys/time.h>

#define BT_FAMILY 31
#define BT_L2CAP 0
#define BT_HCI 1
#define BT_RFCOMM 3
struct dev_req {uint16_t id; uint32_t options;};
struct dev_list {uint16_t count; struct dev_req dev[16];};
struct dev_info {
    uint16_t id;char name[8];uint8_t address[6];uint32_t flags;
    uint8_t type;uint8_t features[8];uint32_t packet_type,link_policy,link_mode;
    uint16_t acl_mtu,acl_packets,sco_mtu,sco_packets;uint32_t stats[10];
};
static void append(char *out,size_t size,const char *fmt,...) {
    size_t n=strlen(out);if(n>=size-1)return;
    va_list a;va_start(a,fmt);vsnprintf(out+n,size-n,fmt,a);va_end(a);
}
static void capability(char *out,size_t size,int type,int protocol,const char *name) {
    int fd=socket(BT_FAMILY,type,protocol);int error=fd<0?errno:0;
    append(out,size,"socket %s result=%d errno=%d (%s)\n",name,fd<0?-1:0,error,strerror(error));
    if(fd>=0){
        if(protocol==BT_HCI){struct dev_list list;memset(&list,0,sizeof(list));list.count=16;
            int result=ioctl(fd,_IOR('H',210,int),&list);error=result<0?errno:0;
            append(out,size,"HCIGETDEVLIST result=%d errno=%d count=%u\n",result,error,list.count);
            if(result>=0)for(unsigned i=0;i<list.count && i<16;i++){
                append(out,size,"HCI id=%u flags=0x%08x\n",list.dev[i].id,list.dev[i].options);
                struct dev_info info;memset(&info,0,sizeof(info));info.id=list.dev[i].id;
                int r=ioctl(fd,_IOR('H',211,int),&info);error=r<0?errno:0;
                append(out,size,"HCIGETDEVINFO result=%d errno=%d\n",r,error);
                if(r>=0){append(out,size,"name=%.8s address=%02x:%02x:%02x:%02x:%02x:%02x features=",info.name,
                    info.address[5],info.address[4],info.address[3],info.address[2],info.address[1],info.address[0]);
                    for(unsigned j=0;j<8;j++){append(out,size,"%02x",info.features[j]);}
                    append(out,size," cached_LE_bit=%u (not CoC proof)\n",(info.features[4]&0x40u)!=0);
                }
            }
        }
        if(protocol==BT_L2CAP){unsigned char opts[64];socklen_t len=sizeof(opts);
            int r=getsockopt(fd,6,1,opts,&len);error=r<0?errno:0;
            append(out,size,"L2CAP_OPTIONS result=%d errno=%d length=%u raw=",r,error,(unsigned)len);
            if(r>=0)for(unsigned i=0;i<len && i<sizeof(opts);i++)append(out,size,"%02x",opts[i]);
            append(out,size,"\nSocket success does not prove LE CoC support.\n");
        }close(fd);
    }
}
JNIEXPORT jstring JNICALL Java_ru_q50_codex_NativeProbe_snapshot(JNIEnv *env,jclass cls) {
    (void)cls;char out[8192]="native v1; no bind/connect/send/write/controller changes\n";
    append(out,sizeof(out),"uid=%u gid=%u pid=%u pointer_bytes=%u\n",(unsigned)getuid(),(unsigned)getgid(),(unsigned)getpid(),(unsigned)sizeof(void*));
    capability(out,sizeof(out),SOCK_SEQPACKET,BT_L2CAP,"L2CAP SEQPACKET");
    capability(out,sizeof(out),SOCK_STREAM,BT_RFCOMM,"RFCOMM STREAM");
    capability(out,sizeof(out),SOCK_RAW,BT_HCI,"HCI RAW");
    return (*env)->NewStringUTF(env,out);
}
JNIEXPORT jstring JNICALL Java_ru_q50_codex_NativeProbe_link(JNIEnv *env,jclass cls,jstring path) {
    (void)cls;char b[4096];const char *p=(*env)->GetStringUTFChars(env,path,0);if(!p)return 0;
    ssize_t n=readlink(p,b,sizeof(b)-1);int error=errno;(*env)->ReleaseStringUTFChars(env,path,p);
    if(n<0)snprintf(b,sizeof(b),"READLINK_FAILED errno=%d (%s)",error,strerror(error));else b[n]=0;
    return (*env)->NewStringUTF(env,b);
}
/* Passive RAW channel, never USER channel: no ownership takeover or HCI send.
 * Bind uses the old 2.6.37 family+device ABI. Filters and direction ancillary
 * data are Linux socket options, not vehicle protocol. All visible controllers
 * are identified by real HCIGETDEVLIST IDs. A denial is evidence, not success.
 */
JNIEXPORT jstring JNICALL Java_ru_q50_codex_NativeProbe_capture(JNIEnv *env,jclass cls,jstring path){
    (void)cls;char result[2048]="";const char *p=(*env)->GetStringUTFChars(env,path,0);if(!p)return 0;
    FILE *out=fopen(p,"w");(*env)->ReleaseStringUTFChars(env,path,p);
    if(!out)return (*env)->NewStringUTF(env,"CAPTURE_OUTPUT_FAILED");
    struct pollfd fds[16];unsigned count=0;int control=socket(BT_FAMILY,SOCK_RAW,BT_HCI);
    if(control<0){fprintf(out,"HCI socket denied errno=%d (%s)\n",errno,strerror(errno));fclose(out);return (*env)->NewStringUTF(env,"CAPTURE_HCI_SOCKET_FAILED; see hci-passive.txt");}
    struct dev_list list;memset(&list,0,sizeof(list));list.count=16;
    int rc=ioctl(control,_IOR('H',210,int),&list);
    if(rc<0){fprintf(out,"HCIGETDEVLIST errno=%d\n",errno);list.count=0;}close(control);
    for(unsigned i=0;i<list.count && i<16;i++){
        int fd=socket(BT_FAMILY,SOCK_RAW,BT_HCI);if(fd<0){fprintf(out,"socket errno=%d\n",errno);continue;}
        struct {sa_family_t family;uint16_t dev;} addr={BT_FAMILY,list.dev[i].id};
        struct {uint32_t types;uint32_t events[2];uint16_t opcode;} filter;
        memset(&filter,0,sizeof(filter));filter.types=(1u<<1)|(1u<<2)|(1u<<4);filter.events[0]=filter.events[1]=0xffffffffu;
        int direction=1;
        if(setsockopt(fd,0,1,&direction,sizeof(direction))<0)fprintf(out,"HCI_DATA_DIR errno=%d; direction may be unknown\n",errno);
        if(setsockopt(fd,0,2,&filter,sizeof(filter))<0 || bind(fd,(struct sockaddr*)&addr,sizeof(addr))<0){
            fprintf(out,"hci%u filter/bind errno=%d (%s)\n",list.dev[i].id,errno,strerror(errno));close(fd);continue;}
        fprintf(out,"PASSIVE hci%u; no TX; captures may include other local Bluetooth sessions\n",list.dev[i].id);
        fds[count].fd=fd;fds[count].events=POLLIN;fds[count].revents=0;count++;
    }
    unsigned packets=0;time_t start=time(0);
    while(count && time(0)-start<30 && packets<4096){
        if(poll(fds,count,500)<0){if(errno==EINTR)continue;fprintf(out,"poll errno=%d\n",errno);break;}
        for(unsigned i=0;i<count;i++)if(fds[i].revents&POLLIN){
            unsigned char data[4096],ancillary[128];struct iovec v={data,sizeof(data)};struct msghdr m;
            memset(&m,0,sizeof(m));m.msg_iov=&v;m.msg_iovlen=1;m.msg_control=ancillary;m.msg_controllen=sizeof(ancillary);
            ssize_t n=recvmsg(fds[i].fd,&m,MSG_DONTWAIT);if(n<0)continue;
            int dir=-1;for(struct cmsghdr *c=CMSG_FIRSTHDR(&m);c;c=CMSG_NXTHDR(&m,c))
                if(c->cmsg_level==0 && c->cmsg_type==1 && c->cmsg_len>=CMSG_LEN(sizeof(int)))memcpy(&dir,CMSG_DATA(c),sizeof(int));
            struct timeval tv;gettimeofday(&tv,0);fprintf(out,"t=%ld.%06ld socket=%u direction=%d flags=%d hex=",(long)tv.tv_sec,(long)tv.tv_usec,i,dir,m.msg_flags);
            for(ssize_t j=0;j<n && j<(ssize_t)sizeof(data);j++){fprintf(out,"%02x",data[j]);}
            fputc('\n',out);packets++;
        }
    }
    for(unsigned i=0;i<count;i++)close(fds[i].fd);
    fprintf(out,"END packets=%u limit=4096 seconds=30; no ECUTEK decoding\n",packets);fclose(out);
    append(result,sizeof(result),"CAPTURE_COMPLETE controllers=%u packets=%u (zero does not prove absence of vendor traffic)",count,packets);
    return (*env)->NewStringUTF(env,result);
}
