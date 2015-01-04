NyARToolkit
===========

forked from NyARToolkit-4.1.1 (http://sourceforge.jp/projects/nyartoolkit/releases/).

- convert NyARToolkit Projects to Maven Project format.
- integrating the webcam-capture library(https://github.com/sarxos/webcam-capture) with NyARToolkit java3d utils(instead of JMF).  
The class for using webcam-capture library is contained in the following projects. 
 - NyARToolkit.utils.java3d.webcam-capture
 - NyARToolkit.utils.webcam-capture
 - NyARToolkit.utils.j2se
 - NyARToolkit.utils.java3d.RaspberryPI

Following projects are sample.  
 - NyARToolkit.sample.java3d.webcam-capture.
 - NyARToolkit.RaspberryPI.sample

## Setup (Windows)
1. download JOGL.  
http://sixwish.jp/Nyartoolkit/Java/section03/  
=> jogl-1.1.1a-windows-i586.zip  
copy gluegen-rt.jar, jogl.jar to ${java.home}/lib/ext  
copy gluegen-rt.dll, jogl.dll, jogl_awt.dll, jogl_cg.dll to ${java.home}/bin
2. download Java 3D and install.  
https://java3d.java.net/binary-builds.html
=> j3d-1_5_2-windows-i586.exe
and copy j3dcore.jar, vecmath.jar, j3dutils.jar to ${java.home}/lib/ext  

### Run sample app.
Run sample application using javafx-maven-plugin.
```
git clone https://github.com/si-ro/NyARToolkit.git
cd NyARToolkit/NyARToolkit.RaspberryPI.sample
mvn jfx:run
```

### (optional) Import project to Eclipse.
1. Run maven eclipse:eclipse goal to several maven projects. 
```
cd NyARToolkit
mvn eclipse:eclipse -f NyARToolkit.utils.webcam-capture\pom.xml
mvn eclipse:eclipse -f NyARToolkit.utils.j2se\pom.xml
mvn eclipse:eclipse -f NyARToolkit.utils.java3d.webcam-capture\pom.xml
mvn eclipse:eclipse -f NyARToolkit.utils.java3d.RaspberryPI\pom.xml
mvn eclipse:eclipse -f NyARToolkit.RaspberryPI.sample\pom.xml
```
or you can use NyARToolkit/setup.bat.  
2. Launch Eclipse and import as thease projects (import as existing projects).  
3. Convert thease projects to Maven Project in package Explorer.
