./gradlew clean shadowJar \
  && java -jar -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
          ./build/libs/batarang-all.jar ---generate-Native-Image-Config \
  && ./gradlew clean nativeImage \
  && ./build/batarang
