REM set PATH=C:\apache-maven-3.1.1\bin;%PATH%
REM mvn eclipse:configure-workspace -Declipse.workspace=D:\c3dev\eclipse\workspace
cd C:\Users\sakaguchi\git\NyARToolkit
call mvn eclipse:eclipse -f NyARToolkit.utils.webcam-capture\pom.xml
call mvn eclipse:eclipse -f NyARToolkit.utils.j2se\pom.xml
call mvn eclipse:eclipse -f NyARToolkit.utils.java3d.webcam-capture\pom.xml
call mvn eclipse:eclipse -f NyARToolkit.utils.java3d.RaspberryPI\pom.xml
call mvn eclipse:eclipse -f NyARToolkit.RapsberryPI.sample\pom.xml
