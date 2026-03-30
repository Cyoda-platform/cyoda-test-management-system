@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  java-client-template startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and JAVA_CLIENT_TEMPLATE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\java-client-template-1.0-SNAPSHOT-plain.jar;%APP_HOME%\lib\cloudevents-json-jackson-4.0.1.jar;%APP_HOME%\lib\spring-boot-starter-web-3.5.3.jar;%APP_HOME%\lib\spring-boot-starter-json-3.5.3.jar;%APP_HOME%\lib\spring-boot-starter-actuator-3.5.3.jar;%APP_HOME%\lib\spring-boot-actuator-autoconfigure-3.5.3.jar;%APP_HOME%\lib\springdoc-openapi-starter-webmvc-ui-2.8.3.jar;%APP_HOME%\lib\springdoc-openapi-starter-webmvc-api-2.8.3.jar;%APP_HOME%\lib\springdoc-openapi-starter-common-2.8.3.jar;%APP_HOME%\lib\swagger-core-jakarta-2.2.27.jar;%APP_HOME%\lib\jackson-datatype-jsr310-2.19.1.jar;%APP_HOME%\lib\jackson-datatype-jdk8-2.19.1.jar;%APP_HOME%\lib\jackson-module-parameter-names-2.19.1.jar;%APP_HOME%\lib\jackson-dataformat-yaml-2.19.1.jar;%APP_HOME%\lib\jackson-databind-2.19.1.jar;%APP_HOME%\lib\swagger-models-jakarta-2.2.27.jar;%APP_HOME%\lib\jackson-annotations-2.19.1.jar;%APP_HOME%\lib\grpc-all-1.73.0.jar;%APP_HOME%\lib\grpc-gcp-csm-observability-1.73.0.jar;%APP_HOME%\lib\opentelemetry-gcp-resources-1.43.0-alpha.jar;%APP_HOME%\lib\jackson-core-2.19.1.jar;%APP_HOME%\lib\jackson-module-kotlin-2.19.1.jar;%APP_HOME%\lib\kotlin-reflect-2.2.21.jar;%APP_HOME%\lib\grpc-okhttp-1.73.0.jar;%APP_HOME%\lib\okio-jvm-3.4.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-2.2.21.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-2.2.21.jar;%APP_HOME%\lib\kotlin-stdlib-2.2.21.jar;%APP_HOME%\lib\spring-boot-starter-oauth2-client-3.5.3.jar;%APP_HOME%\lib\spring-boot-starter-validation-3.5.3.jar;%APP_HOME%\lib\spring-boot-starter-3.5.3.jar;%APP_HOME%\lib\spring-data-commons-3.5.1.jar;%APP_HOME%\lib\grpc-xds-1.73.0.jar;%APP_HOME%\lib\grpc-alts-1.73.0.jar;%APP_HOME%\lib\grpc-grpclb-1.73.0.jar;%APP_HOME%\lib\grpc-rls-1.73.0.jar;%APP_HOME%\lib\grpc-services-1.73.0.jar;%APP_HOME%\lib\grpc-protobuf-1.73.0.jar;%APP_HOME%\lib\cloudevents-protobuf-4.0.1.jar;%APP_HOME%\lib\proto-google-common-protos-2.51.0.jar;%APP_HOME%\lib\protobuf-java-util-3.25.5.jar;%APP_HOME%\lib\protobuf-java-4.31.1.jar;%APP_HOME%\lib\grpc-testing-1.73.0.jar;%APP_HOME%\lib\grpc-inprocess-1.73.0.jar;%APP_HOME%\lib\grpc-netty-1.73.0.jar;%APP_HOME%\lib\grpc-opentelemetry-1.73.0.jar;%APP_HOME%\lib\grpc-servlet-1.73.0.jar;%APP_HOME%\lib\grpc-servlet-jakarta-1.73.0.jar;%APP_HOME%\lib\grpc-netty-shaded-1.73.0.jar;%APP_HOME%\lib\grpc-util-1.73.0.jar;%APP_HOME%\lib\grpc-core-1.73.0.jar;%APP_HOME%\lib\grpc-stub-1.73.0.jar;%APP_HOME%\lib\javax.annotation-api-1.3.2.jar;%APP_HOME%\lib\cloudevents-spring-4.0.1.jar;%APP_HOME%\lib\httpclient5-5.5.jar;%APP_HOME%\lib\java-uuid-generator-4.0.1.jar;%APP_HOME%\lib\caffeine-3.1.8.jar;%APP_HOME%\lib\uuid-3.2.jar;%APP_HOME%\lib\jcommander-1.82.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\spring-boot-starter-tomcat-3.5.3.jar;%APP_HOME%\lib\spring-webmvc-6.2.8.jar;%APP_HOME%\lib\spring-security-oauth2-client-6.5.1.jar;%APP_HOME%\lib\spring-security-oauth2-jose-6.5.1.jar;%APP_HOME%\lib\spring-security-oauth2-core-6.5.1.jar;%APP_HOME%\lib\spring-security-web-6.5.1.jar;%APP_HOME%\lib\spring-web-6.2.8.jar;%APP_HOME%\lib\spring-boot-autoconfigure-3.5.3.jar;%APP_HOME%\lib\spring-boot-actuator-3.5.3.jar;%APP_HOME%\lib\spring-boot-3.5.3.jar;%APP_HOME%\lib\spring-boot-starter-logging-3.5.3.jar;%APP_HOME%\lib\jakarta.annotation-api-2.1.1.jar;%APP_HOME%\lib\spring-security-config-6.5.1.jar;%APP_HOME%\lib\spring-security-core-6.5.1.jar;%APP_HOME%\lib\spring-context-6.2.8.jar;%APP_HOME%\lib\spring-aop-6.2.8.jar;%APP_HOME%\lib\spring-beans-6.2.8.jar;%APP_HOME%\lib\spring-expression-6.2.8.jar;%APP_HOME%\lib\spring-core-6.2.8.jar;%APP_HOME%\lib\snakeyaml-2.4.jar;%APP_HOME%\lib\tomcat-embed-el-10.1.42.jar;%APP_HOME%\lib\hibernate-validator-8.0.2.Final.jar;%APP_HOME%\lib\logback-classic-1.5.18.jar;%APP_HOME%\lib\log4j-to-slf4j-2.24.3.jar;%APP_HOME%\lib\jul-to-slf4j-2.0.17.jar;%APP_HOME%\lib\slf4j-api-2.0.17.jar;%APP_HOME%\lib\grpc-protobuf-lite-1.73.0.jar;%APP_HOME%\lib\google-auth-library-oauth2-http-1.24.1.jar;%APP_HOME%\lib\google-http-client-gson-1.44.2.jar;%APP_HOME%\lib\google-http-client-1.44.2.jar;%APP_HOME%\lib\opencensus-contrib-http-util-0.31.1.jar;%APP_HOME%\lib\opencensus-api-0.31.1.jar;%APP_HOME%\lib\grpc-context-1.73.0.jar;%APP_HOME%\lib\grpc-auth-1.73.0.jar;%APP_HOME%\lib\grpc-api-1.73.0.jar;%APP_HOME%\lib\guava-33.3.1-jre.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\gson-2.13.1.jar;%APP_HOME%\lib\annotations-4.1.1.4.jar;%APP_HOME%\lib\animal-sniffer-annotations-1.24.jar;%APP_HOME%\lib\error_prone_annotations-2.38.0.jar;%APP_HOME%\lib\perfmark-api-0.27.0.jar;%APP_HOME%\lib\cloudevents-core-4.0.1.jar;%APP_HOME%\lib\httpcore5-h2-5.3.4.jar;%APP_HOME%\lib\httpcore5-5.3.4.jar;%APP_HOME%\lib\swagger-ui-5.18.2.jar;%APP_HOME%\lib\webjars-locator-lite-1.1.0.jar;%APP_HOME%\lib\micrometer-jakarta9-1.15.1.jar;%APP_HOME%\lib\micrometer-core-1.15.1.jar;%APP_HOME%\lib\micrometer-observation-1.15.1.jar;%APP_HOME%\lib\checker-qual-3.43.0.jar;%APP_HOME%\lib\tomcat-embed-websocket-10.1.42.jar;%APP_HOME%\lib\tomcat-embed-core-10.1.42.jar;%APP_HOME%\lib\spring-jcl-6.2.8.jar;%APP_HOME%\lib\spring-security-crypto-6.5.1.jar;%APP_HOME%\lib\oauth2-oidc-sdk-9.43.6.jar;%APP_HOME%\lib\nimbus-jose-jwt-9.37.3.jar;%APP_HOME%\lib\jakarta.validation-api-3.0.2.jar;%APP_HOME%\lib\jboss-logging-3.6.1.Final.jar;%APP_HOME%\lib\classmate-1.7.0.jar;%APP_HOME%\lib\failureaccess-1.0.2.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\j2objc-annotations-3.0.0.jar;%APP_HOME%\lib\google-auth-library-credentials-1.24.1.jar;%APP_HOME%\lib\opentelemetry-sdk-extension-autoconfigure-1.49.0.jar;%APP_HOME%\lib\netty-codec-http2-4.1.122.Final.jar;%APP_HOME%\lib\netty-handler-proxy-4.1.122.Final.jar;%APP_HOME%\lib\netty-codec-http-4.1.122.Final.jar;%APP_HOME%\lib\netty-handler-4.1.122.Final.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.122.Final.jar;%APP_HOME%\lib\opentelemetry-sdk-extension-autoconfigure-spi-1.49.0.jar;%APP_HOME%\lib\opentelemetry-sdk-1.49.0.jar;%APP_HOME%\lib\opentelemetry-sdk-trace-1.49.0.jar;%APP_HOME%\lib\opentelemetry-sdk-metrics-1.49.0.jar;%APP_HOME%\lib\opentelemetry-sdk-logs-1.49.0.jar;%APP_HOME%\lib\opentelemetry-sdk-common-1.49.0.jar;%APP_HOME%\lib\opentelemetry-api-1.49.0.jar;%APP_HOME%\lib\auto-value-annotations-1.11.0.jar;%APP_HOME%\lib\junit-4.13.2.jar;%APP_HOME%\lib\re2j-1.8.jar;%APP_HOME%\lib\cloudevents-api-4.0.1.jar;%APP_HOME%\lib\jspecify-1.0.0.jar;%APP_HOME%\lib\micrometer-commons-1.15.1.jar;%APP_HOME%\lib\logback-core-1.5.18.jar;%APP_HOME%\lib\log4j-api-2.24.3.jar;%APP_HOME%\lib\jcip-annotations-1.0-1.jar;%APP_HOME%\lib\content-type-2.2.jar;%APP_HOME%\lib\json-smart-2.5.2.jar;%APP_HOME%\lib\lang-tag-1.7.jar;%APP_HOME%\lib\detector-resources-support-0.33.0.jar;%APP_HOME%\lib\opentelemetry-semconv-1.29.0-alpha.jar;%APP_HOME%\lib\netty-codec-socks-4.1.122.Final.jar;%APP_HOME%\lib\netty-codec-4.1.122.Final.jar;%APP_HOME%\lib\netty-transport-4.1.122.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.122.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.122.Final.jar;%APP_HOME%\lib\netty-common-4.1.122.Final.jar;%APP_HOME%\lib\opentelemetry-context-1.49.0.jar;%APP_HOME%\lib\hamcrest-core-3.0.jar;%APP_HOME%\lib\conscrypt-openjdk-uber-2.5.2.jar;%APP_HOME%\lib\HdrHistogram-2.2.2.jar;%APP_HOME%\lib\LatencyUtils-2.0.3.jar;%APP_HOME%\lib\accessors-smart-2.5.2.jar;%APP_HOME%\lib\hamcrest-3.0.jar;%APP_HOME%\lib\commons-lang3-3.17.0.jar;%APP_HOME%\lib\swagger-annotations-jakarta-2.2.27.jar;%APP_HOME%\lib\jakarta.xml.bind-api-4.0.2.jar;%APP_HOME%\lib\asm-9.7.1.jar;%APP_HOME%\lib\httpclient-4.5.14.jar;%APP_HOME%\lib\httpcore-4.4.16.jar;%APP_HOME%\lib\jakarta.activation-api-2.1.3.jar;%APP_HOME%\lib\commons-codec-1.18.0.jar


@rem Execute java-client-template
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %JAVA_CLIENT_TEMPLATE_OPTS%  -classpath "%CLASSPATH%" com.java_template.common.tool.WorkflowImportTool %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable JAVA_CLIENT_TEMPLATE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%JAVA_CLIENT_TEMPLATE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
