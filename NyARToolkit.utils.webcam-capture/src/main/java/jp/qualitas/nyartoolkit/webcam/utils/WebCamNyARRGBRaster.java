package jp.qualitas.nyartoolkit.webcam.utils;

import java.awt.image.BufferedImage;

import jp.nyatla.nyartoolkit.core.NyARException;
import jp.nyatla.nyartoolkit.utils.j2se.NyARBufferedImageRaster;

public class WebCamNyARRGBRaster extends NyARBufferedImageRaster {

	public WebCamNyARRGBRaster(BufferedImage i_img) throws NyARException {
		super(i_img);
	}
}
