
版本1 :
        实现功能:
                 将android 端 扬声器声音进行截断，并将音效以pcm格式 通过udp发送给pc端

                 （linux）pc端：UDP进行接受，使用SDL2 库进行播放
        编译方式：
            
                将项目放到Android源码 package/apps 下，使用mm命令编译

                安装完成后,重启android端，自动运行。

        audio.c ：属于pc的服务端。
            
                  使用命令 g++ audio.c -lSDL2 -lpthread 进行编译



