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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.awt.geom.Rectangle2D;

public class Measure {

	private static final float DIST_TO_ADD = 40;
	private static final float DIST_STICK_OUT = 60;
	private static final Font font = new Font("Arial", Font.BOLD, 16);
	private Color color;
	private float length;
	private Float p1;
	private Float p2;
	private Point2D.Float p3;
	private Point2D.Float p4;
	private Point2D.Float p5;
	private Point2D.Float p6;
	private Rectangle2D bounds;
	private float scale;
//	private Point2D.Float p7;
//	private Point2D.Float p8;
//	private Point2D.Float p9;
//	private Point2D.Float p10;

	public Measure(Point2D.Float p1, Point2D.Float p2, Area area, Rectangle2D bounds, float scale) {
		this.p1 = p1;
		this.p2 = p2;
		this.bounds = bounds;
		this.scale = scale;
		float angle = 0;
		length = (float) Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
		if (p1.getX() == p2.getX()) {
			angle = (float) (0.5 * Math.PI);
		} else if (p1.getY() == p2.getY()) {
			angle = 0;
		} else {
			angle = (float) Math.atan((p1.getY() - p2.getY()) / (p1.getX() - p2.getX()));
		}
		float addX = (float) (Math.sin(angle) * (DIST_TO_ADD / scale));
		float addY = (float) (Math.cos(angle) * (DIST_TO_ADD / scale));
		float sign = +1;
		for (float i=0.1f; i<1f; i += 0.1) {
			if (area.contains(new Point2D.Float((float) ((p1.getX() + (addX * i) + p2.getX() + (addX * i)) / 2), (float) (p1.getY() + (addY * i) + p2.getY() + (addY * i)) / 2))) {
				sign = -1;
			}
		}
		p3 = new Point2D.Float((float)(p1.getX() + addX * sign), (float)(p1.getY() + addY * sign));
		p4 = new Point2D.Float((float)(p2.getX() + addX * sign), (float)(p2.getY() + addY * sign));
		p5 = new Point2D.Float((float)(p1.getX() + Math.sin(angle) * (DIST_STICK_OUT / scale) * sign), (float)(p1.getY() + Math.cos(angle) * (DIST_STICK_OUT / scale) * sign));
		p6 = new Point2D.Float((float)(p2.getX() + Math.sin(angle) * (DIST_STICK_OUT / scale) * sign), (float)(p2.getY() + Math.cos(angle) * (DIST_STICK_OUT / scale) * sign));
//		p7 = new Point2D.Float((float)(p3.getX() + (20 / scale) + Math.sin(angle) * (30 / scale) * sign), (float)(p1.getY() + Math.cos(angle) * (50 / scale) * sign));
//		p8 = new Point2D.Float((float)(p3.getX() + (20 / scale) + Math.sin(angle) * (30 / scale) * sign), (float)(p1.getY() + Math.cos(angle) * (30 / scale) * sign));
//		p9 = new Point2D.Float((float)(p4.getX() - (20 / scale) + Math.sin(angle) * (30 / scale) * sign), (float)(p2.getY() + Math.cos(angle) * (50 / scale) * sign));
//		p10 = new Point2D.Float((float)(p4.getX() - (20 / scale) + Math.sin(angle) * (30 / scale) * sign), (float)(p2.getY() + Math.cos(angle) * (30 / scale) * sign));
	}

	private Point2D convertToScreenCoordinate(Point2D p1) {
		float newScreenX = 500 + (float) ((p1.getX() - bounds.getCenterX()) * scale);
		float newScreenY = 500 + (float) ((p1.getY() - bounds.getCenterY()) * scale);
		return new Point2D.Float(newScreenX, newScreenY);
	}
	
	public void draw(Graphics2D graphics2d) {
		graphics2d.setFont(font);
		FontMetrics metrics = graphics2d.getFontMetrics(font);
		graphics2d.setColor(Color.GRAY);
		graphics2d.setStroke(new BasicStroke(1f));
		
		Point2D p1Converted = convertToScreenCoordinate(p1);
		Point2D p2Converted = convertToScreenCoordinate(p2);
		Point2D p3Converted = convertToScreenCoordinate(p3);
		Point2D p4Converted = convertToScreenCoordinate(p4);
		Point2D p5Converted = convertToScreenCoordinate(p5);
		Point2D p6Converted = convertToScreenCoordinate(p6);
//		Point2D p7Converted = convertToScreenCoordinate(p7);
//		Point2D p8Converted = convertToScreenCoordinate(p8);
//		Point2D p9Converted = convertToScreenCoordinate(p9);
//		Point2D p10Converted = convertToScreenCoordinate(p10);

		graphics2d.draw(new Line2D.Float(p3Converted, p4Converted));
		graphics2d.draw(new Line2D.Float(p1Converted, p5Converted));
		graphics2d.draw(new Line2D.Float(p2Converted, p6Converted));
//		graphics2d.draw(new Line2D.Float(p3Converted, p7Converted));
//		graphics2d.draw(new Line2D.Float(p3Converted, p8Converted));
//		graphics2d.draw(new Line2D.Float(p4Converted, p9Converted));
//		graphics2d.draw(new Line2D.Float(p4Converted, p10Converted));
		
		String str = "" + (int)length;
		Rectangle2D stringBounds = metrics.getStringBounds(str, graphics2d);
		float textX = (float)(-stringBounds.getCenterX() + p3Converted.getX() + (p4Converted.getX() - p3Converted.getX()) / 2);
		float textY = (float)(-stringBounds.getCenterY() + p3Converted.getY() + (p4Converted.getY() - p3Converted.getY()) / 2);
		graphics2d.setColor(Color.WHITE);
		graphics2d.fillRect((int)(textX), (int)(textY) - (int)stringBounds.getHeight() + metrics.getMaxDescent(), (int)stringBounds.getWidth(), (int)stringBounds.getHeight());
		graphics2d.setColor(color);
		graphics2d.drawString(str, textX, textY);
	}
	
	public void setColor(Color color) {
		this.color = color;
	}

	public float getLength() {
		return length;
	}
}
