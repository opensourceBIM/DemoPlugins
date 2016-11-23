package org.bimserver.demoplugins.pathchecker;

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
