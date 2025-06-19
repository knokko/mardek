FROM fxiangucsd/sapien-swiftshader

RUN apt update
RUN apt install git -y
RUN git clone https://github.com/knokko/mardek.git
WORKDIR ./mardek
RUN git checkout swift-shader-issues
RUN apt install openjdk-21-jdk -y
RUN ./gradlew build -x test

RUN apt install vulkan-tools -y

WORKDIR ../
RUN apt install curl -y
RUN curl https://sdk.lunarg.com/sdk/download/1.4.313.0/linux/vulkansdk-linux-x86_64-1.4.313.0.tar.xz > vulkan-sdk.tar.xz
RUN tar -xJf vulkan-sdk.tar.xz
WORKDIR ./1.4.313.0
RUN rm /bin/sh && ln -s /bin/bash /bin/sh
RUN apt remove mesa-vulkan-drivers -y
#RUN source ./setup-env.sh && vulkaninfo --summary && cd ../mardek && ./gradlew test && while true; do ./gradlew clean -p game && ./gradlew test -p game; done
