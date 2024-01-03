# How-To use gradle

To run a gradle task just call the gradlew* executable of your operation system. You currently need to run this with a java 1.8 jdk. 
Either configure this in a java_home environment variable or use the `-Dorg.gradle.java.home=` parameter.

E.g.:

    gradlew.bat :http-api:clean :http-api:build -Dorg.gradle.java.home="C:/Program Files/Java/jdk1.8.0_251"