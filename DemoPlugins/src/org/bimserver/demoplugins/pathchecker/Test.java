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

import java.awt.geom.Path2D;

public class Test {
	public static void main(String[] args) {
		new Test().start();
	}
	
	private void start() {
		PathCheckerSettings settings = new PathCheckerSettings();
		settings.setMinWidth(2);
		PathChecker pathChecker = new PathChecker(settings);
		SpaceCheckResult spaceCheckResult = pathChecker.checkPath(new SpaceCheckResult(), createExample1());
		new Display().setImage(spaceCheckResult.getImage());

		spaceCheckResult = pathChecker.checkPath(new SpaceCheckResult(), createExample2());
		new Display().setImage(spaceCheckResult.getImage());

		spaceCheckResult = pathChecker.checkPath(new SpaceCheckResult(), createExample3());
		new Display().setImage(spaceCheckResult.getImage());
	}

	private Path2D.Float createExample1() {
		Path2D.Float path2d = new Path2D.Float();
		path2d.moveTo(0, 0);
		
		path2d.lineTo(4, 0);
		path2d.lineTo(4, 4);
		path2d.lineTo(0, 4);
		path2d.lineTo(0, 0);
		
		path2d.closePath();
		return path2d;
	}
	
	private Path2D.Float createExample2() {
		Path2D.Float path2d = new Path2D.Float();
		path2d.moveTo(0, 0);
		
		path2d.lineTo(6, 0);
		path2d.lineTo(6, 1);
		path2d.lineTo(0, 1);
		path2d.lineTo(0, 0);
		
		path2d.closePath();
		return path2d;
	}
	
	private Path2D.Float createExample3() {
		Path2D.Float path2d = new Path2D.Float();
		path2d.moveTo(0, 0);
		
		path2d.lineTo(6, 0);
		path2d.lineTo(6, 1);
		path2d.lineTo(3, 1);
		path2d.lineTo(3, 3);
		path2d.lineTo(0, 3);
		path2d.lineTo(0, 0);
		
		path2d.closePath();
		return path2d;
	}
}
