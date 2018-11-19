
#include <stdio.h>
#include <pthread.h>
#include<unistd.h>
#include<sys/types.h>
#include<sys/socket.h>
#include<arpa/inet.h>
#include <stdlib.h>
       #include <sys/stat.h>
       #include <fcntl.h>
#include <unistd.h>
extern "C"
{
#include <SDL2/SDL.h>
};

//Buffer:
//|-----------|-------------|
//chunk-------pos---len-----|
static  Uint8  *audio_chunk; 
static  Uint32  audio_len; 
static  Uint8  *audio_pos; 

/* Audio Callback
 * The audio function callback takes the following parameters: 
 * stream: A pointer to the audio buffer to be filled 
 * len: The length (in bytes) of the audio buffer 
 * 
*/ 
//socket udp 服务端
#define MINBUFFERSIZE 4096
 struct Content{
    char msg[MINBUFFERSIZE];
};
static int i =0;
static int j  =0;
struct Content *content ;
int flag = 0;
int sockfd;
struct sockaddr_in cli;
socklen_t len=sizeof(cli);  
void* run(void*arg)
{
    //    while(1){
    // int new_fd = accept(sockfd,(struct sockaddr*)&cli,&len);
    //       if(new_fd ==-1){
    //         flag =0;
    //         continue;
    //         }else{
    //umask(0);
    //int fd =open("tttt.pcm",O_RDWR|O_CREAT,0777);
        while(1){
             int lenth = recvfrom(sockfd,content[i].msg,MINBUFFERSIZE,0,(struct sockaddr*)&cli,&len);
             if(lenth !=4096){
              continue;
             }

          //   printf("%s\n",content[i].msg);
          // int len =  recv(new_fd,content[i].msg,640,0);
         // write(fd,content[i].msg,MINBUFFERSIZE);
          i++;
          //printf("len: %d\n",len);
          
          // if(i-j>=2){
          //   flag = 1;
          // }else{
          //   flag = 0;
          // }
          if(i ==999998){
            i =0;
          }
      }
//   }
// }

}

void  fill_audio(void *udata,Uint8 *stream,int len){ 

    //SDL 2.0

    SDL_memset(stream, 0, len);

    if(audio_len==0)

            return; 

    len=(len>audio_len?audio_len:len);
    SDL_MixAudio(stream,audio_pos,len,SDL_MIX_MAXVOLUME);

    audio_pos += len; 

    audio_len -= len; 

} 
int main(int argc, char* argv[])
{

    content = (Content*)malloc(1000000*1024);
    //Init
    if(SDL_Init(SDL_INIT_AUDIO | SDL_INIT_TIMER)) {  
        printf( "Could not initialize SDL - %s\n", SDL_GetError()); 
        return -1;
    }
 	//创建socket对象
    sockfd=socket(AF_INET,SOCK_DGRAM,0);

    //创建网络通信对象
    struct sockaddr_in addr;
    addr.sin_family =AF_INET;
    addr.sin_port =htons(27188);
    addr.sin_addr.s_addr=htonl(INADDR_ANY);
    // bzero(&(addr.sin_zero),8);
    //绑定socket对象与通信链接
    int ret =bind(sockfd,(struct sockaddr*)&addr,sizeof(addr));
    if(0>ret)
    {
        printf("bind\n");
        return -1;

    }
 
    // listen(sockfd,5);
    //SDL_AudioSpec
    SDL_AudioSpec wanted_spec;
    wanted_spec.freq = 44100; 
    wanted_spec.format = AUDIO_S16SYS; 
    wanted_spec.channels = 1; 
    wanted_spec.silence = 0; 
    wanted_spec.samples = 1024; 
    wanted_spec.callback = fill_audio; 

    if (SDL_OpenAudio(&wanted_spec, NULL)<0){ 
        printf("can't open audio.\n"); 
        return -1; 
    } 
    int pcm_buffer_size=MINBUFFERSIZE;
    int data_count=0;
    pthread_t thread;
    pthread_create(&thread,NULL,run,NULL);
    // SDL_Delay(1); 

    //Play
    char* tmp = (char*)malloc(pcm_buffer_size);
    SDL_PauseAudio(0);
        while(1){

         // if(!flag || j>i){
         //     continue;
         // }
       
       printf("Now Playing %10d Bytes data.\n",data_count);
       data_count+=(pcm_buffer_size);
        // sprintf(tmp,"%s%s%s%s%s%s",content[j].msg,content[j+1].msg,content[j+2].msg,content[j+3].msg,content[j+4].msg,content[j+5].msg);
       // printf("msg: %s\n",content[j].msg);
        //Set audio buffer (PCM data)
        audio_chunk = (Uint8 *) content[j].msg; 
        j++;
        // j = j+6;
        //Audio buffer length
        audio_len =pcm_buffer_size;
        audio_pos = audio_chunk;
         if(j ==999998){
                  j=0;
              }
        while(audio_len>0);//Wait until finish
          //  SDL_Delay(1); 
    }


    free(content);
    SDL_Quit();
    return 0;
}
