./gradlew clean shadowJar \
  && java -jar -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
          ./build/libs/batarang-0.3d-all.jar ---generate-Native-Image-Config \
  && ./gradlew nativeImage \
  && ./build/bat
