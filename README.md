# micro-tomcat

## 运行：
```
mvn clean package
java -cp target/micro-tomcat-1.0-SNAPSHOT.jar com.microtomcat.HttpServer
```

```
mvn clean compile exec:java -Dexec.mainClass="com.microtomcat.Main"
```