package org.bimserver.demoplugins.pathchecker;

/******************************************************************************
 * Copyright (C) 2009-2019  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see {@literal<http://www.gnu.org/licenses/>}.
 *****************************************************************************/

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
