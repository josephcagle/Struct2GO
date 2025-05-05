
- Install JDK 8 and set `JAVA_HOME` (or, prefix all commands with `JAVA_HOME=/path/to/jdk8`)
  - e.g., `JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 ...`
- Use Maven 3.8.7 or later
- Install Spark 2.4.0 (Hadoop 2.7)
- Download `https://algs4.cs.princeton.edu/code/algs4.jar`, rename and install to local repository
```
mvn install:install-file -Dfile=$HOME/Downloads/algs4-1.0.3.jar -DgroupId=edu.princeton.cs -DartifactId=algs4 -Dversion=1.0.3 -Dpackaging=jar
```
- cd to THIS DIRECTORY (`Struct2GO/spark-on-angel/examples`)
- `mvn clean install -U`
- `spark-submit --class com.tencent.angel.spark.examples.local.Node2VecExample target/spark-on-angel-examples-3.2.0.jar`
