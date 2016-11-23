package org.bimserver.demoplugins.pathchecker;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

public class Display extends JFrame {

	private static final long serialVersionUID = -4323346123771019062L;
	private BufferedImage bufferedImage;

	public Display() {
		setTitle("Display");
		setSize(1000, 1010);
		setVisible(true);
	}
	
	public void setImage(BufferedImage bufferedImage) {
		this.bufferedImage = bufferedImage;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.drawImage(bufferedImage, 0, 0, this);
	}
}
