@echo off
setlocal

rem === Set library paths ===
set CC_JAR=C:\Users\Eudald\Downloads\commons-compress-1.27.1-bin\commons-compress-1.27.1\commons-compress-1.27.1.jar
set IO_JAR=C:\Users\Eudald\Downloads\commons-io-2.20.0-bin\commons-io-2.20.0\commons-io-2.20.0.jar
set LANG_JAR=C:\Users\Eudald\Downloads\commons-lang3-3.18.0-bin\commons-lang3-3.18.0\commons-lang3-3.18.0.jar
set XZ_JAR=C:\Users\Eudald\Downloads\xz-java-1.10\build\jar\xz.jar

rem === Compile ===
echo Compiling sources...
javac -cp ".;%CC_JAR%;%IO_JAR%;%LANG_JAR%;%XZ_JAR%" ^
 DiskUsageAnalyzer\DiskUsageAnalyzer.java ^
 DiskUsageAnalyzer\utils\TarUtils.java ^
 DiskUsageAnalyzer\utils\Bzip2Utils.java ^
 DiskUsageAnalyzer\utils\XZUtils.java

if errorlevel 1 (
    echo Compilation failed!
    exit /b 1
)

rem === Run ===
echo Running DiskUsageAnalyzer...
java -cp ".;%CC_JAR%;%IO_JAR%;%LANG_JAR%;%XZ_JAR%" DiskUsageAnalyzer.DiskUsageAnalyzer

endlocal
