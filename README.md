NyARToolkit
===========

forked from NyARToolkit-4.1.1 (http://sourceforge.jp/projects/nyartoolkit/releases/).

- convert NyARToolkit Projects to Maven Project format.
- integrating the webcam-capture library(https://github.com/sarxos/webcam-capture) with NyARToolkit java3d utils(instead of JMF).  
The class for using webcam-capture library is contained in the following projects. 
 - NyARToolkit.utils.java3d.webcam-capture
 - NyARToolkit.utils.webcam-capture
 - Sample project is NyARToolkit.sample.java3d.webcam-capture.

## Setup (Windows)
1. download JOGL.  
http://sixwish.jp/Nyartoolkit/Java/section03/  
=> jogl-1.1.1a-windows-i586.zip  
copy gluegen-rt.jar, jogl.jar to ${java.home}/lib/ext  
copy gluegen-rt.dll, jogl.dll, jogl_awt.dll, jogl_cg.dll to ${java.home}/bin
2. download JMF and install.  
http://www.oracle.com/technetwork/java/javase/download-142937.html  
=> jmf-2_1_1e-windows-i586.exe
3. download Java 3D and install.  
https://java3d.java.net/binary-builds.html
=> j3d-1_5_2-windows-i586.exe



