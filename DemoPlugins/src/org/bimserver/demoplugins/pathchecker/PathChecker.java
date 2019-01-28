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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.geometry.Matrix;
import org.bimserver.geometry.Vector;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcArbitraryClosedProfileDef;
import org.bimserver.models.ifc2x3tc1.IfcArbitraryProfileDefWithVoids;
import org.bimserver.models.ifc2x3tc1.IfcAxis2Placement3D;
import org.bimserver.models.ifc2x3tc1.IfcCartesianPoint;
import org.bimserver.models.ifc2x3tc1.IfcClassificationNotationSelect;
import org.bimserver.models.ifc2x3tc1.IfcClassificationReference;
import org.bimserver.models.ifc2x3tc1.IfcCompositeCurve;
import org.bimserver.models.ifc2x3tc1.IfcCompositeCurveSegment;
import org.bimserver.models.ifc2x3tc1.IfcCurve;
import org.bimserver.models.ifc2x3tc1.IfcExtrudedAreaSolid;
import org.bimserver.models.ifc2x3tc1.IfcPolyline;
import org.bimserver.models.ifc2x3tc1.IfcProductRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcProfileDef;
import org.bimserver.models.ifc2x3tc1.IfcRectangleProfileDef;
import org.bimserver.models.ifc2x3tc1.IfcRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcRepresentationItem;
import org.bimserver.models.ifc2x3tc1.IfcShapeRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.utils.IfcUtils;
import org.eclipse.emf.common.util.EList;

public class PathChecker {

	private PathCheckerSettings settings;
	private float multiplierMillimeters = 1;

	public PathChecker(PathCheckerSettings settings) {
		this.settings = settings;
	}

	private SpaceCheckResult checkSpace(IfcSpace ifcSpace) {
		SpaceCheckResult spaceCheckResult = new SpaceCheckResult();
		spaceCheckResult.setIfcSpace(ifcSpace);
		GeometryInfo geometry = ifcSpace.getGeometry();
		if (geometry != null) {
			double height = geometry.getBounds().getMax().getZ() - geometry.getBounds().getMin().getZ();

			spaceCheckResult.setWidthMustBe(settings.getMinWidth());
			spaceCheckResult.setHeightMustBe(settings.getMinHeadRoomMM());
			spaceCheckResult.setHeight((float) height * multiplierMillimeters);

			IfcProductRepresentation representation = ifcSpace.getRepresentation();
			for (IfcRepresentation ifcRepresentation : representation.getRepresentations()) {
				if (ifcRepresentation instanceof IfcShapeRepresentation) {
					IfcShapeRepresentation ifcShapeRepresentation = (IfcShapeRepresentation) ifcRepresentation;
					for (IfcRepresentationItem ifcRepresentationItem : ifcShapeRepresentation.getItems()) {
						if (ifcRepresentationItem instanceof IfcExtrudedAreaSolid) {
							IfcExtrudedAreaSolid ifcExtrudedAreaSolid = (IfcExtrudedAreaSolid) ifcRepresentationItem;
							IfcAxis2Placement3D position = ifcExtrudedAreaSolid.getPosition();
//							IfcCartesianPoint location = position.getLocation();
							
//								1 0 0 0 <- 3de argument
//								0 1 0 0 <- cross product van 2 en 3 (levert ortogonale vector op)
//								0 0 1 0 <- 2st argument
//								5 0 0 1 <- 1st argument
							
							double[] matrix = Matrix.identity();
							if (position.getAxis() != null && position.getRefDirection() != null) {
								double[] cross = Vector.crossProduct(new double[]{position.getAxis().getDirectionRatios().get(0), position.getAxis().getDirectionRatios().get(1), position.getAxis().getDirectionRatios().get(2), 1}, new double[]{position.getRefDirection().getDirectionRatios().get(0), position.getRefDirection().getDirectionRatios().get(1), position.getRefDirection().getDirectionRatios().get(2), 1});
								matrix = new double[]{
									position.getRefDirection().getDirectionRatios().get(0), position.getRefDirection().getDirectionRatios().get(1), position.getRefDirection().getDirectionRatios().get(2), 0,
									cross[0], cross[1], cross[2], 0,
									position.getAxis().getDirectionRatios().get(0), position.getAxis().getDirectionRatios().get(1), position.getAxis().getDirectionRatios().get(2), 0,
									position.getLocation().getCoordinates().get(0), position.getLocation().getCoordinates().get(1), position.getLocation().getCoordinates().get(2), 1
								};
							} else if (position.getLocation() != null) {
								matrix = new double[]{
									1, 0, 0, 0,
									0, 1, 0, 0,
									0, 0, 1, 0,
									position.getLocation().getCoordinates().get(0), position.getLocation().getCoordinates().get(1), 0, 1
								};
							}
							
							IfcProfileDef ifcProfileDef = ifcExtrudedAreaSolid.getSweptArea();
							spaceCheckResult.setExtraDescription(ifcProfileDef.eClass().getName());
							if (ifcProfileDef instanceof IfcArbitraryProfileDefWithVoids) {
								IfcArbitraryProfileDefWithVoids ifcArbitraryProfileDefWithVoids = (IfcArbitraryProfileDefWithVoids) ifcProfileDef;
								IfcCurve outerCurve = ifcArbitraryProfileDefWithVoids.getOuterCurve();
								Path2D outerPath = null;
								if (outerCurve instanceof IfcPolyline) {
									outerPath = curveToPath(matrix, outerCurve);
								} else {
									System.out.println("Unimplemented: " + outerCurve);
								}

								Area area = new Area(outerPath);
								for (IfcCurve innerCurve : ifcArbitraryProfileDefWithVoids.getInnerCurves()) {
									Path2D.Float innerPath = curveToPath(matrix, innerCurve);
									area.subtract(new Area(innerPath));
								}

								checkArea(spaceCheckResult, area);
							} else if (ifcProfileDef instanceof IfcArbitraryClosedProfileDef) {
								IfcArbitraryClosedProfileDef ifcArbitraryClosedProfileDef = (IfcArbitraryClosedProfileDef) ifcProfileDef;
								Path2D.Float path2d = new Path2D.Float();
								IfcCurve outerCurve = ifcArbitraryClosedProfileDef.getOuterCurve();
								boolean first = true;
								if (outerCurve instanceof IfcPolyline) {
									IfcPolyline ifcPolyline = (IfcPolyline) outerCurve;

									double[] res = new double[4];

									int i=0;
									for (IfcCartesianPoint cartesianPoint : ifcPolyline.getPoints()) {
										EList<Double> coords = cartesianPoint.getCoordinates();

										Matrix.multiplyMV(res, 0, matrix, 0, new double[]{coords.get(0), coords.get(1), 0, 1}, 0);
										
										if (first) {
											path2d.moveTo(res[0] * multiplierMillimeters, res[1] * multiplierMillimeters);
											first = false;
										} else {
											if (i > 1) {
												
											}
											path2d.lineTo(res[0] * multiplierMillimeters, res[1] * multiplierMillimeters);
										}
										i++;
									}
									path2d.closePath();

									checkPath(spaceCheckResult, path2d);
								} else if (outerCurve instanceof IfcCompositeCurve) {
									IfcCompositeCurve ifcCompositeCurve = (IfcCompositeCurve)outerCurve;

									for (IfcCompositeCurveSegment ifcCompositeCurveSegment : ifcCompositeCurve.getSegments()) {
										IfcCurve curve = ifcCompositeCurveSegment.getParentCurve();
										if (curve instanceof IfcPolyline) {
											IfcPolyline ifcPolyline = (IfcPolyline)curve;
											double[] res = new double[4];
											for (IfcCartesianPoint cartesianPoint : ifcPolyline.getPoints()) {
												EList<Double> coords = cartesianPoint.getCoordinates();

												Matrix.multiplyMV(res, 0, matrix, 0, new double[]{coords.get(0), coords.get(1), 0, 1}, 0);
												
												if (first) {
													path2d.moveTo(res[0] * multiplierMillimeters, res[1] * multiplierMillimeters);
													first = false;
												} else {
													path2d.lineTo(res[0] * multiplierMillimeters, res[1] * multiplierMillimeters);
												}
											}
										} else {
											System.out.println("Unimplemented: " + curve);
										}
									}
									path2d.closePath();
									checkPath(spaceCheckResult, path2d);
								}
							} else if (ifcProfileDef instanceof IfcRectangleProfileDef) {
								IfcRectangleProfileDef ifcRectangleProfileDef = (IfcRectangleProfileDef) ifcProfileDef;

								double[] min = new double[]{ifcRectangleProfileDef.getPosition().getLocation().getCoordinates().get(0) - ifcRectangleProfileDef.getXDim() / 2, ifcRectangleProfileDef.getPosition().getLocation().getCoordinates().get(1) - ifcRectangleProfileDef.getYDim() / 2, 0, 1};
								double[] max = new double[]{ifcRectangleProfileDef.getPosition().getLocation().getCoordinates().get(0) + ifcRectangleProfileDef.getXDim() / 2, ifcRectangleProfileDef.getPosition().getLocation().getCoordinates().get(1) + ifcRectangleProfileDef.getYDim() / 2, 0, 1};

								Cube cube = new Cube(min, max);
								cube.transform(matrix);
								double[] transformedMin = cube.getMin();
								double[] transformedMax = cube.getMax();
								
								Path2D.Float path2d = new Path2D.Float();
								path2d.moveTo(transformedMin[0] * multiplierMillimeters, transformedMin[1] * multiplierMillimeters);
								path2d.lineTo(transformedMax[0] * multiplierMillimeters, transformedMin[1] * multiplierMillimeters);
								path2d.lineTo(transformedMax[0] * multiplierMillimeters, transformedMax[1] * multiplierMillimeters);
								path2d.lineTo(transformedMin[0] * multiplierMillimeters, transformedMax[1] * multiplierMillimeters);
								path2d.lineTo(transformedMin[0] * multiplierMillimeters, transformedMin[1] * multiplierMillimeters);
								
								path2d.closePath();
								
								checkPath(spaceCheckResult, path2d);
							} else {
								System.out.println("Unimplemented: " + ifcProfileDef);
							}
						}
					}
				}
			}
		}
		return spaceCheckResult;
	}

	private Path2D.Float curveToPath(double[] matrix, IfcCurve outerCurve) {
		Path2D.Float path2d = new Path2D.Float();
		IfcPolyline ifcPolyline = (IfcPolyline) outerCurve;
		IfcCartesianPoint first = ifcPolyline.getPoints().get(0);
		double[] res = new double[4];

		Matrix.multiplyMV(res, 0, matrix, 0, new double[]{first.getCoordinates().get(0), first.getCoordinates().get(1), 0, 1}, 0);
		path2d.moveTo(res[0] * multiplierMillimeters, res[1] * multiplierMillimeters);
		
		for (IfcCartesianPoint cartesianPoint : ifcPolyline.getPoints()) {
			EList<Double> coords = cartesianPoint.getCoordinates();
			Matrix.multiplyMV(res, 0, matrix, 0, new double[]{coords.get(0), coords.get(1), 0, 1}, 0);
			path2d.lineTo(res[0] * multiplierMillimeters, res[1] * multiplierMillimeters);
		}
		path2d.closePath();
		return path2d;
	}

	public SpaceCheckResult checkPath(SpaceCheckResult spaceCheckResult, Path2D.Float path) {
		return checkArea(spaceCheckResult, new Area(path));
	}	
	
	public SpaceCheckResult checkArea(SpaceCheckResult spaceCheckResult, Area area) {
		BufferedImage bufferedImage = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D graphics2d = (Graphics2D)bufferedImage.getGraphics();
		graphics2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		
		graphics2d.setColor(Color.WHITE);
		graphics2d.fillRect(0, 0, 1000, 1000);

		double scaleX = 800 / area.getBounds().getWidth();
		double scaleY = 800 / area.getBounds().getHeight();
		double scale = Math.min(scaleX, scaleY);

		AffineTransform affineTransform = new AffineTransform();
		affineTransform.translate(500, 500);
		affineTransform.scale(scale, scale);
		affineTransform.translate(-area.getBounds().getCenterX(), -area.getBounds2D().getCenterY());
		
		Area areaBeforeTranformation = (Area) area.clone();
		Area areaResult = (Area) area.clone();
		Rectangle boundsBeforeTransformation = areaBeforeTranformation.getBounds();
		area.transform(affineTransform);
		
		graphics2d.setStroke(new BasicStroke(3f));
		graphics2d.setColor(Color.RED);
		graphics2d.fill(area);

		PathIterator pathIterator = areaBeforeTranformation.getPathIterator(null);
		float[] last = new float[6];
		float[] first = new float[6];
		while (!pathIterator.isDone()) {
			float[] coords = new float[6];
			int type = pathIterator.currentSegment(coords);
			
			if (type == PathIterator.SEG_MOVETO) {
				pathIterator.currentSegment(coords);
				first = Arrays.copyOf(coords, coords.length);
			} else if (type == PathIterator.SEG_LINETO) {
				Point2D.Float p1 = new Point2D.Float(coords[0], coords[1]);
				Point2D.Float p2 = new Point2D.Float(last[0], last[1]);
				
				test(spaceCheckResult, graphics2d, scale, affineTransform, areaBeforeTranformation, boundsBeforeTransformation, areaResult, p1, p2);
			} else if (type == PathIterator.SEG_CLOSE) {
				Point2D.Float p1 = new Point2D.Float(first[0], first[1]);
				Point2D.Float p2 = new Point2D.Float(last[0], last[1]);

				test(spaceCheckResult, graphics2d, scale, affineTransform, areaBeforeTranformation, boundsBeforeTransformation, areaResult, p1, p2);
			}
			pathIterator.next();
			last = Arrays.copyOf(coords, coords.length);
		}
		// This is a hack, the 2D CSG should have removed all curves when an object is valid
		double bbArea = areaResult.getBounds2D().getWidth() * areaResult.getBounds2D().getHeight();
		if (!areaResult.isEmpty() || bbArea > 1) {
			spaceCheckResult.setValid(false);
		} else {
			spaceCheckResult.setValid(true);
		}
		
		graphics2d.setStroke(new BasicStroke(3f));
		graphics2d.setColor(Color.BLACK);
//		areaResult.transform(affineTransform);
//		graphics2d.draw(areaResult);
		graphics2d.draw(area);

		spaceCheckResult.setImage(bufferedImage);
		return spaceCheckResult;
	}

	private void test(SpaceCheckResult spaceCheckResult, Graphics2D graphics2d, double scale, AffineTransform affineTransform, Area areaBeforeTranformation, Rectangle boundsBeforeTransformation, Area areaResult, Point2D.Float p1, Point2D.Float p2) {
		Measure measure = new Measure(p1, p2, areaBeforeTranformation, boundsBeforeTransformation, (float)scale);

		float lineLength = (float) Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
		
		for (float i=0; i<lineLength; i+=10f) {
			float x = p1.x + ((p2.x - p1.x) * (i / lineLength));
			float y = p1.y + ((p2.y - p1.y) * (i / lineLength));
			for (Rectangle2D.Float rectangle : getAll(new Rectangle2D.Float(x, y, settings.getMinWidth(), settings.getMinWidth()))) {
				if (areaBeforeTranformation.contains(rectangle)) {
					float xSign = rectangle.getX() < x ? -1 : 1;
					float ySign = rectangle.getY() < y ? -1 : 1;
					
					graphics2d.setColor(Color.GREEN);
					Area area2 = new Area(new Rectangle2D.Float(x - 0.01f, y - 0.01f, settings.getMinWidth() + 0.02f, settings.getMinWidth() + 0.02f));
					areaResult.subtract(area2);
					Area area2ToFill = new Area(rectangle);
					area2ToFill.transform(affineTransform);
					graphics2d.fill(area2ToFill);
					
					// To make sure the big blocks in the middle are also filled, we will now fill the whole row
					float angle = 0;
					if (p1.getX() == p2.getX()) {
						angle = (float) (0.5 * Math.PI);
					} else if (p1.getY() == p2.getY()) {
						angle = 0;
					} else {
						angle = (float) Math.atan((p1.getY() - p2.getY()) / (p1.getX() - p2.getX()));
					}
					float rectangleX = x;
					float rectangleY = y;
					while (rectangleX <= areaBeforeTranformation.getBounds2D().getMaxX() && rectangleX >= areaBeforeTranformation.getBounds2D().getMinX() && rectangleY <= areaBeforeTranformation.getBounds2D().getMaxY() && rectangleY >= areaBeforeTranformation.getBounds2D().getMinY()) {
						rectangleX += xSign * Math.sin(angle) * settings.getMinWidth();
						rectangleY += ySign * Math.cos(angle) * settings.getMinWidth();
						Rectangle2D newRect = new Rectangle2D.Float(rectangleX, rectangleY, settings.getMinWidth(), settings.getMinWidth());
						if (areaBeforeTranformation.contains(newRect)) {
							graphics2d.setColor(Color.GREEN);
							Area areaToSubtract = new Area(new Rectangle2D.Float(rectangleX - 0.01f, rectangleY - 0.01f, settings.getMinWidth() + 0.02f, settings.getMinWidth() + 0.02f));
							areaResult.subtract(areaToSubtract);
							Area areadToFill = new Area(newRect);
							areadToFill.transform(affineTransform);
							graphics2d.fill(areadToFill);
						}
					}
				}
			}
		}
		
		spaceCheckResult.setSmallestWidth(measure.getLength());
		if (measure.getLength() >= settings.getMinWidth()) {
			measure.setColor(Color.BLACK);
		} else {
			measure.setColor(Color.RED);
		}
		measure.draw(graphics2d);
	}
	
	private Set<Rectangle2D.Float> getAll(Rectangle2D.Float input) {
		Set<Rectangle2D.Float> result = new HashSet<>();
		result.add(new Rectangle2D.Float(input.x, input.y, input.width, input.height));
		result.add(new Rectangle2D.Float(input.x, input.y - input.height, input.width, input.height));
		result.add(new Rectangle2D.Float(input.x - input.width, input.y, input.width, input.height));
		result.add(new Rectangle2D.Float(input.x - input.width, input.y - input.height, input.width, input.height));
		return result;
	}

	public Set<SpaceCheckResult> check(IfcModelInterface model) {
		multiplierMillimeters = IfcUtils.getLengthUnitPrefix(model) * 1000f;
		Set<SpaceCheckResult> results = Collections.synchronizedSet(new HashSet<>());

		List<IfcSpace> spaces = model.getAll(IfcSpace.class);
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(8, 8, 1, TimeUnit.HOURS, new ArrayBlockingQueue<>(spaces.size()));
		
		for (IfcSpace ifcSpace : spaces) {
			boolean check = false;
			List<IfcClassificationNotationSelect> classifications = IfcUtils.getClassifications(ifcSpace, model);
			for (IfcClassificationNotationSelect ifcClassificationNotationSelect : classifications) {
				if (ifcClassificationNotationSelect instanceof IfcClassificationReference) {
					IfcClassificationReference ifcClassificationReference = (IfcClassificationReference)ifcClassificationNotationSelect;
					if (ifcClassificationReference.getItemReference().equals("13-25 00 00")) {
						check = true;
						break;
					}
				}
			}
			if (IfcUtils.hasProperty(ifcSpace, "HandicapAccessible")) {
				check = true;
			}
			if (IfcUtils.hasProperty(ifcSpace, "CirculationZoneName")) {
				if (IfcUtils.getStringProperty(ifcSpace, "CirculationZoneName").contains("Circulation Zone")) {
					check = true;
				}
			}
			if (check) {
				threadPoolExecutor.submit(new Runnable(){
					@Override
					public void run() {
						SpaceCheckResult spaceCheckResult = checkSpace(ifcSpace);
						results.add(spaceCheckResult);
					}});
			}
		}
		
		threadPoolExecutor.shutdown();
		try {
			threadPoolExecutor.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return results;
	}
}